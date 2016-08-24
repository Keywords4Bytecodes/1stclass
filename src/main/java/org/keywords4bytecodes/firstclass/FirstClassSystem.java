package org.keywords4bytecodes.firstclass;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.keywords4bytecodes.firstclass.extract.BytecodeData;

public interface FirstClassSystem extends Serializable {

    public void train(TrainData data, TermVocabulary vocab) throws Exception;

    public Map<String, Double> predict(BytecodeData.MethodData method) throws Exception;

    public List<String> terms();

    public TermVocabulary vocab();

    public void reset();

}
