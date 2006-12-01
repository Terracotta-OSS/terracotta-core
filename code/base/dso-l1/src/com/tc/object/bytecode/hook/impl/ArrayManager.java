/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode.hook.impl;

import com.tc.object.TCObject;
import com.tc.object.util.IdentityWeakHashMap;
import com.tc.util.Assert;

import java.util.HashMap;
import java.util.Map;

/**
 * Global array manager. The basic purpose of this class to maintain the relationship to DSO managed arrays to their
 * respective TCObject
 */
public class ArrayManager {

  // some day we might want to make this stuff externally configurable
  private final static int        CACHE_DEPTH      = 2;
  private final static int        NUM_MAPS         = 32;
  private final static int        INITIAL_CAPACITY = 25000;
  private final static float      LOAD_FACTOR      = 0.75F;

  private static final Map[]      maps             = new Map[NUM_MAPS];
  private static final Map        primClasses      = new HashMap();
  private static final Object[]   keys             = new Object[NUM_MAPS * CACHE_DEPTH];
  private static final TCObject[] values           = new TCObject[NUM_MAPS * CACHE_DEPTH];

  static {
    for (int i = 0; i < maps.length; i++) {
      maps[i] = new IdentityWeakHashMap(INITIAL_CAPACITY, LOAD_FACTOR);
    }
  }

  private ArrayManager() {
    // not to be instantiated
  }

  public static void register(Object array, TCObject tco) {
    if ((array == null) || (tco == null)) { throw new NullPointerException(); }

    final int index = array.hashCode() % NUM_MAPS;
    final Map map = maps[index];
    final int start = index * CACHE_DEPTH;
    final int end = start + CACHE_DEPTH;
    final Object prev;

    synchronized (map) {
      for (int i = start; i < end; i++) {
        if (keys[i] == array) {
          values[i] = tco;
          break;
        }
      }

      prev = map.put(array, tco);
    }
    if (prev != null) { throw new AssertionError("replaced mapping for " + array); }
  }

  public static TCObject getObject(Object array) {
    final int hash = array.hashCode();
    final int index = hash % NUM_MAPS;
    final Map map = maps[index];
    final int start = index * CACHE_DEPTH;
    final int end = start + CACHE_DEPTH;

    synchronized (map) {
      for (int i = start; i < end; i++) {
        if (keys[i] == array) { return values[i]; }
      }

      int evict = start + (hash % CACHE_DEPTH);
      TCObject rv = (TCObject) map.get(array);
      keys[evict] = array;
      values[evict] = rv;
      return rv;
    }
  }

  public static TCObject getCloneObject(Object array) {
    return getObject(array);
  }

  // For java.lang.reflect.Array.get()
  public static Object get(Object array, int index) {
    if (array == null) throw new NullPointerException();

    if (array instanceof boolean[]) return ((boolean[]) array)[index] ? Boolean.TRUE : Boolean.FALSE;
    if (array instanceof byte[]) return new Byte(((byte[]) array)[index]);
    if (array instanceof char[]) return new Character(((char[]) array)[index]);
    if (array instanceof short[]) return new Short(((short[]) array)[index]);
    if (array instanceof int[]) return new Integer(((int[]) array)[index]);
    if (array instanceof long[]) return new Long(((long[]) array)[index]);
    if (array instanceof float[]) return new Float(((float[]) array)[index]);
    if (array instanceof double[]) return new Double(((double[]) array)[index]);

    if (array instanceof Object[]) {
      TCObject tco = getObject(array);
      if (tco != null) {
        synchronized (tco.getResolveLock()) {
          tco.resolveArrayReference(index);
          return ((Object[]) array)[index];
        }
      } else {
        return ((Object[]) array)[index];
      }
    }

    throw new IllegalArgumentException("Not an array type: " + array.getClass().getName());

  }

  public static void objectArrayChanged(Object[] array, int index, Object value) {
    Object existingObj = array[index]; // do array operation first (fail fast, NPE and array index out of bounds)
    if (false && existingObj != existingObj) Assert.fail(); // silence compiler warning

    TCObject tco = getObject(array);
    if (tco != null) {
      tco.objectFieldChanged(array.getClass().getName(), null, value, index);
    }
    array[index] = value;
  }

  public static void shortArrayChanged(short[] array, int index, short value) {
    short existingVal = array[index]; // do array operation first (fail fast, NPE and array index out of bounds)
    if (false && existingVal != existingVal) Assert.fail(); // silence compiler warning

    TCObject tco = getObject(array);
    if (tco != null) {
      tco.shortFieldChanged(null, null, value, index);
    }
    array[index] = value;
  }

  public static void longArrayChanged(long[] array, int index, long value) {
    long existingVal = array[index]; // do array operation first (fail fast, NPE and array index out of bounds)
    if (false && existingVal != existingVal) Assert.fail(); // silence compiler warning

    TCObject tco = getObject(array);
    if (tco != null) {
      tco.longFieldChanged(null, null, value, index);
    }
    array[index] = value;
  }

  public static void intArrayChanged(int[] array, int index, int value) {
    int existingVal = array[index]; // do array operation first (fail fast, NPE and array index out of bounds)
    if (false && existingVal != existingVal) Assert.fail(); // silence compiler warning

    TCObject tco = getObject(array);
    if (tco != null) {
      tco.intFieldChanged(null, null, value, index);
    }
    array[index] = value;
  }

  public static void floatArrayChanged(float[] array, int index, float value) {
    float existingVal = array[index]; // do array operation first (fail fast, NPE and array index out of bounds)
    if (false && existingVal != existingVal) Assert.fail(); // silence compiler warning

    TCObject tco = getObject(array);
    if (tco != null) {
      tco.floatFieldChanged(null, null, value, index);
    }
    array[index] = value;
  }

  public static void doubleArrayChanged(double[] array, int index, double value) {
    double existingVal = array[index]; // do array operation first (fail fast, NPE and array index out of bounds)
    if (false && existingVal != existingVal) Assert.fail(); // silence compiler warning

    TCObject tco = getObject(array);
    if (tco != null) {
      tco.doubleFieldChanged(null, null, value, index);
    }
    array[index] = value;
  }

  public static void charArrayChanged(char[] array, int index, char value) {
    char existingVal = array[index]; // do array operation first (fail fast, NPE and array index out of bounds)
    if (false && existingVal != existingVal) Assert.fail(); // silence compiler warning

    TCObject tco = getObject(array);
    if (tco != null) {
      tco.charFieldChanged(null, null, value, index);
    }
    array[index] = value;
  }

  public static void byteOrBooleanArrayChanged(Object array, int index, byte value) {
    if (array == null) { throw new NullPointerException(); }

    // Hack to deal with the fact that booleans and bytes are treated the same in bytecode
    if (array.getClass().getComponentType().equals(Boolean.TYPE)) {
      boolean[] booleanArray = (boolean[]) array;
      boolean existingBooleanVal = booleanArray[index]; // do array operation first (fail fast, NPE and array index out
      // of bounds)
      if (false && existingBooleanVal != existingBooleanVal) Assert.fail(); // silence compiler warning
      boolean booleanValue = value == 1;

      TCObject tco = getObject(array);
      if (tco != null) {
        tco.booleanFieldChanged(null, null, booleanValue, index);
      }
      booleanArray[index] = booleanValue;
    } else {
      byte[] byteArray = (byte[]) array;
      byte existingByteVal = byteArray[index]; // do array operation first (fail fast, NPE and array index out of
      // bounds)
      if (false && existingByteVal != existingByteVal) Assert.fail(); // silence compiler warning

      TCObject tco = getObject(array);
      if (tco != null) {
        tco.byteFieldChanged(null, null, value, index);
      }
      byteArray[index] = value;
    }
  }

  public static void arraycopy(final Object src, final int srcPos, final Object dest, final int destPos,
                               final int length) {
    // preserve behavior of System.arraycopy()
    if ((src == null) || (dest == null)) { throw new NullPointerException(); }

    TCObject tcDest = getObject(dest);
    Class destType = dest.getClass().getComponentType();
    if (destType == null) { throw new ArrayStoreException(); }

    boolean isDestPrimitive = destType.isPrimitive();

    // copying into a primitive non-managed array doesn't need any special treatment (even if the source array is
    // managed. If the source array is managed and non-primitive, you'll get an ArrayStoreException as expected)
    if (isDestPrimitive && tcDest == null) {
      System.arraycopy(src, srcPos, dest, destPos, length);
      return;
    }

    // avoid this lookup if we returned above
    TCObject tcSrc = getObject(src);

    if ((tcDest != null) || (tcSrc != null)) {

      Class srcType = src.getClass().getComponentType();
      if (srcType == null) { throw new ArrayStoreException(); }
      boolean isSrcPrimitive = srcType.isPrimitive();

      if (isDestPrimitive) {
        int destCode = getCodeForType(destType);

        // Check if both arrays have the same primitive types. If not, throw an ArrayStoreException.
        if (isSrcPrimitive) {
          int srcCode = getCodeForType(srcType);
          if (srcCode != destCode) { throw new ArrayStoreException(); }
        } else {
          throw new ArrayStoreException();
        }

        switch (destCode) {
          case BOOLEAN:
            booleanArrayCopy((boolean[]) src, srcPos, (boolean[]) dest, destPos, length, tcDest);
            break;
          case BYTE:
            byteArrayCopy((byte[]) src, srcPos, (byte[]) dest, destPos, length, tcDest);
            break;
          case CHAR:
            charArrayCopy((char[]) src, srcPos, (char[]) dest, destPos, length, tcDest);
            break;
          case DOUBLE:
            doubleArrayCopy((double[]) src, srcPos, (double[]) dest, destPos, length, tcDest);
            break;
          case FLOAT:
            floatArrayCopy((float[]) src, srcPos, (float[]) dest, destPos, length, tcDest);
            break;
          case INT:
            intArrayCopy((int[]) src, srcPos, (int[]) dest, destPos, length, tcDest);
            break;
          case LONG:
            longArrayCopy((long[]) src, srcPos, (long[]) dest, destPos, length, tcDest);
            break;
          case SHORT:
            shortArrayCopy((short[]) src, srcPos, (short[]) dest, destPos, length, tcDest);
            break;
          default:
            throw Assert.failure("unexpected type code: " + destCode);
        }
      } else {
        if (isSrcPrimitive) { throw new ArrayStoreException(); }

        Object[] destArray = (Object[]) dest;
        Object[] srcArray = (Object[]) src;

        if ((srcPos < 0) || (destPos < 0) || (length < 0) || (srcPos + length > srcArray.length)
            || (destPos + length > destArray.length)) throw new ArrayIndexOutOfBoundsException();

        Object[] l2subset = new Object[length];

        if (tcSrc != null) {
          synchronized (tcSrc.getResolveLock()) {
            for (int i = 0; i < length; i++) {
              tcSrc.resolveArrayReference(srcPos + i);
              // this read of the source array MUST be performed under the resolve lock
              l2subset[i] = srcArray[srcPos + i];
            }
          }
        } else {
          System.arraycopy(src, srcPos, l2subset, 0, length);
        }

        // make a second copy because the l2subset can be mutated (refs --> ObjectID)
        Object[] localSubset = new Object[length];
        System.arraycopy(l2subset, 0, localSubset, 0, length);

        if (tcDest != null) {
          tcDest.objectArrayChanged(destPos, l2subset, length);
        }

        // only mutate the local array once DSO has a chance to throw portability and TXN exceptions (above)
        System.arraycopy(localSubset, 0, dest, destPos, length);
      }
    } else {
      // no managed arrays in the mix, just do a regular arraycopy
      System.arraycopy(src, srcPos, dest, destPos, length);
    }
  }

  private static int getCodeForType(Class type) {
    Integer code = (Integer) primClasses.get(type);
    if (code == null) { throw new RuntimeException("No code for type " + type); }
    return code.intValue();
  }

  private static void booleanArrayCopy(boolean[] src, int srcPos, boolean[] dest, int destPos, int length,
                                       TCObject tcDest) {
    if ((srcPos < 0) || (destPos < 0) || (length < 0) || (srcPos + length > src.length)
        || (destPos + length > dest.length)) throw new ArrayIndexOutOfBoundsException();

    boolean[] l2subset = new boolean[length];
    System.arraycopy(src, srcPos, l2subset, 0, length);
    tcDest.primitiveArrayChanged(destPos, l2subset, length);

    // don't mutate local objects until DSO accepts/records the change (above)
    System.arraycopy(l2subset, 0, dest, destPos, length);
  }

  private static void byteArrayCopy(byte[] src, int srcPos, byte[] dest, int destPos, int length, TCObject tcDest) {
    if ((srcPos < 0) || (destPos < 0) || (length < 0) || (srcPos + length > src.length)
        || (destPos + length > dest.length)) throw new ArrayIndexOutOfBoundsException();

    byte[] l2subset = new byte[length];
    System.arraycopy(src, srcPos, l2subset, 0, length);
    tcDest.primitiveArrayChanged(destPos, l2subset, length);

    // don't mutate local objects until DSO accepts/records the change (above)
    System.arraycopy(l2subset, 0, dest, destPos, length);
  }

  public static void charArrayCopy(char[] src, int srcPos, char[] dest, int destPos, int length, TCObject tcDest) {
    if ((srcPos < 0) || (destPos < 0) || (length < 0) || (srcPos + length > src.length)
        || (destPos + length > dest.length)) throw new ArrayIndexOutOfBoundsException();

    char[] l2subset = new char[length];
    System.arraycopy(src, srcPos, l2subset, 0, length);
    tcDest.primitiveArrayChanged(destPos, l2subset, length);

    // don't mutate local objects until DSO accepts/records the change (above)
    System.arraycopy(l2subset, 0, dest, destPos, length);
  }

  private static void doubleArrayCopy(double[] src, int srcPos, double[] dest, int destPos, int length, TCObject tcDest) {
    if ((srcPos < 0) || (destPos < 0) || (length < 0) || (srcPos + length > src.length)
        || (destPos + length > dest.length)) throw new ArrayIndexOutOfBoundsException();

    double[] l2subset = new double[length];
    System.arraycopy(src, srcPos, l2subset, 0, length);
    tcDest.primitiveArrayChanged(destPos, l2subset, length);

    // don't mutate local objects until DSO accepts/records the change (above)
    System.arraycopy(l2subset, 0, dest, destPos, length);
  }

  private static void floatArrayCopy(float[] src, int srcPos, float[] dest, int destPos, int length, TCObject tcDest) {
    if ((srcPos < 0) || (destPos < 0) || (length < 0) || (srcPos + length > src.length)
        || (destPos + length > dest.length)) throw new ArrayIndexOutOfBoundsException();

    float[] l2subset = new float[length];
    System.arraycopy(src, srcPos, l2subset, 0, length);
    tcDest.primitiveArrayChanged(destPos, l2subset, length);

    // don't mutate local objects until DSO accepts/records the change (above)
    System.arraycopy(l2subset, 0, dest, destPos, length);
  }

  private static void intArrayCopy(int[] src, int srcPos, int[] dest, int destPos, int length, TCObject tcDest) {
    if ((srcPos < 0) || (destPos < 0) || (length < 0) || (srcPos + length > src.length)
        || (destPos + length > dest.length)) throw new ArrayIndexOutOfBoundsException();

    int[] l2subset = new int[length];
    System.arraycopy(src, srcPos, l2subset, 0, length);
    tcDest.primitiveArrayChanged(destPos, l2subset, length);

    // don't mutate local objects until DSO accepts/records the change (above)
    System.arraycopy(l2subset, 0, dest, destPos, length);
  }

  private static void longArrayCopy(long[] src, int srcPos, long[] dest, int destPos, int length, TCObject tcDest) {
    if ((srcPos < 0) || (destPos < 0) || (length < 0) || (srcPos + length > src.length)
        || (destPos + length > dest.length)) throw new ArrayIndexOutOfBoundsException();

    long[] l2subset = new long[length];
    System.arraycopy(src, srcPos, l2subset, 0, length);
    tcDest.primitiveArrayChanged(destPos, l2subset, length);

    // don't mutate local objects until DSO accepts/records the change (above)
    System.arraycopy(l2subset, 0, dest, destPos, length);
  }

  private static void shortArrayCopy(short[] src, int srcPos, short[] dest, int destPos, int length, TCObject tcDest) {
    if ((srcPos < 0) || (destPos < 0) || (length < 0) || (srcPos + length > src.length)
        || (destPos + length > dest.length)) throw new ArrayIndexOutOfBoundsException();

    short[] l2subset = new short[length];
    System.arraycopy(src, srcPos, l2subset, 0, length);
    tcDest.primitiveArrayChanged(destPos, l2subset, length);

    // don't mutate local objects until DSO accepts/records the change (above)
    System.arraycopy(l2subset, 0, dest, destPos, length);
  }

  private static final int BOOLEAN = 1;
  private static final int BYTE    = 2;
  private static final int CHAR    = 3;
  private static final int DOUBLE  = 4;
  private static final int FLOAT   = 5;
  private static final int INT     = 6;
  private static final int LONG    = 7;
  private static final int SHORT   = 8;

  static {
    primClasses.put(Boolean.TYPE, new Integer(BOOLEAN));
    primClasses.put(Byte.TYPE, new Integer(BYTE));
    primClasses.put(Character.TYPE, new Integer(CHAR));
    primClasses.put(Double.TYPE, new Integer(DOUBLE));
    primClasses.put(Float.TYPE, new Integer(FLOAT));
    primClasses.put(Integer.TYPE, new Integer(INT));
    primClasses.put(Long.TYPE, new Integer(LONG));
    primClasses.put(Short.TYPE, new Integer(SHORT));
  }

}
