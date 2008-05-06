/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import org.apache.commons.lang.ClassUtils;

/**
 * A {@link Stringifier}that generally uses {@link Object#toString()}on objects, but uses special cases for certain
 * objects (like arrays) for which {@link Object#toString()}doesn't do a very good job.
 */
public class StandardStringifier implements Stringifier {
  
  public static final StandardStringifier INSTANCE = new StandardStringifier();
  
  private StandardStringifier() {
    // Use INSTANCE instead.
  }

  public String toString(Object o) {
    if (o == null) return "<null>";
    else if (o instanceof Object[]) {
      return toString((Object[]) o);
    } else if (o instanceof byte[]) {
      return toString((byte[]) o);
    } else if (o instanceof short[]) {
      return toString((short[]) o);
    } else if (o instanceof int[]) {
      return toString((int[]) o);
    } else if (o instanceof long[]) {
      return toString((long[]) o);
    } else if (o instanceof boolean[]) {
      return toString((boolean[]) o);
    } else if (o instanceof char[]) {
      return toString((char[]) o);
    } else if (o instanceof float[]) {
      return toString((float[]) o);
    } else if (o instanceof double[]) {
      return toString((double[]) o);
    } else {
      return o.toString();
    }
  }
  
  private static final int MAX_ITEMS_TO_SHOW = 20;
  
  private String toString(Object[] arr) {
    StringBuffer out = new StringBuffer();
    
    out.append(ClassUtils.getShortClassName(arr.getClass().getComponentType()) + "[" + arr.length + "]");
    if (arr.length > 0) {
      out.append(": ");
      int count = Math.min(arr.length, MAX_ITEMS_TO_SHOW);
      for (int i = 0; i < count; ++i) {
        if (i > 0) out.append(", ");
        out.append(toString(arr[i]));
      }
      
      if (arr.length > count) {
        out.append(", ...");
      }
    }
    
    return out.toString();
  }

  private String toString(byte[] arr) {
    return HexDump.dump(arr);
  }

  private String toString(short[] arr) {
    StringBuffer out = new StringBuffer();
    
    out.append(ClassUtils.getShortClassName(arr.getClass().getComponentType()) + "[" + arr.length + "]");
    if (arr.length > 0) {
      out.append(": ");
      int count = Math.min(arr.length, MAX_ITEMS_TO_SHOW);
      for (int i = 0; i < count; ++i) {
        if (i > 0) out.append(", ");
        out.append("" + arr[i]);
      }
      
      if (arr.length > count) {
        out.append(", ...");
      }
    }
    
    return out.toString();
  }

  private String toString(int[] arr) {
    StringBuffer out = new StringBuffer();
    
    out.append(ClassUtils.getShortClassName(arr.getClass().getComponentType()) + "[" + arr.length + "]");
    if (arr.length > 0) {
      out.append(": ");
      int count = Math.min(arr.length, MAX_ITEMS_TO_SHOW);
      for (int i = 0; i < count; ++i) {
        if (i > 0) out.append(", ");
        out.append("" + arr[i]);
      }
      
      if (arr.length > count) {
        out.append(", ...");
      }
    }
    
    return out.toString();
  }

  private String toString(long[] arr) {
    StringBuffer out = new StringBuffer();
    
    out.append(ClassUtils.getShortClassName(arr.getClass().getComponentType()) + "[" + arr.length + "]");
    if (arr.length > 0) {
      out.append(": ");
      int count = Math.min(arr.length, MAX_ITEMS_TO_SHOW);
      for (int i = 0; i < count; ++i) {
        if (i > 0) out.append(", ");
        out.append("" + arr[i]);
      }
      
      if (arr.length > count) {
        out.append(", ...");
      }
    }
    
    return out.toString();
  }

  private String toString(char[] arr) {
    StringBuffer out = new StringBuffer();
    
    out.append(ClassUtils.getShortClassName(arr.getClass().getComponentType()) + "[" + arr.length + "]");
    if (arr.length > 0) {
      out.append(": ");
      int count = Math.min(arr.length, MAX_ITEMS_TO_SHOW);
      for (int i = 0; i < count; ++i) {
        if (i > 0) out.append(", ");
        out.append("" + arr[i]);
      }
      
      if (arr.length > count) {
        out.append(", ...");
      }
    }
    
    return out.toString();
  }

  private String toString(float[] arr) {
    StringBuffer out = new StringBuffer();
    
    out.append(ClassUtils.getShortClassName(arr.getClass().getComponentType()) + "[" + arr.length + "]");
    if (arr.length > 0) {
      out.append(": ");
      int count = Math.min(arr.length, MAX_ITEMS_TO_SHOW);
      for (int i = 0; i < count; ++i) {
        if (i > 0) out.append(", ");
        out.append("" + arr[i]);
      }
      
      if (arr.length > count) {
        out.append(", ...");
      }
    }
    
    return out.toString();
  }

  private String toString(double[] arr) {
    StringBuffer out = new StringBuffer();
    
    out.append(ClassUtils.getShortClassName(arr.getClass().getComponentType()) + "[" + arr.length + "]");
    if (arr.length > 0) {
      out.append(": ");
      int count = Math.min(arr.length, MAX_ITEMS_TO_SHOW);
      for (int i = 0; i < count; ++i) {
        if (i > 0) out.append(", ");
        out.append("" + arr[i]);
      }
      
      if (arr.length > count) {
        out.append(", ...");
      }
    }
    
    return out.toString();
  }

  private String toString(boolean[] arr) {
    StringBuffer out = new StringBuffer();
    
    out.append(ClassUtils.getShortClassName(arr.getClass().getComponentType()) + "[" + arr.length + "]");
    if (arr.length > 0) {
      out.append(": ");
      int count = Math.min(arr.length, MAX_ITEMS_TO_SHOW);
      for (int i = 0; i < count; ++i) {
        if (i > 0) out.append(", ");
        out.append("" + arr[i]);
      }
      
      if (arr.length > count) {
        out.append(", ...");
      }
    }
    
    return out.toString();
  }

}
