/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import com.tc.exception.TCRuntimeException;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Allows for deep cloning of objects: to deep clone an object, call {@link #deepClone(Object)}. This object must
 * either be one of the 'built-in' types this class knows about (like a {@link Cloneable}{@link Collection}or
 * {@link Map}, an array, or certain value types, like the object wrappers for primitives, {@link String}s,
 * {@link Date}s, and so on), or it must implement the {@link DeepCloneable}interface itself.
 * </p>
 * <p>
 * <strong>FROM WITHIN A {@link com.tc.util.DeepCloneable#deepClone(DeepCloner)}METHOD </strong>, you must <strong>NOT
 * </strong> use the {@link #deepClone(Object)}method. The reason for this is a little subtle. Consider a set of
 * objects A, B, and C, where A and B both hold references to C. A 'naive' deep-cloning implementation will go to A and
 * deep-clone it, which will make a copy of A that points to a new copy of C; it will then deep-clone B, which will
 * point to a new, <em>separate</em> copy of C. This is bad, and will break your system in strange and wondrous ways.
 * </p>
 * <p>
 * The solution to this is embedded in this class: a {@link DeepCloner}uses an {@link IdentityHashMap}that maps
 * existing objects to their new clones. When cloning an object in {@link #subClone(Object)}, if we've already created
 * a clone for that object, we simply return the existing clone, rather than creating a new one. Thus, in the situation
 * above, when B goes to deep-clone C, the call to {@link DeepCloner#subClone(OBject)}will look up the original C in
 * its map, find that it already has a clone, and return that; thus the clones of A and B will get hooked up to a single
 * clone of C, rather than two different copies.
 * </p>
 * <p>
 * Because of this, it is <em>critical</em> that, within a {@link DeepCloneable}'s
 * {@link DeepCloneable#deepClone(DeepCloner)}method, you clone sub-objects by calling {@link #subClone(Object)}on the
 * passed-in {@link DeepCloner}, rather than calling {@link #deepClone(Object)}yourself &mdash; otherwise, yes, your
 * deep clone will break in strange and wondrous ways.
 * </p>
 * <p>
 * (Note that we can and do prevent external clients &mdash; <em>i.e.</em>, code that isn't within a
 * {@link DeepCloneable#deepClone(DeepCloner)}method &mdash; from calling {@link #subClone}directly, because that's an
 * instance method, and the constructor of this class is private. Thus, the only way to do a deep clone &mdash; if
 * you're not already in one &mdash; is to call {@link #deepClone(Object}directly.
 * </p>
 * <p>
 * Finally, a note: object graphs that have a complete cycle of {@link DeepCloneable}objects that aren't value types (
 * <em>i.e.</em>, which don't just <code>return this</code> in their {@link DeepCloneable}methods) cause problems
 * if you don't additionally add a call to {@link #setClone(Object, Object)}in their methods. See the documentation for
 * {@link #setClone(Object, Object)}for details.
 */
public class DeepCloner {

  /**
   * The list of immutable 'value types' we know about. These are types for which we can simply return the original,
   * rather than making a copy, because they're immutable and cannot possibly have an outbound reference to any other
   * object that itself is not a value type. (Note that we consider objects of any subtype of these types to also be a
   * value type.)
   * </p>
   * <p>
   * (In other words, the entire transitive closure under object reference of any object of any of these types, or a
   * subtype of any of these types, is immutable.)
   */
  private static final Class[] IMMUTABLE_VALUE_TYPES = new Class[] { Boolean.class, Character.class, Byte.class,
      Short.class, Integer.class, Long.class, Float.class, Double.class, String.class, Date.class };

  private static final Object  DUMMY_VALUE           = new Object();

  private final Map            clones;
  private final Map            pendingClones;

  private DeepCloner() {
    this.clones = new IdentityHashMap();
    this.pendingClones = new IdentityHashMap();
  }

  /**
   * Deep-clones an object: that is, returns an object <code>out</code> such that <code>source != out</code>,
   * <code>out.equals(source)</code> (assuming <code>source</code>'s{@link Object#equals(Object}method works
   * correctly and the {@link DeepCloneable#deepClone(DeepCloner)}method is implemented correctly on
   * <code>source</code> and all referred-to classes), and &mdash; again, assuming <code>source</code>'s
   * {@link DeepCloneable#deepClone(DeepCloner)}method is implemented correctly &mdash; <code>source</code> and
   * <code>out</code> share no structure other than sub-trees that consist of entirely immutable objects.
   * </p>
   * <p>
   * This method <strong>MUST NOT BE CALLED </strong> from within an object's
   * {@link DeepCloneable#deepClone(DeepCloner)}method; see the class comments for why. You must use
   * {@link #subClone(Object}instead.
   * 
   * @param source The object to clone
   * @return The deep clone
   * @throws UnsupportedOperationException if <code>source</code> does not implement {@link DeepCloneable}and is not
   *         one of the object types that {@link DeepCloner}knows how to clone itself &mdash; currently,
   *         {@link Cloneable}{@link Collection}s,{@link Map}s, and arrays (of objects or primitives).
   */
  public static Object deepClone(Object source) {
    if (source == null) return null;
    return new DeepCloner().subClone(source);
  }

  /**
   * Deep-clones an object. This method works exactly the same as {@link #deepClone(Object)}, except that it's smart
   * enough to handle object graphs with multiple references to the same object. (That is, in such cases, the object
   * that's referred to multiple times gets deep-cloned exactly once, and all references point to the same object.) See
   * the class comment for more details.
   * </p>
   * <p>
   * (This method cannot be called from anywhere else, because there's no way to create an instance of
   * {@link DeepCloner}to call it on other than via {@link #deepClone(Object}).
   * 
   * @param source The object to clone
   * @return The deep clone; this will always be a different object from <code>source</code>, but may not be a
   *         brand-new object (<em>i.e.</em>, created by this method), for the reasons given in the class comment.
   * @throws UnsupportedOperationException if <code>source</code> does not implement {@link DeepCloneable}and is not
   *        one of the object types that {@link DeepCloner}knows how to clone itself &mdash; currently,
   *        {@link Cloneable}{@link Collection}s,{@link Map}s, and arrays (of objects or primitives).
   */
  public Object subClone(Object source) {
    if (source == null) return null;

    Object out = this.clones.get(source);

    if (out == null) {
      Assert
          .eval(
                "You're trying to clone an object that's currently being cloned -- in other words, someone tried "
                                + "to clone some object A, and, somewhere in the set of calls that generated, some other object went back "
                                + "and tried to clone A again. (Any kind of cycle in an object graph will typically generate this problem.) "
                                + "To fix this, you need to break the chain: call the DeepCloner.setClone(Object, Object) method somewhere "
                                + "*after* the original object A is created, but *before* the call to some other object's deepClone() method "
                                + "that leads back to A. (This is often in the 'cloning constructor' of A.) See the JavaDoc for "
                                + "DeepCloner.subClone() for details.", !this.pendingClones.containsKey(source));
      this.pendingClones.put(source, DUMMY_VALUE);

      out = doDeepClone(source);
      this.clones.put(source, out);

      this.pendingClones.remove(source);
    }

    return out;
  }

  /**
   * This method is used to break circular reference graphs. For example, if you have two {@link DeepCloneable}objects,
   * <code>A</code> and <code>B</code>, each of which contain a reference to each other, here's what will happen
   * when someone calls {@link #deepClone(Object)}on <code>A</code>:
   * <ol>
   * <li>{@link #deepClone(Object)}creates a new {@link DeepCloner}, and calls {@link #subClone(Object)}on it,
   * passing it <code>A</code>.</li>
   * <li>{@link #subClone(Object)}looks for <code>A</code> in its map, doesn't find it, and so calls
   * {@link DeepCloneable#deepClone(DeepCloner)}on <code>A</code>.</li>
   * <li><code>A</code>'s{@link DeepCloneable#deepClone(Object)}method typically just does
   * <code>return new A(this, deepCloner)</code>.</li>
   * <li>This constructor on <code>A</code> then typically says
   * <code>this.b = deepCloner.subClone(originalA.b)</code>.</li>
   * <li>{#link #subClone(Object)} looks for <code>B</code> in its map, doesn't find it, and so calls
   * {@link DeepCloneable#deepClone(DeepCloner)}on <code>B</code>.</li>
   * <li><code>B</code>'s{@link DeepCloneable#deepClone(Object)}method typically just does
   * <code>return new B(this, deepCloner)</code>.</li>
   * <li>This constructor on <code>B</code> then typically says
   * <code>this.a = deepCloner.subClone(originalB.a)</code>.</li>
   * <li>We return to step 2.</li>
   * <li>Eventually, get a {@link StackOverflowError}. Baboom.</li>
   * </ol>
   * Obviously, this is a problem. Further, it will occur in any object graph that has a full cycle composed solely of
   * {@link DeepCloneable}objects that aren't value types (<em>i.e.</em>, those that don't just return
   * <code>this</code> from their {@link DeepCloneable#deepClone(DeepCloner)}methods).
   * </p>
   * <p>
   * To avoid this, we use this method. As an example, the proper use above is to add a call to this method in both A's
   * 'deep cloning' constructor, and B's 'deep cloning' constructor, <em>before</em> the call to
   * {@link #subClone(Object)}in each of them that clones the other object. For example, A's 'deep cloning' constructor
   * might look like:
   * 
   * <pre>
   * private A(A source, DeepCloner cloner) {
   *   cloner.setClone(source, this);
   *   this.b = cloner.subClone(source.b);
   * }
   * </pre>
   * 
   * This way, the {@link DeepCloner}will know what <code>A</code>'s correct clone is <em>before</em> <code>B</code>
   * 's call to {@link #subClone(Object)}that clones it &mdash; so the {@link DeepCloner}will return the correct
   * object, rather than getting a {@link StackOverflowError}.
   * </p>
   * <p>
   * (Of note: there is actually a mechanism &mdash; the {@link #pendingClones}{@link Map}&mdash; that prevents the
   * above scenario from actually playing out. Basically, we take note of which objects we're working on cloning just
   * before we actually clone them, and if, before their clone completes, someone asks for them to be cloned, we throw a
   * {@link TCAssertionError}describing the problem and what the solution is.)
   */
  public void setClone(Object original, Object clone) {
    Assert.eval("You're trying to set the clone of an object that isn't currently being cloned. Perhaps you passed " +
        "your arguments backwards?", this.pendingClones.containsKey(original));
    this.clones.put(original, clone);
  }

  private Object doDeepClone(Object source) {
    if (source instanceof DeepCloneable) {
      Object out = ((DeepCloneable) source).deepClone(this);
      Assert
          .eval("You've returned an object from deepClone() that is of a DIFFERENT class than the source object; "
                + "this almost certainly means you forgot to override deepClone() on a subclass. You should fix this.",
                out.getClass().equals(source.getClass()));
      return out;
    } else if (isValueType(source.getClass())) {
      return source;
    } else if (source instanceof Collection && source instanceof Cloneable) {
      return deepClone((Collection) source);
    } else if (source instanceof Map && source instanceof Cloneable) {
      return deepClone((Map) source);
    } else if (source instanceof Object[]) {
      return deepClone((Object[]) source);
    } else if (source instanceof boolean[]) {
      return deepClone((boolean[]) source);
    } else if (source instanceof char[]) {
      return deepClone((char[]) source);
    } else if (source instanceof byte[]) {
      return deepClone((byte[]) source);
    } else if (source instanceof short[]) {
      return deepClone((short[]) source);
    } else if (source instanceof int[]) {
      return deepClone((int[]) source);
    } else if (source instanceof long[]) {
      return deepClone((long[]) source);
    } else if (source instanceof float[]) {
      return deepClone((float[]) source);
    } else if (source instanceof double[]) {
      return deepClone((double[]) source);
    } else {
      throw new UnsupportedOperationException("You can't deep-clone " + source + ", a " + source.getClass().getName()
                                              + "; it does not implement DeepCloneable, and is not one of the "
                                              + "predefined 'known' classes.");
    }
  }

  private boolean isValueType(Class c) {
    for (int i = 0; i < IMMUTABLE_VALUE_TYPES.length; ++i) {
      if (IMMUTABLE_VALUE_TYPES[i].isAssignableFrom(c)) return true;
    }

    return false;
  }

  private Object deepClone(Collection source) {
    Collection out = (Collection) doShallowClone(source);
    out.clear();

    Iterator iter = source.iterator();
    while (iter.hasNext()) {
      out.add(subClone(iter.next()));
    }

    return out;
  }

  private Object deepClone(Map source) {
    Map out = (Map) doShallowClone(source);
    out.clear();

    Iterator iter = source.entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry entry = (Map.Entry) iter.next();
      out.put(subClone(entry.getKey()), subClone(entry.getValue()));
    }

    return out;
  }

  private Object deepClone(Object[] source) {
    Object[] out = (Object[]) Array.newInstance(source.getClass().getComponentType(), source.length);
    for (int i = 0; i < out.length; ++i) {
      out[i] = subClone(source[i]);
    }
    return out;
  }

  private Object deepClone(boolean[] source) {
    boolean[] out = new boolean[source.length];
    System.arraycopy(source, 0, out, 0, source.length);
    return out;
  }

  private Object deepClone(char[] source) {
    char[] out = new char[source.length];
    System.arraycopy(source, 0, out, 0, source.length);
    return out;
  }

  private Object deepClone(short[] source) {
    short[] out = new short[source.length];
    System.arraycopy(source, 0, out, 0, source.length);
    return out;
  }

  private Object deepClone(int[] source) {
    int[] out = new int[source.length];
    System.arraycopy(source, 0, out, 0, source.length);
    return out;
  }

  private Object deepClone(byte[] source) {
    byte[] out = new byte[source.length];
    System.arraycopy(source, 0, out, 0, source.length);
    return out;
  }

  private Object deepClone(long[] source) {
    long[] out = new long[source.length];
    System.arraycopy(source, 0, out, 0, source.length);
    return out;
  }

  private Object deepClone(float[] source) {
    float[] out = new float[source.length];
    System.arraycopy(source, 0, out, 0, source.length);
    return out;
  }

  private Object deepClone(double[] source) {
    double[] out = new double[source.length];
    System.arraycopy(source, 0, out, 0, source.length);
    return out;
  }

  private Object doShallowClone(Object source) {
    Class c = source.getClass();
    Method cloneMethod;
    try {
      cloneMethod = c.getMethod("clone", null);
      return cloneMethod.invoke(source, null);
    } catch (SecurityException se) {
      throw new TCRuntimeException("Unexpected exception when trying to clone a " + source.getClass(), se);
    } catch (NoSuchMethodException nsme) {
      throw new TCRuntimeException("Unexpected exception when trying to clone a " + source.getClass(), nsme);
    } catch (IllegalArgumentException iae) {
      throw new TCRuntimeException("Unexpected exception when trying to clone a " + source.getClass(), iae);
    } catch (IllegalAccessException iae) {
      throw new TCRuntimeException("Unexpected exception when trying to clone a " + source.getClass(), iae);
    } catch (InvocationTargetException ite) {
      throw new TCRuntimeException("Unexpected exception when trying to clone a " + source.getClass(), ite);
    }
  }

}