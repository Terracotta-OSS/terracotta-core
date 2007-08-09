/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.MethodVisitor;

public interface ManagerHelper {
  /**
   * Call the given Manager method.
   *
   * @param methodName The method to call
   * @param mv CodeVisitor for the current method that wants to make the Manager call
   */
  public void callManagerMethod(String methodName, MethodVisitor mv);

}