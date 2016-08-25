package org.keywords4bytecodes.firstclass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.keywords4bytecodes.firstclass.extract.BytecodeData;

import weka.classifiers.bayes.NaiveBayes;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class NaiveBayesSystem implements FirstClassSystem {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private NaiveBayes nb;

	private TermVocabulary vocab;

	private Map<String, Integer> attToPos = null;

	public static List<String> bytecodes = new ArrayList<String>(256);

	static {
		for (int i = 0; i < 256; i++)
			bytecodes.add("B" + i);
	}

	private double[] seqToFeats(int[] seq) {
		int attSize = attToPos.size();
		double[] v = new double[attSize];

		int[] counts = new int[(int) bytecodes.size()];
		for (int op : seq)
			counts[op]++;
		for (int i = 0; i < counts.length; i++)
			v[i] = counts[i];

		return v;
	}

	private transient Instances testHeader;

	private Instances buildInstances() {

		attToPos = new HashMap<>();

		ArrayList<Attribute> attInfo = new ArrayList<>();
		attToPos = new HashMap<String, Integer>();
		// opcode BOW for all seq
		for (int i = 0; i < bytecodes.size(); i++) {
			String name = "T" + bytecodes.get(i);
			attToPos.put(name, attInfo.size());
			attInfo.add(new Attribute(name));
		}
		// class
		attToPos.put("Verb", attInfo.size());
		attInfo.add(new Attribute("Verb", vocab.terms()));

		Instances result = new Instances("Verb", attInfo, 0);
		result.setClassIndex(attInfo.size() - 1);
		return result;
	}

	public NaiveBayesSystem() {
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

		nb = new NaiveBayes();
		nb.buildClassifier(trainset);
	}

	public Map<String, Double> predict(BytecodeData.MethodData method) throws Exception {
		if (testHeader == null)
			testHeader = buildInstances();

		double[] instV = seqToFeats(method.getExtOpcodeSeq());
		instV[instV.length - 1] = 0.0;

		Instance inst = new DenseInstance(1.0, instV);
		inst.setDataset(testHeader);
		testHeader.add(inst);

		double[] probs = nb.distributionForInstance(inst);
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
		this.nb = null;
		this.testHeader = null;
	}
}
