/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.transform;

import com.tc.asm.MethodVisitor;

import com.tc.aspectwerkz.transform.inlining.spi.AspectModel;

/**
 * Generic interface for the code generation compilers used in the AspectWerkz weaver.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public interface JoinPointCompiler {

  /**
   * Compiles the code and returns the bytecode for the compiled code.
   *
   * @return the bytecode for the compiled code
   */
  byte[] compile();

  /**
   * @return the caller class name (slash)
   */
  String getCallerClassName();

  /**
   * @return the callee class name (slash)
   */
  String getCalleeClassName();

  /**
   * @return the caller class signature (slash and L..;)
   */
  String getCallerClassSignature();

  /**
   * @return the callee class signature (slash and L..;)
   */
  String getCalleeClassSignature();

  /**
   * @return the main compiled artifact (i.e. jit joinpoint) signature (slash)
   */
  String getJoinPointClassName();

  /**
   * Create an Object[] array on stack at the given index that host the join point arguments
   *
   * @param cv
   * @param stackFreeIndex
   */
  void createArgumentArrayAt(final MethodVisitor cv, final int stackFreeIndex);

  /**
   * @return all the aspect model interacting with this compiler
   */
  AspectModel[] getAspectModels();


}
