/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode.rwsync;

import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;

public class AddOrRemoveSynchronizedKeywordMethodStrategy implements MethodStrategy, Opcodes {

  public static MethodStrategy addSynchronized(MethodStrategy strategy) {
    return new AddOrRemoveSynchronizedKeywordMethodStrategy(true, strategy);
  }

  public static MethodStrategy removeSynchronized(MethodStrategy strategy) {
    return new AddOrRemoveSynchronizedKeywordMethodStrategy(false, strategy);
  }

  private final boolean        addSynchronized;
  private final MethodStrategy delegate;

  private AddOrRemoveSynchronizedKeywordMethodStrategy(boolean addSynchronized, MethodStrategy delegateMethodStrategy) {
    super();
    this.addSynchronized = addSynchronized;
    this.delegate = delegateMethodStrategy;
  }

  public MethodVisitor visitMethod(ClassVisitor cv, String ownerType, String outerType, String outerDesc, int access,
                                   String name, String desc, String signature, String[] exceptions) {

    final int newAccess;
    if (this.addSynchronized) {
      newAccess = access | ACC_SYNCHRONIZED;
    } else {
      newAccess = access & ~ACC_SYNCHRONIZED;
    }
    return delegate.visitMethod(cv, ownerType, outerType, outerDesc, newAccess, name, desc, signature, exceptions);
  }

}
