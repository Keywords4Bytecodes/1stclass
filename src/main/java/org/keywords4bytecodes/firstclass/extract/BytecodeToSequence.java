package org.keywords4bytecodes.firstclass.extract;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class BytecodeToSequence {

    private static class TheClassVisitor extends ClassVisitor {

        private List<Pair<String, String[]>> opseq = new ArrayList<>();

        public TheClassVisitor() {
            super(Opcodes.ASM5);
        }

        public List<Pair<String, String[]>> getOpSeq() {
            return opseq;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
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
        }

        @Override
        public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
            return null;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            return new TheMethodVisitor(name, this);
        }

        public void endMethod(String methodName, List<String> opseq) {
            this.opseq.add(Pair.of(methodName, opseq.toArray(new String[0])));
        }
    }

    // private static class TheFieldVisitor extends FieldVisitor {
    // public TheFieldVisitor() {
    // super(Opcodes.ASM5);
    // }
    // }

    private static class TheMethodVisitor extends MethodVisitor {
        private List<String> opseq = new ArrayList<String>();
        private TheClassVisitor parent;
        private String methodName;

        public TheMethodVisitor(String methodName, TheClassVisitor parent) {
            super(Opcodes.ASM5);
            this.parent = parent;
            this.methodName = methodName;
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
            opseq.add("_" + opcode);
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            opseq.add("I" + opcode);
        }

        @Override
        public void visitVarInsn(int opcode, int var) {
            opseq.add("V" + opcode);
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            opseq.add("T" + opcode);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            opseq.add("F" + opcode);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc) {
            opseq.add("M" + opcode);
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            opseq.add("J" + opcode);
        }

        @Override
        public void visitLabel(Label label) {
            opseq.add("L");
        }

        @Override
        public void visitLdcInsn(Object cst) {
            opseq.add("LDC");
        }

        @Override
        public void visitIincInsn(int var, int increment) {
            opseq.add("II");
        }

        @Override
        public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
            opseq.add("T");
        }

        @Override
        public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
            opseq.add("LS");
        }

        @Override
        public void visitMultiANewArrayInsn(String desc, int dims) {
            opseq.add("MA");
        }

        @Override
        public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
            opseq.add("TC");
        }

        @Override
        public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
            opseq.add("LV");
        }

        @Override
        public void visitLineNumber(int line, Label start) {
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
        }

        @Override
        public void visitEnd() {
            parent.endMethod(methodName, opseq);
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
        for (File f : dir.listFiles()) {
            if (!f.getName().startsWith(".") && f.isDirectory())
                recurse(f);
            else if (f.getName().endsWith(".class")) {
                List<Pair<String, String[]>> opseq = dump(f);
                for (Pair<String, String[]> p : opseq) {
                    System.out.print(p.getLeft());
                    for (String o : p.getRight())
                        System.out.print(" " + o);
                    System.out.println();
                }
                System.out.println();
            }
        }
    }

    private static List<Pair<String, String[]>> dump(File f) throws Exception {
        TheClassVisitor v = new TheClassVisitor();
        ClassReader cr = new ClassReader(new FileInputStream(f));
        cr.accept(v, 0);
        return v.getOpSeq();
    }

}
