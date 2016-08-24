package org.keywords4bytecodes.firstclass;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang3.tuple.Pair;
import org.keywords4bytecodes.firstclass.extract.BytecodeData;
import org.keywords4bytecodes.firstclass.extract.BytecodeToSequence;

public class Experiment {

    private FirstClassSystem system;
    private TermVocabulary vocab;

    private List<Pair<String, BytecodeData.MethodData>> trainData;
    private List<Pair<String, BytecodeData.MethodData>> testData;

    public Experiment(FirstClassSystem system, TermVocabulary vocab) {
        this.system = system;
        this.vocab = vocab;

        this.trainData = new ArrayList<>();
        this.testData = new ArrayList<>();
    }

    public static enum DataType {
        TRAIN, TEST, BOTH
    }

    public void addData(TrainData trainData, DataType _type) {
        addData(trainData.data(), _type);
    }

    public void addData(List<Pair<String, BytecodeData.MethodData>> trainData, DataType _type) {
        if (_type == DataType.TRAIN || _type == DataType.BOTH)
            this.trainData.addAll(trainData);
        if (_type == DataType.TEST || _type == DataType.BOTH)
            this.testData.addAll(trainData);
    }

    public void addData(String term, BytecodeData.MethodData data, DataType _type) {
        if (_type == DataType.TRAIN || _type == DataType.BOTH)
            this.trainData.add(Pair.of(term, data));
        if (_type == DataType.TEST || _type == DataType.BOTH)
            this.testData.add(Pair.of(term, data));
    }

    public void addData(File f, DataType _type) throws IOException {
        addData(new TrainData(loadFile(f), vocab), _type);
    }

    public List<BytecodeData> loadFile(File f) throws IOException {
        if (f.isDirectory())
            return loadFolder(f);
        else if (f.getName().endsWith(".jar"))
            return loadJar(f);
        else if (f.getName().endsWith(".class"))
            return loadClass(f);

        return Collections.emptyList();
    }

    private List<BytecodeData> loadFolder(File folder) throws IOException {
        List<BytecodeData> all = new ArrayList<>();

        for (File f : folder.listFiles())
            if (!f.getName().startsWith("."))
                all.addAll(loadFile(f));

        return all;
    }

    private List<BytecodeData> loadJar(File f) throws IOException {
        List<BytecodeData> all = new ArrayList<>();
        ZipInputStream zip = new ZipInputStream(new FileInputStream(f));

        ZipEntry e = zip.getNextEntry();

        while (e != null) {
            if (e.getName().endsWith(".class")) {
                byte[] bytes = new byte[(int) e.getSize()];
                int off = 0;
                while (off < bytes.length) {
                    int read = zip.read(bytes, off, bytes.length - off);
                    if (read < 0)
                        off = bytes.length;
                    else
                        off += read;
                }

                all.addAll(BytecodeToSequence.extract(bytes));
            }

            e = zip.getNextEntry();
        }
        zip.close();

        return all;
    }

    private List<BytecodeData> loadClass(File f) throws IOException {
        return BytecodeToSequence.extract(f);
    }

    public void addObfuscatedData(File clearFile, File obfuscatedFile, DataType _type) throws IOException {
        addData(new TrainData(loadObfuscatedFiles(clearFile, obfuscatedFile), vocab), _type);
    }

    public List<BytecodeData> loadObfuscatedFiles(File clearFile, File obfuscatedFile) throws IOException {
        List<BytecodeData> clearData = null;
        List<BytecodeData> obfuscatedData = null;
        if (clearFile.isDirectory() || clearFile.isDirectory()) {
            if (!(clearFile.isDirectory() && clearFile.isDirectory()))
                throw new IllegalArgumentException("Both files should be directories: " + clearFile.getPath() + " and "
                        + obfuscatedFile.getPath());
            clearData = loadFolder(clearFile);
            obfuscatedData = loadFolder(clearFile);
        } else if (clearFile.getName().endsWith(".jar") && clearFile.getName().endsWith(".jar")) {
            clearData = loadJar(clearFile);
            obfuscatedData = loadJar(clearFile);
        } else if (clearFile.getName().endsWith(".class") && clearFile.getName().endsWith(".class")) {
            clearData = loadClass(clearFile);
            obfuscatedData = loadClass(clearFile);
        }

        if (clearData == null)
            return Collections.emptyList();

        // dovetail the clear data
        Map<String, BytecodeData> classNameToObfuscated = new HashMap<>();
        for (BytecodeData obfuscatedClass : obfuscatedData)
            classNameToObfuscated.put(obfuscatedClass.getClassName(), obfuscatedClass);

        for (BytecodeData clearClass : clearData) {
            BytecodeData obfuscated = classNameToObfuscated.get(clearClass.getClassName());
            if (obfuscated != null) {
                List<BytecodeData.MethodData> obMethods = obfuscated.getMethods();
                if (obMethods.size() != clearClass.getMethods().size())
                    System.err.println("loadObfuscatedFiles: skipping " + clearClass.getClassName()
                            + " length mismatch, clear: " + clearClass.getMethods().size() + " obfuscated: "
                            + obMethods.size());
                for (int i = 0; i < obMethods.size(); i++)
                    clearClass.getMethods().get(i).setExtOpcodeSeq(obMethods.get(i).getExtOpcodeSeq());
            }
        }

        return clearData;
    }

    public void train() {

    }

    public Results test() {
        return null;
    }

    public Results crossValidate(int folds) {
        return null;
    }

    public static class Results {
        Map<String, Table> termTable;

        public static final String TOTAL = "TOTAL";

        public Table totals() {
            return termTable.get(TOTAL);
        }
    }

    public static class Table {
        int tp, fp, fn, tn;

        public Table(int tp, int fp, int fn, int tn) {
            this.tp = tp;
            this.fp = fp;
            this.fn = fn;
            this.tn = tn;
        }

        public double precision() {
            return 0.0; // TODO
        }

        public double recall() {
            return 0.0; // TODO
        }

        public double f1() {
            return 0.0; // TODO
        }
    }

}
