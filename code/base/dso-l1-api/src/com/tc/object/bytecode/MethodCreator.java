/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassVisitor;

/**
 * Interface for adding in a class level object with responisbilites for creating support methods
 */
public interface MethodCreator {
  public void createMethods(ClassVisitor classVisitor);
}
