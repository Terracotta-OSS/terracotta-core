/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassVisitor;

/**
 * Interface for adding in a class level object with responsibilities for creating support methods
 */
public interface MethodCreator {

  /**
   * Create methods in the class
   * @param classVisitor Class visitor
   */
  public void createMethods(ClassVisitor classVisitor);
}
