/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.exception.TCNonPortableObjectError;
import com.tc.object.appevent.NonPortableEventContext;
import com.tc.object.bytecode.MockClassProvider;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.field.TCFieldFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author steve
 */
public class TraverserTest extends BaseDSOTestCase {

  /**
   * Constructor for TraverserTest.
   * 
   * @param arg0
   */
  public TraverserTest(String arg0) {
    super(arg0);
  }

  private static class MyTraverseTester implements TraverseTest {

    private boolean shouldTraverse;

    public MyTraverseTester(boolean should) {
      this.shouldTraverse = should;
    }

    public void checkPortability(TraversedReference obj, Class referringClass, NonPortableEventContext context)
        throws TCNonPortableObjectError {
      //
    }

    public boolean shouldTraverse(Object object) {
      return shouldTraverse;
    }

    void setShouldTraverse(boolean should) {
      this.shouldTraverse = should;
    }
  }

  private static class Ref {
    Object ref;

    Object getRef() {
      return this.ref;
    }

    void setRef(Object ref) {
      this.ref = ref;
    }
  }

  private class MyPortableObjectProvider implements PortableObjectProvider {

    public TraversedReferences getPortableObjects(Class clazz, Object start, TraversedReferences addTo) {
      Ref ref = (Ref) start;
      Object next = ref.getRef();
      if (next != null) {
        addTo.addAnonymousReference(next);
      }
      return addTo;
    }
  }

  private static TraverseTest[] makeTests(boolean test1, boolean test2) {
    return new TraverseTest[] { new MyTraverseTester(test1), new MyTraverseTester(test2) };
  }

  public void testMultipleTraverseTest() {
    final Ref o1 = new Ref();
    final Ref o2 = new Ref();
    o1.setRef(o2);
    o2.setRef(null);

    new Traverser(new TraversalAction() {
      public void visit(List objects) {
        // expected
        if (!objects.contains(o1)) throw new AssertionError();
        if (!objects.contains(o2)) throw new AssertionError();
      }
    }, new MyPortableObjectProvider()).traverse(o1, makeTests(true, true), null);

    new Traverser(new TraversalAction() {
      public void visit(List objects) {
        if (!objects.contains(o1)) throw new AssertionError();
        if (objects.contains(o2)) throw new AssertionError();
      }
    }, new MyPortableObjectProvider()).traverse(o1, makeTests(true, false), null);

    new Traverser(new TraversalAction() {
      public void visit(List objects) {
        if (objects.contains(o2)) throw new AssertionError();
        if (!objects.contains(o1)) throw new AssertionError();
      }
    }, new MyPortableObjectProvider()).traverse(o1, makeTests(false, true), null);

    new Traverser(new TraversalAction() {
      public void visit(List objects) {
        if (objects.contains(o2)) throw new AssertionError();
        if (!objects.contains(o1)) throw new AssertionError();
      }
    }, new MyPortableObjectProvider()).traverse(o1, makeTests(false, false), null);
  }

  public void testTraverse() throws Exception {
    System.out.println("Starting");
    TestA ta1 = new TestA(null, null);
    TestB tb1 = new TestB(ta1, null, ta1, null);
    TestA ta2 = new TestA(ta1, tb1);
    final ArrayList results = new ArrayList();
    new Traverser(new TraversalAction() {
      public void visit(List objects) {
        System.out.println("Adding:" + objects);
        results.addAll(objects);
      }
    }, new TestPortableObjectProvider()).traverse(ta2);
    assertTrue("Expected 3 but got:" + results.size(), results.size() == 3);
    assertTrue(results.contains(ta1));
    assertTrue(results.contains(ta2));
    assertTrue(results.contains(tb1));

    String[] strings = new String[] { "one", "two", "three" };
    new Traverser(new TraversalAction() {
      public void visit(List objects) {
        results.add(objects);
      }
    }, new TestPortableObjectProvider()).traverse(strings);

    // Test stack overflows don't happen
    final LinkedList list = new LinkedList();
    System.out.println("Adding");
    for (int i = 0; i < 100000; i++) {
      list.add(new Object());
    }
    try {
      new Traverser(new TraversalAction() {
        public void visit(List objects) {
          //
        }
      }, new TestPortableObjectProvider()).traverse(list);
      System.out.println("Traversed");
      assertTrue(true);
    } catch (StackOverflowError e) {
      assertTrue(false);
    }
  }

  public static void main(String[] args) {
    //
  }

  private class TestPortableObjectProvider implements PortableObjectProvider {
    TCClassFactory cf;

    public TestPortableObjectProvider() throws ConfigurationSetupException {
      DSOClientConfigHelper config = configHelper();
      cf = new TCClassFactoryImpl(new TCFieldFactory(config), config, new MockClassProvider());
    }

    public TraversedReferences getPortableObjects(Class clazz, Object start, TraversedReferences addTo) {
      Object[] values = new Object[0];
      if (start instanceof TestB) {
        TestB tb = (TestB) start;
        values = new Object[] { tb.ta, tb.tb, tb.tc, tb.td };
      } else if (start instanceof TestA) {
        TestA tb = (TestA) start;
        values = new Object[] { tb.ta, tb.tb };
      }
      for (int i = 0; i < values.length; i++) {
        addTo.addAnonymousReference(values[i]);
      }
      return addTo;
    }

  }

  private static class TestA implements Serializable {
    public TestA   ta;

    public TestB   tb;

    public boolean tboolean;

    public String  aString = "aString";

    public TestA(TestA ta, TestB tb) {
      this.ta = ta;
      this.tb = tb;
      if (false) {
        this.ta.equals(null);
        this.tb.equals(null);
        if (tboolean) {/**/
        }
        this.aString.equals(null);
      }
    }

    public void setTA(TestA ta) {
      this.ta = ta;
    }

    public void setTB(TestB tb) {
      this.tb = tb;
    }
  }

  private static class TestB extends TestA implements Serializable {
    private TestA  tc;

    private TestB  td;

    private String bstring = "bstring";

    public TestB(TestA ta, TestB tb, TestA tc, TestB td) {
      super(ta, tb);
      this.tc = tc;
      this.td = td;
      if (false) {
        this.tc.equals(null);
        this.td.equals(null);
        this.bstring.equals(null);
      }
    }

    public void setTC(TestA tc) {
      this.tc = tc;
    }

    public void setTD(TestB td) {
      this.td = td;
    }
  }
}