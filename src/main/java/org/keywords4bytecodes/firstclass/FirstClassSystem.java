package org.keywords4bytecodes.firstclass;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public interface FirstClassSystem extends Serializable {

	public void train(TrainData data);

	public Map<String, Double> predict(int[] sequence);

	public List<String> terms();

}
