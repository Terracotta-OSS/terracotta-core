/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.bytecode.Clearable;
import com.tc.object.bytecode.Manageable;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.Properties;
import java.util.Hashtable;

public class HashtableCloneTestApp extends AbstractErrorCatchingTransparentApp {

  private Properties       rootProperties   = new Properties();
  private AnotherHashtable anotherHashtable = new AnotherHashtable("aName");
  private CyclicBarrier    barrier;

  public HashtableCloneTestApp(final String appId, final ApplicationConfig cfg, final ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = HashtableCloneTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    spec.addRoot("rootProperties", "theRootName");
    spec.addRoot("anotherHashtable", "anotherRoot");
    String testInnerClass = testClass + "$AnotherHashtable";
    config.getOrCreateSpec(testInnerClass);
    testInnerClass = testClass + "$JunkData";
    config.getOrCreateSpec(testInnerClass);
    spec.addRoot("barrier", "barrier");
    config.addWriteAutolock("* " + testClass + ".testClone(..)");
    config.addWriteAutolock("* " + testClass + ".testClearedReferences(..)");
    config.addWriteAutolock("* " + testClass + ".testSubClass(..)");
    new CyclicBarrierSpec().visit(visitor, config);
  }

  protected void runTest() throws Throwable {
    barrier = new CyclicBarrier(getParticipantCount());

    int myId = barrier.barrier();

    testClone(myId);

    barrier.barrier();

    testClearedReferences(myId);

    barrier.barrier();

    // TODO: remove the if(false) when CDV-703 is fixed
    if (false) {
      testSubClass(myId);
    }
  }

  private void testClearedReferences(final int myId) throws InterruptedException {
    final JunkData firstData = new JunkData("junkData");
    if (myId == 0) {
      synchronized (rootProperties) {
        rootProperties.put("1", firstData);
      }
    }

    barrier.barrier();

    log("Clearing value object references...");
    ((Clearable) rootProperties).__tc_clearReferences(Integer.MAX_VALUE);

    log("Cloning properties... " + System.identityHashCode(rootProperties));
    Object clone = rootProperties.clone();

    log("Asserting " + rootProperties.getClass().getName() + " equals " + clone.getClass().getName());
    Assert.assertEquals(rootProperties.getClass().getName(), clone.getClass().getName());

    Properties cloneProps = (Properties) clone;
    log("Clone.isManaged: " + ((Manageable) cloneProps).__tc_isManaged());
    printClassHierarchy(cloneProps);
    final Object obj = cloneProps.get("1");

    log("After clear, got obj Class:" + obj.getClass().getName());
    Assert.assertEquals(JunkData.class.getName(), obj.getClass().getName());

    log("the cleared ref: " + obj.toString());
    Assert.assertEquals(new JunkData("junkData"), obj);
    log("Asserted key-value");

    barrier.barrier();

    // modify clone on node-1, it should not be visible on node-0
    if (myId == 1) {
      cloneProps.put("1", new JunkData("secondJunk"));
    }
    barrier.barrier();

    JunkData secondData = (JunkData) cloneProps.get("1");
    log("firstData=" + firstData);
    log("secondData=" + secondData);
    if (myId == 0) {
      Assert.assertTrue(secondData.equals(firstData));
    } else if (myId == 1) {
      Assert.assertFalse(secondData.equals(firstData));
      Assert.assertEquals(new JunkData("secondJunk"), secondData);
    }

    barrier.barrier();

    // modify clone on node-0, it should not be visible on node-1
    if (myId == 0) {
      cloneProps.put("1", new JunkData("thirdJunk"));
    }
    barrier.barrier();

    JunkData thirdData = (JunkData) cloneProps.get("1");
    log("firstData=" + firstData);
    log("thirdData=" + thirdData);
    if (myId == 1) {
      Assert.assertTrue(thirdData.equals(secondData));
    } else if (myId == 0) {
      Assert.assertFalse(thirdData.equals(firstData));
      Assert.assertEquals(new JunkData("thirdJunk"), thirdData);
    }

  }

  private void printClassHierarchy(Object obj) {
    Class klass = obj.getClass();
    Class t = klass;
    System.err.println("Super classes: ");
    while (t != null) {
      System.err.print(t + " ");
      t = t.getSuperclass();
    }
    System.err.println("");
    System.err.println("Interfaces: ");
    for (int i = 0; i < klass.getInterfaces().length; i++) {
      System.err.print(klass.getInterfaces()[i] + " ");
    }
    System.err.println("");
  }

  private void testSubClass(final int myId) throws InterruptedException {
    if (myId == 0) {
      synchronized (anotherHashtable) {
        anotherHashtable.put("key", "correctValue");
      }
      log("Added a property");
    }

    barrier.barrier();

    Object clone = anotherHashtable.clone();
    log("Assert " + anotherHashtable.getClass().getName() + " equals " + clone.getClass().getName());
    Assert.assertEquals(anotherHashtable.getClass().getName(), clone.getClass().getName());

    AnotherHashtable prop = (AnotherHashtable) clone;
    Assert.assertEquals("correctValue", prop.get("key"));
    log("Asserted key-value");
  }

  private void testClone(final int myId) throws InterruptedException {
    if (myId == 0) {
      rootProperties.setProperty("key", "correctValue");
      log("Added a property");
    }

    barrier.barrier();

    Object clone = rootProperties.clone();
    log("Assert " + rootProperties.getClass().getName() + " equals " + clone.getClass().getName());
    Assert.assertEquals(rootProperties.getClass().getName(), clone.getClass().getName());

    Properties prop = (Properties) clone;
    Assert.assertEquals("correctValue", prop.getProperty("key"));
    log("Asserted key-value");
  }

  private void log(String msg) {
    System.err.println(ManagerUtil.getClientID() + ": " + msg);
  }

  private static class AnotherHashtable extends Hashtable {

    private String name;

    private AnotherHashtable(final String name) {
      this.name = name;
    }

    public String toString() {
      return "AnotherHashtable{" + "name=" + name + ", super.toString()=" + super.toString() + '}';
    }
  }

  private static class JunkData {
    private String junk;

    public JunkData(final String junk) {
      this.junk = junk;
    }

    public boolean equals(final Object o) {
      if (this == o) return true;
      if (!(o instanceof JunkData)) return false;

      final JunkData junkData = (JunkData) o;

      if (junk != null ? !junk.equals(junkData.junk) : junkData.junk != null) return false;

      return true;
    }

    public int hashCode() {
      return (junk != null ? junk.hashCode() : 0);
    }

    public String toString() {
      return "JunkData{" + "junk='" + junk + '\'' + '}';
    }
  }
}
