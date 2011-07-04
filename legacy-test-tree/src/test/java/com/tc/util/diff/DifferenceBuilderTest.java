/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.diff;

import org.apache.commons.lang.builder.EqualsBuilder;

import com.tc.test.TCTestCase;
import com.tc.util.Stringifier;

/**
 * Unit test for {@link DifferenceBuilder}.
 */
public class DifferenceBuilderTest extends TCTestCase {

  private static boolean USE_REFLECTION = false;

  public static class TestObject implements Differenceable {
    private final boolean   aBoolean;
    private final char      aChar;
    private final byte      aByte;
    private final short     aShort;
    private final int       anInt;
    private final long      aLong;
    private final float     aFloat;
    private final double    aDouble;
    private final Object    anObject;

    private final boolean[] arrBoolean;
    private final char[]    arrChar;
    private final byte[]    arrByte;
    private final short[]   arrShort;
    private final int[]     arrInt;
    private final long[]    arrLong;
    private final float[]   arrFloat;
    private final double[]  arrDouble;
    private final Object[]  arrObject;

    public TestObject(boolean boolean1, char char1, byte byte1, short short1, int anInt, long long1, float float1,
                      double double1, Object anObject, boolean[] arrBoolean, char[] arrChar, byte[] arrByte,
                      short[] arrShort, int[] arrInt, long[] arrLong, float[] arrFloat, double[] arrDouble,
                      Object[] arrObject) {
      super();

      this.aBoolean = boolean1;
      this.aChar = char1;
      this.aByte = byte1;
      this.aShort = short1;
      this.anInt = anInt;
      this.aLong = long1;
      this.aFloat = float1;
      this.aDouble = double1;
      this.anObject = anObject;
      this.arrBoolean = arrBoolean;
      this.arrChar = arrChar;
      this.arrByte = arrByte;
      this.arrShort = arrShort;
      this.arrInt = arrInt;
      this.arrLong = arrLong;
      this.arrFloat = arrFloat;
      this.arrDouble = arrDouble;
      this.arrObject = arrObject;
    }

    public TestObject() {
      this(true, 'x', (byte) 37, (short) 137, 1238415, 1238947198L, 4.34e+27f, 1.32523482759e+109, "foo",
           new boolean[] { true, false, true }, new char[] { 'a', 'b', 'c' }, new byte[] { 3, 4, 5 }, new short[] { 4,
               5, 6 }, new int[] { 7, 8, 9 }, new long[] { 10, 11, 12 }, new float[] { 0.1f, 0.2f, 0.3f },
           new double[] { 0.4, 0.5, 0.6 }, new Object[] { "foo", "bar", "baz" });
    }

    public TestObject(boolean bool) {
      this(bool, 'x', (byte) 37, (short) 137, 1238415, 1238947198L, 4.34e+27f, 1.32523482759e+109, "foo",
           new boolean[] { true, false, true }, new char[] { 'a', 'b', 'c' }, new byte[] { 3, 4, 5 }, new short[] { 4,
               5, 6 }, new int[] { 7, 8, 9 }, new long[] { 10, 11, 12 }, new float[] { 0.1f, 0.2f, 0.3f },
           new double[] { 0.4, 0.5, 0.6 }, new Object[] { "foo", "bar", "baz" });
    }

    public TestObject(char ch) {
      this(true, ch, (byte) 37, (short) 137, 1238415, 1238947198L, 4.34e+27f, 1.32523482759e+109, "foo", new boolean[] {
          true, false, true }, new char[] { 'a', 'b', 'c' }, new byte[] { 3, 4, 5 }, new short[] { 4, 5, 6 },
           new int[] { 7, 8, 9 }, new long[] { 10, 11, 12 }, new float[] { 0.1f, 0.2f, 0.3f }, new double[] { 0.4, 0.5,
               0.6 }, new Object[] { "foo", "bar", "baz" });
    }

    public TestObject(byte b) {
      this(true, 'x', b, (short) 137, 1238415, 1238947198L, 4.34e+27f, 1.32523482759e+109, "foo", new boolean[] { true,
          false, true }, new char[] { 'a', 'b', 'c' }, new byte[] { 3, 4, 5 }, new short[] { 4, 5, 6 }, new int[] { 7,
          8, 9 }, new long[] { 10, 11, 12 }, new float[] { 0.1f, 0.2f, 0.3f }, new double[] { 0.4, 0.5, 0.6 },
           new Object[] { "foo", "bar", "baz" });
    }

    public TestObject(short s) {
      this(true, 'x', (byte) 37, s, 1238415, 1238947198L, 4.34e+27f, 1.32523482759e+109, "foo", new boolean[] { true,
          false, true }, new char[] { 'a', 'b', 'c' }, new byte[] { 3, 4, 5 }, new short[] { 4, 5, 6 }, new int[] { 7,
          8, 9 }, new long[] { 10, 11, 12 }, new float[] { 0.1f, 0.2f, 0.3f }, new double[] { 0.4, 0.5, 0.6 },
           new Object[] { "foo", "bar", "baz" });
    }

    public TestObject(int i) {
      this(true, 'x', (byte) 37, (short) 137, i, 1238947198L, 4.34e+27f, 1.32523482759e+109, "foo", new boolean[] {
          true, false, true }, new char[] { 'a', 'b', 'c' }, new byte[] { 3, 4, 5 }, new short[] { 4, 5, 6 },
           new int[] { 7, 8, 9 }, new long[] { 10, 11, 12 }, new float[] { 0.1f, 0.2f, 0.3f }, new double[] { 0.4, 0.5,
               0.6 }, new Object[] { "foo", "bar", "baz" });
    }

    public TestObject(long l) {
      this(true, 'x', (byte) 37, (short) 137, 1238415, l, 4.34e+27f, 1.32523482759e+109, "foo", new boolean[] { true,
          false, true }, new char[] { 'a', 'b', 'c' }, new byte[] { 3, 4, 5 }, new short[] { 4, 5, 6 }, new int[] { 7,
          8, 9 }, new long[] { 10, 11, 12 }, new float[] { 0.1f, 0.2f, 0.3f }, new double[] { 0.4, 0.5, 0.6 },
           new Object[] { "foo", "bar", "baz" });
    }

    public TestObject(float f) {
      this(true, 'x', (byte) 37, (short) 137, 1238415, 1238947198L, f, 1.32523482759e+109, "foo", new boolean[] { true,
          false, true }, new char[] { 'a', 'b', 'c' }, new byte[] { 3, 4, 5 }, new short[] { 4, 5, 6 }, new int[] { 7,
          8, 9 }, new long[] { 10, 11, 12 }, new float[] { 0.1f, 0.2f, 0.3f }, new double[] { 0.4, 0.5, 0.6 },
           new Object[] { "foo", "bar", "baz" });
    }

    public TestObject(double d) {
      this(true, 'x', (byte) 37, (short) 137, 1238415, 1238947198L, 4.34e+27f, d, "foo", new boolean[] { true, false,
          true }, new char[] { 'a', 'b', 'c' }, new byte[] { 3, 4, 5 }, new short[] { 4, 5, 6 }, new int[] { 7, 8, 9 },
           new long[] { 10, 11, 12 }, new float[] { 0.1f, 0.2f, 0.3f }, new double[] { 0.4, 0.5, 0.6 }, new Object[] {
               "foo", "bar", "baz" });
    }

    public TestObject(Object o) {
      this(true, 'x', (byte) 37, (short) 137, 1238415, 1238947198L, 4.34e+27f, 1.32523482759e+109, o, new boolean[] {
          true, false, true }, new char[] { 'a', 'b', 'c' }, new byte[] { 3, 4, 5 }, new short[] { 4, 5, 6 },
           new int[] { 7, 8, 9 }, new long[] { 10, 11, 12 }, new float[] { 0.1f, 0.2f, 0.3f }, new double[] { 0.4, 0.5,
               0.6 }, new Object[] { "foo", "bar", "baz" });
    }

    public TestObject(boolean[] ab) {
      this(true, 'x', (byte) 37, (short) 137, 1238415, 1238947198L, 4.34e+27f, 1.32523482759e+109, "foo", ab,
           new char[] { 'a', 'b', 'c' }, new byte[] { 3, 4, 5 }, new short[] { 4, 5, 6 }, new int[] { 7, 8, 9 },
           new long[] { 10, 11, 12 }, new float[] { 0.1f, 0.2f, 0.3f }, new double[] { 0.4, 0.5, 0.6 }, new Object[] {
               "foo", "bar", "baz" });
    }

    public TestObject(char[] ac) {
      this(true, 'x', (byte) 37, (short) 137, 1238415, 1238947198L, 4.34e+27f, 1.32523482759e+109, "foo",
           new boolean[] { true, false, true }, ac, new byte[] { 3, 4, 5 }, new short[] { 4, 5, 6 }, new int[] { 7, 8,
               9 }, new long[] { 10, 11, 12 }, new float[] { 0.1f, 0.2f, 0.3f }, new double[] { 0.4, 0.5, 0.6 },
           new Object[] { "foo", "bar", "baz" });
    }

    public TestObject(byte[] ab) {
      this(true, 'x', (byte) 37, (short) 137, 1238415, 1238947198L, 4.34e+27f, 1.32523482759e+109, "foo",
           new boolean[] { true, false, true }, new char[] { 'a', 'b', 'c' }, ab, new short[] { 4, 5, 6 }, new int[] {
               7, 8, 9 }, new long[] { 10, 11, 12 }, new float[] { 0.1f, 0.2f, 0.3f }, new double[] { 0.4, 0.5, 0.6 },
           new Object[] { "foo", "bar", "baz" });
    }

    public TestObject(short[] as) {
      this(true, 'x', (byte) 37, (short) 137, 1238415, 1238947198L, 4.34e+27f, 1.32523482759e+109, "foo",
           new boolean[] { true, false, true }, new char[] { 'a', 'b', 'c' }, new byte[] { 3, 4, 5 }, as, new int[] {
               7, 8, 9 }, new long[] { 10, 11, 12 }, new float[] { 0.1f, 0.2f, 0.3f }, new double[] { 0.4, 0.5, 0.6 },
           new Object[] { "foo", "bar", "baz" });
    }

    public TestObject(int[] ai) {
      this(true, 'x', (byte) 37, (short) 137, 1238415, 1238947198L, 4.34e+27f, 1.32523482759e+109, "foo",
           new boolean[] { true, false, true }, new char[] { 'a', 'b', 'c' }, new byte[] { 3, 4, 5 }, new short[] { 4,
               5, 6 }, ai, new long[] { 10, 11, 12 }, new float[] { 0.1f, 0.2f, 0.3f }, new double[] { 0.4, 0.5, 0.6 },
           new Object[] { "foo", "bar", "baz" });
    }

    public TestObject(long[] al) {
      this(true, 'x', (byte) 37, (short) 137, 1238415, 1238947198L, 4.34e+27f, 1.32523482759e+109, "foo",
           new boolean[] { true, false, true }, new char[] { 'a', 'b', 'c' }, new byte[] { 3, 4, 5 }, new short[] { 4,
               5, 6 }, new int[] { 7, 8, 9 }, al, new float[] { 0.1f, 0.2f, 0.3f }, new double[] { 0.4, 0.5, 0.6 },
           new Object[] { "foo", "bar", "baz" });
    }

    public TestObject(float[] af) {
      this(true, 'x', (byte) 37, (short) 137, 1238415, 1238947198L, 4.34e+27f, 1.32523482759e+109, "foo",
           new boolean[] { true, false, true }, new char[] { 'a', 'b', 'c' }, new byte[] { 3, 4, 5 }, new short[] { 4,
               5, 6 }, new int[] { 7, 8, 9 }, new long[] { 10, 11, 12 }, af, new double[] { 0.4, 0.5, 0.6 },
           new Object[] { "foo", "bar", "baz" });
    }

    public TestObject(double[] ad) {
      this(true, 'x', (byte) 37, (short) 137, 1238415, 1238947198L, 4.34e+27f, 1.32523482759e+109, "foo",
           new boolean[] { true, false, true }, new char[] { 'a', 'b', 'c' }, new byte[] { 3, 4, 5 }, new short[] { 4,
               5, 6 }, new int[] { 7, 8, 9 }, new long[] { 10, 11, 12 }, new float[] { 0.1f, 0.2f, 0.3f }, ad,
           new Object[] { "foo", "bar", "baz" });
    }

    public TestObject(Object[] ao) {
      this(true, 'x', (byte) 37, (short) 137, 1238415, 1238947198L, 4.34e+27f, 1.32523482759e+109, "foo",
           new boolean[] { true, false, true }, new char[] { 'a', 'b', 'c' }, new byte[] { 3, 4, 5 }, new short[] { 4,
               5, 6 }, new int[] { 7, 8, 9 }, new long[] { 10, 11, 12 }, new float[] { 0.1f, 0.2f, 0.3f },
           new double[] { 0.4, 0.5, 0.6 }, ao);
    }

    public void addDifferences(DifferenceContext context, Object rawThat) {
      TestObject that = (TestObject) rawThat;

      if (USE_REFLECTION) {
        new DifferenceBuilder(context).reflectionDifference(this, rawThat);
      } else {
        new DifferenceBuilder(context).append("aBoolean", this.aBoolean, that.aBoolean).append("aChar", this.aChar,
                                                                                               that.aChar)
            .append("aByte", this.aByte, that.aByte).append("aShort", this.aShort, that.aShort).append("anInt",
                                                                                                       this.anInt,
                                                                                                       that.anInt)
            .append("aLong", this.aLong, that.aLong).append("aFloat", this.aFloat, that.aFloat).append("aDouble",
                                                                                                       this.aDouble,
                                                                                                       that.aDouble)
            .append("anObject", this.anObject, that.anObject).append("arrBoolean", this.arrBoolean, that.arrBoolean)
            .append("arrChar", this.arrChar, that.arrChar).append("arrByte", this.arrByte, that.arrByte)
            .append("arrShort", this.arrShort, that.arrShort).append("arrInt", this.arrInt, that.arrInt)
            .append("arrLong", this.arrLong, that.arrLong).append("arrFloat", this.arrFloat, that.arrFloat)
            .append("arrDouble", this.arrDouble, that.arrDouble).append("arrObject", this.arrObject, that.arrObject);
      }
    }

    @Override
    public boolean equals(Object that) {
      if (!(that instanceof TestObject)) return false;

      TestObject testThat = (TestObject) that;

      return EqualsBuilder.reflectionEquals(this, testThat);
    }
  }

  public void testConstruction() throws Exception {
    try {
      new DifferenceBuilder(null);
      fail("Didn't get NPE on no context");
    } catch (NullPointerException npe) {
      // ok
    }
  }

  public void testNoDifference() throws Exception {
    TestObject one = new TestObject();
    TestObject two = new TestObject();

    assertEqualsUnordered(new Object[0], DifferenceBuilder.getDifferences(one, two));
    assertEqualsUnordered(new Object[0], DifferenceBuilder.getDifferencesAsArray(one, two));
  }

  public void testPrimitives() throws Exception {
    USE_REFLECTION = false;
    checkPrimitives();
    USE_REFLECTION = true;
    checkPrimitives();
  }

  public void testPrimitiveArrays() throws Exception {
    USE_REFLECTION = false;
    checkPrimitiveArrays();
    USE_REFLECTION = true;
    checkPrimitiveArrays();
  }

  public void testObjects() throws Exception {
    USE_REFLECTION = true;
    checkObjects();
    USE_REFLECTION = false;
    checkObjects();
  }

  public void testObjectArrays() throws Exception {
    USE_REFLECTION = true;
    checkObjectArrays();
    USE_REFLECTION = false;
    checkObjectArrays();
  }

  public void testNestedObjects() throws Exception {
    USE_REFLECTION = true;
    checkNestedObjects();
    USE_REFLECTION = false;
    checkNestedObjects();
  }

  public void testUsesStringifier() throws Exception {
    USE_REFLECTION = true;
    checkStringifier();
    USE_REFLECTION = false;
    checkStringifier();
  }

  public void testDifferentClasses() throws Exception {
    USE_REFLECTION = true;
    checkDifferentClasses();
    USE_REFLECTION = false;
    checkDifferentClasses();
  }

  public void testDerivedClasses() throws Exception {
    USE_REFLECTION = true;
    checkDerivedClasses();
    USE_REFLECTION = false;
    checkDerivedClasses();
  }

  private static class OtherDifferenceable implements Differenceable {
    private final Object a;

    public OtherDifferenceable(Object a) {
      this.a = a;
    }

    public void addDifferences(DifferenceContext context, Object that) {
      if (USE_REFLECTION) {
        new DifferenceBuilder(context).reflectionDifference(this, that);
      } else {
        new DifferenceBuilder(context).append("a", this.a, ((OtherDifferenceable) that).a);
      }
    }

    @Override
    public boolean equals(Object o) {
      return (o instanceof OtherDifferenceable) && (this.a.equals(((OtherDifferenceable) o).a));
    }
  }

  private static class FieldAdded extends OtherDifferenceable {
    private final Object b;

    public FieldAdded(Object a, Object b) {
      super(a);
      this.b = b;
    }

    @Override
    public void addDifferences(DifferenceContext context, Object that) {
      super.addDifferences(context, that);
      if (USE_REFLECTION) {
        new DifferenceBuilder(context).reflectionDifference(this, that);
      } else {
        new DifferenceBuilder(context).append("b", this.b, ((FieldAdded) that).b);
      }
    }

    @Override
    public boolean equals(Object that) {
      if (!super.equals(that)) return false;
      if (!(that instanceof FieldAdded)) return false;
      return ((FieldAdded) that).b.equals(this.b);
    }
  }

  private static class FieldIgnored extends OtherDifferenceable {
    // This field is presumably relevant to the test logic (don't remove it)
    private final Object b;

    public FieldIgnored(Object a, Object b) {
      super(a);
      this.b = b;
    }

    @SuppressWarnings("unused")
    Object getB() {
      return this.b;
    }
  }

  private void checkDifferentClasses() throws Exception {
    TestObject oneSub = new TestObject();
    TestObject one = new TestObject(oneSub);
    OtherDifferenceable twoSub = new OtherDifferenceable("foo");
    TestObject two = new TestObject(twoSub);
    OtherDifferenceable three = new OtherDifferenceable(oneSub);

    DifferenceContext initial = DifferenceContext.createInitial(), context;
    context = initial.sub("anObject");
    assertEqualsUnordered(new Object[] { contextify(context, new BasicObjectDifference(context, oneSub, twoSub)) },
                          DifferenceBuilder.getDifferences(one, two));

    initial = DifferenceContext.createInitial();
    context = initial.sub("anObject");
    assertEqualsUnordered(new Object[] { contextify(context, new BasicObjectDifference(context, twoSub, oneSub)) },
                          DifferenceBuilder.getDifferences(two, one));

    initial = DifferenceContext.createInitial();
    context = initial;
    assertEqualsUnordered(new Object[] { contextify(context, new BasicObjectDifference(context, one, three)) },
                          DifferenceBuilder.getDifferences(one, three));

    initial = DifferenceContext.createInitial();
    context = initial;
    assertEqualsUnordered(new Object[] { contextify(context, new BasicObjectDifference(context, three, one)) },
                          DifferenceBuilder.getDifferences(three, one));
  }

  private void checkDerivedClasses() throws Exception {
    OtherDifferenceable base = new OtherDifferenceable("a");
    OtherDifferenceable added = new FieldAdded("a", "b");
    OtherDifferenceable ignored = new FieldIgnored("a", "b");

    DifferenceContext initial = DifferenceContext.createInitial(), context;
    context = initial;
    assertEqualsUnordered(new Object[] { contextify(context, new BasicObjectDifference(context, base, added)) },
                          DifferenceBuilder.getDifferences(base, added));

    initial = DifferenceContext.createInitial();
    context = initial;
    assertEqualsUnordered(new Object[] { contextify(context, new BasicObjectDifference(context, added, base)) },
                          DifferenceBuilder.getDifferences(added, base));

    initial = DifferenceContext.createInitial();
    context = initial;
    assertEqualsUnordered(new Object[] { contextify(context, new BasicObjectDifference(context, base, ignored)) },
                          DifferenceBuilder.getDifferences(base, ignored));

    initial = DifferenceContext.createInitial();
    context = initial;
    assertEqualsUnordered(new Object[] { contextify(context, new BasicObjectDifference(context, ignored, base)) },
                          DifferenceBuilder.getDifferences(ignored, base));
  }

  private void checkStringifier() throws Exception {
    Stringifier s = new Stringifier() {
      public String toString(Object o) {
        return "X" + o + "Y";
      }
    };

    TestObject one, two;
    DifferenceContext initial = DifferenceContext.createInitial(s);

    one = new TestObject("foo");
    two = new TestObject("foof");
    initial.sub("anObject");
    Difference[] differences = DifferenceBuilder.getDifferencesAsArray(one, two, s);
    String theString = differences[0].toString();
    assertTrue(theString.indexOf("XfooY") >= 0);
    assertTrue(theString.indexOf("XfoofY") >= 0);
  }

  private void checkNestedObjects() throws Exception {
    TestObject one, two;
    DifferenceContext initial = DifferenceContext.createInitial(), context;

    TestObject diffOne = new TestObject("foo");
    TestObject diffTwo = new TestObject("bar");

    one = new TestObject(new TestObject(new Object[] { new TestObject(),
        new TestObject(new Object[] { "a", "b", "c", "d", new TestObject(diffOne) }) }));
    two = new TestObject(new TestObject(new Object[] { new TestObject(),
        new TestObject(new Object[] { "a", "b", "c", "d", new TestObject(diffTwo) }) }));

    context = initial.sub("anObject").sub("arrObject[1]").sub("arrObject[4]").sub("anObject").sub("anObject");
    assertEqualsUnordered(new Object[] { contextify(context, new BasicObjectDifference(context, "foo", "bar")) },
                          DifferenceBuilder.getDifferences(one, two));
  }

  private void checkObjects() throws Exception {
    TestObject one, two;
    DifferenceContext initial = DifferenceContext.createInitial(), context;

    one = new TestObject("foo");
    two = new TestObject("foof");
    context = initial.sub("anObject");
    assertEqualsUnordered(new Object[] { contextify(context, new BasicObjectDifference(context, "foo", "foof")) },
                          DifferenceBuilder.getDifferences(one, two));
  }

  private void checkObjectArrays() throws Exception {
    TestObject one, two;
    DifferenceContext initial = DifferenceContext.createInitial(), context;

    one = new TestObject(new String[] { "a", "foo", "b" });
    two = new TestObject(new String[] { "a", "foof", "b" });
    context = initial.sub("arrObject[1]");
    assertEqualsUnordered(new Object[] { contextify(context, new BasicObjectDifference(context, "foo", "foof")) },
                          DifferenceBuilder.getDifferences(one, two));
  }

  private void checkPrimitives() throws Exception {
    TestObject one, two;
    DifferenceContext initial = DifferenceContext.createInitial(), context;

    one = new TestObject(true);
    two = new TestObject(false);
    context = initial.sub("aBoolean");
    assertEqualsUnordered(new Object[] { contextify(context, new PrimitiveDifference(context, true, false)) },
                          DifferenceBuilder.getDifferences(one, two));

    one = new TestObject('x');
    two = new TestObject('y');
    context = initial.sub("aChar");
    assertEqualsUnordered(new Object[] { contextify(context, new PrimitiveDifference(context, 'x', 'y')) },
                          DifferenceBuilder.getDifferences(one, two));

    one = new TestObject((byte) 14);
    two = new TestObject((byte) 15);
    context = initial.sub("aByte");
    assertEqualsUnordered(new Object[] { contextify(context, new PrimitiveDifference(context, (byte) 14, (byte) 15)) },
                          DifferenceBuilder.getDifferences(one, two));

    one = new TestObject((short) 14);
    two = new TestObject((short) 15);
    context = initial.sub("aShort");
    assertEqualsUnordered(
                          new Object[] { contextify(context, new PrimitiveDifference(context, (short) 14, (short) 15)) },
                          DifferenceBuilder.getDifferences(one, two));

    one = new TestObject(14);
    two = new TestObject(15);
    context = initial.sub("anInt");
    assertEqualsUnordered(new Object[] { contextify(context, new PrimitiveDifference(context, 14, 15)) },
                          DifferenceBuilder.getDifferences(one, two));

    one = new TestObject(14L);
    two = new TestObject(15L);
    context = initial.sub("aLong");
    assertEqualsUnordered(new Object[] { contextify(context, new PrimitiveDifference(context, 14L, 15L)) },
                          DifferenceBuilder.getDifferences(one, two));

    one = new TestObject(14.0f);
    two = new TestObject(15.0f);
    context = initial.sub("aFloat");
    assertEqualsUnordered(new Object[] { contextify(context, new PrimitiveDifference(context, 14.0f, 15.0f)) },
                          DifferenceBuilder.getDifferences(one, two));

    one = new TestObject(14.0);
    two = new TestObject(15.0);
    context = initial.sub("aDouble");
    assertEqualsUnordered(new Object[] { contextify(context, new PrimitiveDifference(context, 14.0, 15.0)) },
                          DifferenceBuilder.getDifferences(one, two));
  }

  private void checkPrimitiveArrays() throws Exception {
    TestObject one, two;
    DifferenceContext initial = DifferenceContext.createInitial(), context;

    one = new TestObject(new boolean[] { true, false, true });
    two = new TestObject(new boolean[] { true, false, false });
    context = initial.sub("arrBoolean[2]");
    assertEqualsUnordered(new Object[] { contextify(context, new PrimitiveDifference(context, true, false)) },
                          DifferenceBuilder.getDifferences(one, two));

    one = new TestObject(new char[] { 'a', 'x', 'c' });
    two = new TestObject(new char[] { 'a', 'y', 'c' });
    context = initial.sub("arrChar[1]");
    assertEqualsUnordered(new Object[] { contextify(context, new PrimitiveDifference(context, 'x', 'y')) },
                          DifferenceBuilder.getDifferences(one, two));

    one = new TestObject(new byte[] { (byte) 14, (byte) 14, (byte) 15 });
    two = new TestObject(new byte[] { (byte) 15, (byte) 14, (byte) 15 });
    context = initial.sub("arrByte[0]");
    assertEqualsUnordered(new Object[] { contextify(context, new PrimitiveDifference(context, (byte) 14, (byte) 15)) },
                          DifferenceBuilder.getDifferences(one, two));

    one = new TestObject(new short[] { (short) 3, (short) 4, (short) 14 });
    two = new TestObject(new short[] { (short) 3, (short) 4, (short) 15 });
    context = initial.sub("arrShort[2]");
    assertEqualsUnordered(
                          new Object[] { contextify(context, new PrimitiveDifference(context, (short) 14, (short) 15)) },
                          DifferenceBuilder.getDifferences(one, two));

    one = new TestObject(new int[] { 3, 14, 5 });
    two = new TestObject(new int[] { 3, 15, 5 });
    context = initial.sub("arrInt[1]");
    assertEqualsUnordered(new Object[] { contextify(context, new PrimitiveDifference(context, 14, 15)) },
                          DifferenceBuilder.getDifferences(one, two));

    one = new TestObject(new long[] { 14, 3, 4 });
    two = new TestObject(new long[] { 15, 3, 4 });
    context = initial.sub("arrLong[0]");
    assertEqualsUnordered(new Object[] { contextify(context, new PrimitiveDifference(context, 14L, 15L)) },
                          DifferenceBuilder.getDifferences(one, two));

    one = new TestObject(new float[] { 3.0f, 4.0f, 14.0f });
    two = new TestObject(new float[] { 3.0f, 4.0f, 15.0f });
    context = initial.sub("arrFloat[2]");
    assertEqualsUnordered(new Object[] { contextify(context, new PrimitiveDifference(context, 14.0f, 15.0f)) },
                          DifferenceBuilder.getDifferences(one, two));

    one = new TestObject(new double[] { 3.0, 14.0, 5.0 });
    two = new TestObject(new double[] { 3.0, 15.0, 5.0 });
    context = initial.sub("arrDouble[1]");
    assertEqualsUnordered(new Object[] { contextify(context, new PrimitiveDifference(context, 14.0, 15.0)) },
                          DifferenceBuilder.getDifferences(one, two));
  }

  private Difference contextify(DifferenceContext context, Difference difference) {
    context.addDifference(difference);
    return difference;
  }

}