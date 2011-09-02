/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.loaders;

import com.tc.object.logging.NullRuntimeLogger;

import junit.framework.TestCase;

public class StandardClassProviderTest extends TestCase {
  
  private StandardClassProvider scp;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    scp = new StandardClassProvider(new NullRuntimeLogger());
  }

  @Override
  protected void tearDown() throws Exception {
    scp = null;
    super.tearDown();
  }

  /**
   * Verify that accessing a non-registered loader causes an exception to be thrown.
   * See CDV-1183.
   */
  public void testUnregisteredLoader() throws Exception {
    NamedClassLoader loader = (NamedClassLoader) new NullLoader();
    try {
      loader.__tc_getClassLoaderName();
    } catch (Exception e) {
      // exact text may vary, but it better mention the possibility of a missing TIM.
      assertTrue(e.getMessage().contains("Terracotta Integration Module"));
      assertTrue(e.getMessage().contains("http://www.terracotta.org/tim-error"));
      return;
    }
    fail("Expected an IllegalStateException but did not receive one");
  }

  private class NullLoader extends ClassLoader {
    // do nothing
  }

  /**
   * Register a loader without an app-group
   */
  public void testRegisterNonAppGroupLoader() throws Exception {
    NamedClassLoader loader = new MockClassLoader("loader", this.getClass().getClassLoader());
    scp.registerNamedLoader(loader, null);
    ClassLoader cl = scp.getClassLoader(new LoaderDescription(null, "loader"));
    assertEquals(loader, cl);
    assertEquals(null, scp.checkIntegrity());
  }
  
  /**
   * Register a loader with an app-group
   */
  public void testRegisterAppGroupLoader() throws Exception {
    NamedClassLoader loader = new MockClassLoader("loader", this.getClass().getClassLoader());
    scp.registerNamedLoader(loader, null);
    ClassLoader cl = scp.getClassLoader(new LoaderDescription("ag1", "loader"));
    assertEquals(loader, cl);
    assertEquals(null, scp.checkIntegrity());
  }
  
  /**
   * Check that loaders in the same app group can be substituted
   */
  public void testAppGroupSubstitution() throws Exception {
    NamedClassLoader loader = new MockClassLoader("loaderA", this.getClass().getClassLoader());
    scp.registerNamedLoader(loader, "ag1");
    ClassLoader cl = scp.getClassLoader(new LoaderDescription("ag1", "loaderB"));
    assertEquals(loader, cl);
    assertEquals(null, scp.checkIntegrity());
  }
  
  /**
   * Check that app group substitution works correctly when there are two app groups defined
   */
  public void testTwoAppGroupSubstitution() throws Exception {
    NamedClassLoader loaderA = new MockClassLoader("loaderA", this.getClass().getClassLoader());
    scp.registerNamedLoader(loaderA, "ag1");
    NamedClassLoader loaderB = new MockClassLoader("loaderB", this.getClass().getClassLoader());
    scp.registerNamedLoader(loaderB, "ag2");
    ClassLoader loaderAX = scp.getClassLoader(new LoaderDescription("ag1", "loaderAX"));
    assertEquals(loaderA, loaderAX);
    ClassLoader loaderBX = scp.getClassLoader(new LoaderDescription("ag2", "loaderBX"));
    assertEquals(loaderB, loaderBX);
    assertEquals(null, scp.checkIntegrity());
  }
  
  /**
   * Register a loader that has a unique child.  Per the spec:
   * <pre>
   * if (the DNA specifies an app-group, 
   *     and there is a loader that exactly matches both the app-group and the name, 
   *     and there is exactly one loader registered in that app-group that is a *child* of the exact match) { 
   *   use the child; 
   * } 
   * </pre>
   */
  public void testRegisterAppGroupLoaderWithUniqueChild() throws Exception {
    NamedClassLoader parent = new MockClassLoader("parent", this.getClass().getClassLoader());
    scp.registerNamedLoader(parent, "ag1");
    ClassLoader parentX = scp.getClassLoader(new LoaderDescription("ag1", "parent"));
    assertEquals(parent, parentX);
    
    NamedClassLoader child = new MockClassLoader("child", parentX);
    scp.registerNamedLoader(child, "ag1");
    ClassLoader childX = scp.getClassLoader(new LoaderDescription("ag1", "child"));
    assertEquals(child, childX);
    assertEquals(null, scp.checkIntegrity());
    assertEquals(childX.getParent(), parentX);
    
    // parent now has one unique child, so if we ask for parent we will get child
    ClassLoader childY = scp.getClassLoader(new LoaderDescription("ag1", "parent"));
    assertEquals(child, childY);
    assertEquals(null, scp.checkIntegrity());
  }
  
  /**
   * Register a loader that has a two children.  Per the spec:
   * <pre>
   * if (the DNA specifies an app-group, 
   *     and there is a loader that exactly matches both the app-group and the name, 
   *     and there is exactly one loader registered in that app-group that is a *child* of the exact match) { 
   *   use the child; 
   * } 
   * </pre>
   */
  public void testRegisterAppGroupLoaderWithTwoChildren() throws Exception {
    NamedClassLoader parent = new MockClassLoader("parent", this.getClass().getClassLoader());
    scp.registerNamedLoader(parent, null);
    ClassLoader parentX = scp.getClassLoader(new LoaderDescription("ag1", "parent"));
    assertEquals(parent, parentX);
    
    NamedClassLoader child1 = new MockClassLoader("child", parentX);
    scp.registerNamedLoader(child1, null);
    ClassLoader childX = scp.getClassLoader(new LoaderDescription("ag1", "child"));
    assertEquals(child1, childX);
    assertEquals(null, scp.checkIntegrity());
    
    NamedClassLoader child2 = new MockClassLoader("child", parentX);
    scp.registerNamedLoader(child2, null);
    ClassLoader childY = scp.getClassLoader(new LoaderDescription("ag1", "child"));
    assertEquals(child2, childY);
    assertEquals(null, scp.checkIntegrity());
    
    // parent now has two children, so unique-child substitution rule does not apply
    ClassLoader parentY = scp.getClassLoader(new LoaderDescription("ag1", "parent"));
    assertEquals(parentX, parentY);
    assertEquals(null, scp.checkIntegrity());
  }
  
  /**
   * Verify that searching for a removed loader causes dangling weak references
   * to be cleaned up
   */
  public void testWeakReferenceCleanup() throws Exception {
    registerGCableLoader("loader", this.getClass().getClassLoader(), null);
    if (!releaseDeadLoaders()) {
      // couldn't get GC to happen, so give up
      System.err.println("testWeakReferenceCleanup was unable to force GC; test results inconclusive");
      return;
    }
    
    // Try to get the loader; doing so should cause the map entries to be cleaned up.
    boolean caught = false;
    try {
      scp.getClassLoader(new LoaderDescription(null, "loader"));
    } catch (IllegalArgumentException iae) {
      caught = true;
    }
    assertTrue(caught);
    assertFalse(scp.checkWeakReferences());
    assertEquals(null, scp.checkIntegrity());
  }
  
  /**
   * Verify that if a unique child is GC'ed, it is removed from
   * the maps and the child relationships are correctly updated
   */
  public void testLoaderWithUniqueChildRemoved() throws Exception {
    MockClassLoader parent = new MockClassLoader("parent", this.getClass().getClassLoader());
    scp.registerNamedLoader(parent, "ag1");
    registerGCableLoader("child", parent, "ag1");
    if (!releaseDeadLoaders()) {
      // couldn't get GC to happen, so give up
      System.err.println("testLoaderWithUniqueChildRemoved was unable to force GC; test results inconclusive");
      return;
    }
    
    // Try to get the parent loader; if the child still existed, it would be substituted.
    ClassLoader parentX = scp.getClassLoader(new LoaderDescription("ag1", "parent"));
    assertEquals(parent, parentX);
    assertFalse(scp.checkWeakReferences());
    assertEquals(null, scp.checkIntegrity());
  }
  
  /**
   * Verify that if a unique grandchild is GC'ed, it is removed from
   * the maps and the child relationships are correctly updated
   */
  public void testUniqueGrandChildRemoved() throws Exception {
    MockClassLoader grandparent = new MockClassLoader("grandparent", this.getClass().getClassLoader());
    scp.registerNamedLoader(grandparent, "ag1");
    MockClassLoader parent = new MockClassLoader("parent", grandparent);
    scp.registerNamedLoader(parent, "ag1");
    registerGCableLoader("child", parent, "ag1");
    if (!releaseDeadLoaders()) {
      // couldn't get GC to happen, so give up
      System.err.println("testUniqueGrandChildRemoved was unable to force GC; test results inconclusive");
      return;
    }
    
    // Try to get the parent loader; if the child still existed, it would be substituted.
    ClassLoader parentX = scp.getClassLoader(new LoaderDescription("ag1", "parent"));
    assertEquals(parent, parentX);
    
    // Grandparent now has a unique child (the "parent")
    ClassLoader parentY = scp.getClassLoader(new LoaderDescription("ag1", "grandparent"));
    assertEquals(parent, parentY);
    
    assertFalse(scp.checkWeakReferences());
    assertEquals(null, scp.checkIntegrity());
  }
  
  /**
   * Verify that if a loader has two children and one is GC'ed, it ends up
   * with the correct unique child relationship
   */
  public void testLoaderRemovalCreatesUniqueChild() throws Exception {
    MockClassLoader parent = new MockClassLoader("parent", this.getClass().getClassLoader());
    scp.registerNamedLoader(parent, "ag1");
    MockClassLoader child1 = new MockClassLoader("child1", parent);
    scp.registerNamedLoader(child1, "ag1");
    registerGCableLoader("child2", parent, "ag1");
    if (!releaseDeadLoaders()) {
      // couldn't get GC to happen, so give up
      System.err.println("testLoaderWithUniqueChildRemoved was unable to force GC; test results inconclusive");
      return;
    }
    
    // Try to get the parent loader; now there is a unique child, so it should be substituted.
    ClassLoader childX = scp.getClassLoader(new LoaderDescription("ag1", "parent"));
    assertEquals(child1, childX);
    assertEquals(null, scp.checkIntegrity());
    assertFalse(scp.checkWeakReferences());
  }
  
  /**
   * Register a loader in the specified app-group and then lose the 
   * reference to the loader, so that it can be GC'ed. 
   * This is a separate method in order to be able to ensure losing the reference.
   */
  private void registerGCableLoader(String name, ClassLoader parent, String appGroup) {
    MockClassLoader loader = new MockClassLoader(name, parent);
    scp.registerNamedLoader(loader, appGroup);
    loader = null;
  }
  
  
  /**
   * Attempt to release any unreferenced loaders, which should result in the
   * presence of a dangling weak reference (which should then be cleared the
   * next time the loader is looked up).
   * @return true if a dangling weak reference was successfully created
   */
  private boolean releaseDeadLoaders() {
    // Do our best to make GC happen. If it just won't go away, there's not much
    // more we can do with the test, but it's not really a failure as such.
    final int RETRIES = 5;
    for (int retry = 0; retry < RETRIES; ++retry) {
      System.gc();
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        fail("Thread was interrupted");
      }
      System.gc();
      if (scp.checkWeakReferences()) {
        // hurray! the GC happened
        return true;
      }
    }
    return false;
  }
  
  private static class MockClassLoader extends ClassLoader implements NamedClassLoader {
    
    private final String name;
    
    public MockClassLoader(String name, ClassLoader parent) {
      super(parent);
      this.name = name;
    }
    
    public String __tc_getClassLoaderName() {
      return name;
    }

    public void __tc_setClassLoaderName(String loaderName) {
      throw new IllegalStateException("Changing the name is not supported");
    }
    
    public String toString() {
      return "MockClassLoader[" + name + "] parented by " + getParent();
    }
  }
}

