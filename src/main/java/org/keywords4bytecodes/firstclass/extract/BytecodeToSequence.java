package org.keywords4bytecodes.firstclass.extract;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

public class BytecodeToSequence {

    private static class TheClassVisitor extends ClassVisitor {

        private List<BytecodeData> data;

        private int index;

        public TheClassVisitor() {
            super(Opcodes.ASM5);
            this.data = new ArrayList<>();
            this.index = -1;
        }

        public List<BytecodeData> getBytecodeData() {
            return data;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            BytecodeData top = new BytecodeData(name);
            data.add(top);
            index++;
        }

        @Override
        public void visitSource(String source, String debug) {
        }

        @Override
        public void visitOuterClass(String owner, String name, String desc) {
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            return null;
        }

        @Override
        public void visitAttribute(Attribute attr) {
        }

        @Override
        public void visitInnerClass(String name, String outerName, String innerName, int access) {
            // create new bytecodedata?
        }

        @Override
        public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
            return null;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            BytecodeData.MethodData methodData = new BytecodeData.MethodData(access, name, desc, signature, exceptions);
            this.data.get(index).addMethod(methodData);
            return new TheMethodVisitor(methodData);
        }
    }

    // private static class TheFieldVisitor extends FieldVisitor {
    // public TheFieldVisitor() {
    // super(Opcodes.ASM5);
    // }
    // }

    private static class TheMethodVisitor extends MethodVisitor {
        private List<Integer> opseq = new ArrayList<Integer>();
        private BytecodeData.MethodData methodData;

        public TheMethodVisitor(BytecodeData.MethodData methodData) {
            super(Opcodes.ASM5);
            this.methodData = methodData;
        }

        @Override
        public AnnotationVisitor visitAnnotationDefault() {
            return null;
        }

        @Override
        public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
            return null;
        }

        @Override
        public void visitCode() {
        }

        @Override
        public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
        }

        @Override
        public void visitInsn(int opcode) {
            opseq.add(opcode);
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            opseq.add(opcode);
        }

        @Override
        public void visitVarInsn(int opcode, int var) {
            opseq.add(opcode);
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            opseq.add(opcode);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            opseq.add(opcode);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc) {
            opseq.add(opcode);
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            opseq.add(opcode);
        }

        @Override
        public void visitLabel(Label label) {
            // opseq.add("L"); // ignore labels
        }

        @Override
        public void visitLdcInsn(Object cst) {
            opseq.add(Opcodes.LDC);
        }

        @Override
        public void visitIincInsn(int var, int increment) {
            opseq.add(Opcodes.IINC);
        }

        @Override
        public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
            opseq.add(Opcodes.TABLESWITCH);
        }

        @Override
        public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
            opseq.add(Opcodes.LOOKUPSWITCH);
        }

        @Override
        public void visitMultiANewArrayInsn(String desc, int dims) {
            opseq.add(Opcodes.MULTIANEWARRAY);
        }

        @Override
        public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
            opseq.add(BytecodeData.MethodData.TRY_CATCH);
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
            opseq.add(Opcodes.INVOKEDYNAMIC);
        }

        @Override
        public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
            // opseq.add("LV"); // ignore local variables
        }

        @Override
        public void visitLineNumber(int line, Label start) {
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
        }

        @Override
        public void visitEnd() {
            int[] opseq = new int[this.opseq.size()];
            for (int i = 0; i < opseq.length; i++)
                opseq[i] = this.opseq.get(i);
            methodData.setExtOpcodeSeq(opseq);
        }
    }

    public static void main(String[] args) throws Exception {
        File f = new File(args[0]);
        if (f.isDirectory())
            recurse(f);
        else
            dump(f);
    }

    private static void recurse(File dir) throws Exception {
        for (File f : dir.listFiles())
            if (!f.getName().startsWith(".") && f.isDirectory())
                recurse(f);
            else if (f.getName().endsWith(".class"))
                dump(f);
    }

    public static List<BytecodeData> extract(byte[] classBytes) {
        TheClassVisitor v = new TheClassVisitor();
        ClassReader cr = new ClassReader(classBytes);
        cr.accept(v, 0);
        return v.getBytecodeData();
    }

    public static List<BytecodeData> extract(File f) throws IOException {
	try{
	    TheClassVisitor v = new TheClassVisitor();
	    InputStream is = new FileInputStream(f);
	    ClassReader cr = new ClassReader(is);
	    cr.accept(v, 0);
	    is.close();
	    return v.getBytecodeData();
	}catch(Exception e){
	    System.err.println(f);
	    e.printStackTrace();
	    return new ArrayList<BytecodeData>();
	}
    }

    public static List<BytecodeData> extract(ClassNode classNode) {
        TheClassVisitor v = new TheClassVisitor();
        classNode.accept(v);
        return v.getBytecodeData();
    }

    private static void dump(File f) throws Exception {
        TheClassVisitor v = new TheClassVisitor();
        InputStream is = new FileInputStream(f);
        ClassReader cr = new ClassReader(is);
        cr.accept(v, 0);
        is.close();
        for (BytecodeData data : v.getBytecodeData()) {
            System.out.println(data.getClassName());
            for (BytecodeData.MethodData method : data.getMethods()) {
                System.out.print(method.getName());
                int[] opseq = method.getExtOpcodeSeq();
                for (int i = 0; i < opseq.length; i++) {
                    int c = opseq[i];
                    System.out.print(" " + opcodeLabel(c) + c);
                }
                System.out.println();
            }
            System.out.println();
        }
    }

    private static String opcodeLabel(int c) {
        String s = "_";
        if (c <= Opcodes.DCONST_1)
            s = "_";
        else if (c <= Opcodes.SIPUSH)
            s = "I";
        else if (c <= Opcodes.LDC)
            s = "LDC";
        else if (c <= Opcodes.ALOAD)
            s = "V";
        else if (c <= Opcodes.ALOAD)
            s = "_";
        else if (c <= Opcodes.ASTORE)
            s = "V";
        else if (c <= Opcodes.LXOR)
            s = "_";
        else if (c <= Opcodes.IINC)
            s = "II";
        else if (c <= Opcodes.DCMPG)
            s = "_";
        else if (c <= Opcodes.JSR)
            s = "J";
        else if (c <= Opcodes.RET)
            s = "V";
        else if (c <= Opcodes.TABLESWITCH)
            s = "TS";
        else if (c <= Opcodes.LOOKUPSWITCH)
            s = "LS";
        else if (c <= Opcodes.RETURN)
            s = "_";
        else if (c <= Opcodes.PUTFIELD)
            s = "F";
        else if (c <= Opcodes.INVOKEINTERFACE)
            s = "M";
        else if (c <= Opcodes.INVOKEDYNAMIC)
            s = "MD";
        else if (c <= Opcodes.NEW)
            s = "T";
        else if (c <= Opcodes.NEWARRAY)
            s = "I";
        else if (c <= Opcodes.ANEWARRAY)
            s = "T";
        else if (c <= Opcodes.ATHROW)
            s = "_";
        else if (c <= Opcodes.INSTANCEOF)
            s = "T";
        else if (c <= Opcodes.MONITOREXIT)
            s = "_";
        else if (c <= Opcodes.MULTIANEWARRAY)
            s = "MA";
        else if (c <= Opcodes.IFNONNULL)
            s = "J";
        else if (c <= BytecodeData.MethodData.TRY_CATCH)
            s = "TC";
        else
            throw new IllegalArgumentException("Unknown opcode: " + c);
        return s;

    }

}
