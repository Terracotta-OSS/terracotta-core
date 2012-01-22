/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.Type;
import com.tc.asm.commons.LocalVariablesSorter;
import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.bytecode.ClassAdapterFactory;
import com.tc.object.bytecode.Manager;

public class DSOUnsafeAdapter extends ClassAdapter implements Opcodes, ClassAdapterFactory {

  public DSOUnsafeAdapter() {
    super(null);
  }

  private DSOUnsafeAdapter(ClassVisitor cv, ClassLoader loader) {
    super(cv);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new DSOUnsafeAdapter(visitor, loader);
  }

  public final void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    superName = "sun/misc/Unsafe";
    super.visit(version, access, name, signature, superName, interfaces);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

    if ("<init>".equals(name)) {
      mv.visitCode();
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKESPECIAL, "sun/misc/Unsafe", "<init>", "()V");
      mv.visitInsn(RETURN);
      mv.visitMaxs(0, 0);
      mv.visitEnd();
      return null;
    }

    return mv;
  }

  public void visitEnd() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "compareAndSwapInt", "(Ljava/lang/Object;JII)Z", null, null);
    addUnsafeWrapperMethodCode(mv, ACC_PUBLIC, "compareAndSwapInt", "(Ljava/lang/Object;JII)Z");

    mv = super.visitMethod(ACC_PUBLIC, "compareAndSwapLong", "(Ljava/lang/Object;JJJ)Z", null, null);
    addUnsafeWrapperMethodCode(mv, ACC_PUBLIC, "compareAndSwapLong", "(Ljava/lang/Object;JJJ)Z");

    mv = super.visitMethod(ACC_PUBLIC, "compareAndSwapObject",
                           "(Ljava/lang/Object;JLjava/lang/Object;Ljava/lang/Object;)Z", null, null);
    addUnsafeWrapperMethodCode(mv, ACC_PUBLIC, "compareAndSwapObject",
                               "(Ljava/lang/Object;JLjava/lang/Object;Ljava/lang/Object;)Z");

    super.visitEnd();
  }

  private void addBeginVolatileInstrumentedCode(MethodVisitor mv, Type[] params) {
    int pos = 0;
    mv.visitVarInsn(params[0].getOpcode(ILOAD), pos + 1);
    pos += params[0].getSize();
    mv.visitVarInsn(params[1].getOpcode(ILOAD), pos + 1);
    mv.visitIntInsn(BIPUSH, Manager.LOCK_TYPE_WRITE);
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "beginVolatile",
                       "(Ljava/lang/Object;JI)V");
  }

  private void addCommitVolatileInstrumentedCode(MethodVisitor mv, Type[] params) {
    int pos = 0;
    mv.visitVarInsn(params[0].getOpcode(ILOAD), pos + 1);
    pos += params[0].getSize();
    mv.visitVarInsn(params[1].getOpcode(ILOAD), pos + 1);
    mv.visitIntInsn(BIPUSH, Manager.LOCK_TYPE_WRITE);
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "commitVolatile",
                       "(Ljava/lang/Object;JI)V");
  }

  private void addCheckedManagedConditionCode(MethodVisitor mv, Type[] params, int objParamIndex, int offsetParamIndex, Label nonSharedLabel,
                                              Label sharedLabel) {
  Label checkPortableFieldLabel = new Label();
    mv.visitVarInsn(params[objParamIndex].getOpcode(ILOAD), objParamIndex + 1);
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "isManaged", "(Ljava/lang/Object;)Z");
    mv.visitJumpInsn(IFEQ, nonSharedLabel);
    mv.visitJumpInsn(GOTO, checkPortableFieldLabel);
    mv.visitLabel(checkPortableFieldLabel);
    mv.visitVarInsn(params[objParamIndex].getOpcode(ILOAD), objParamIndex + 1);
    mv.visitVarInsn(params[offsetParamIndex].getOpcode(ILOAD), offsetParamIndex + 1);
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "isFieldPortableByOffset", "(Ljava/lang/Object;J)Z");
    mv.visitJumpInsn(IFEQ, nonSharedLabel);
    mv.visitJumpInsn(GOTO, sharedLabel);
  }

  private int getParameterPosition(Type[] params, int index) {
    int pos = 0;
    for (int i = 0; i < index; i++) {
      pos += params[i].getSize();
    }
    return pos;
  }

  private void addUnsafeWrapperMethodCode(MethodVisitor mv, int access, String methodName, String description) {
    mv = new FieldMethodAdapter(access, description, mv);

    Type[] params = Type.getArgumentTypes(description);
    Type returnType = Type.getReturnType(description);

    int newLocalVar1 = ((FieldMethodAdapter) mv).newLocal(Type.INT_TYPE);
    int newLocalVar2 = ((FieldMethodAdapter) mv).newLocal(Type.getObjectType("java/lang/Object"));
    int newLocalVar3 = ((FieldMethodAdapter) mv).newLocal(Type.getObjectType("java/lang/Object"));

    mv.visitCode();

    Label l0 = new Label();
    Label l1 = new Label();
    mv.visitTryCatchBlock(l0, l1, l1, null);
    Label l2 = new Label();
    Label l3 = new Label();
    mv.visitTryCatchBlock(l2, l3, l1, null);
    Label l4 = new Label();
    mv.visitLabel(l4);
    Label l5 = new Label();
    Label l6 = new Label();
    addCheckedManagedConditionCode(mv, params, 0, 1, l6, l5);

    mv.visitLabel(l6);
    invokeSuperMethod(mv, methodName, description, params);
    mv.visitInsn(returnType.getOpcode(IRETURN));

    mv.visitLabel(l5);
    mv.visitInsn(ICONST_0);
    mv.visitVarInsn(ISTORE, newLocalVar1);

    Label l7 = new Label();
    mv.visitLabel(l7);
    addBeginVolatileInstrumentedCode(mv, params);

    mv.visitLabel(l0);
    invokeSuperMethod(mv, methodName, description, params);
    mv.visitVarInsn(ISTORE, newLocalVar1);

    Label l8 = new Label();
    mv.visitLabel(l8);
    mv.visitVarInsn(ILOAD, newLocalVar1);
    mv.visitJumpInsn(IFEQ, l2);

    Label l9 = new Label();
    mv.visitLabel(l9);
    mv.visitVarInsn(params[0].getOpcode(ILOAD), getParameterPosition(params, 0) + 1);
    mv.visitVarInsn(params[1].getOpcode(ILOAD), getParameterPosition(params, 1) + 1);
    ByteCodeUtil.addTypeSpecificParameterLoad(mv, params[3], getParameterPosition(params, 3) + 1);
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/util/UnsafeUtil", "updateDSOSharedField",
                       "(Ljava/lang/Object;JLjava/lang/Object;)V");
    mv.visitJumpInsn(GOTO, l2);

    mv.visitLabel(l1);
    mv.visitVarInsn(ASTORE, newLocalVar3);
    Label l10 = new Label();
    mv.visitJumpInsn(JSR, l10);
    Label l11 = new Label();
    mv.visitLabel(l11);
    mv.visitVarInsn(ALOAD, newLocalVar3);
    mv.visitInsn(ATHROW);
    mv.visitLabel(l10);
    mv.visitVarInsn(ASTORE, newLocalVar2);
    Label l12 = new Label();
    mv.visitLabel(l12);
    addCommitVolatileInstrumentedCode(mv, params);
    Label l13 = new Label();
    mv.visitLabel(l13);
    mv.visitVarInsn(RET, newLocalVar2);

    mv.visitLabel(l2);
    mv.visitJumpInsn(JSR, l10);
    mv.visitLabel(l3);
    mv.visitVarInsn(ILOAD, newLocalVar1);
    mv.visitInsn(returnType.getOpcode(IRETURN));
    Label l14 = new Label();
    mv.visitLabel(l14);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  protected void addCheckWriteAccessInstrumentedCode(MethodVisitor mv, Type[] params, int objParamIndex) {
    mv.visitVarInsn(params[objParamIndex].getOpcode(ILOAD), objParamIndex + 1);
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "checkWriteAccess", "(Ljava/lang/Object;)V");
  }

  private void invokeSuperMethod(MethodVisitor mv, String methodName, String description, Type[] params) {
    ByteCodeUtil.pushThis(mv);
    int pos = 0;
    for (int i = 0; i < params.length; i++) {
      mv.visitVarInsn(params[i].getOpcode(ILOAD), pos + 1);
      pos += params[i].getSize();
    }
    mv.visitMethodInsn(INVOKESPECIAL, "sun/misc/Unsafe", methodName, description);
  }

  private static class FieldMethodAdapter extends LocalVariablesSorter implements Opcodes {
    public FieldMethodAdapter(int access, String desc, MethodVisitor mv) {
      super(access, desc, mv);
    }
  }

}
