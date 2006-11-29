/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.config;

import junit.framework.TestCase;

public class LockTypeTest extends TestCase {

  public void testLockTypeByName() {
    assertSame(ConfigLockLevel.WRITE, ConfigLockLevel.lockLevelByName(ConfigLockLevel.WRITE_NAME));
    assertSame(ConfigLockLevel.READ, ConfigLockLevel.lockLevelByName(ConfigLockLevel.READ_NAME));
  }

}
