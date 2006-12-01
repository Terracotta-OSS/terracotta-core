/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Type;

public class LinkedHashMapMethodAdapter extends LogicalMethodAdapter {

  public LinkedHashMapMethodAdapter() {
    super();
  }

  protected void createWrapperMethod(ClassVisitor classVisitor) {
    addGetMethodWrapper(classVisitor);
  }

  private void addGetMethodWrapper(ClassVisitor classVisitor) {
    MethodVisitor mv = classVisitor.visitMethod(getWrapperAccess(), getMethodName(), getDescription(), getSignature(),
                                                getExceptions());
    Type[] params = Type.getArgumentTypes(getDescription());

    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, getOwnerSlashes(), "accessOrder", "Z");
    Label notManaged = new Label();
    mv.visitJumpInsn(IFEQ, notManaged);
    mv.visitVarInsn(ALOAD, 0);
    mv
        .visitMethodInsn(INVOKEVIRTUAL, getOwnerSlashes(), ClassAdapterBase.MANAGED_METHOD,
                         "()Lcom/tc/object/TCObject;");
    mv.visitJumpInsn(IFNULL, notManaged);
    addCheckWriteAccessInstrumentedCode(mv, false);
    ByteCodeUtil.pushThis(mv);
    mv.visitLdcInsn(getMethodName() + getDescription());
    ByteCodeUtil.createParametersToArrayByteCode(mv, params);
    getManagerHelper().callManagerMethod("logicalInvoke", mv);
    mv.visitLabel(notManaged);
    addInvokeOriginalMethodInstrumentedCode(mv, params);
    mv.visitInsn(ARETURN);
    mv.visitMaxs(0, 0);
  }

  private void addInvokeOriginalMethodInstrumentedCode(MethodVisitor mv, Type[] params) {
    ByteCodeUtil.pushThis(mv);
    for (int i = 0; i < params.length; i++) {
      mv.visitVarInsn(params[i].getOpcode(ILOAD), i + 1);
    }

    mv.visitMethodInsn(INVOKESPECIAL, getOwnerSlashes(), getNewName(), getDescription());
  }

}
