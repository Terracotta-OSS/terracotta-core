/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.diff;

import org.apache.commons.lang.builder.EqualsBuilder;

import com.tc.util.Assert;
import com.tc.util.StandardStringifier;
import com.tc.util.Stringifier;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * An object like {@link EqualsBuilder}, but for use building the implementation of
 * {@link Differenceable.addDifferences}instead.
 */
public class DifferenceBuilder {

  // private static final String DIFFERENT_CLASS_CONTEXT = "#class";
  private static final Stringifier DEFAULT_STRINGIFIER = StandardStringifier.INSTANCE;

  public static String describeDifferences(Differenceable a, Differenceable b) {
    return describeDifferences(a, b, DEFAULT_STRINGIFIER);
  }
  
  public static String describeDifferences(Differenceable a, Differenceable b, Stringifier stringifier) {
    Difference[] differences = getDifferencesAsArray(a, b);
    if (differences.length == 0) return "";
    
    StringBuffer descrip = new StringBuffer();
    descrip.append("differences are:\n");
    for (int i = 0; i < differences.length; ++i) {
      descrip.append(differences[i].toString());
      descrip.append("\n");
    }

    return descrip.toString();
  }

  public static Iterator getDifferences(Differenceable a, Differenceable b, Stringifier stringifier) {
    Assert.assertNotNull(a);
    Assert.assertNotNull(b);

    DifferenceContext context = DifferenceContext.createInitial(stringifier);

    if (! (a.getClass().equals(b.getClass()))) {
      context.addDifference(new BasicObjectDifference(context, a, b));
    } else {
      a.addDifferences(context, b);
    }
    
    return context.getDifferences();
  }

  public static Iterator getDifferences(Differenceable a, Differenceable b) {
    return getDifferences(a, b, DEFAULT_STRINGIFIER);
  }

  public static Difference[] getDifferencesAsArray(Differenceable a, Differenceable b) {
    return getDifferencesAsArray(a, b, DEFAULT_STRINGIFIER);
  }

  public static Difference[] getDifferencesAsArray(Differenceable a, Differenceable b, Stringifier stringifier) {
    List out = new ArrayList();
    Iterator i = getDifferences(a, b, stringifier);
    while (i.hasNext()) {
      out.add(i.next());
    }

    return (Difference[]) out.toArray(new Difference[out.size()]);
  }

  private final DifferenceContext previous;

  public DifferenceBuilder(DifferenceContext previous) {
    Assert.assertNotNull(previous);

    this.previous = previous;
  }

  public DifferenceBuilder reflectionDifference(Object a, Object b) {
    Assert.assertNotNull(a);
    Assert.assertNotNull(b);
    Assert.eval(a instanceof Differenceable);
    Assert.eval(b instanceof Differenceable);
    Assert.eval(a.getClass().isInstance(b));

    if (a == b) return this;

    Field[] fields = a.getClass().getDeclaredFields();
    AccessibleObject.setAccessible(fields, true);
    for (int i = 0; i < fields.length; ++i) {
      Field f = fields[i];
      if (!Modifier.isStatic(f.getModifiers())) {
        try {
          doReflectiveAppend(f.getName(), f.get(a), f.get(b), f.getType());
        } catch (IllegalAccessException iae) {
          // We should've gotten a SecurityException above, instead.
          throw Assert.failure("This should be impossible", iae);
        }
      }
    }

    return this;
  }

  private void doReflectiveAppend(String context, Object a, Object b, Class c) {
    if (c.isPrimitive()) {
      if (c.equals(Boolean.TYPE)) append(context, ((Boolean) a).booleanValue(), ((Boolean) b).booleanValue());
      else if (c.equals(Character.TYPE)) append(context, ((Character) a).charValue(), ((Character) b).charValue());
      else if (c.equals(Byte.TYPE)) append(context, ((Byte) a).byteValue(), ((Byte) b).byteValue());
      else if (c.equals(Short.TYPE)) append(context, ((Short) a).shortValue(), ((Short) b).shortValue());
      else if (c.equals(Integer.TYPE)) append(context, ((Integer) a).intValue(), ((Integer) b).intValue());
      else if (c.equals(Long.TYPE)) append(context, ((Long) a).longValue(), ((Long) b).longValue());
      else if (c.equals(Float.TYPE)) append(context, ((Float) a).floatValue(), ((Float) b).floatValue());
      else if (c.equals(Double.TYPE)) append(context, ((Double) a).doubleValue(), ((Double) b).doubleValue());
      else throw Assert.failure("Unknown primitive type " + c);
    } else {
      append(context, a, b);
    }
  }

  public DifferenceBuilder append(String context, boolean a, boolean b) {
    if (a != b) add(new PrimitiveDifference(this.previous.sub(context), a, b));
    return this;
  }

  public DifferenceBuilder append(String context, char a, char b) {
    if (a != b) add(new PrimitiveDifference(this.previous.sub(context), a, b));
    return this;
  }

  public DifferenceBuilder append(String context, byte a, byte b) {
    if (a != b) add(new PrimitiveDifference(this.previous.sub(context), a, b));
    return this;
  }

  public DifferenceBuilder append(String context, short a, short b) {
    if (a != b) add(new PrimitiveDifference(this.previous.sub(context), a, b));
    return this;
  }

  public DifferenceBuilder append(String context, int a, int b) {
    if (a != b) add(new PrimitiveDifference(this.previous.sub(context), a, b));
    return this;
  }

  public DifferenceBuilder append(String context, long a, long b) {
    if (a != b) add(new PrimitiveDifference(this.previous.sub(context), a, b));
    return this;
  }

  public DifferenceBuilder append(String context, float a, float b) {
    if (a != b) add(new PrimitiveDifference(this.previous.sub(context), a, b));
    return this;
  }

  public DifferenceBuilder append(String context, double a, double b) {
    if (a != b) add(new PrimitiveDifference(this.previous.sub(context), a, b));
    return this;
  }

  public DifferenceBuilder append(String context, Object a, Object b) {
    if (a != null && b != null) {
      if ((a instanceof Differenceable) && (b instanceof Differenceable) &&
          (a.getClass().equals(b.getClass()))) {
        handleDifferenceables(context, a, b);
      } else if (a.getClass().isArray() && b.getClass().isArray()) {
        handleArrays(context, a, b);
      } else if (!(a.equals(b))) {
        add(new BasicObjectDifference(this.previous.sub(context), a, b));
      }
    } else if (a != null || b != null) {
      add(new BasicObjectDifference(this.previous.sub(context), a, b));
    }

    return this;
  }

  private void handleArrays(String context, Object a, Object b) {
    if ((a.getClass().getComponentType().isPrimitive() || b.getClass().getComponentType().isPrimitive())
        && (!a.getClass().getComponentType().equals(b.getClass().getComponentType()))) {
      add(new BasicObjectDifference(this.previous.sub(context), a, b));
    }

    if (a.getClass().getComponentType().isPrimitive()) {
      if (a instanceof boolean[]) {
        handleArrays(context, (boolean[]) a, (boolean[]) b);
      } else if (a instanceof char[]) {
        handleArrays(context, (char[]) a, (char[]) b);
      } else if (a instanceof byte[]) {
        handleArrays(context, (byte[]) a, (byte[]) b);
      } else if (a instanceof short[]) {
        handleArrays(context, (short[]) a, (short[]) b);
      } else if (a instanceof int[]) {
        handleArrays(context, (int[]) a, (int[]) b);
      } else if (a instanceof long[]) {
        handleArrays(context, (long[]) a, (long[]) b);
      } else if (a instanceof float[]) {
        handleArrays(context, (float[]) a, (float[]) b);
      } else if (a instanceof double[]) {
        handleArrays(context, (double[]) a, (double[]) b);
      } else {
        throw Assert.failure("Unknown primitive type " + a.getClass().getComponentType());
      }
    } else {
      handleArrays(context, (Object[]) a, (Object[]) b);
    }
  }

  private void handleArrays(String context, boolean[] a, boolean[] b) {
    if (a.length != b.length) {
      add(new PrimitiveDifference(this.previous.sub(context + ".length"), a.length, b.length));
    } else {
      for (int i = 0; i < a.length; ++i) {
        if (a[i] != b[i]) add(new PrimitiveDifference(this.previous.sub(context + "[" + i + "]"), a[i], b[i]));
      }
    }
  }

  private void handleArrays(String context, char[] a, char[] b) {
    if (a.length != b.length) {
      add(new PrimitiveDifference(this.previous.sub(context + ".length"), a.length, b.length));
    } else {
      for (int i = 0; i < a.length; ++i) {
        if (a[i] != b[i]) add(new PrimitiveDifference(this.previous.sub(context + "[" + i + "]"), a[i], b[i]));
      }
    }
  }

  private void handleArrays(String context, byte[] a, byte[] b) {
    if (a.length != b.length) {
      add(new PrimitiveDifference(this.previous.sub(context + ".length"), a.length, b.length));
    } else {
      for (int i = 0; i < a.length; ++i) {
        if (a[i] != b[i]) add(new PrimitiveDifference(this.previous.sub(context + "[" + i + "]"), a[i], b[i]));
      }
    }
  }

  private void handleArrays(String context, short[] a, short[] b) {
    if (a.length != b.length) {
      add(new PrimitiveDifference(this.previous.sub(context + ".length"), a.length, b.length));
    } else {
      for (int i = 0; i < a.length; ++i) {
        if (a[i] != b[i]) add(new PrimitiveDifference(this.previous.sub(context + "[" + i + "]"), a[i], b[i]));
      }
    }
  }

  private void handleArrays(String context, int[] a, int[] b) {
    if (a.length != b.length) {
      add(new PrimitiveDifference(this.previous.sub(context + ".length"), a.length, b.length));
    } else {
      for (int i = 0; i < a.length; ++i) {
        if (a[i] != b[i]) add(new PrimitiveDifference(this.previous.sub(context + "[" + i + "]"), a[i], b[i]));
      }
    }
  }

  private void handleArrays(String context, long[] a, long[] b) {
    if (a.length != b.length) {
      add(new PrimitiveDifference(this.previous.sub(context + ".length"), a.length, b.length));
    } else {
      for (int i = 0; i < a.length; ++i) {
        if (a[i] != b[i]) add(new PrimitiveDifference(this.previous.sub(context + "[" + i + "]"), a[i], b[i]));
      }
    }
  }

  private void handleArrays(String context, float[] a, float[] b) {
    if (a.length != b.length) {
      add(new PrimitiveDifference(this.previous.sub(context + ".length"), a.length, b.length));
    } else {
      for (int i = 0; i < a.length; ++i) {
        if (a[i] != b[i]) add(new PrimitiveDifference(this.previous.sub(context + "[" + i + "]"), a[i], b[i]));
      }
    }
  }

  private void handleArrays(String context, double[] a, double[] b) {
    if (a.length != b.length) {
      add(new PrimitiveDifference(this.previous.sub(context + ".length"), a.length, b.length));
    } else {
      for (int i = 0; i < a.length; ++i) {
        if (a[i] != b[i]) add(new PrimitiveDifference(this.previous.sub(context + "[" + i + "]"), a[i], b[i]));
      }
    }
  }

  private void handleArrays(String context, Object[] a, Object[] b) {
    if (a.length != b.length) {
      add(new PrimitiveDifference(this.previous.sub(context + ".length"), a.length, b.length));
    } else {
      for (int i = 0; i < a.length; ++i) {
        if ((a[i] == null) != (b[i] == null)) {
          add(new BasicObjectDifference(this.previous.sub(context + "[" + i + "]"), a[i], b[i]));
        } else if (a[i] != null) {
          this.append(context + "[" + i + "]", a[i], b[i]);
        }
      }
    }
  }

  private void handleDifferenceables(String context, Object a, Object b) {
    int preSize = countDifferences();
    ((Differenceable) a).addDifferences(this.previous.sub(context), b);
    boolean hadDifferences = (countDifferences() - preSize) > 0;

    boolean equals = a.equals(b);
    Assert.eval("differences added (" + hadDifferences + ") is not same as equals(" + equals + ")",
                hadDifferences != equals);
  }

  private void add(Difference difference) {
    difference.where().addDifference(difference);
  }

  private int countDifferences() {
    return this.previous.countDifferences();
  }

}