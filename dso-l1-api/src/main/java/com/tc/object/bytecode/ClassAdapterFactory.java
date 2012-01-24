/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;

/**
 * A factory for class adapters
 */
public interface ClassAdapterFactory {

  /**
   * Create an adapter
   * @param visitor ASM class visitor
   * @param loader Class loader to use
   * @return Adapter that can modify classes
   */
  ClassAdapter create(ClassVisitor visitor, ClassLoader loader);

}
