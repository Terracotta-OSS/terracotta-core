/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.mockl2.test;

public interface MockPlatformListener {
  
   void logicalInvoke(Object object, String methodName, Object[] params);

}
