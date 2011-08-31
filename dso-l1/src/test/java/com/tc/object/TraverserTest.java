/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.net.GroupID;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author steve
 */
public class TraverserTest extends BaseDSOTestCase {

  public void testTraverse() throws Exception {
    System.out.println("Starting");
    TestA ta1 = new TestA(null, null);
    TestB tb1 = new TestB(ta1, null, ta1, null);
    TestA ta2 = new TestA(ta1, tb1);
    final ArrayList results = new ArrayList();
    new Traverser(new TestPortableObjectProvider()).traverse(ta2, new TraversalAction() {
      public void visit(List objects, GroupID gid) {
        System.out.println("Adding:" + objects);
        results.addAll(objects);
      }
    });
    assertTrue("Expected 3 but got:" + results.size(), results.size() == 3);
    assertTrue(results.contains(ta1));
    assertTrue(results.contains(ta2));
    assertTrue(results.contains(tb1));

    String[] strings = new String[] { "one", "two", "three" };
    new Traverser(new TestPortableObjectProvider()).traverse(strings, new TraversalAction() {
      public void visit(List objects, GroupID gid) {
        results.add(objects);
      }
    });

    // Test stack overflows don't happen
    final LinkedList list = new LinkedList();
    System.out.println("Adding");
    for (int i = 0; i < 100000; i++) {
      list.add(new Object());
    }
    try {
      new Traverser(new TestPortableObjectProvider()).traverse(list, new TraversalAction() {
        public void visit(List objects, GroupID gid) {
          //
        }
      });
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

    public TestPortableObjectProvider() {
      //
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
      for (Object value : values) {
        addTo.addAnonymousReference(value);
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
  }

  private static class TestB extends TestA implements Serializable {
    private final TestA  tc;

    private final TestB  td;

    private final String bstring = "bstring";

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

  }
}