/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.mockl2.test;

import com.tc.object.LogicalOperation;

public interface MockPlatformListener {
  
   void logicalInvoke(Object object, LogicalOperation method, Object[] params);

}
