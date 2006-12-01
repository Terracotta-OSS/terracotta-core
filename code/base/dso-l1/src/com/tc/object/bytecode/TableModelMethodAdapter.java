/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;

/**
 *
 */
public class TableModelMethodAdapter extends AbstractMethodAdapter {

  public static final String METHOD = "fireTableChanged(Ljavax/swing/event/TableModelEvent;)V";

  public MethodVisitor adapt(ClassVisitor classVisitor) {
    createNewUpdateMethod(classVisitor);
    createNewDistributedUpdateMethod(classVisitor);
    return classVisitor.visitMethod(access, getNewName(), description, signature, exceptions);
  }

  private void createNewDistributedUpdateMethod(ClassVisitor classVisitor) {
    String desc = "(IIII)V";

    MethodVisitor ca = classVisitor.visitMethod(ACC_PUBLIC, getNewName(), desc, null, null);
    Label l0 = new Label();
    ca.visitLabel(l0);
    ca.visitVarInsn(ALOAD, 0);
    ca.visitLdcInsn(getNewName() + desc);
    Label l1 = new Label();
    ca.visitLabel(l1);
    ca.visitInsn(ICONST_4);
    ca.visitTypeInsn(ANEWARRAY, "java/lang/Object");
    ca.visitInsn(DUP);
    ca.visitInsn(ICONST_0);
    ca.visitTypeInsn(NEW, "java/lang/Integer");
    ca.visitInsn(DUP);
    ca.visitVarInsn(ILOAD, 1);
    ca.visitMethodInsn(INVOKESPECIAL, "java/lang/Integer", "<init>", "(I)V");
    ca.visitInsn(AASTORE);
    ca.visitInsn(DUP);
    ca.visitInsn(ICONST_1);
    ca.visitTypeInsn(NEW, "java/lang/Integer");
    ca.visitInsn(DUP);
    ca.visitVarInsn(ILOAD, 2);
    ca.visitMethodInsn(INVOKESPECIAL, "java/lang/Integer", "<init>", "(I)V");
    ca.visitInsn(AASTORE);
    ca.visitInsn(DUP);
    ca.visitInsn(ICONST_2);
    Label l2 = new Label();
    ca.visitLabel(l2);
    ca.visitTypeInsn(NEW, "java/lang/Integer");
    ca.visitInsn(DUP);
    ca.visitVarInsn(ILOAD, 3);
    ca.visitMethodInsn(INVOKESPECIAL, "java/lang/Integer", "<init>", "(I)V");
    ca.visitInsn(AASTORE);
    ca.visitInsn(DUP);
    ca.visitInsn(ICONST_3);
    ca.visitTypeInsn(NEW, "java/lang/Integer");
    ca.visitInsn(DUP);
    ca.visitVarInsn(ILOAD, 4);
    ca.visitMethodInsn(INVOKESPECIAL, "java/lang/Integer", "<init>", "(I)V");
    Label l3 = new Label();
    ca.visitLabel(l3);
    ca.visitInsn(AASTORE);

    managerHelper.callManagerMethod("distributedMethodCall", ca);

    Label l4 = new Label();
    ca.visitLabel(l4);
    ca.visitVarInsn(ALOAD, 0);
    ca.visitTypeInsn(NEW, "javax/swing/event/TableModelEvent");
    ca.visitInsn(DUP);
    ca.visitVarInsn(ALOAD, 0);
    ca.visitVarInsn(ILOAD, 1);
    ca.visitVarInsn(ILOAD, 2);
    ca.visitVarInsn(ILOAD, 3);
    ca.visitVarInsn(ILOAD, 4);
    ca.visitMethodInsn(INVOKESPECIAL, "javax/swing/event/TableModelEvent", "<init>",
                       "(Ljavax/swing/table/TableModel;IIII)V");
    ca.visitMethodInsn(INVOKESPECIAL, ownerDots.replace('.', '/'), getNewName(), "(Ljavax/swing/event/TableModelEvent;)V");
    Label l5 = new Label();
    ca.visitLabel(l5);
    ca.visitInsn(RETURN);
    Label l6 = new Label();
    ca.visitLabel(l6);

    ca.visitMaxs(9, 5);
  }

  private void createNewUpdateMethod(ClassVisitor classVisitor) {

    Label l0 = new Label();
    MethodVisitor methodVisitor = classVisitor.visitMethod(access, methodName, description, signature, exceptions);
    methodVisitor.visitLabel(l0);
    methodVisitor.visitVarInsn(ALOAD, 0);
    methodVisitor.visitVarInsn(ALOAD, 1);
    methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "javax/swing/event/TableModelEvent", "getFirstRow", "()I");
    methodVisitor.visitVarInsn(ALOAD, 1);
    methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "javax/swing/event/TableModelEvent", "getLastRow", "()I");
    methodVisitor.visitVarInsn(ALOAD, 1);
    methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "javax/swing/event/TableModelEvent", "getColumn", "()I");
    methodVisitor.visitVarInsn(ALOAD, 1);
    methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "javax/swing/event/TableModelEvent", "getType", "()I");
    methodVisitor.visitMethodInsn(INVOKESPECIAL, ownerDots.replace('.', '/'), getNewName(), "(IIII)V");
    Label l1 = new Label();
    methodVisitor.visitLabel(l1);
    methodVisitor.visitInsn(RETURN);
    Label l2 = new Label();
    methodVisitor.visitLabel(l2);

    methodVisitor.visitMaxs(5, 2);
  }

  public boolean doesOriginalNeedAdapting() {
    return false;
  }

  public String getNewName() {
    return ByteCodeUtil.TC_METHOD_PREFIX + methodName;
  }
}
