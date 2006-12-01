/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.lang;

import com.tc.util.Assert;

import java.util.Arrays;

public class TCEqualsBuilder {

  private boolean isEquals = true;

  public TCEqualsBuilder() {
    // no-op
  }

  public TCEqualsBuilder append(boolean arg0, boolean arg1) {
    if (!isEquals) return this;
    isEquals = arg0 == arg1;
    return this;
  }

  public TCEqualsBuilder append(boolean[] arg0, boolean[] arg1) {
    if (!isEquals) return this;
    isEquals = Arrays.equals(arg0, arg1);
    return this;
  }

  public TCEqualsBuilder append(byte arg0, byte arg1) {
    if (!isEquals) return this;
    isEquals = arg0 == arg1;
    return this;
  }

  public TCEqualsBuilder append(byte[] arg0, byte[] arg1) {
    if (!isEquals) return this;
    isEquals = Arrays.equals(arg0, arg1);
    return this;
  }

  public TCEqualsBuilder append(char arg0, char arg1) {
    if (!isEquals) return this;
    isEquals = arg0 == arg1;
    return this;
  }

  public TCEqualsBuilder append(char[] arg0, char[] arg1) {
    if (!isEquals) return this;
    isEquals = Arrays.equals(arg0, arg1);
    return this;
  }

  public TCEqualsBuilder append(double lhs, double rhs) {
    if (!isEquals) return this;
    isEquals = Double.doubleToLongBits(lhs) == Double.doubleToLongBits(rhs);
    return this;
  }

  public TCEqualsBuilder append(double[] arg0, double[] arg1) {
    if (!isEquals) return this;
    isEquals = Arrays.equals(arg0, arg1);
    return this;
  }

  public TCEqualsBuilder append(float arg0, float arg1) {
    if (!isEquals) return this;
    isEquals = Float.floatToIntBits(arg0) == Float.floatToIntBits(arg1);
    return this;
  }

  public TCEqualsBuilder append(float[] arg0, float[] arg1) {
    if (!isEquals) return this;
    isEquals = Arrays.equals(arg0, arg1);
    return this;
  }

  public TCEqualsBuilder append(int arg0, int arg1) {
    if (!isEquals) return this;
    isEquals = arg0 == arg1;
    return this;
  }

  public TCEqualsBuilder append(int[] arg0, int[] arg1) {
    if (!isEquals) return this;
    isEquals = Arrays.equals(arg0, arg1);
    return this;
  }

  public TCEqualsBuilder append(long arg0, long arg1) {
    if (!isEquals) return this;
    isEquals = arg0 == arg1;
    return this;
  }

  public TCEqualsBuilder append(long[] arg0, long[] arg1) {
    if (!isEquals) return this;
    isEquals = Arrays.equals(arg0, arg1);
    return this;
  }

  public TCEqualsBuilder append(Object lhs, Object rhs) {
    if (!isEquals) return this;
    if (lhs == rhs) return this;
    if (lhs == null || rhs == null) {
      isEquals = false;
      return this;
    }
    Assert.assertNotNull(lhs);
    Assert.assertNotNull(rhs);

    Class lhsClass = lhs.getClass();
    if (!lhsClass.isArray()) {
      isEquals = lhs.equals(rhs);
      return this;
    }
    Class rhsClass = rhs.getClass();
    if (!rhsClass.isArray()) {
      isEquals = rhs.equals(lhs);
      return this;
    }

    Assert.eval(lhsClass.isArray() && rhsClass.isArray());

    if (lhs instanceof long[]) {
      return append((long[]) lhs, (long[]) rhs);
    } else if (lhs instanceof int[]) {
      return append((int[]) lhs, (int[]) rhs);
    } else if (lhs instanceof short[]) {
      return append((short[]) lhs, (short[]) rhs);
    } else if (lhs instanceof char[]) {
      return append((char[]) lhs, (char[]) rhs);
    } else if (lhs instanceof byte[]) {
      return append((byte[]) lhs, (byte[]) rhs);
    } else if (lhs instanceof double[]) {
      return append((double[]) lhs, (double[]) rhs);
    } else if (lhs instanceof float[]) {
      return append((float[]) lhs, (float[]) rhs);
    } else if (lhs instanceof boolean[]) {
      return append((boolean[]) lhs, (boolean[]) rhs);
    } else {
      // Not an array of primitives
      return append((Object[]) lhs, (Object[]) rhs);
    }
  }

  public TCEqualsBuilder append(Object[] lhs, Object[] rhs) {
    if (!isEquals) return this;

    if (lhs == rhs) return this;

    if (lhs == null || rhs == null) {
      isEquals = false;
      return this;
    }

    if (lhs.length != rhs.length) {
      isEquals = false;
      return this;
    }

    for (int i = 0; isEquals && i < lhs.length; i++) {
      append(lhs[i], rhs[i]);
    }
    return this;
  }

  public TCEqualsBuilder append(short lhs, short rhs) {
    if (!isEquals) return this;
    isEquals = lhs == rhs;
    return this;
  }

  public TCEqualsBuilder append(short[] lhs, short[] rhs) {
    if (!isEquals) return this;
    isEquals = Arrays.equals(lhs, rhs);
    return this;
  }

  public TCEqualsBuilder appendSuper(boolean lhs) {
    isEquals = isEquals && lhs;
    return this;
  }

  public boolean isEquals() {
    return isEquals;
  }

}