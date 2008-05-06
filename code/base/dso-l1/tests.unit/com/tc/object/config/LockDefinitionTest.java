/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config;

import junit.framework.TestCase;

/**
 * Unit test for LockDefinition
 */
public class LockDefinitionTest extends TestCase {

  private String          lockName;
  private ConfigLockLevel lockType;
  private LockDefinition  ld1;
  private LockDefinition  ld2;
  private LockDefinition  ld3;
  private LockDefinition  ld4;
  private LockDefinition  autolock;
  private String          ld3LockName;
  private ConfigLockLevel ld3LockType;

  public void setUp() throws Exception {
    lockName = "myLockName";
    lockType = ConfigLockLevel.WRITE;
    ld1 = new LockDefinitionImpl(lockName, lockType);
    ld2 = new LockDefinitionImpl(lockName, lockType);
    ld3LockName = "ld3LockName";
    ld3LockType = ConfigLockLevel.READ;
    ld3 = new LockDefinitionImpl(ld3LockName, ld3LockType);
    autolock = new LockDefinitionImpl(LockDefinition.TC_AUTOLOCK_NAME, lockType);
    ld4 = new LockDefinitionImpl(lockName, ConfigLockLevel.SYNCHRONOUS_WRITE);
  }

  public void testReadBeforeCommit() {
    try {
      ld1.getLockName();
      fail("Expected IllegalStateException.");
    } catch (IllegalStateException e) {
      // expected
    }
    try {
      ld1.getLockLevel();
      fail("Expected IllegalStateException.");
    } catch (IllegalStateException e) {
      // expected
    }

    try {
      ld1.getLockName();
      fail("Expected IllegalStateException.");
    } catch (IllegalStateException e) {
      // expected
    }
  }

  public void testWriteAfterCommit() {
    ld1.commit();
    ld4.commit();

    try {
      ld1.setLockName("ldkfaj;kdjf");
      fail("Expected IllegalStateException.");
    } catch (IllegalStateException e) {
      // expected.
    }

    try {
      ld1.setLockLevel(null);
      fail("Expected IllegalStateException.");
    } catch (IllegalStateException e) {
      // expected
    }

    try {
      ld4.setLockName("ldkfaj;kdjf");
      fail("Expected IllegalStateException.");
    } catch (IllegalStateException e) {
      // expected.
    }

    try {
      ld4.setLockLevel(null);
      fail("Expected IllegalStateException.");
    } catch (IllegalStateException e) {
      // expected
    }
  }

  public void testEquals() throws Exception {
    assertEquals(ld1, ld2);
    assertFalse(ld1.equals(ld3));
    assertFalse(ld1.equals(ld4));
  }

  public void testHashCode() throws Exception {
    assertEquals(ld1.hashCode(), ld2.hashCode());
  }

  public void testIsAutolock() throws Exception {
    autolock.commit();
    ld1.commit();
    ld2.commit();
    ld3.commit();
    ld4.commit();
    assertTrue(autolock.isAutolock());
    assertFalse(ld1.isAutolock());
    assertFalse(ld2.isAutolock());
    assertFalse(ld3.isAutolock());
    assertFalse(ld4.isAutolock());
  }
}