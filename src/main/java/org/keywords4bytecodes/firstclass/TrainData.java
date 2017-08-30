package org.keywords4bytecodes.firstclass;

import java.io.File;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.keywords4bytecodes.firstclass.extract.BytecodeData;

public class TrainData {

    private List<Pair<String, BytecodeData.MethodData>> data;

    private TrainData() {
    }

    public TrainData(List<BytecodeData> data) {
        this(data, null);
    }

    public static TrainData from(List<Pair<String, BytecodeData.MethodData>> data) {
        TrainData result = new TrainData();
        result.data = data;
        return result;
    }

    public TrainData(List<BytecodeData> data, TermVocabulary vocab) {
        this.data = new ArrayList<>(data.size());
        Map<String, List<BytecodeData.MethodData>> byFullName = new HashMap<>();
        for (BytecodeData b : data) {

            // wrapper identification
            byFullName.clear();
            for (BytecodeData.MethodData m : b.getMethods()) {
                if (!byFullName.containsKey(m.getName()))
                    byFullName.put(m.getName(), new ArrayList<BytecodeData.MethodData>());
                byFullName.get(m.getName()).add(m);
            }

            for (Map.Entry<String, List<BytecodeData.MethodData>> e : byFullName.entrySet()) {
                // sort from longer to shorter. Longer is the actual, the rest are wrappers
                Collections.sort(e.getValue(), new Comparator<BytecodeData.MethodData>() {

                    @Override
                    public int compare(BytecodeData.MethodData m1, BytecodeData.MethodData m2) {
                        return Integer.valueOf(m2.size()).compareTo(Integer.valueOf(m1.size()));
                    }
                });

                String methodName = e.getKey();
                boolean first = true;
                for (BytecodeData.MethodData m : e.getValue())
                    if (first) {
                        String term = vocab == null ? TermVocabulary.parseFirstTerm(methodName) : vocab
                                .getFirstTerm(methodName);
                        if (term == null)
                            break; // ignore all
                        this.data.add(Pair.of(term, m));
                        first = false;
                    } else
                        this.data.add(Pair.of(TermVocabulary.WRAPPER, m));

            }
        }
    }

    public List<Pair<String, BytecodeData.MethodData>> data() {
        return data;
    }

    /**
     * Utility to size a corpus
     */
    public static void main(String[] args) throws Exception {
	List<BytecodeData> data = Experiment.loadFile(new File(args[0]));
	int methodCount = 0;
	int instCount = 0;
	for(BytecodeData bd : data){
	    for(BytecodeData.MethodData md: bd.getMethods()){
		methodCount++;
		instCount += md.size();
	    }
	}
	System.out.println("For folder: " + args[0]);
	System.out.println("Total number of classes: " + data.size());
	System.out.println("Total number of methods: " + methodCount);
	System.out.println("Total number of instructions: " + instCount);
    }

}
