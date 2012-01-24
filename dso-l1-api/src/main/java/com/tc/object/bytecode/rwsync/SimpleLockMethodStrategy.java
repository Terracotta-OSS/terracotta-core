/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package com.tc.object.bytecode.rwsync;

import static com.tc.object.bytecode.rwsync.LockNames.LOCK_METHOD_DESC;
import static com.tc.object.bytecode.rwsync.LockNames.LOCK_METHOD_NAME;
import static com.tc.object.bytecode.rwsync.LockNames.UNLOCK_METHOD_DESC;
import static com.tc.object.bytecode.rwsync.LockNames.UNLOCK_METHOD_NAME;

import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Type;
import com.tc.object.bytecode.ByteCodeUtil;

import java.lang.reflect.Modifier;

/**
 * Instrument a method by renaming it and then calling it inside a wrapper method that takes a
 * {@link java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock ReadLock} or
 * {@link java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock WriteLock}.
 * <p>
 * MethodStrategy instances are kept in statically initialized tables and reused, so implementations should not contain
 * any mutable state.
 */
public class SimpleLockMethodStrategy extends LockingMethodStrategy {

  /**
   * @param lockField the name of the field that is the lock. If the method owner is an inner class, the read lock field
   *        is assumed to belong to the outer class.
   */
  public SimpleLockMethodStrategy(LockType lockType) {
    super(lockType);
  }

  public MethodVisitor visitMethod(ClassVisitor cv, String ownerType, String outerType, String outerDesc, int access,
                                   String name, String desc, String signature, String[] exceptions) {
    // create wrapper method
    MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
    mv = new LockWrapAdapter(mv, ownerType, outerType, outerDesc, access, name, desc);
    mv.visitCode();
    mv.visitMaxs(0, 0);
    mv.visitEnd();

    // rename original method, and set private and synthetic attributes
    return cv.visitMethod(getWrappedAttributes(access), getWrappedName(name), desc, signature, exceptions);
  }

  private class LockWrapAdapter extends MethodAdapter {

    private final String ownerType;
    private final String outerType;
    private final String outerDesc;
    private final int    access;
    private final String name;
    private final String desc;

    public LockWrapAdapter(MethodVisitor mv, String ownerType, String outerType, String outerDesc, int access,
                           String name, String desc) {
      super(mv);
      this.ownerType = ownerType;
      this.outerType = outerType;
      this.outerDesc = outerDesc;
      this.access = access;
      this.name = name;
      this.desc = desc;
    }

    @Override
    public void visitCode() {
      // get opcodes for dealing with return type of method
      Type type = Type.getReturnType(desc);
      boolean isVoid = type.getSort() == Type.VOID;
      int opStore = type.getOpcode(ISTORE);
      int opLoad = type.getOpcode(ILOAD);
      int opReturn = type.getOpcode(IRETURN);
      // local variable indices
      int lviCaughtException = ByteCodeUtil.getFirstLocalVariableOffset(access, desc);
      int lviReturn = lviCaughtException + 1;

      super.visitCode();
      Label lblTryStart = new Label();
      Label lblTryEnd = new Label();
      Label lblTryHandler = new Label();
      visitTryCatchBlock(lblTryStart, lblTryEnd, lblTryHandler, null);
      visitVarInsn(ALOAD, 0); // 0 == this
      if (outerType != null) {
        visitFieldInsn(GETFIELD, ownerType, THIS$0, outerDesc);
        visitFieldInsn(GETFIELD, outerType, lockField, lockTypeDesc);
      } else {
        visitFieldInsn(GETFIELD, ownerType, lockField, lockTypeDesc);
      }
      visitMethodInsn(INVOKEVIRTUAL, lockTypeName, LOCK_METHOD_NAME, LOCK_METHOD_DESC);
      visitLabel(lblTryStart);
      callRenamedMethod();
      if (!isVoid) {
        visitVarInsn(opStore, lviReturn);
      }
      visitLabel(lblTryEnd);
      visitVarInsn(ALOAD, 0);
      if (outerType != null) {
        visitFieldInsn(GETFIELD, ownerType, THIS$0, outerDesc);
        visitFieldInsn(GETFIELD, outerType, lockField, lockTypeDesc);
      } else {
        visitFieldInsn(GETFIELD, ownerType, lockField, lockTypeDesc);
      }
      visitMethodInsn(INVOKEVIRTUAL, lockTypeName, UNLOCK_METHOD_NAME, UNLOCK_METHOD_DESC);
      if (!isVoid) {
        visitVarInsn(opLoad, lviReturn);
      }
      visitInsn(opReturn);
      visitLabel(lblTryHandler);
      visitVarInsn(ASTORE, lviCaughtException);
      visitVarInsn(ALOAD, 0);
      if (outerType != null) {
        visitFieldInsn(GETFIELD, ownerType, THIS$0, outerDesc);
        visitFieldInsn(GETFIELD, outerType, lockField, lockTypeDesc);
      } else {
        visitFieldInsn(GETFIELD, ownerType, lockField, lockTypeDesc);
      }
      visitMethodInsn(INVOKEVIRTUAL, lockTypeName, UNLOCK_METHOD_NAME, UNLOCK_METHOD_DESC);
      visitVarInsn(ALOAD, lviCaughtException);
      visitInsn(ATHROW);
    }

    /**
     * Call the renamed version of a method, using the same parameters and return type as the original.
     */
    private void callRenamedMethod() {
      ByteCodeUtil.prepareStackForMethodCall(access, desc, mv);
      int invokeOp = Modifier.isStatic(access) ? INVOKESTATIC : INVOKESPECIAL;
      visitMethodInsn(invokeOp, ownerType, getWrappedName(name), desc);
    }

  }

}
