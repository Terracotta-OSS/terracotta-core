/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.util;

import com.tc.object.ObjectID;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.util.BitSetObjectIDSet.BitSet;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class StripedObjectIDSet implements SortedSet<ObjectID>, PrettyPrintable {
  private final static int               DEFAULT_CONCURRENCY = 64;

  private final ObjectIDSet[]            objectIdSets;
  private final ReentrantReadWriteLock[] locks;
  private final int                      concurrency;

  public StripedObjectIDSet() {
    this(DEFAULT_CONCURRENCY);
  }

  public StripedObjectIDSet(int concurrency) {
    this.concurrency = concurrency;
    this.objectIdSets = new ObjectIDSet[this.concurrency];
    this.locks = new ReentrantReadWriteLock[this.concurrency];
    for (int i = 0; i < this.concurrency; i++) {
      this.objectIdSets[i] = new BitSetObjectIDSet();
      this.locks[i] = new ReentrantReadWriteLock();
    }
  }

  @Override
  public boolean add(ObjectID o) {
    final int index = getIndex(o);
    final ReentrantReadWriteLock lock = locks[index];
    final ObjectIDSet oidSet = objectIdSets[index];

    lock.writeLock().lock();
    try {
      return oidSet.add(o);
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Take a lock on all interested ObjectIDSets and add.
   */
  @Override
  public boolean addAll(Collection<? extends ObjectID> collection) {
    boolean success = true;

    for (ObjectID oid : collection) {
      if (!add(oid)) {
        success = false;
      }
    }

    return success;
  }

  @Override
  public void clear() {
    for (int index = 0; index < concurrency; index++) {
      Lock l = locks[index].writeLock();
      l.lock();
      try {
        objectIdSets[index].clear();
      } finally {
        l.unlock();
      }
    }
  }

  @Override
  public boolean contains(Object o) {
    if (!(o instanceof ObjectID)) { return false; }

    final int index = getIndex((ObjectID) o);
    final ReentrantReadWriteLock lock = locks[index];
    final ObjectIDSet oidSet = objectIdSets[index];

    lock.readLock().lock();
    try {
      return oidSet.contains(o);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public boolean containsAll(Collection<?> collection) {
    for (Object oid : collection) {
      if (!contains(oid)) { return false; }
    }

    return true;
  }

  private void lockAll() {
    for (ReentrantReadWriteLock lock : locks) {
      lock.readLock().lock();
    }
  }

  private void unlockAll() {
    for (ReentrantReadWriteLock lock : locks) {
      lock.readLock().unlock();
    }
  }

  @Override
  public boolean isEmpty() {
    lockAll();
    try {
      for (int index = 0; index < concurrency; index++) {
        if (!objectIdSets[index].isEmpty()) { return false; }
      }
    } finally {
      unlockAll();
    }
    return true;
  }

  @Override
  public boolean remove(Object o) {
    if (!(o instanceof ObjectID)) { return false; }

    final int index = getIndex((ObjectID) o);
    final ReentrantReadWriteLock lock = locks[index];
    final ObjectIDSet oidSet = objectIdSets[index];

    lock.writeLock().lock();
    try {
      return oidSet.remove(o);
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public boolean removeAll(Collection<?> collection) {
    boolean success = true;
    for (Object oid : collection) {
      if (!remove(oid)) {
        success = false;
      }
    }

    return success;
  }

  @Override
  public boolean retainAll(Collection<?> collection) {
    boolean success = false;
    for (int index = 0; index < concurrency; index++) {
      final Lock lock = locks[index].writeLock();
      lock.lock();

      try {
        if (!objectIdSets[index].retainAll(collection)) {
          success = true;
        }
      } finally {
        lock.unlock();
      }
    }

    return success;
  }

  @Override
  public int size() {
    int size = 0;
    lockAll();
    try {
      for (int index = 0; index < concurrency; index++) {
        size += objectIdSets[index].size();
      }
    } finally {
      unlockAll();
    }

    return size;
  }

  @Override
  public Object[] toArray() {
    return toArray(new Object[0]);
  }

  @Override
  public <T> T[] toArray(T[] a) {
    final SortedSet<ObjectID> sortedSet = new BitSetObjectIDSet();

    for (int index = 0; index < concurrency; index++) {
      locks[index].readLock().lock();
    }

    try {
      for (int index = 0; index < concurrency; index++) {
        sortedSet.addAll(objectIdSets[index]);
      }
    } finally {
      for (int index = 0; index < concurrency; index++) {
        locks[index].readLock().unlock();
      }
    }

    int size = sortedSet.size();
    if (a.length < size) {
      a = (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);
    }

    Iterator<ObjectID> it = sortedSet.iterator();
    Object[] result = a;
    for (int i = 0; i < size; i++) {
      result[i] = it.next();
    }

    if (a.length > size) {
      a[size] = null;
    }
    return a;
  }

  @Override
  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    out.print("Striped ObjectIDSet: concurreny = " + concurrency).flush();
    lockAll();
    try {
      for (int index = 0; index < concurrency; index++) {
        out.print("ObjectIDSet Index: " + index).flush();
        out.print(objectIdSets[index]).flush();
      }
    } finally {
      unlockAll();
    }

    return out;
  }

  private int getIndex(ObjectID oid) {
    long maskedOid = Math.abs(BitSetObjectIDSet.calculateStart(oid.toLong()) / BitSet.RANGE_SIZE);
    return (int) Math.abs(maskedOid % concurrency);
  }

  /**
   * Implement it when the need arises
   */
  @Override
  public ObjectID first() {
    throw new UnsupportedOperationException();
  }

  /**
   * Implement it when the need arises
   */
  @Override
  public ObjectID last() {
    throw new UnsupportedOperationException();
  }

  /**
   * Implement it when the need arises
   */
  @Override
  public SortedSet<ObjectID> headSet(ObjectID toElement) {
    throw new UnsupportedOperationException();
  }

  /**
   * Implement it when the need arises
   */
  @Override
  public SortedSet<ObjectID> subSet(ObjectID fromElement, ObjectID toElement) {
    throw new UnsupportedOperationException();
  }

  /**
   * Implement it when the need arises
   */
  @Override
  public SortedSet<ObjectID> tailSet(ObjectID fromElement) {
    throw new UnsupportedOperationException();
  }

  /**
   * Implement it when the need arises
   */
  @Override
  public Iterator<ObjectID> iterator() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Comparator<? super ObjectID> comparator() {
    return null;
  }

  /**
   * To be used only in tests
   */
  ObjectIDSet[] getObjectIDSets() {
    return objectIdSets;
  }
}
