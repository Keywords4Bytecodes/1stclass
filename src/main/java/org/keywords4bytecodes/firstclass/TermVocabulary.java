package org.keywords4bytecodes.firstclass;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TermVocabulary implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public static final int OTHER_POS = 0;
    public static final int WRAPPER_POS = 1;
    public static final String OTHER = "OTHER";
    public static final String WRAPPER = "WRAPPER";

    private Map<String, Integer> termToPos;
    private String[] posToTerm;

    public TermVocabulary(String[] terms) {
        posToTerm = new String[terms.length + 2];
        posToTerm[0] = OTHER;
        posToTerm[1] = WRAPPER;
        System.arraycopy(terms, 0, posToTerm, 2, terms.length);
        termToPos = new HashMap<>();
        for (int i = 0; i < posToTerm.length; i++)
            termToPos.put(posToTerm[i], i);
    }

    public int termToPos(String term) {
        Integer pos = termToPos.get(term);
        if (pos == null)
            return OTHER_POS;
        return pos;
    }

    public String posToTerm(int pos) {
        return posToTerm[pos];
    }

    public int size() {
        return posToTerm.length;
    }

    public String getFirstTerm(String methodName) {
        if (methodName.equals(OTHER) || methodName.equals(WRAPPER))
            return methodName;
        String parsed = parseFirstTerm(methodName);
        if (termToPos.containsKey(parsed))
            return parsed;
        return OTHER;
    }

    public static String parseFirstTerm(String methodName) {
        String result = methodName.replaceFirst("\\_.*", "").replaceFirst("[A-Z].*", "");
        if (result.startsWith("<"))
            return null;
        if (!result.matches(".*[a-z].*"))
            return null;
        if (result.indexOf('$') >= 0)
            return null;
        return result;
    }

    public List<String> terms() {
        return Arrays.asList(posToTerm);
    }

}
