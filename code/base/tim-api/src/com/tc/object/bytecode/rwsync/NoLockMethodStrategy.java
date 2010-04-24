/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode.rwsync;

import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodVisitor;

/**
 * A null "locking strategy" that leaves the method unaltered.
 */
public class NoLockMethodStrategy implements MethodStrategy {

  public MethodVisitor visitMethod(ClassVisitor cv, String ownerType, String outerType, String outerDesc, int access,
                                   String name, String desc, String signature, String[] exceptions) {
    return cv.visitMethod(access, name, desc, signature, exceptions);
  }

}
