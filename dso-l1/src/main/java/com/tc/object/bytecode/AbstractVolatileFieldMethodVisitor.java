/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Type;
import com.tc.object.locks.LockLevel;

public abstract class AbstractVolatileFieldMethodVisitor extends MaxLocalVarStoreDetectingMethodAdapter {
  public AbstractVolatileFieldMethodVisitor(MethodVisitor mv) {
    super(mv);
  }

  public abstract void modifyVolatileValue(int tcobjectVarStore, Label labelCommitVolatile);

  public void doVolatileBeginCommitLogic(String fieldName, int nextFreeLocalVarStore) {
    int tcobject_var_store = nextFreeLocalVarStore + 1;
    int exception_var_store = tcobject_var_store + 1;

    // load the reference to the currently executing object instance
    mv.visitVarInsn(ALOAD, 0);

    // look up the TCObject from the TC manager that corresponds
    // to the current object instance
    mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(ManagerUtil.class), "lookupExistingOrNull",
                       "(Ljava/lang/Object;)Lcom/tc/object/TCObjectExternal;");

    // store the TCObject in the appropriate local variable slot
    mv.visitVarInsn(ASTORE, tcobject_var_store);

    // the labels that are used in the code ahead
    Label label_begin_volatile = new Label();
    Label label_exception = new Label();
    Label label_commit_volatile = new Label();
    Label label_tcobject_null = new Label();

    // setup the try catch block
    mv.visitTryCatchBlock(label_begin_volatile, label_exception, label_exception, null);

    mv.visitVarInsn(ALOAD, 0);

    // check if the TCObject instance is null, and jump over the state
    // modification code that follows
    mv.visitVarInsn(ALOAD, tcobject_var_store);
    mv.visitJumpInsn(IFNULL, label_tcobject_null);

    // // call the begin volatile method on the managed object
    mv.visitLabel(label_begin_volatile);
    mv.visitVarInsn(ALOAD, tcobject_var_store);
    mv.visitLdcInsn(fieldName);
    mv.visitInsn(LockLevel.WRITE.toInt());
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "beginVolatile",
                       "(Lcom/tc/object/TCObjectExternal;Ljava/lang/String;I)V");

    modifyVolatileValue(tcobject_var_store, label_commit_volatile);

    mv.visitJumpInsn(GOTO, label_commit_volatile);

    // ensure that commit is called in case an exception occurs
    mv.visitLabel(label_exception);
    mv.visitVarInsn(ASTORE, exception_var_store);
    mv.visitVarInsn(ALOAD, tcobject_var_store);
    mv.visitLdcInsn(fieldName);
    mv.visitInsn(LockLevel.WRITE.toInt());
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "commitVolatile",
                       "(Lcom/tc/object/TCObjectExternal;Ljava/lang/String;I)V");
    mv.visitVarInsn(ALOAD, exception_var_store);
    mv.visitInsn(ATHROW);

    // call the commit volatile on the managed object
    mv.visitLabel(label_commit_volatile);
    mv.visitVarInsn(ALOAD, tcobject_var_store);
    mv.visitLdcInsn(fieldName);
    mv.visitInsn(LockLevel.WRITE.toInt());
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "commitVolatile",
                       "(Lcom/tc/object/TCObjectExternal;Ljava/lang/String;I)V");

    mv.visitLabel(label_tcobject_null);
  }
}
