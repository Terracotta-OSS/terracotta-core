/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.util;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;


/**
 * Subclass of WeakHashSet implemented using the IdentityWeahHashMap
 * This class is NOT Serializable   
 * 
 */
public class IdentityWeakHashSet extends AbstractSet implements Set, Cloneable {
  private transient IdentityWeakHashMap map;

  // Dummy value to associate with an Object in the backing Map
  private static final Object           PRESENT = new Object();

  public IdentityWeakHashSet() {
    map = new IdentityWeakHashMap();
  }

  public IdentityWeakHashSet(Collection c) {
    map = new IdentityWeakHashMap(Math.max((int) (c.size() / .75f) + 1, 16));
    addAll(c);
  }

  /**
   * Constructs a new, empty set; the backing <tt>HashMap</tt> instance has the specified initial capacity and the
   * specified load factor.
   * 
   * @param initialCapacity the initial capacity of the hash map.
   * @param loadFactor the load factor of the hash map.
   * @throws IllegalArgumentException if the initial capacity is less than zero, or if the load factor is nonpositive.
   */
  public IdentityWeakHashSet(int initialCapacity, float loadFactor) {
    map = new IdentityWeakHashMap(initialCapacity, loadFactor);
  }

  /**
   * Constructs a new, empty set; the backing <tt>HashMap</tt> instance has the specified initial capacity and default
   * load factor, which is <tt>0.75</tt>.
   * 
   * @param initialCapacity the initial capacity of the hash table.
   * @throws IllegalArgumentException if the initial capacity is less than zero.
   */
  public IdentityWeakHashSet(int initialCapacity) {
    map = new IdentityWeakHashMap(initialCapacity);
  }

  /**
   * Constructs a new, empty linked hash set. (This package private constructor is only used by LinkedHashSet.) The
   * backing HashMap instance is a LinkedHashMap with the specified initial capacity and the specified load factor.
   * 
   * @param initialCapacity the initial capacity of the hash map.
   * @param loadFactor the load factor of the hash map.
   * @param dummy ignored (distinguishes this constructor from other int, float constructor.)
   * @throws IllegalArgumentException if the initial capacity is less than zero, or if the load factor is nonpositive.
   */
  IdentityWeakHashSet(int initialCapacity, float loadFactor, boolean dummy) {
    map = new IdentityWeakHashMap(initialCapacity, loadFactor);
  }

  /**
   * Returns an iterator over the elements in this set. The elements are returned in no particular order.
   * 
   * @return an Iterator over the elements in this set.
   * @see ConcurrentModificationException
   */
  public Iterator iterator() {
    return map.keySet().iterator();
  }

  /**
   * Returns the number of elements in this set (its cardinality).
   * 
   * @return the number of elements in this set (its cardinality).
   */
  public int size() {
    return map.size();
  }

  /**
   * Returns <tt>true</tt> if this set contains no elements.
   * 
   * @return <tt>true</tt> if this set contains no elements.
   */
  public boolean isEmpty() {
    return map.isEmpty();
  }

  /**
   * Returns <tt>true</tt> if this set contains the specified element.
   * 
   * @param o element whose presence in this set is to be tested.
   * @return <tt>true</tt> if this set contains the specified element.
   */
  public boolean contains(Object o) {
    return map.containsKey(o);
  }

  /**
   * Adds the specified element to this set if it is not already present.
   * 
   * @param o element to be added to this set.
   * @return <tt>true</tt> if the set did not already contain the specified element.
   */
  public boolean add(Object o) {
    return map.put(o, PRESENT) == null;
  }

  /**
   * Removes the specified element from this set if it is present.
   * 
   * @param o object to be removed from this set, if present.
   * @return <tt>true</tt> if the set contained the specified element.
   */
  public boolean remove(Object o) {
    return map.remove(o) == PRESENT;
  }

  /**
   * Removes all of the elements from this set.
   */
  public void clear() {
    map.clear();
  }

  /**
   * Returns a shallow copy of this <tt>IdentityWeakHashSet</tt> instance: the elements themselves are not cloned.
   * 
   * @return a shallow copy of this set.
   */
  public Object clone() {
    try { 
        IdentityWeakHashSet newSet = (IdentityWeakHashSet) super.clone();
        newSet.map = (IdentityWeakHashMap) map.clone();
        return newSet;
    } catch (CloneNotSupportedException e) {
        throw new InternalError();
    }
  }
}
