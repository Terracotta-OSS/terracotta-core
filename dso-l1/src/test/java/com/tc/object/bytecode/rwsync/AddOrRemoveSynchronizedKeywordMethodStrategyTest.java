/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode.rwsync;

import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;

import junit.framework.TestCase;

public class AddOrRemoveSynchronizedKeywordMethodStrategyTest extends TestCase implements Opcodes {

  private MockStrategy mockStrategy;

  protected void setUp() throws Exception {
    super.setUp();
    mockStrategy = new MockStrategy();
  }

  public void testAdd() throws Exception {
    final MethodStrategy strategy = AddOrRemoveSynchronizedKeywordMethodStrategy.addSynchronized(mockStrategy);
    strategy.visitMethod(null, null, null, null, ACC_PUBLIC + ACC_STATIC, null, null, null, null);
    assertEquals(ACC_PUBLIC + ACC_STATIC + ACC_SYNCHRONIZED, mockStrategy.getAccess());
  }

  public void testAddNotNecessary() throws Exception {
    final MethodStrategy strategy = AddOrRemoveSynchronizedKeywordMethodStrategy.addSynchronized(mockStrategy);
    strategy.visitMethod(null, null, null, null, ACC_PUBLIC + ACC_STATIC + ACC_SYNCHRONIZED, null, null, null, null);
    assertEquals(ACC_PUBLIC + ACC_STATIC + ACC_SYNCHRONIZED, mockStrategy.getAccess());
  }

  public void testRemoveSynchronized() throws Exception {
    final MethodStrategy strategy = AddOrRemoveSynchronizedKeywordMethodStrategy.removeSynchronized(mockStrategy);
    strategy.visitMethod(null, null, null, null, ACC_PROTECTED + ACC_SYNCHRONIZED, null, null, null, null);
    assertEquals(ACC_PROTECTED, mockStrategy.getAccess());
  }

  public void testRemoveNotNecessary() throws Exception {
    final MethodStrategy strategy = AddOrRemoveSynchronizedKeywordMethodStrategy.removeSynchronized(mockStrategy);
    strategy.visitMethod(null, null, null, null, ACC_PROTECTED, null, null, null, null);
    assertEquals(ACC_PROTECTED, mockStrategy.getAccess());
  }

  private static final class MockStrategy implements MethodStrategy {

    private int access;

    public MethodVisitor visitMethod(ClassVisitor cv, String ownerType, String outerType, String outerDesc,
                                     int accessParam, String name, String desc, String signature, String[] exceptions) {
      this.access = accessParam;
      return null;
    }

    private int getAccess() {
      return this.access;
    }
  }

}
