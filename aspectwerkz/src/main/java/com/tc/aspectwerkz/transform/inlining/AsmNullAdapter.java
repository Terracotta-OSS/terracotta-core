/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.transform.inlining;

import com.tc.asm.AnnotationVisitor;
import com.tc.asm.Attribute;
import com.tc.asm.ClassVisitor;
import com.tc.asm.FieldVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;

/**
 * Visitors that are not writing any bytecode and using a Null ClassVisitor / Code Visitor as a target instead.
 *
 * @author <a href="mailto:alex@gnilux.com">Alexandre Vasseur</a>
 */
public class AsmNullAdapter {

  /**
   * A NullClassAdapter that does nothing.
   * Can be used to speed up ASM and avoid unecessary bytecode writing thru a regular ClassWriter when this is not
   * needed (read only purpose).
   */
  public static class NullClassAdapter implements ClassVisitor {

    public final static ClassVisitor NULL_CLASS_ADAPTER = new NullClassAdapter();

    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    }

    public void visitInnerClass(String name, String outerName, String innerName, int access) {
    }

    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
      return NullFieldAdapter.NULL_FIELD_ADAPTER;
    }

    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
      return NullMethodAdapter.NULL_METHOD_ADAPTER;
    }

    public void visitSource(String source, String debug) {
    }

    public void visitOuterClass(String owner, String name, String desc) {
    }

    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
      return NullAnnotationVisitor.NULL_ANNOTATION_ADAPTER;
    }

    public void visitAttribute(Attribute attribute) {
    }

    public void visitEnd() {
    }
  }

  /**
   * A NullMethodAdapter that does nothing.
   * Can be used to speed up ASM and avoid unecessary bytecode writing thru a regular CodeWriter when this is not
   * needed (read only purpose)
   */
  public static class NullMethodAdapter implements MethodVisitor {

    public final static MethodVisitor NULL_METHOD_ADAPTER = new NullMethodAdapter();

    public void visitInsn(int opcode) {
    }

    public void visitIntInsn(int opcode, int operand) {
    }

    public void visitVarInsn(int opcode, int var) {
    }

    public void visitTypeInsn(int opcode, String desc) {
    }

    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
    }

    public void visitJumpInsn(int opcode, Label label) {
    }

    public void visitLabel(Label label) {
    }

    public void visitLdcInsn(Object cst) {
    }

    public void visitIincInsn(int var, int increment) {
    }

    public void visitTableSwitchInsn(int min, int max, Label dflt, Label labels[]) {
    }

    public void visitLookupSwitchInsn(Label dflt, int keys[], Label labels[]) {
    }

    public void visitMultiANewArrayInsn(String desc, int dims) {
    }

    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
    }

    public void visitMaxs(int maxStack, int maxLocals) {
    }

    public void visitLocalVariable(String name, String desc, String sig, Label start, Label end, int index) {
    }

    public void visitLineNumber(int line, Label start) {
    }

    public void visitAttribute(Attribute attr) {
    }

    public AnnotationVisitor visitAnnotationDefault() {
      return NullAnnotationVisitor.NULL_ANNOTATION_ADAPTER;
    }

    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
      return NullAnnotationVisitor.NULL_ANNOTATION_ADAPTER;
    }

    public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
      return NullAnnotationVisitor.NULL_ANNOTATION_ADAPTER;
    }

    public void visitCode() {
    }

    public void visitEnd() {
    }

    public void visitFrame(int type, int local, Object[] local2, int stack, Object[] stack2) {
    }
  }

  /**
   * A NullFieldAdapter
   */
  public static class NullFieldAdapter implements FieldVisitor {

    public final static FieldVisitor NULL_FIELD_ADAPTER = new NullFieldAdapter();

    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
      return NullAnnotationVisitor.NULL_ANNOTATION_ADAPTER;
    }

    public void visitAttribute(Attribute attr) {
    }

    public void visitEnd() {
    }
  }

  /**
   * A NullAnnotationVisitor
   */
  public static class NullAnnotationVisitor implements AnnotationVisitor {

    public final static AnnotationVisitor NULL_ANNOTATION_ADAPTER = new NullAnnotationVisitor();

    public void visit(String name, Object value) {
    }

    public void visitEnum(String name, String desc, String value) {
    }

    public AnnotationVisitor visitAnnotation(String name, String desc) {
      return NULL_ANNOTATION_ADAPTER;
    }

    public AnnotationVisitor visitArray(String name) {
      return NULL_ANNOTATION_ADAPTER;
    }

    public void visitEnd() {
    }
  }
}
