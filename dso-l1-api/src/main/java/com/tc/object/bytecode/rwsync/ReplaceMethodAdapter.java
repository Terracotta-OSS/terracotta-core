/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package com.tc.object.bytecode.rwsync;

import com.tc.asm.AnnotationVisitor;
import com.tc.asm.Attribute;
import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.commons.EmptyVisitor;

/**
 * A method adapter that replaces the contents of the method. When visitCode() is called, it executes visitNewBody(),
 * and then it discards all subsequent instructions.
 */
public abstract class ReplaceMethodAdapter implements MethodVisitor {

  /**
   * Starts out as the next MethodVisitor in the chain, but after visitEnd() is called for the first time, is replaced
   * with a NullMethodAdapter.
   */
  protected MethodVisitor mv;

  private boolean         visitedCode = false;

  public ReplaceMethodAdapter(MethodVisitor mv) {
    this.mv = mv;
  }

  /**
   * Subclasses should use this event to generate the new content of the adapted method, by using calls to visitXxx
   * methods in the normal way. This is called from visitCode(), so to avoid recursion, the implementation of this
   * method should not itself call visitCode(). The implementation should finish by calling visitMaxs() and then
   * visitEnd(). After the implementing method calls visitEnd(), any subsequent visitXxx() calls coming from whoever is
   * visiting this adapter will be ignored. For example:
   * 
   * <pre>
   *   public void visitNewBody() {
   *     // note no call to visitCode()
   *     visitVarInsn(ALOAD, 0);
   *     visitFieldInsn(GETFIELD, &quot;LFoo;&quot;, &quot;val&quot;, &quot;Z&quot;);
   *     visitInsn(IRETURN);
   *     visitMaxs(1,0)
   *     visitEnd();
   *   }
   * </pre>
   */
  public abstract void visitNewBody();

  public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
    return mv.visitAnnotation(desc, visible);
  }

  public AnnotationVisitor visitAnnotationDefault() {
    return mv.visitAnnotationDefault();
  }

  public void visitAttribute(Attribute attr) {
    mv.visitAttribute(attr);
  }

  public void visitCode() {
    if (!visitedCode) {
      // avoid recursion if visitNewBody() calls super.visitCode() by mistake
      visitedCode = true;
      mv.visitCode();
      visitNewBody();
    }
  }

  public void visitEnd() {
    mv.visitEnd();
    mv = new EmptyVisitor();
  }

  public void visitFieldInsn(int opcode, String owner, String name, String desc) {
    mv.visitFieldInsn(opcode, owner, name, desc);
  }

  public void visitFrame(int type, int local, Object[] local2, int stack, Object[] stack2) {
    mv.visitFrame(type, local, local2, stack, stack2);
  }

  public void visitIincInsn(int var, int increment) {
    mv.visitIincInsn(var, increment);
  }

  public void visitInsn(int opcode) {
    mv.visitInsn(opcode);
  }

  public void visitIntInsn(int opcode, int operand) {
    mv.visitIntInsn(opcode, operand);
  }

  public void visitJumpInsn(int opcode, Label label) {
    mv.visitJumpInsn(opcode, label);
  }

  public void visitLabel(Label label) {
    mv.visitLabel(label);
  }

  public void visitLdcInsn(Object cst) {
    mv.visitLdcInsn(cst);
  }

  public void visitLineNumber(int line, Label start) {
    mv.visitLineNumber(line, start);
  }

  public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
    mv.visitLocalVariable(name, desc, signature, start, end, index);
  }

  public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
    mv.visitLookupSwitchInsn(dflt, keys, labels);
  }

  public void visitMaxs(int maxStack, int maxLocals) {
    mv.visitMaxs(maxStack, maxLocals);
  }

  public void visitMethodInsn(int opcode, String owner, String name, String desc) {
    mv.visitMethodInsn(opcode, owner, name, desc);
  }

  public void visitMultiANewArrayInsn(String desc, int dims) {
    mv.visitMultiANewArrayInsn(desc, dims);
  }

  public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
    return mv.visitParameterAnnotation(parameter, desc, visible);
  }

  public void visitTableSwitchInsn(int min, int max, Label dflt, Label[] labels) {
    mv.visitTableSwitchInsn(min, max, dflt, labels);
  }

  public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
    mv.visitTryCatchBlock(start, end, handler, type);
  }

  public void visitTypeInsn(int opcode, String type) {
    mv.visitTypeInsn(opcode, type);
  }

  public void visitVarInsn(int opcode, int var) {
    mv.visitVarInsn(opcode, var);
  }
}
