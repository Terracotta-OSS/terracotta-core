/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.MethodVisitor;

/**
 * Helper that can add instructions to call the Manager
 */
public interface ManagerHelper {
  
  /**
   * Add instructions to call the given Manager method.
   *
   * @param methodName The method to call
   * @param mv CodeVisitor for the current method that wants to make the Manager call
   */
  public void callManagerMethod(String methodName, MethodVisitor mv);

}