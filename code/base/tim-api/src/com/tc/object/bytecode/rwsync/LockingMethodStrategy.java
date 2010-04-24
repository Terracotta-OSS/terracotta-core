/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package com.tc.object.bytecode.rwsync;

import static com.tc.object.bytecode.rwsync.LockNames.READLOCK_DESC;
import static com.tc.object.bytecode.rwsync.LockNames.READLOCK_NAME;
import static com.tc.object.bytecode.rwsync.LockNames.READLOCK_TYPE;
import static com.tc.object.bytecode.rwsync.LockNames.WRITELOCK_DESC;
import static com.tc.object.bytecode.rwsync.LockNames.WRITELOCK_NAME;
import static com.tc.object.bytecode.rwsync.LockNames.WRITELOCK_TYPE;

import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ByteCodeUtil;

import java.lang.reflect.Modifier;

/**
 * Contains constants and utility methods convenient for method instrumentation strategies involving locking with
 * ReentrantReadWriteLocks
 */
public abstract class LockingMethodStrategy implements MethodStrategy, Opcodes {

  public static final String THIS$0 = "this$0";

  public enum LockType {
    READ, WRITE
  }

  protected final String lockField;
  protected final String lockTypeName;
  protected final String lockTypeDesc;

  /**
   * @param lockField the name of the field that is the lock. If the method owner is an inner class, the read lock field
   *        is assumed to belong to the outer class.
   */
  public LockingMethodStrategy(LockType lockType) {
    if (lockType == LockType.READ) {
      lockField = READLOCK_NAME;
      lockTypeName = READLOCK_TYPE;
      lockTypeDesc = READLOCK_DESC;
    } else {
      lockField = WRITELOCK_NAME;
      lockTypeName = WRITELOCK_TYPE;
      lockTypeDesc = WRITELOCK_DESC;
    }
  }

  protected static String getWrappedName(String name) {
    // The method name can't start with "__tc_", or it will not get further DSO instrumentation!
    return "__wrapped_" + name;
  }

  /**
   * Set access to private and add the "synthetic" attribute. This creates access attributes appropriate for a wrapped
   * method.
   */
  protected static int getWrappedAttributes(int attr) {
    return (attr & ~(ACC_PROTECTED | ACC_PUBLIC)) | ACC_SYNTHETIC | ACC_PRIVATE;
  }

  /**
   * Call the renamed version of a method, using the same parameters and return type as the original.
   * <p>
   * TODO: breaks when used with LocalVariablesSorter
   * 
   * @param owner the slash-and-dollar-delimited name of the type whose method is being called
   */
  public static void callRenamedMethod(String owner, int access, String name, String desc, MethodVisitor mv) {
    ByteCodeUtil.prepareStackForMethodCall(access, desc, mv);
    int invokeOp = Modifier.isStatic(access) ? INVOKESTATIC : INVOKESPECIAL;
    mv.visitMethodInsn(invokeOp, owner, getWrappedName(name), desc);
  }

}
