package org.keywords4bytecodes.firstclass;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

public class Predictions {

    public static List<Pair<Double, String>> sortPredictions(Map<String, Double> predictions){
        List<Pair<Double, String>> result = new ArrayList<>();
        
        for(Map.Entry<String, Double>e:predictions.entrySet())
            result.add(Pair.of(e.getValue(), e.getKey()));
        result.sort(new Comparator<Pair<Double,String>>(){
    
            @Override
            public int compare(Pair<Double, String> o1, Pair<Double, String> o2) {
                return o2.compareTo(o1);
            }
            
        });
        
        return result;
    }

}
