/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import org.apache.commons.lang.ArrayUtils;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.object.util.ReadOnlyException;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import gnu.trove.TObjectIntHashMap;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class PrimitiveArrayTestApp extends AbstractErrorCatchingTransparentApp {

  private static final int               BOOLEAN         = 1;
  private static final int               BYTE            = 2;
  private static final int               CHAR            = 3;
  private static final int               DOUBLE          = 4;
  private static final int               FLOAT           = 5;
  private static final int               INT             = 6;
  private static final int               LONG            = 7;
  private static final int               SHORT           = 8;

  private static final int               BOOLEAN_WRAPPER = 9;
  private static final int               BYTE_WRAPPER    = 10;
  private static final int               CHAR_WRAPPER    = 11;
  private static final int               DOUBLE_WRAPPER  = 12;
  private static final int               FLOAT_WRAPPER   = 13;
  private static final int               INT_WRAPPER     = 14;
  private static final int               LONG_WRAPPER    = 15;
  private static final int               SHORT_WRAPPER   = 16;

  private static final TObjectIntHashMap classToInt      = new TObjectIntHashMap();
  private static final SecureRandom      secure          = new SecureRandom();

  static {
    classToInt.put(Boolean.TYPE, BOOLEAN);
    classToInt.put(Byte.TYPE, BYTE);
    classToInt.put(Character.TYPE, CHAR);
    classToInt.put(Double.TYPE, DOUBLE);
    classToInt.put(Float.TYPE, FLOAT);
    classToInt.put(Integer.TYPE, INT);
    classToInt.put(Long.TYPE, LONG);
    classToInt.put(Short.TYPE, SHORT);

    classToInt.put(Boolean.class, BOOLEAN_WRAPPER);
    classToInt.put(Byte.class, BYTE_WRAPPER);
    classToInt.put(Character.class, CHAR_WRAPPER);
    classToInt.put(Double.class, DOUBLE_WRAPPER);
    classToInt.put(Float.class, FLOAT_WRAPPER);
    classToInt.put(Integer.class, INT_WRAPPER);
    classToInt.put(Long.class, LONG_WRAPPER);
    classToInt.put(Short.class, SHORT_WRAPPER);
  }

  // roots
  private final DataRoot                 root            = new DataRoot();
  private final CyclicBarrier            barrier;

  public PrimitiveArrayTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    this.barrier = new CyclicBarrier(getParticipantCount());
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = PrimitiveArrayTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    String writeAllowedMethodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(writeAllowedMethodExpression);

    String readOnlyMethodExpression = "* " + testClass + "*.*ReadOnly*(..)";
    config.addReadAutolock(readOnlyMethodExpression);

    spec.addRoot("root", "the-data-root-yo");
    spec.addRoot("barrier", "barrier");
    config.addIncludePattern(DataRoot.class.getName());
    new CyclicBarrierSpec().visit(visitor, config);
  }

  protected void runTest() throws Throwable {
    Object array = root.getArray();
    Class type = array.getClass().getComponentType();
    Object compare = root.getCompareData(type);

    // wait for all nodes to get arrays
    barrier.barrier();

    // make sure all the data was consumed
    if (root.getIndex() != root.getNumArrays()) { throw new RuntimeException("Not all data consumed"); }

    // read only tests.
    readOnlyTest(array);

    // calling this has the side effect of making sure none of the shared arrays have actually changed yet
    validate(false);

    // wait for all read only tests to finish.
    barrier.barrier();

    // modify my one type locally
    modifyDataWithWriteLock(array, intClassType(type));

    // wait for all nodes to do modifications
    barrier.barrier();

    // validate all the changes made by the other nodes
    validate(true);

    // wait for all nodes to do validations
    barrier.barrier();

    synchronized (array) {
      // test System.arrayCopy()
      System.arraycopy(compare, 0, array, 0, Array.getLength(array));
    }

    // wait for all nodes to do finish arraycopy (data should be back to original values)
    barrier.barrier();

    // validate the arraycopy calls
    validate(false);
  }

  private void readOnlyTest(Object array) {
    Class type = array.getClass().getComponentType();

    try {
      tryModifyDataWithReadOnlyLock(array, intClassType(type));
      throw new AssertionError("I should have thrown an ReadOnlyException, type " + type.getName());
    } catch (ReadOnlyException roe) {
      // expected
    }
  }

  private void differentDataError(Object a1, Object a2, Class type) {
    String msg = "Data of type [" + type + "] not equal\n";
    msg += ArrayUtils.toString(a1) + "\n\n";
    msg += ArrayUtils.toString(a2);
    throw new RuntimeException(msg);
  }

  private void validate(boolean withMods) {
    for (Iterator iter = root.getArrayIterator(); iter.hasNext();) {
      Object actual = iter.next();
      Class type = actual.getClass().getComponentType();
      Object expect = root.getCompareData(type);

      synchronized (actual) {
        if (withMods) {
          modifyData(expect, intClassType(type));
        }
  
        if (!compareData(actual, expect)) {
          differentDataError(actual, expect, type);
        }
      }
    }
  }

  private static void tryModifyDataWithReadOnlyLock(Object array, int type) {
    synchronized (array) {
      modifyData(array, type);
    }
  }

  /*
   * The synchronized statement inside each individual modify data method such as modifyBoolean() are moved to the
   * caller method in order to provide both test for modifying data with read only lock and write lock.
   */
  private static void modifyDataWithWriteLock(Object array, int type) {
    synchronized (array) {
      modifyData(array, type);
    }
  }

  private static void modifyData(Object array, int type) {
    switch (type) {
      case BOOLEAN:
        modifyBoolean((boolean[]) array);
        break;
      case BYTE:
        modifyByte((byte[]) array);
        break;
      case CHAR:
        modifyChar((char[]) array);
        break;
      case DOUBLE:
        modifyDouble((double[]) array);
        break;
      case FLOAT:
        modifyFloat((float[]) array);
        break;
      case INT:
        modifyInt((int[]) array);
        break;
      case LONG:
        modifyLong((long[]) array);
        break;
      case SHORT:
        modifyShort((short[]) array);
        break;
      case BOOLEAN_WRAPPER:
        modifyBooleanWrapper((Boolean[]) array);
        break;
      case BYTE_WRAPPER:
        modifyByteWrapper((Byte[]) array);
        break;
      case CHAR_WRAPPER:
        modifyCharWrapper((Character[]) array);
        break;
      case DOUBLE_WRAPPER:
        modifyDoubleWrapper((Double[]) array);
        break;
      case FLOAT_WRAPPER:
        modifyFloatWrapper((Float[]) array);
        break;
      case INT_WRAPPER:
        modifyIntegerWrapper((Integer[]) array);
        break;
      case LONG_WRAPPER:
        modifyLongWrapper((Long[]) array);
        break;
      case SHORT_WRAPPER:
        modifyShortWrapper((Short[]) array);
        break;
      default:
        throw new RuntimeException("bad type " + type);
    }
  }

  private static void modifyShortWrapper(Short[] s) {
    for (int i = 0; i < s.length; i++) {
      s[i] = new Short((short) (s[i].shortValue() + 1));
    }
  }

  private static void modifyLongWrapper(Long[] l) {
    for (int i = 0; i < l.length; i++) {
      l[i] = new Long(l[i].longValue() + 1);
    }
  }

  private static void modifyIntegerWrapper(Integer[] i) {
    for (int x = 0; x < i.length; x++) {
      i[x] = new Integer(i[x].intValue() + 1);
    }
  }

  private static void modifyFloatWrapper(Float[] f) {
    for (int i = 0; i < f.length; i++) {
      f[i] = new Float(f[i].floatValue() + 1);
    }
  }

  private static void modifyDoubleWrapper(Double[] d) {
    for (int i = 0; i < d.length; i++) {
      d[i] = new Double(d[i].doubleValue() + 1);
    }
  }

  private static void modifyCharWrapper(Character[] c) {
    for (int i = 0; i < c.length; i++) {
      c[i] = new Character((char) (c[i].charValue() + 1));
    }
  }

  private static void modifyByteWrapper(Byte[] b) {
    for (int i = 0; i < b.length; i++) {
      b[i] = new Byte((byte) (b[i].byteValue() + 1));
    }
  }

  private static void modifyBooleanWrapper(Boolean[] b) {
    for (int i = 0; i < b.length; i++) {
      b[i] = new Boolean(!b[i].booleanValue());
    }
  }

  private static void modifyShort(short[] s) {
    for (int i = 0; i < s.length; i++) {
      s[i]++;
    }
  }

  private static void modifyLong(long[] l) {
    for (int i = 0; i < l.length; i++) {
      l[i]++;
    }
  }

  private static void modifyInt(int[] i) {
    for (int x = 0; x < i.length; x++) {
      i[x]++;
    }
  }

  private static void modifyFloat(float[] f) {
    for (int i = 0; i < f.length; i++) {
      f[i]++;
    }
  }

  private static void modifyDouble(double[] d) {
    for (int i = 0; i < d.length; i++) {
      d[i]++;
    }
  }

  private static void modifyChar(char[] c) {
    for (int i = 0; i < c.length; i++) {
      c[i]++;
    }
  }

  private static void modifyByte(byte[] b) {
    for (int i = 0; i < b.length; i++) {
      b[i]++;
    }
  }

  private static void modifyBoolean(boolean[] b) {
    for (int i = 0; i < b.length; i++) {
      b[i] = !b[i];
    }
  }

  private static boolean compareData(Object array, Object compare) {
    int type = intClassType(array.getClass().getComponentType());

    switch (type) {
      case BOOLEAN:
        return Arrays.equals((boolean[]) array, (boolean[]) compare);
      case BYTE:
        return Arrays.equals((byte[]) array, (byte[]) compare);
      case CHAR:
        return Arrays.equals((char[]) array, (char[]) compare);
      case DOUBLE:
        return Arrays.equals((double[]) array, (double[]) compare);
      case FLOAT:
        return Arrays.equals((float[]) array, (float[]) compare);
      case INT:
        return Arrays.equals((int[]) array, (int[]) compare);
      case LONG:
        return Arrays.equals((long[]) array, (long[]) compare);
      case SHORT:
        return Arrays.equals((short[]) array, (short[]) compare);
      default:
        return Arrays.equals((Object[]) array, (Object[]) compare);
    }

    // unreachable
  }

  private static Object createRandomArray(Random random, Class type) {
    int length = 10 + random.nextInt(100);
    Assert.assertTrue("length = " + length, length > 0);

    switch (intClassType(type)) {
      case BOOLEAN:
        boolean[] b = new boolean[length];
        for (int i = 0; i < b.length; i++) {
          b[i] = random.nextBoolean();
        }
        return b;
      case BYTE:
        return makeBuffer(random, length).array();
      case CHAR:
        char[] c = new char[length];
        makeBuffer(random, length * 2).asCharBuffer().get(c);
        return c;
      case DOUBLE:
        double[] d = new double[length];
        makeBuffer(random, length * 8).asDoubleBuffer().get(d);
        return d;
      case FLOAT:
        float f[] = new float[length];
        makeBuffer(random, length * 4).asFloatBuffer().get(f);
        return f;
      case INT:
        int[] i = new int[length];
        makeBuffer(random, length * 4).asIntBuffer().get(i);
        return i;
      case LONG:
        long[] l = new long[length];
        makeBuffer(random, length * 8).asLongBuffer().get(l);
        return l;
      case SHORT:
        short[] s = new short[length];
        makeBuffer(random, length * 2).asShortBuffer().get(s);
        return s;
      case BOOLEAN_WRAPPER:
        return ArrayUtils.toObject((boolean[]) createRandomArray(random, Boolean.TYPE));
      case BYTE_WRAPPER:
        return ArrayUtils.toObject((byte[]) createRandomArray(random, Byte.TYPE));
      case CHAR_WRAPPER:
        // I don't know why there isn't char[] version of ArrayUtils.toObject()
        return makeCharacterArray((char[]) createRandomArray(random, Character.TYPE));
      case DOUBLE_WRAPPER:
        return ArrayUtils.toObject((double[]) createRandomArray(random, Double.TYPE));
      case FLOAT_WRAPPER:
        return ArrayUtils.toObject((float[]) createRandomArray(random, Float.TYPE));
      case INT_WRAPPER:
        return ArrayUtils.toObject((int[]) createRandomArray(random, Integer.TYPE));
      case LONG_WRAPPER:
        return ArrayUtils.toObject((long[]) createRandomArray(random, Long.TYPE));
      case SHORT_WRAPPER:
        return ArrayUtils.toObject((short[]) createRandomArray(random, Short.TYPE));
      default:
        throw new RuntimeException("bad type: " + type);
    }
  }

  private static Character[] makeCharacterArray(char[] c) {
    Character rv[] = new Character[c.length];
    for (int i = 0; i < rv.length; i++) {
      rv[i] = new Character(c[i]);
    }
    return rv;
  }

  private static int intClassType(Class type) {
    if (classToInt.containsKey(type)) { return classToInt.get(type); }
    throw new RuntimeException("No mapping for " + type);
  }

  private static ByteBuffer makeBuffer(Random random, int length) {
    byte[] data = new byte[length];
    random.nextBytes(data);
    return ByteBuffer.wrap(data);
  }

  private static class DataRoot {
    private final Map  seeds  = new HashMap();
    private final List arrays = new ArrayList();
    private int        index  = 0;

    DataRoot() {

      boolean[] booleanData = (boolean[]) makeArray(secure.nextLong(), Boolean.TYPE);
      byte[] byteData = (byte[]) makeArray(secure.nextLong(), Byte.TYPE);
      char[] charData = (char[]) makeArray(secure.nextLong(), Character.TYPE);
      double[] doubleData = (double[]) makeArray(secure.nextLong(), Double.TYPE);
      float[] floatData = (float[]) makeArray(secure.nextLong(), Float.TYPE);
      int[] intData = (int[]) makeArray(secure.nextLong(), Integer.TYPE);
      long[] longData = (long[]) makeArray(secure.nextLong(), Long.TYPE);
      short[] shortData = (short[]) makeArray(secure.nextLong(), Short.TYPE);

      Boolean[] booleanWrapperData = (Boolean[]) makeArray(secure.nextLong(), Boolean.class);
      Byte[] byteWrapperData = (Byte[]) makeArray(secure.nextLong(), Byte.class);
      Character[] charWrapperData = (Character[]) makeArray(secure.nextLong(), Character.class);
      Double[] doubleWrapperData = (Double[]) makeArray(secure.nextLong(), Double.class);
      Float[] floatWrapperData = (Float[]) makeArray(secure.nextLong(), Float.class);
      Integer[] intWrapperData = (Integer[]) makeArray(secure.nextLong(), Integer.class);
      Long[] longWrapperData = (Long[]) makeArray(secure.nextLong(), Long.class);
      Short[] shortWrapperData = (Short[]) makeArray(secure.nextLong(), Short.class);

      arrays.add(booleanData);
      arrays.add(byteData);
      arrays.add(charData);
      arrays.add(doubleData);
      arrays.add(floatData);
      arrays.add(intData);
      arrays.add(longData);
      arrays.add(shortData);

      arrays.add(booleanWrapperData);
      arrays.add(byteWrapperData);
      arrays.add(charWrapperData);
      arrays.add(doubleWrapperData);
      arrays.add(floatWrapperData);
      arrays.add(intWrapperData);
      arrays.add(longWrapperData);
      arrays.add(shortWrapperData);
    }

    public Iterator getArrayIterator() {
      return Collections.unmodifiableList(arrays).iterator();
    }

    int getIndex() {
      return this.index;
    }

    int getNumArrays() {
      return this.arrays.size();
    }

    Object getArray() {
      synchronized (arrays) {
        return arrays.get(index++);
      }
    }

    Object getCompareData(Class type) {
      long seed = ((Long) seeds.get(type.getName())).longValue();
      return createRandomArray(new Random(seed), type);
    }

    private Object makeArray(long seed, Class type) {
      System.err.println("Seed for type " + type + "=" + seed);
      seeds.put(type.getName(), new Long(seed));
      return createRandomArray(new Random(seed), type);
    }
  }

}
