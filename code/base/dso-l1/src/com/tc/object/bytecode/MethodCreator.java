/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassVisitor;

/**
 * Interface for adding in a class level object with responisbilites for creating support methods
 */
public interface MethodCreator {
  public void createMethods(ClassVisitor classVisitor);
}
