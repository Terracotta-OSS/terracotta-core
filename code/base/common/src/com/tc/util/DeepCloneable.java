/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

/**
 * An object that can be 'deep' cloned &mdash; <em>i.e.</em>, any references to mutable objects are themselves
 * cloned, rather than shared.
 * </p>
 * <p>
 * Proper implementation of this method is as follows:
 * <ul>
 * <li>If your object should never be deep-cloned, simply don't implement this interface. The {@link DeepCloner}will
 * refuse to deep-clone it, returning an exception to anybody who tries to deep-clone it (or any object graph that
 * contains a reference to it somewhere).</li>
 * <li>If your object is immutable (<em>and</em> you know all objects it points to are immutable), simply
 * <code>return this</code> from your {@link #deepClone(DeepCloner)}method. This is valid because immutable objects
 * never need to be cloned: the clone would be indistinguishable from the original in all circumstances. (If not, your
 * object isn't really immutable.</li>
 * <li>If your object isn't immutable, your {@link #deepClone(DeepCloner)}method should typically do something like
 * <code>return new Foo(this, cloner)</code> &mdash; that is, create a constructor that takes in an object of the same
 * type, plus a {@link DeepCloner}, like <code>private Foo(Foo source, DeepCloner cloner)</code>.</li>
 * <li>This constructor should copy over references to any immutable objects (<em>e.g.</em>, primitive types,
 * {@link String}s,{@link Date}s, and any user-defined immutable types), and assign a deep-cloned copy to the others
 * via the {@link DeepCloner#subClone}method &mdash; something like <code>this.bar = cloner.subClone(source.bar)</code>.
 * </li>
 * <li>You <strong>MUST NOT</code> call {@link DeepCloner#deepClone}from your 'deep-cloning constructor' directly.
 * See the documentation for {@link DeepCloner}for details; basically, this breaks object graphs badly that have any
 * objects with indegree greater than 1 (<em>i.e.</em>, objects to which more than one other object holds a
 * reference). <strong>THIS IS THE ONE MISTAKE YOU CAN MAKE HERE THAT THE {@link DeepCloner}CANNOT CATCH FOR YOU. YOU
 * HAVE BEEN WARNED.</li>
 * </ul>
 */
public interface DeepCloneable {

  /**
   * Because you cannot create a {@link DeepCloner}yourself, you can't call this method directly. You must either use
   * {@link DeepCloner#subClone(DeepCloneable)}, or call this from a {@link #deepClone(DeepCloner)}method itself.
   */
  Object deepClone(DeepCloner cloner);

}