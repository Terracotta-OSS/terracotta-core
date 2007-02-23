/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.walker;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ClassUtils;

import com.tc.object.walker.ObjectGraphWalker;
import com.tc.object.walker.PrintVisitor;
import com.tc.object.walker.WalkTest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import junit.framework.TestCase;

public class BasicWalkerTest extends TestCase {

  public void test() throws IOException {
    Root r = new Root();
    r.m.put("timmy", new Foo());
    r.m.put("yo", null);
    r.m.put("foo", new Foo());
    r.m.put("foo foo", new Foo(new Foo()));

    MyWalkTest test = new MyWalkTest();

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(baos);

    ObjectGraphWalker t = new ObjectGraphWalker(r, test, new PrintVisitor(ps, test, new MyValueFormatter()));
    t.walk();

    ps.flush();
    String output = new String(baos.toByteArray());
    System.err.println(output);
    validate(output);
  }

  private void validate(String output) throws IOException {
    String expected = getExpected();
    assertEquals(expected, output);
  }

  private String getExpected() throws IOException {
    String resource = ClassUtils.getShortClassName(getClass()) + "-output.txt";
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    InputStream in = null;
    try {
      in = getClass().getResourceAsStream(resource);
      if (in == null) {
        fail("missing resource: " + resource);
      }
      IOUtils.copy(in, baos);
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    }

    baos.flush();
    return new String(baos.toByteArray());
  }

  private static class MyValueFormatter implements PrintVisitor.ValueFormatter {

    public String format(Object o) {
      if (o instanceof String) { return "\"" + o + "\""; }
      return String.valueOf(o);
    }

    public String valueAdornment(MemberValue value) {
      return null;
    }

  }

  private static class MyWalkTest implements WalkTest {

    public boolean shouldTraverse(MemberValue value) {
      Object val = value.getValueObject();

      if ((val == null) || val.getClass().isPrimitive()) { return false; }

      if (val.getClass() == String.class) return false;
      if (val.getClass() == Class.class) return false;

      if (val.getClass() == Byte.class) return false;
      if (val.getClass() == Boolean.class) return false;
      if (val.getClass() == Character.class) return false;
      if (val.getClass() == Double.class) return false;
      if (val.getClass() == Integer.class) return false;
      if (val.getClass() == Long.class) return false;
      if (val.getClass() == Short.class) return false;
      if (val.getClass() == Float.class) return false;

      return true;
    }
  }

  private static class Root {
    int        i          = 12;
    Map        m          = new LinkedHashMap();
    String     s          = "root";
    Object     b          = new B();
    int[]      intArray   = new int[] { 1, 2, 3 };
    Object[]   objArray   = new Object[] { this, new Foo(), new Integer(3) };
    Object     self       = this;
    Object[][] multi      = new Object[][] { { "timmy", new Integer(4) }, { null, null }, { null, this } };

    double[]   emptyArray = new double[] {};
    HashMap    emptyMap   = new HashMap();
    LinkedList emptyList  = new LinkedList();

    Class      clazz      = getClass();

    Collection c          = new ArrayList();
    {
      c.add(new Foo(new Foo()));
      Map tm = new TreeMap();
      tm.put("key", new Foo());
      tm.put("k", null);
      c.add(tm);
      c.add(c);
      c.add(null);
      c.add(new Double(Math.PI));
    }
  }

  private static class B extends A {
    // this variable is "shadowed" on purpose, please do not rename them to remove the eclipse warning
    private int i = 1;
  }

  private static class A {
    // this variable is "shadowed" on purpose, please do not rename them to remove the eclipse warning
    private int i = 0;
  }

  private static class Foo {
    private final int i = next();

    private final Foo next;

    public Foo() {
      this(null);
    }

    public Foo(Foo foo) {
      this.next = foo;
    }

    public int getI() {
      return i;
    }

    public Foo getNext() {
      return next;
    }

    private static int num = 0;

    private synchronized static int next() {
      return num++;
    }
  }

}
