package org.keywords4bytecodes.firstclass;

import hr.irb.fastRandomForest.FastRandomForest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.lang3.tuple.Pair;

import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.Saver;

public class FastRF {

    private static final int TOP_TERMS = 30;
    private static final int SEQ_LENGTH = 28;

    private static final String PADDING = "PADDING";
    private static final String OTHER = "OTHER";
    private static final String WRAPPER = "WRAPPER";

    private static Map<String, Integer> attToPos = null;

    private static List<String> posToVocab = null;
    private static Map<String, Integer> vocabToPos = null;
    private static int paddingPos = 0;

    private static double SAMPLE_TRAIN = 0.2;
    private static boolean BINARY = false;
    private static int CLASSIFIERS = 10;

    private static double[] seqToFeats(int[] seq) {
        double[] v = new double[attToPos.size()];

        for (int s = 0; s < SEQ_LENGTH; s++)
            v[s] = (double) (s < seq.length ? seq[s] : paddingPos);
        int[] counts = new int[posToVocab.size()];
        for (int op : seq)
            counts[op]++;
        for (int i = 0; i < counts.length; i++)
            v[SEQ_LENGTH + i] = counts[i];
        v[attToPos.get("LENGTH")] = seq.length;

        return v;
    }

    private static List<Pair<String, int[]>> readRawData(String filename) throws FileNotFoundException, IOException {
        List<Pair<String, int[]>> rawdata = new ArrayList<>();

        BufferedReader br = new BufferedReader(new FileReader(filename));

        String line = br.readLine();
        Map<String, List<int[]>> thisClassRows = new HashMap<>();
        while (line != null) {
            if (line.equals("")) {
                // separate wrappers from methods
                for (Map.Entry<String, List<int[]>> e : thisClassRows.entrySet()) {
                    String shortName = e.getKey().replaceFirst("\\_.*", "").replaceFirst("[A-Z].*", "");
                    if (shortName.startsWith("<") || !shortName.matches(".*[a-z].*") || shortName.contains("$"))
                        continue;

                    int maxLenIdx = -1;
                    int maxLen = -1;
                    for (int i = 0; i < e.getValue().size(); i++)
                        if (e.getValue().get(i).length > maxLen) {
                            maxLen = e.getValue().get(i).length;
                            maxLenIdx = i;
                        }
                    for (int i = 0; i < e.getValue().size(); i++)
                        if (i == maxLenIdx)
                            rawdata.add(Pair.of(shortName, e.getValue().get(i)));
                        else
                            rawdata.add(Pair.of(WRAPPER, e.getValue().get(i)));
                }
                thisClassRows.clear();
            } else if (line.indexOf(' ') > 0 && !line.startsWith("<") && !line.contains("$")) {
                String[] parts = line.split(" ");
                if (parts.length > 1) {
                    String name = null;
                    List<Integer> seq = new ArrayList<Integer>();

                    boolean first = true;
                    for (String part : parts) {
                        if (first) {
                            name = part;
                            first = false;
                        } else {
                            if (part.length() > 0) {
                                Integer pos = vocabToPos.get(part);
                                if (pos == null)
                                    pos = paddingPos;
                                seq.add(pos);
                            }
                        }
                    }
                    int[] seqArr = new int[seq.size()];
                    for (int i = 0; i < seqArr.length; i++)
                        seqArr[i] = seq.get(i);
                    if (!thisClassRows.containsKey(name))
                        thisClassRows.put(name, new ArrayList<int[]>());
                    thisClassRows.get(name).add(seqArr);
                }
            }

            line = br.readLine();
        }
        br.close();
        return rawdata;
    }

    public static void main(String[] args) throws Exception {

        // first pass, determine vocabulary
        BufferedReader br = new BufferedReader(new FileReader(args[0]));

        Map<String, AtomicInteger> verbCounts = new HashMap<String, AtomicInteger>();
        Set<String> vocab = new HashSet<>();
        vocab.add(PADDING);
        String line = br.readLine();
        int useful = 0;
        Set<String> thisClassSeenNames = new HashSet<String>();
        while (line != null) {
            if (line.equals("")) {
                thisClassSeenNames.clear();
            } else if (line.indexOf(' ') > 0 && !line.startsWith("<") && !line.contains("$")) {
                String[] parts = line.split(" ");
                if (parts.length > 1) {
                    boolean first = true;
                    useful++;
                    for (String part : parts) {
                        if (first) {
                            if (!thisClassSeenNames.contains(part)) {
                                thisClassSeenNames.add(part);
                                part = part.replaceFirst("\\_.*", "").replaceFirst("[A-Z].*", "");
                                if (!part.startsWith("<") && part.matches(".*[a-z].*")) {
                                    if (!verbCounts.containsKey(part))
                                        verbCounts.put(part, new AtomicInteger(0));
                                    verbCounts.get(part).incrementAndGet();
                                }
                            }
                            first = false;
                        } else {
                            if (part.length() > 0)
                                vocab.add(part);
                        }
                    }
                }
            }

            line = br.readLine();
        }
        br.close();

        System.out.println("Useful lines: " + useful);
        System.out.println("Vocabulary: " + vocab.size());

        List<Pair<String, Integer>> toSort = new ArrayList<>(verbCounts.size());
        for (Map.Entry<String, AtomicInteger> e : verbCounts.entrySet())
            toSort.add(Pair.of(e.getKey(), e.getValue().get()));
        Collections.sort(toSort, new Comparator<Pair<String, Integer>>() {

            @Override
            public int compare(Pair<String, Integer> p1, Pair<String, Integer> p2) {
                int rec = p2.getRight().compareTo(p1.getRight());
                if (rec == 0)
                    return p2.getLeft().compareTo(p1.getLeft());
                else
                    return rec;
            }
        });

        Set<String> topTerms = new HashSet<String>();
        List<String> sortedTopTerms = new ArrayList<>();
        int topTermsOcc = 0;
        for (int i = 0; i < TOP_TERMS; i++) {
            topTerms.add(toSort.get(i).getLeft());
            sortedTopTerms.add(toSort.get(i).getLeft());
            topTermsOcc += toSort.get(i).getRight();
        }

        System.out.println("Top terms(" + topTerms.size() + " / " + topTermsOcc + " / " + (topTermsOcc * 1.0 / useful)
                + "%): " + sortedTopTerms);

        posToVocab = new ArrayList<>(vocab.size());
        vocabToPos = new HashMap<>();

        int pos = 0;
        for (String v : vocab) {
            posToVocab.add(v);
            vocabToPos.put(v, (int) pos);
            pos++;
        }
        int vocab_size = vocab.size();
        paddingPos = vocabToPos.get(PADDING);

        List<String> posToTerm = new ArrayList<>();
        Map<String, Integer> termToPos = new HashMap<>();
        pos = 0;
        for (String t : topTerms) {
            posToTerm.add(t);
            termToPos.put(t, pos);
            pos++;
        }
        posToTerm.add(WRAPPER);
        termToPos.put(WRAPPER, pos);
        @SuppressWarnings("unused")
        int wrapperPos = pos;
        int otherPos = 0;
        if (!BINARY) {
            pos++;
            posToTerm.add(OTHER);
            termToPos.put(OTHER, pos);
            otherPos = pos;
        }

        // second pass, read data
        List<Pair<String, int[]>> rawdata = new ArrayList<>(useful);

        rawdata = readRawData(args[0]);

        // full model
        ArrayList<Attribute> attInfo = new ArrayList<>();
        attToPos = new HashMap<String, Integer>();
        // nominal for beginning sq
        for (int s = 0; s < SEQ_LENGTH; s++) {
            String name = "S" + s;
            attToPos.put(name, attInfo.size());
            attInfo.add(new Attribute(name, posToVocab));
        }
        // opcode BOW for all seq
        for (int i = 0; i < vocab_size; i++) {
            String name = "T" + posToVocab.get(i);
            attToPos.put(name, attInfo.size());
            // attToPos.put("t" + i, attInfo.size());
            attInfo.add(new Attribute(name));
        }
        // length
        attToPos.put("LENGTH", attInfo.size());
        attInfo.add(new Attribute("LENGTH"));
        // class
        attToPos.put("Verb", attInfo.size());
        List<String> binary = new ArrayList<>(Arrays.asList(OTHER, "named"));
        if (BINARY)
            attInfo.add(new Attribute("isNamed", binary));
        else
            attInfo.add(new Attribute("Verb", posToTerm));

        System.out.println("Row size: " + attInfo.size());

        Random sampler = new Random(1993);
        for (int cn = 0; cn < CLASSIFIERS; cn++) {

            // assemble train set
            Instances trainset = new Instances(BINARY ? "isNamed" : "Verb", attInfo, useful);
            trainset.setClassIndex(attInfo.size() - 1);

            for (Pair<String, int[]> p : rawdata) {
                if (sampler.nextFloat() > SAMPLE_TRAIN)
                    continue;

                double[] instV = seqToFeats(p.getRight());
                if (BINARY)
                    instV[instV.length - 1] = termToPos.containsKey(p.getLeft()) ? 1.0 : 0.0;
                else
                    instV[instV.length - 1] = (double) (termToPos.containsKey(p.getLeft()) ? termToPos.get(p.getLeft())
                            : otherPos);

                Instance inst = new DenseInstance(1.0, instV);
                inst.setDataset(trainset);
                trainset.add(inst);
            }

            // ArffSaver saver = new ArffSaver();
            // saver.setFile(new File("/tmp/train" + cn + ".arff"));
            // saver.setInstances(trainset);
            // saver.writeBatch();

            System.out.println(new Date() + " about to build classifier for " + trainset.size() + " instances...");
            FastRandomForest rf = new FastRandomForest();
            rf.setSeed(1993);
            // rf.setNumExecutionSlots(1);
            rf.buildClassifier(trainset);

            ObjectOutputStream oos = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream("/tmp/rf" + cn
                    + ".ser.gz")));
            oos.writeObject(rf);
            oos.close();
        }

        rawdata = null;
        List<Pair<String, int[]>> testdata = readRawData(args[1]);

        Instances testset = new Instances(BINARY ? "isNamed" : "Verb", attInfo, testdata.size());
        testset.setClassIndex(attInfo.size() - 1);
        int tp = 0, tn = 0, fp = 0, fn = 0;
        int[][] recAtThr = new int[termToPos.size()][];
        int[][] confTables = new int[termToPos.size()][];
        for (int i = 0; i < confTables.length; i++) {
            confTables[i] = new int[confTables.length];
            recAtThr[i] = new int[confTables.length];
        }

        double[][] dists = new double[testdata.size()][];
        for (int cn = 0; cn < CLASSIFIERS; cn++) {
            System.out.println(new Date() + " about to test classifier on " + testdata.size() + " instances...");

            ObjectInputStream ois = new ObjectInputStream(new GZIPInputStream(new FileInputStream("/tmp/rf" + cn
                    + ".ser.gz")));
            RandomForest rf = (RandomForest) ois.readObject();
            ois.close();

            for (int pIdx = 0; pIdx < testdata.size(); pIdx++) {
                Pair<String, int[]> p = testdata.get(pIdx);
                if (cn == 0) {
                    dists[pIdx] = BINARY ? new double[1] : new double[termToPos.size()];

                    double[] instV = seqToFeats(p.getRight());
                    Instance inst = new DenseInstance(1.0, instV);
                    inst.setDataset(testset);
                    if (BINARY) {
                        String trueClass = termToPos.containsKey(p.getLeft()) ? "named" : "OTHER";
                        inst.setClassValue(trueClass);
                    } else {
                        String trueClass = termToPos.containsKey(p.getLeft()) ? p.getLeft() : OTHER;
                        inst.setClassValue(trueClass);
                    }
                    testset.add(inst);
                }

                Instance inst = testset.get(pIdx);

                if (BINARY) {
                    dists[pIdx][0] += rf.classifyInstance(inst);
                    if (cn == CLASSIFIERS - 1) {
                        double clazz = dists[pIdx][0];
                        if (clazz > CLASSIFIERS / 2.0)
                            clazz = 1.0;
                        else
                            clazz = 0.0;

                        if (inst.classValue() == 0.0) {
                            if (clazz == 0.0)
                                tp++;
                            else
                                fn++;
                        } else {
                            if (clazz == 0.0)
                                tn++;
                            else
                                fp++;
                        }
                    }
                } else {
                    double[] thisDist = rf.distributionForInstance(inst);
                    for (int j = 0; j < thisDist.length; j++)
                        dists[pIdx][j] += thisDist[j];

                    if (cn == CLASSIFIERS - 1) {

                        int correct = (int) inst.classValue();
                        double[] dist = dists[pIdx];
                        double correctValue = dist[correct];
                        int biggerThanCorrect = 0;
                        double better = -1;
                        int betterIdx = -1;
                        for (int i = 0; i < dist.length; i++) {
                            if (dist[i] > better) {
                                better = dist[i];
                                betterIdx = i;
                            }
                            if (i != correct && dist[i] >= correctValue)
                                biggerThanCorrect++;
                        }
                        confTables[correct][betterIdx]++;
                        for (int i = biggerThanCorrect; i < termToPos.size(); i++)
                            recAtThr[correct][i]++;
                    }
                }
            }
        }
        Saver saver = new ArffSaver();
        saver.setFile(new File("/tmp/test.arff"));
        saver.setInstances(testset);
        saver.writeBatch();

        if (BINARY) {
            System.out.println(tp + " " + fp + "\n" + fn + " " + tn);
            double prec = 1.0 * tp / (tp + fp);
            double rec = 1.0 * tp / (tp + fn);

            System.out.println("prec = " + prec);
            System.out.println("rec = " + rec);
            System.out.println("F = " + (2.0 * prec * rec / (prec + rec)));
        } else {
            System.out.println("Confusion table:");
            int total = 0;
            for (int i = 0; i < confTables.length; i++) {
                if (i == 0) {
                    for (int j = 0; j < confTables[i].length; j++)
                        System.out.print(" " + posToTerm.get(j));
                    System.out.println();
                }

                System.out.print(posToTerm.get(i));
                for (int j = 0; j < confTables[i].length; j++) {
                    System.out.print(" " + confTables[i][j]);
                    total += confTables[i][j];
                }
                System.out.println();
            }

            System.out.println("\nPrec/rec:");
            for (int i = 0; i < confTables.length; i++) {
                tp = confTables[i][i];
                fp = 0;
                fn = 0;
                for (int j = 0; j < confTables[i].length; j++)
                    if (i != j) {
                        fp += confTables[j][i];
                        fn += confTables[i][j];
                    }
                tn = total - tp - fp - fn;

                double prec = 1.0 * tp / (tp + fp);
                double rec = 1.0 * tp / (tp + fn);

                System.out.println(posToTerm.get(i) + " " + tp + " " + fp + " " + fn + " " + tn + " prec = " + prec
                        + " rec = " + rec + " F = " + (2.0 * prec * rec / (prec + rec)));
            }

            System.out.println("\nRec@N:");
            for (int i = 0; i < confTables.length; i++) {
                if (i == 0) {
                    System.out.print("N");
                    for (int j = 0; j < confTables.length; j++)
                        System.out.print(" " + (j + 1));
                    System.out.println();
                }

                System.out.print(posToTerm.get(i));
                int[] rec = recAtThr[i];
                for (int j = 0; j < confTables.length; j++)
                    System.out.print(" " + (rec[j] * 1.0 / rec[rec.length - 1]));
                System.out.println();
            }
        }
    }
}
