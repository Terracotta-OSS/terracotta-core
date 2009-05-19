/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package com.tc.object.bytecode.rwsync;

import static com.tc.object.bytecode.rwsync.LockNames.READLOCK_DESC;
import static com.tc.object.bytecode.rwsync.LockNames.READLOCK_METHOD_DESC;
import static com.tc.object.bytecode.rwsync.LockNames.READLOCK_METHOD_NAME;
import static com.tc.object.bytecode.rwsync.LockNames.READLOCK_NAME;
import static com.tc.object.bytecode.rwsync.LockNames.RWLOCK_DESC;
import static com.tc.object.bytecode.rwsync.LockNames.RWLOCK_NAME;
import static com.tc.object.bytecode.rwsync.LockNames.RWLOCK_TYPE;
import static com.tc.object.bytecode.rwsync.LockNames.WRITELOCK_DESC;
import static com.tc.object.bytecode.rwsync.LockNames.WRITELOCK_METHOD_DESC;
import static com.tc.object.bytecode.rwsync.LockNames.WRITELOCK_METHOD_NAME;
import static com.tc.object.bytecode.rwsync.LockNames.WRITELOCK_NAME;

import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodVisitor;
import com.tc.asm.commons.AdviceAdapter;

/**
 * A strategy to process constructors of classes containing locking fields. In general the class adapter of such a class
 * will extend {@link LockingClassAdapter}, and will declare a map of MethodId to MethodStrategy. Such class adapters
 * must call {@link LockingClassAdapter#addLockFields()} to add the fields to the class, and must map constructor
 * methods to this strategy.
 */
public class InitMethodStrategy implements MethodStrategy {

  public MethodVisitor visitMethod(ClassVisitor cv, String ownerType, String outerType, String outerDesc, int access,
                                   String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
    mv = new InitMethodAdapter(mv, ownerType, access, name, desc);
    return mv;
  }

  /**
   * Adapter to modify AbstractPersistentCollection constructor. Initializes the lock fields added to the class.
   * 
   * @see LockingClassAdapter#addLockFields()
   */
  private class InitMethodAdapter extends AdviceAdapter {

    private final String ownerType;

    protected InitMethodAdapter(MethodVisitor mv, String ownerType, int access, String name, String desc) {
      super(mv, access, name, desc);
      this.ownerType = ownerType;
    }

    @Override
    protected void onMethodEnter() {
      super.onMethodEnter();

      // __tc_readWriteLock = new ReentrantReadWriteLock();
      visitVarInsn(ALOAD, 0);
      visitTypeInsn(NEW, RWLOCK_TYPE);
      visitInsn(DUP);
      visitMethodInsn(INVOKESPECIAL, RWLOCK_TYPE, "<init>", "()V");
      visitFieldInsn(PUTFIELD, ownerType, RWLOCK_NAME, RWLOCK_DESC);

      // __tc_readLock = __tc_readWriteLock.readLock();
      visitVarInsn(ALOAD, 0);
      visitVarInsn(ALOAD, 0);
      visitFieldInsn(GETFIELD, ownerType, RWLOCK_NAME, RWLOCK_DESC);
      visitMethodInsn(INVOKEVIRTUAL, RWLOCK_TYPE, READLOCK_METHOD_NAME, READLOCK_METHOD_DESC);
      visitFieldInsn(PUTFIELD, ownerType, READLOCK_NAME, READLOCK_DESC);

      // __tc_writeLock = __tc_readWriteLock.writeLock();
      visitVarInsn(ALOAD, 0);
      visitVarInsn(ALOAD, 0);
      visitFieldInsn(GETFIELD, ownerType, RWLOCK_NAME, RWLOCK_DESC);
      visitMethodInsn(INVOKEVIRTUAL, RWLOCK_TYPE, WRITELOCK_METHOD_NAME, WRITELOCK_METHOD_DESC);
      visitFieldInsn(PUTFIELD, ownerType, WRITELOCK_NAME, WRITELOCK_DESC);
    }
  }

}