/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.transparency;

import org.apache.commons.collections.ArrayStack;
import org.apache.commons.collections.MultiHashMap;

import com.tc.exception.TCNonPortableObjectError;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.SequenceID;
import com.tctest.runner.AbstractTransparentApp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

public class ClassNotPortableTestApp extends AbstractTransparentApp {

  SubClassA            root1;
  SubClassB            root2;
  SubClassC            root3;

  SuperClassWithFields root4;

  Object               root5;
  List                 root6;

  Collection           root7;
  Map                  root8;
  Worker               root9;

  public ClassNotPortableTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = ClassNotPortableTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    String methodExpression = "* " + testClass + "*.*(..)";
    config.addIncludePattern(testClass);
    config.addWriteAutolock(methodExpression);
    spec.addRoot("root1", "root1");
    spec.addRoot("root2", "root2");
    spec.addRoot("root3", "root3");
    spec.addRoot("root4", "root4");
    spec.addRoot("root5", "root5");
    spec.addRoot("root6", "root6");
    spec.addRoot("root7", "root7");
    spec.addRoot("root8", "root8");
    spec.addRoot("root9", "root9");

    // Only the Subclass is instructed and its super classes are not instructed.
    String classname1 = SubClassA.class.getName();
    String classname2 = SubClassB.class.getName();
    String classname3 = SubClassC.class.getName();

    String classname4 = Worker.class.getName();

    String classname5 = ArrayStack.class.getName();
    String classname6 = MultiHashMap.class.getName();

    String classname7 = Thread.class.getName();

    config.addIncludePattern(classname1);
    config.addIncludePattern(classname2);
    config.addIncludePattern(classname3);
    config.addIncludePattern(classname4);
    config.addIncludePattern(classname5);
    config.addIncludePattern(classname6);

    config.addIncludePattern(classname7);
    config.addIncludePattern(ReferenceHolder.class.getName());
    config.addIncludePattern(TreeNode.class.getName());

    config.addWriteAutolock("* " + classname1 + ".*(..)");
    config.addWriteAutolock("* " + classname2 + ".*(..)");
    config.addWriteAutolock("* " + classname3 + ".*(..)");
  }

  public void run() {

    testSuperClassNotPortable();

    testNonPortableClassAddedToSharedGraph();

    testThreadAndSubClassOfThreads();

    testSetAndGetOnPortableAdaptableClasses();

    testComplexReferenceGraphAddedToSharedMap();
  }

  // This test is half baked in the sense that out of 6 possible failure cases, it would probably
  // catch 3. In the other 3 cases, it woudl still work except it might be a little slow.
  // I dont know how to verify those cases with doing a ASM inspect on the instrumented code.
  private void testSetAndGetOnPortableAdaptableClasses() {
    SubClassC cc = new SubClassC();
    cc.checkedUnCheckedSetsAndGets();

    Worker w = new Worker();
    w.checkedUnCheckedSetsAndGets();
  }

  private void testSuperClassNotPortable() {
    // Should fail - behavior changed. We dont check for fields anymore
    try {
      root1 = new SubClassA();
      root1.method1();
      throw new AssertionError("Should have failed");
    } catch (TCNonPortableObjectError tcp) {
      // Expected
    }

    // Should fail even if it doesnt change any of the superclasses' fields.
    try {
      root2 = new SubClassB();
      root2.method1();
      throw new AssertionError("Should have failed");
    } catch (TCNonPortableObjectError tcp) {
      // Expected
    }

    // Should fail
    try {
      root3 = new SubClassC();
      root3.method1();
      throw new AssertionError("Should have failed");
    } catch (TCNonPortableObjectError tcp) {
      // Expected
    }

  }

  private void testNonPortableClassAddedToSharedGraph() {
    try {
      root4 = new SuperClassWithFields();
      throw new AssertionError("Should have failed");
    } catch (TCNonPortableObjectError tcp) {
      // Expected
    }

    try {
      root5 = new SuperClassWithFields();
      throw new AssertionError("Should have failed");
    } catch (TCNonPortableObjectError tcp) {
      // Expected
    }

    try {
      root5 = new SubClassD();
      throw new AssertionError("Should have failed");
    } catch (TCNonPortableObjectError tcp) {
      // Expected
    }

    root6 = new ArrayList();

    synchronized (root6) {
      try {
        // Adding non-portable object to a shared object.
        root6.add(new SuperClassWithNoFields());
        throw new AssertionError("Should have failed");
      } catch (TCNonPortableObjectError tcp) {
        // Expected
      }

      Map m = new HashMap();
      m.put(new Integer(10), new SuperClassWithFields());

      try {
        // Adding non-portable Map to a shared object.
        root6.add(m);
        throw new AssertionError("Should have failed");
      } catch (TCNonPortableObjectError tcp) {
        // Expected
      }

      Map tm = new TreeMap();
      root6.add(tm);

      try {
        tm.put(new Integer(10), new SuperClassWithFields());
        // Adding non-portable Map to a shared object.
        throw new AssertionError("Should have failed");
      } catch (TCNonPortableObjectError tcp) {
        // Expected
      }

      ReferenceHolder ref = new ReferenceHolder(new Integer(120));
      root6.add(ref);
      try {
        ref.setReference(new SuperClassWithNoFields());
        throw new AssertionError("Should have failed");
      } catch (TCNonPortableObjectError tcp) {
        // Expected
      }

      ReferenceHolder refs[] = new ReferenceHolder[2];
      root6.add(refs);
      refs[0] = new ReferenceHolder("Hello String literal !");
      try {
        // Adding non-portable Object to a shared object.
        refs[1] = new ReferenceHolder(new SuperClassWithFields());
        throw new AssertionError("Should have failed");
      } catch (TCNonPortableObjectError tcp) {
        // Expected
      }

      // Try adding TC classes to see that it fails
      try {
        root6.add(new SequenceID(10));
        throw new AssertionError("Should have failed");
      } catch (TCNonPortableObjectError tcp) {
        // Expected
      }
    }
  }

  private void testThreadAndSubClassOfThreads() {
    if (root8 == null) {
      root8 = new HashMap();
    }
    try {
      synchronized (root8) {
        root8.put("Hello Thread ", new Thread("hello (1)\n"));
      }
      throw new AssertionError("Should have failed");
    } catch (TCNonPortableObjectError tcp) {
      // Expected
    }

    try {
      root9 = new Worker();
      throw new AssertionError("Should have failed");
    } catch (TCNonPortableObjectError tcp) {
      // Expected
    }
  }

  private void testComplexReferenceGraphAddedToSharedMap() {
    if (root8 == null) {
      root8 = new HashMap();
    }
    Vector v = new Vector();
    synchronized (root8) {
      root8.put("Shared Vector", v);
    }
    Object o = buildComplexStructure();
    try {
      synchronized (v) {
        v.add(o);
      }
      throw new AssertionError("Should have failed");
    } catch (TCNonPortableObjectError ex) {
      // Expected
    }
  }

  private Object buildComplexStructure() {
    int level = 6;
    TreeNode root = new TreeNode();
    buildRecursively(root, level);
    return new ReferenceHolder(root);
  }

  private void buildRecursively(TreeNode root, int level) {
    root.left = new TreeNode();
    root.right = new TreeNode();
    level--;
    if (level == 1) {
      root.left = new Date();
      root.right = getNonPortableList();
    } else {
      buildRecursively((TreeNode) root.left, level);
      buildRecursively((TreeNode) root.right, level);
    }
  }

  private Object getNonPortableList() {
    List l = new ArrayList();
    int c = 10;
    while (c-- > 0) {
      l.add(new Date());
    }
    l.add(new SubClassC());
    return l;
  }

  static class TreeNode {
    Object left;
    Object right;

    TreeNode() {
      super();
    }

    TreeNode(Object l, Object r) {
      this.left = l;
      this.right = r;
    }
  }

  static class ReferenceHolder {
    Object reference;

    public ReferenceHolder(Object o) {
      setReference(o);
    }

    public Object getReference() {
      return reference;
    }

    public void setReference(Object reference) {
      this.reference = reference;
    }

  }

  static class SubClassOfArrayList extends ArrayList {
    private int localint;

    SubClassOfArrayList(int localint) {
      this.localint = localint;
    }

    int getLocalInt() {
      return this.localint;
    }

    void setLocalInt(int i) {
      this.localint = i;
    }

    public String toString() {
      return "SubClassOfArrayList(" + localint + "):" + super.toString();
    }
  }

  static class SubClassOfHashMap extends HashMap {
    private int localint;

    SubClassOfHashMap(int localint) {
      this.localint = localint;
    }

    int getLocalInt() {
      return this.localint;
    }

    void setLocalInt(int i) {
      this.localint = i;
    }

    public String toString() {
      return "SubClassOfHashMap(" + localint + "):" + super.toString();
    }
  }

  static class Worker extends Thread {
    private List    works = new ArrayList();
    private boolean stop  = false;

    private int     k;

    public synchronized void addWork(Runnable r) {
      works.add(r);
      notifyAll();
    }

    public void checkedUnCheckedSetsAndGets() {
      // these should not be modified.
      int newk = k + 109;
      k = newk;

      // This should be checked
      SubClassD d = new SubClassD();
      d.d = d.d++;
    }

    public void notSynchronizedOnMethod(Object o) {
      synchronized (o) {
        o.notifyAll();
      }
    }

    public synchronized void requestStop() {
      stop = true;
      notifyAll();
    }

    public void run() {
      while (true) {
        while (works.isEmpty() && !stop) {
          try {
            wait();
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        }
        if (stop) break;
        Runnable r = (Runnable) works.remove(0);
        r.run();
      }
    }
  }

}
