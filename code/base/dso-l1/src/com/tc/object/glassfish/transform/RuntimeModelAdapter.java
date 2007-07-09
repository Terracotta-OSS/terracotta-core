/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.glassfish.transform;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.bytecode.ClassAdapterFactory;

public class RuntimeModelAdapter extends ClassAdapter implements ClassAdapterFactory, Opcodes {

  private static final String REMOVE_TC_FIELDS_METHOD      = ByteCodeUtil.TC_METHOD_PREFIX + "removeTCFields";
  private static final String REMOVE_TC_FIELDS_METHOD_DESC = "(Ljava/util/List;)Ljava/util/List;";

  private String              thisClassName;

  public RuntimeModelAdapter(ClassVisitor cv) {
    super(cv);
  }

  public RuntimeModelAdapter() {
    super(null);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new RuntimeModelAdapter(visitor);
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    super.visit(version, access, name, signature, superName, interfaces);
    thisClassName = name;
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

    if ("getFields".equals(name)) {
      mv = new GetFieldsAdapter(mv);
    }

    return mv;
  }

  public void visitEnd() {
    addRemoveTCFieldsMethod();
    super.visitEnd();
  }

  private void addRemoveTCFieldsMethod() {
    MethodVisitor mv = super.visitMethod(ACC_PRIVATE + ACC_STATIC, REMOVE_TC_FIELDS_METHOD,
                                         REMOVE_TC_FIELDS_METHOD_DESC, null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitLineNumber(71, l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "iterator", "()Ljava/util/Iterator;");
    mv.visitVarInsn(ASTORE, 1);
    Label l1 = new Label();
    mv.visitLabel(l1);
    Label l2 = new Label();
    mv.visitJumpInsn(GOTO, l2);
    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitLineNumber(72, l3);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;");
    mv.visitTypeInsn(CHECKCAST, "java/lang/String");
    mv.visitVarInsn(ASTORE, 2);
    Label l4 = new Label();
    mv.visitLabel(l4);
    mv.visitLineNumber(73, l4);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitLdcInsn(ByteCodeUtil.TC_FIELD_PREFIX);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "startsWith", "(Ljava/lang/String;)Z");
    mv.visitJumpInsn(IFEQ, l2);
    Label l5 = new Label();
    mv.visitLabel(l5);
    mv.visitLineNumber(74, l5);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "remove", "()V");
    mv.visitLabel(l2);
    mv.visitLineNumber(71, l2);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z");
    mv.visitJumpInsn(IFNE, l3);
    Label l6 = new Label();
    mv.visitLabel(l6);
    mv.visitLineNumber(78, l6);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitInsn(ARETURN);
    Label l7 = new Label();
    mv.visitLabel(l7);
    mv.visitLocalVariable("fields", "Ljava/util/List;", null, l0, l7, 0);
    mv.visitLocalVariable("iterator", "Ljava/util/Iterator;", null, l1, l6, 1);
    mv.visitLocalVariable("field", "Ljava/lang/String;", null, l4, l2, 2);
    mv.visitMaxs(2, 3);
    mv.visitEnd();
  }

  private class GetFieldsAdapter extends MethodAdapter implements Opcodes {

    public GetFieldsAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitInsn(int opcode) {
      if (ARETURN == opcode) {
        mv.visitMethodInsn(INVOKESTATIC, thisClassName, REMOVE_TC_FIELDS_METHOD, REMOVE_TC_FIELDS_METHOD_DESC);
      }

      super.visitInsn(opcode);
    }
  }

}
