package org.keywords4bytecodes.firstclass;

import hr.irb.fastRandomForest.FastRandomForest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.keywords4bytecodes.firstclass.extract.BytecodeData;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class RandomForestSystem implements FirstClassSystem {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private FastRandomForest frf;

    private TermVocabulary vocab;

    private int seqLength;
    private int numTrees;

    private Map<String, Integer> attToPos = null;

    public static List<String> bytecodes = new ArrayList<String>(256);

    static {
        for (int i = 0; i < 256; i++)
            bytecodes.add("B" + i);
    }

    private double[] seqToFeats(int[] seq) {
        int attSize = attToPos.size();
        double[] v = new double[attSize];

        for (int s = 0; s < seqLength; s++)
            v[s] = (double) (s < seq.length ? seq[s] : BytecodeData.MethodData.PADDING);
        int[] counts = new int[(int) bytecodes.size()];
        for (int op : seq)
            counts[op]++;
        for (int i = 0; i < counts.length; i++)
            v[seqLength + i] = counts[i];
        v[v.length - 2] = seq.length;

        return v;
    }

    private transient Instances testHeader;

    private Instances buildInstances() {

        attToPos = new HashMap<>();

        ArrayList<Attribute> attInfo = new ArrayList<>();
        attToPos = new HashMap<String, Integer>();
        // nominal for beginning sq
        for (int s = 0; s < seqLength; s++) {
            String name = "S" + s;
            attToPos.put(name, attInfo.size());
            attInfo.add(new Attribute(name, bytecodes));
        }
        // opcode BOW for all seq
        for (int i = 0; i < bytecodes.size(); i++) {
            String name = "T" + bytecodes.get(i);
            attToPos.put(name, attInfo.size());
            attInfo.add(new Attribute(name));
        }
        // length
        attToPos.put("LENGTH", attInfo.size());
        attInfo.add(new Attribute("LENGTH"));
        // class
        attToPos.put("Verb", attInfo.size());
        attInfo.add(new Attribute("Verb", vocab.terms()));

        Instances result = new Instances("Verb", attInfo, 0);
        result.setClassIndex(attInfo.size() - 1);
        return result;
    }

    public RandomForestSystem(int seqLength, int numTrees) {
        this.seqLength = seqLength;
        this.numTrees=numTrees;
    }

    public void train(TrainData data, TermVocabulary vocab) throws Exception {
        this.vocab = vocab;
        Instances trainset = buildInstances();

        for (Pair<String, BytecodeData.MethodData> p : data.data()) {

            double[] instV = seqToFeats(p.getRight().getExtOpcodeSeq());
            instV[instV.length - 1] = (double) vocab.termToPos(p.getKey());

            Instance inst = new DenseInstance(1.0, instV);
            inst.setDataset(trainset);
            trainset.add(inst);
        }

        frf = new FastRandomForest();
        frf.setSeed(1993);
        // frf.setMaxDepth(20);
        // frf.setNumThreads(1);
        frf.setNumTrees(numTrees);
        frf.buildClassifier(trainset);
    }

    public Map<String, Double> predict(BytecodeData.MethodData method) throws Exception {
        if (testHeader == null)
            testHeader = buildInstances();

        double[] instV = seqToFeats(method.getExtOpcodeSeq());
        instV[instV.length - 1] = 0.0;

        Instance inst = new DenseInstance(1.0, instV);
        inst.setDataset(testHeader);
        testHeader.add(inst);

        double[] probs = frf.distributionForInstance(inst);
        Map<String, Double> result = new HashMap<>();
        int i = 0;
        for (String term : vocab.terms()) {
            result.put(term, probs[i]);
            i++;
        }

        testHeader.clear();

        return result;
    }

    public List<String> terms() {
        return vocab.terms();
    }

    public TermVocabulary vocab() {
        return vocab;
    }

    public void reset() {
        this.vocab = null;
        this.frf = null;
        this.testHeader = null;
    }

}
