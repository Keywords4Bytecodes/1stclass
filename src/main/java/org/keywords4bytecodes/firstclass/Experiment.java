package org.keywords4bytecodes.firstclass;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
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

    public List<Pair<String, BytecodeData.MethodData>> getTrainData() {
        return trainData;
    }

    public List<Pair<String, BytecodeData.MethodData>> getTestData() {
        return testData;
    }

    public void addData(File f, DataType _type) throws IOException {
        addData(new TrainData(loadFile(f), vocab), _type);
    }

    public static List<BytecodeData> loadFile(File f) throws IOException {
        if (f.isDirectory())
            return loadFolder(f);
        else if (f.getName().endsWith(".jar"))
            return loadJar(f);
        else if (f.getName().endsWith(".class"))
            return loadClass(f);

        return Collections.emptyList();
    }

    public static List<BytecodeData> loadFolder(File folder) throws IOException {
        List<BytecodeData> all = new ArrayList<>();

        for (File f : folder.listFiles())
            if (!f.getName().startsWith("."))
                all.addAll(loadFile(f));

        return all;
    }

    public static List<BytecodeData> loadJar(File f) throws IOException {
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

    public static List<BytecodeData> loadClass(File f) throws IOException {
        return BytecodeToSequence.extract(f);
    }

    public void addObfuscatedData(File clearFile, File obfuscatedFile, DataType _type) throws IOException {
        addData(new TrainData(loadObfuscatedFiles(clearFile, obfuscatedFile), vocab), _type);
    }

    public static List<BytecodeData> loadObfuscatedFiles(File clearFile, File obfuscatedFile) throws IOException {
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
        try {
            system.train(TrainData.from(trainData), vocab);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Results test() {

        Results results = new Results();
        try {
            for (Pair<String, BytecodeData.MethodData> p : testData)
                results.tally(p.getKey(), system.predict(p.getValue()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }

    @SuppressWarnings("unchecked")
    public Results crossValidate(int folds, Random random) {
        Results results = new Results();
        try {

            @SuppressWarnings("rawtypes")
            List[] foldData = new List[folds];
            for (int i = 0; i < folds; i++)
                foldData[i] = new ArrayList<Pair<String, BytecodeData.MethodData>>();

            List<Pair<String, BytecodeData.MethodData>> shuffled = trainData;
            Collections.shuffle(shuffled, random); // shuffle train data in place

            for (int i = 0; i < shuffled.size(); i++)
                foldData[i % folds].add(shuffled.get(i));

            for (int f = 0; f < folds; f++) {
                system.reset();
                List<Pair<String, BytecodeData.MethodData>> foldTrain = new ArrayList<>();
                for (int i = 0; i < folds; i++)
                    if (i != f)
                        foldTrain.addAll(foldData[i]);

                system.train(TrainData.from(foldTrain), vocab);

                for (Object o : foldData[f]) {
                    @SuppressWarnings("rawtypes")
                    Pair<String, BytecodeData.MethodData> p = (Pair) o;
                    results.tally(p.getKey(), system.predict(p.getValue()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }

    public static class Results {
        Map<String, Table> termTable;

        Map<String, int[]> recAtThr;

        private Table totalTable;

        public static final String TOTAL = "TOTAL";

        public Results() {
            this.termTable = new HashMap<String, Table>();
            this.recAtThr = new HashMap<String, int[]>();
            this.totalTable = new Table();
            termTable.put(TOTAL, this.totalTable);
        }

        public Table totals() {
            return termTable.get(TOTAL);
        }

        public Table tableFor(String term) {
            return termTable.get(term);
        }

        public double[] recallAtThresholdFor(String term) {
            int[] rec = this.recAtThr.get(term);

            if (rec == null)
                return null;

            double[] result = new double[rec.length];
            double norm = 1.0 / rec[rec.length - 1];
            for (int i = 0; i < rec.length; i++)
                result[i] = rec[i] * norm;
            return result;
        }

        public void tally(String target, Map<String, Double> predicted) {
            if (!termTable.containsKey(target))
                termTable.put(target, new Table());
            Table targetTable = termTable.get(target);
            List<Map.Entry<String, Double>> entries = new ArrayList<>(predicted.size());
            entries.addAll(predicted.entrySet());
            entries.sort(Map.Entry.<String, Double> comparingByValue());
            String confusor = entries.get(entries.size() - 1).getKey();

            int pos = 0;
            boolean recalled = false;
            if (!recAtThr.containsKey(target))
                recAtThr.put(target, new int[entries.size()]);
            int[] rec = recAtThr.get(target);
            while (entries.size() - pos - 1 >= 0) {
                if (entries.get(entries.size() - pos - 1).getKey().equals(target))
                    recalled = true;
                if (recalled)
                    rec[pos]++;
                pos++;
            }

            if (target.equals(confusor)) {
                targetTable.tp++;
                totalTable.tp++;
            } else {
                targetTable.fn++;
                totalTable.fn++;
                totalTable.fp++;

                if (!termTable.containsKey(confusor))
                    termTable.put(confusor, new Table());
                Table confusorTable = termTable.get(confusor);
                confusorTable.fp++;
            }
        }
    }

    public static class Table {
        int tp, fp, fn;

        public Table() {
            tp = fp = fn = 0;
        }

        public Table(int tp, int fp, int fn) {
            this.tp = tp;
            this.fp = fp;
            this.fn = fn;
        }

        public double precision() {

            return 1.0 * tp / (tp + fp);
        }

        public double recall() {
            return 1.0 * tp / (tp + fn);
        }

        public double f1() {
            double prec = precision();
            double rec = recall();
            return (2.0 * prec * rec / (prec + rec));
        }
    }

    public static void main(String[] args) throws Exception {

        String[] terms = new String[] { "run", "convert", "tight", "put", "find", "create", "has", "write", "generate",
                "add", "init", "new", "read", "test", "is", "handle", "check", "execute", "contains", "size", "loose",
                "reset", "evaluate", "hash", "next", "jj", "compare", "values", "update", "remove", "load", "get",
                "end", "copy", "make", "value", "close", "process", "set", "start", "clear", "parse", "build",
                "equals", "clone", "visit", "to", "initialize", "append" };

        if (args[0].equals("train")) {
            RandomForestSystem system = new RandomForestSystem(29, 250);
            TermVocabulary vocab = new TermVocabulary(terms);
            System.out.println("Number of terms: " + vocab.size());
            Experiment exp = new Experiment(system, vocab);
            exp.addData(new File(args[1]), DataType.TRAIN);
            System.out.println("Training data size: " + exp.getTrainData().size());

            System.out.println("Training started: " + new Date());
            exp.train();
            System.out.println("Training ended: " + new Date());

            ObjectOutputStream oos = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(args[2])));
            oos.writeObject(system);
            oos.close();
        } else if (args[0].equals("test")) {
            ObjectInputStream ois = new ObjectInputStream(new GZIPInputStream(new FileInputStream(args[2])));
            FirstClassSystem system = (FirstClassSystem) ois.readObject();
            ois.close();
            System.out.println("Read system " + system.getClass().getName());
            TermVocabulary vocab = system.vocab();
            System.out.println("Number of terms: " + vocab.size());

            Experiment exp = new Experiment(system, vocab);
            exp.addData(new File(args[1]), DataType.TEST);
            System.out.println("Testing data size: " + exp.getTestData().size());

            System.out.println("Testing started: " + new Date());
            Results results = exp.test();
            System.out.println("Testing ended: " + new Date());
            List<String> sortedTerms = new ArrayList<>(vocab.size());
            sortedTerms.addAll(vocab.terms());
            Collections.sort(sortedTerms);

            for (String term : sortedTerms) {
                System.out.print(term);
                Table t = results.tableFor(term);
                System.out.print("\t" + t.tp + "\t" + t.fn + "\t" + t.fp + "\t" + t.f1() + "\t" + t.precision() + "\t"
                        + t.recall());
                double[] rec = results.recallAtThresholdFor(term);
                if (rec != null)
                    for (double recAt : rec)
                        System.out.print("\t" + recAt);
                System.out.println();
            }
        } else {
            System.err.println("Unknown task: " + args[0]);
        }

    }

}
