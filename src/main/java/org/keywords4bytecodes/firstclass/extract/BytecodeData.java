package org.keywords4bytecodes.firstclass.extract;

import java.util.ArrayList;
import java.util.List;

public class BytecodeData {

    private String className;

    private List<MethodData> methods;

    public BytecodeData(String className) {
        this.className = className;
        this.methods = new ArrayList<>();
    }

    public void addMethod(MethodData method) {
        this.methods.add(method);
    }

    public List<MethodData> getMethods() {
        return methods;
    }

    public String getClassName() {
        return className;
    }

    public static class MethodData {

        // we do not use LABEL nor LOCAL_VARIABLE as they are usually scrubbed in obfuscation

        public static final int TRY_CATCH = 250;
        public static final int PADDING = 251;

        private byte[] extOpcodeSeq;

        private int access;
        private String name;
        private String desc;
        private String signature;
        private String[] exceptions;

        public MethodData(int access, String name, String desc, String signature, String[] exceptions) {
            super();
            this.access = access;
            this.name = name;
            this.desc = desc;
            this.signature = signature;
            this.exceptions = exceptions;
            this.extOpcodeSeq = new byte[0];
        }

        public void setExtOpcodeSeq(int[] seq) {
            extOpcodeSeq = new byte[seq.length];
            for (int i = 0; i < seq.length; i++)
                extOpcodeSeq[i] = (byte) seq[i];
        }

        public int[] getExtOpcodeSeq() {
            int[] result = new int[extOpcodeSeq.length];
            for (int i = 0; i < result.length; i++)
                result[i] = (int) (extOpcodeSeq[i] & 0xFF);
            return result;
        }

        public int getAccess() {
            return access;
        }

        public String getName() {
            return name;
        }

        public String getDesc() {
            return desc;
        }

        public String getSignature() {
            return signature;
        }

        public String[] getExceptions() {
            return exceptions;
        }

        public int size() {
            return extOpcodeSeq.length;
        }

    }

}
