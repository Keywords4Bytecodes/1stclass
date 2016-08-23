package org.keywords4bytecodes.firstclass.extract;

import java.io.File;
import java.io.FileInputStream;

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
        public TheClassVisitor() {
            super(Opcodes.ASM5);
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
            System.out.print(name);
            return new TheMethodVisitor();
        }

        @Override
        public void visitEnd() {
            System.out.println();
        }

    }

    // private static class TheFieldVisitor extends FieldVisitor {
    // public TheFieldVisitor() {
    // super(Opcodes.ASM5);
    // }
    // }

    private static class TheMethodVisitor extends MethodVisitor {
        public TheMethodVisitor() {
            super(Opcodes.ASM5);
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
            System.out.print(" _" + opcode);
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            System.out.print(" I" + opcode);
        }

        @Override
        public void visitVarInsn(int opcode, int var) {
            System.out.print(" V" + opcode);
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            System.out.print(" T" + opcode);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            System.out.print(" F" + opcode);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc) {
            System.out.print(" M" + opcode);
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            System.out.print(" J" + opcode);
        }

        @Override
        public void visitLabel(Label label) {
            System.out.print(" L");
        }

        @Override
        public void visitLdcInsn(Object cst) {
            System.out.print(" LDC");
        }

        @Override
        public void visitIincInsn(int var, int increment) {
            System.out.print(" II");
        }

        @Override
        public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
            System.out.print(" T");
        }

        @Override
        public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
            System.out.print(" LS");
        }

        @Override
        public void visitMultiANewArrayInsn(String desc, int dims) {
            System.out.print(" MA");
        }

        @Override
        public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
            System.out.print(" TC");
        }

        @Override
        public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
            System.out.print(" LV");
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            // TODO Auto-generated method stub

        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            // TODO Auto-generated method stub

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
            else if (f.getName().endsWith(".class"))
                dump(f);
        }
    }

    private static void dump(File f) throws Exception {
        TheClassVisitor v = new TheClassVisitor();
        ClassReader cr = new ClassReader(new FileInputStream(f));
        cr.accept(v, 0);
    }

}
