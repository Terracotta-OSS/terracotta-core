/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.lockmanager.api;

import junit.framework.TestCase;

/**
 * TODO Nov 18, 2004:
 */
public class LockIDTest extends TestCase {
  public void tests() throws Exception {
    assertTrue(LockLevel.isWrite(LockLevel.WRITE));
    assertFalse(LockLevel.isRead(LockLevel.WRITE));

    assertTrue(LockLevel.isWrite(LockLevel.SYNCHRONOUS_WRITE));
    assertFalse(LockLevel.isRead(LockLevel.SYNCHRONOUS_WRITE));

    assertFalse(LockLevel.isWrite(LockLevel.READ));
    assertTrue(LockLevel.isRead(LockLevel.READ));

  }
}