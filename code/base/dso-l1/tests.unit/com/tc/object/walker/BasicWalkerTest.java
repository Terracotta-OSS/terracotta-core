/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.walker;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ClassUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import junit.framework.TestCase;

public class BasicWalkerTest extends TestCase {

  public void helper(Object root, String fileNamePrefix) throws IOException {
    WalkTest test = new MyWalkTestImpl();

    MyOutputSink sink = new MyOutputSink();

    ObjectGraphWalker t = new ObjectGraphWalker(root, test, new PrintVisitor(sink, test, new MyValueFormatter()));
    t.walk();

    String output = sink.buffer.toString();
    System.err.println(output);
    validate(fileNamePrefix, output);
  }

  public void test() throws IOException {
    Root r = new Root();
    r.m.put("timmy", new Foo());
    r.m.put("yo", null);
    r.m.put("foo", new Foo());
    r.m.put("foo foo", new Foo(new Foo()));

    helper(r, ClassUtils.getShortClassName(getClass()));
  }

  public void testArrayListSubclass() throws IOException {
    helper(new ArrayListSubclass(), "ArrayListSubclassWalkerTest");
  }

  public void testAbstractListSubclass() throws IOException {
    helper(new AbstractListSubclass(), "AbstractListSubclassWalkerTest");
  }

  public void testHashMapSubclass() throws IOException {
    helper(new HashMapSubclass(), "HashMapSubclassWalkerTest");
  }

  public void testAbstractMapSubclass() throws IOException {
    helper(new AbstractMapSubclass(), "AbstractMapSubclassWalkerTest");
  }

  private void validate(String name, String output) throws IOException {
    String expected = getExpected(name);

    expected = expected.replaceAll("\r", "");
    output = output.replaceAll("\r", "");

    assertEquals(expected, output);
  }

  private String getExpected(String name) throws IOException {
    String resource = name + "-output.txt";
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

  private static class MyOutputSink implements PrintVisitor.OutputSink {
    final StringBuffer buffer = new StringBuffer();

    public void output(String line) {
      buffer.append(line + "\n");
    }
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

  private static class MyWalkTestImpl implements WalkTest {
    private static Set logicalTypes = new HashSet();

    static {
      logicalTypes.add(ArrayList.class);
      logicalTypes.add(HashMap.class);
      logicalTypes.add(TreeMap.class);
      logicalTypes.add(LinkedHashMap.class);
      logicalTypes.add(LinkedList.class);
    }

    public boolean includeFieldsForType(Class type) {
      return !logicalTypes.contains(type);
    }

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

  @SuppressWarnings("unused")
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

  @SuppressWarnings("unused")
  private static class B extends A {
    // STOP: this variable is "shadowed" on purpose, please do not rename them to remove the eclipse warning
    private final int i = 1;

  }

  @SuppressWarnings("unused")
  private static class A {
    // STOP: this variable is "shadowed" on purpose, please do not rename them to remove the eclipse warning
    private final int i = 0;

  }

  @SuppressWarnings("unused")
  private static class Foo {
    private final int i = next();

    private final Foo next;

    public Foo() {
      this(null);
    }

    public Foo(Foo foo) {
      this.next = foo;
    }

    private static int num = 0;

    private synchronized static int next() {
      return num++;
    }
  }

  @SuppressWarnings("unused")
  private static class ArrayListSubclass extends ArrayList {
    private final int    iVal;
    private final String sVal;

    ArrayListSubclass() {
      super();
      addAll(Arrays.asList(new String[] { "foo", "bar" }));
      iVal = 42;
      sVal = "timmy";
    }

  }

  @SuppressWarnings("unused")
  private static class AbstractListSubclass extends AbstractList {
    private final int    iVal;
    private final String sVal;

    AbstractListSubclass() {
      iVal = 42;
      sVal = "timmy";
    }

    @Override
    public Object get(int index) {
      return null;
    }

    @Override
    public int size() {
      return 0;
    }

  }

  @SuppressWarnings("unused")
  private static class HashMapSubclass extends LinkedHashMap {
    private final int    iVal;
    private final String sVal;

    HashMapSubclass() {
      super();
      put("sVal", "timmy");
      put("iVal", new Integer(42));
      iVal = 42;
      sVal = "timmy";
    }

  }

  @SuppressWarnings("unused")
  private static class AbstractMapSubclass extends AbstractMap {
    private final int    iVal;
    private final String sVal;

    AbstractMapSubclass() {
      iVal = 42;
      sVal = "timmy";
    }

    @Override
    public Set entrySet() {
      return new HashSet();
    }

  }

}
