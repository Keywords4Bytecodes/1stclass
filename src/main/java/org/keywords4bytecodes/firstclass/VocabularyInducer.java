package org.keywords4bytecodes.firstclass;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.tuple.Pair;
import org.keywords4bytecodes.firstclass.extract.BytecodeData;

public class VocabularyInducer {

    public static final int TRAILS = 10;
    public static final int FOLDS = 2;

    public static TermVocabulary induce(List<BytecodeData> rawData, FirstClassSystem system, double threshold,
            int minCount, boolean verbose) {

        TrainData data = new TrainData(rawData);

        Map<String, AtomicInteger> termCounts = new HashMap<>();
        for (Pair<String, BytecodeData.MethodData> p : data.data()) {
            String term = p.getKey();
            AtomicInteger ai = termCounts.get(term);
            if (ai == null)
                termCounts.put(term, new AtomicInteger(1));
            else
                ai.incrementAndGet();
        }
        Set<String> toConsider = new HashSet<String>();
        for (Map.Entry<String, AtomicInteger> e : termCounts.entrySet())
            if (e.getValue().get() >= minCount)
                toConsider.add(e.getKey());

        if (verbose)
            System.out.println("Got " + data.data().size() + " methods with " + toConsider.size()
                    + " unique terms at cut point " + minCount);

        Random random = new Random(1993);
        Set<String> goodTerms = new HashSet<String>();
        for (String term : toConsider) {
            boolean good = true;
            int counts = termCounts.get(term).get();
            for (int t = 0; t < TRAILS; t++) {
                Experiment exp = new Experiment(system, null);
                List<Pair<String, BytecodeData.MethodData>> shuffled = data.data();
                Collections.shuffle(shuffled, random);
                int otherAdded = 0;
                for (Pair<String, BytecodeData.MethodData> p : shuffled) {
                    if (p.getKey().equals(term))
                        exp.addData(term, p.getValue(), Experiment.DataType.BOTH);
                    else if (otherAdded < counts) {
                        exp.addData("OTHER", p.getValue(), Experiment.DataType.BOTH);
                        otherAdded++;
                    }
                }
                Experiment.Results results = exp.crossValidate(FOLDS);
                if (results.totals().f1() < threshold) {
                    good = false;
                    break;
                }
            }
            if (good)
                goodTerms.add(term);
        }

        if (verbose)
            System.out.println("Found " + goodTerms + " at F1 threshold " + threshold);

        return new TermVocabulary(goodTerms.toArray(new String[0]));
    }
}
