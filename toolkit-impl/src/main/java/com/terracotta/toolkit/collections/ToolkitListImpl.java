/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.internal.collections.ToolkitListInternal;
import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;

import com.tc.net.GroupID;
import com.tc.object.LiteralValues;
import com.tc.object.SerializationUtil;
import com.tc.object.TCObject;
import com.terracotta.toolkit.concurrent.locks.ToolkitLockingApi;
import com.terracotta.toolkit.object.AbstractTCToolkitObject;
import com.terracotta.toolkit.object.serialization.SerializedClusterObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class ToolkitListImpl<E> extends AbstractTCToolkitObject implements ToolkitListInternal<E> {
  private final transient List                    localList = new ArrayList();

  private volatile transient Object               localResolveLock;
  private volatile transient ToolkitReadWriteLock lock;
  protected transient int                         modCount;

  @Override
  public String getName() {
    throw new UnsupportedOperationException();
  }

  private void writeLock() {
    lock.writeLock().lock();
  }

  private void writeUnlock() {
    lock.writeLock().unlock();
  }

  private void readLock() {
    lock.readLock().lock();
  }

  private void readUnlock() {
    lock.readLock().unlock();
  }

  @Override
  public ToolkitReadWriteLock getReadWriteLock() {
    return lock;
  }

  private E getValueFromTCCompatibleObject(Object o) {
    boolean isLiteral = LiteralValues.isLiteralInstance(o);
    if (isLiteral) { return (E) o; }

    return ((SerializedClusterObject<E>) o).getValue(strategy, false, false);
  }

  @Override
  public boolean add(E e) {
    if (e == null) { throw new NullPointerException("Object passed in to add was null"); }

    writeLock();
    try {
      return unlockedAdd(e);
    } finally {
      writeUnlock();
    }
  }

  @Override
  public boolean unlockedAdd(E e) {
    if (e == null) { throw new NullPointerException("Object passed in to add was null"); }

    synchronized (localResolveLock) {
      Object o = createTCCompatibleObject(e);
      logicalInvoke(SerializationUtil.ADD_SIGNATURE, new Object[] { o });
      modCount++;
      localList.add(o);
      return true;
    }
  }

  @Override
  public void add(int index, E e) {
    writeLock();
    try {
      synchronized (localResolveLock) {
        Object o = createTCCompatibleObject(e);
        logicalInvoke(SerializationUtil.ADD_AT_SIGNATURE, new Object[] { Integer.valueOf(index), o });
        localList.add(index, o);
        modCount++;
      }
    } finally {
      writeUnlock();
    }
  }

  @Override
  public boolean addAll(Collection<? extends E> c) {
    writeLock();
    try {
      synchronized (localResolveLock) {

        Collection collection = createTCCompatiableCollection(c);
        logicalInvoke(SerializationUtil.ADD_ALL_SIGNATURE, new Object[] { collection });
        boolean rv = localList.addAll(collection);
        if (rv) {
          modCount++;
        }
        return rv;
      }
    } finally {
      writeUnlock();
    }
  }

  @Override
  public boolean addAll(int index, Collection<? extends E> c) {
    writeLock();
    try {
      synchronized (localResolveLock) {

        Collection collection = createTCCompatiableCollection(c);
        logicalInvoke(SerializationUtil.ADD_ALL_AT_SIGNATURE, new Object[] { Integer.valueOf(index), collection });
        boolean rv = localList.addAll(index, collection);
        if (rv) {
          modCount++;
        }
        return rv;
      }
    } finally {
      writeUnlock();
    }
  }

  @Override
  public void clear() {
    writeLock();
    try {
      synchronized (localResolveLock) {
        logicalInvoke(SerializationUtil.CLEAR_SIGNATURE, new Object[] {});
        localList.clear();
        modCount++;
      }
    } finally {
      writeUnlock();
    }
  }

  @Override
  public E get(int index) {
    readLock();
    try {
      synchronized (localResolveLock) {

        Object o = localList.get(index);
        if (o == null) { return null; }
        return getValueFromTCCompatibleObject(o);
      }
    } finally {
      readUnlock();
    }
  }

  @Override
  public int indexOf(Object o) {
    if (o == null) { return -1; }

    readLock();
    try {
      synchronized (localResolveLock) {

        int index = 0;
        for (Object tempObj : this.localList) {
          if (o.equals(getValueFromTCCompatibleObject(tempObj))) { return index; }
          index++;
        }

        return -1;
      }
    } finally {
      readUnlock();
    }
  }

  @Override
  public int lastIndexOf(Object o) {
    if (o == null) { return -1; }

    readLock();
    try {
      synchronized (localResolveLock) {

        int index = 0;
        int setIndex = -1;
        for (Object tempObj : this.localList) {
          if (o.equals(getValueFromTCCompatibleObject(tempObj))) {
            setIndex = index;
          }
          index++;
        }

        return setIndex;
      }
    } finally {
      readUnlock();
    }
  }

  @Override
  public boolean isEmpty() {
    readLock();
    try {
      synchronized (localResolveLock) {
        return localList.isEmpty();
      }
    } finally {
      readUnlock();
    }
  }

  private void checkModification(int expectedModCount) {
    if (expectedModCount != modCount) { throw new ConcurrentModificationException(); }
  }

  private void logicalInvoke(String signature, Object[] params) {
    platformService.logicalInvoke(this, signature, params);
  }

  @Override
  public void __tc_managed(TCObject t) {
    tcObject = t;
    gid = new GroupID(t.getObjectID().getGroupID());
    localResolveLock = tcObject.getResolveLock();
    lock = ToolkitLockingApi.createUnnamedReadWriteLock(ToolkitObjectType.LIST, tcObject.getObjectID(),
                                                        platformService, ToolkitLockTypeInternal.WRITE);
  }

  @Override
  public TCObject __tc_managed() {
    return tcObject;
  }

  @Override
  public boolean __tc_isManaged() {
    return tcObject != null;
  }

  @Override
  public Iterator<E> iterator() {
    readLock();
    try {
      return new SimpleListIterator(localList.iterator());
    } finally {
      readUnlock();
    }
  }

  @Override
  public ListIterator<E> listIterator() {
    readLock();
    try {
      return listIterator(0);
    } finally {
      readUnlock();
    }
  }

  @Override
  public ListIterator<E> listIterator(int start) {
    readLock();
    try {
      if (start < 0 || start > size()) { throw new IndexOutOfBoundsException(); }
      return new FullListIterator(localList.listIterator(start));
    } finally {
      readUnlock();
    }
  }

  /**
   * Used by SubList
   */
  private Iterator<E> subListIterator(int fromIndex, int toIndex) {
    readLock();
    try {
      return new SimpleListIterator(localList.subList(fromIndex, toIndex).iterator());
    } finally {
      readUnlock();
    }
  }

  /**
   * Used by SubList
   */
  private ListIterator<E> listIterator(int fromIndex, int toIndex) {
    readLock();
    try {
      if (fromIndex < 0 || fromIndex > size() || fromIndex > toIndex || toIndex > size()) { throw new IndexOutOfBoundsException(); }
      return new FullListIterator(localList.subList(fromIndex, toIndex).listIterator(0));
    } finally {
      readUnlock();
    }
  }

  @Override
  public E remove(int index) {
    writeLock();
    try {
      synchronized (localResolveLock) {

        logicalInvoke(SerializationUtil.REMOVE_AT_SIGNATURE, new Object[] { Integer.valueOf(index) });
        Object o = localList.remove(index);
        modCount++;
        return getValueFromTCCompatibleObject(o);
      }
    } finally {
      writeUnlock();
    }
  }

  /**
   * @param o should be an instance of Serializable
   */
  @Override
  public boolean remove(Object o) {
    if (!(o instanceof Serializable)) { throw new ClassCastException("Object passed should be Serializable " + o); }
    writeLock();
    try {
      return unlockedRemove(o);
    } finally {
      writeUnlock();
    }
  }

  private boolean unlockedRemove(Object o) {
    synchronized (localResolveLock) {
      Iterator<SerializedClusterObject<E>> iterator = localList.iterator();
      int index = 0;
      while (iterator.hasNext()) {
        Object tempObject = iterator.next();
        if (o.equals(getValueFromTCCompatibleObject(tempObject))) {
          logicalInvoke(SerializationUtil.REMOVE_AT_SIGNATURE, new Object[] { Integer.valueOf(index) });
          iterator.remove();
          modCount++;
          return true;
        }
        index++;
      }
      return false;
    }
  }

  @Override
  public E set(int index, E e) {
    writeLock();
    try {
      synchronized (localResolveLock) {

        Object o = createTCCompatibleObject(e);
        logicalInvoke(SerializationUtil.SET_SIGNATURE, new Object[] { Integer.valueOf(index), o });
        Object prev = localList.set(index, o);
        return getValueFromTCCompatibleObject(prev);
      }
    } finally {
      writeUnlock();
    }
  }

  @Override
  public int size() {
    readLock();
    try {
      synchronized (localResolveLock) {
        return localList.size();
      }
    } finally {
      readUnlock();
    }
  }

  @Override
  public List<E> subList(int fromIndex, int toIndex) {
    readLock();
    try {
      if (fromIndex < 0 || toIndex > size() || fromIndex > toIndex) { throw new IndexOutOfBoundsException(); }
      return new SubTerracottaList(fromIndex, toIndex);
    } finally {
      readUnlock();
    }
  }

  @Override
  public Object[] toArray() {
    readLock();
    try {
      synchronized (localResolveLock) {

        Object[] array = new Object[localList.size()];
        int index = 0;
        for (Object tempObj : localList) {
          array[index] = getValueFromTCCompatibleObject(tempObj);
          index++;
        }
        return array;
      }
    } finally {
      readUnlock();
    }
  }

  @Override
  public <T> T[] toArray(T[] a) {
    readLock();
    try {
      synchronized (localResolveLock) {

        if (a.length < size()) {
          return (T[]) toArray();
        } else {
          int index = 0;
          for (Object tempObj : localList) {
            a[index] = (T) getValueFromTCCompatibleObject(tempObj);
            index++;
          }
          for (int i = localList.size(); i < a.length; i++) {
            a[i] = null;
          }

          return a;
        }
      }
    } finally {
      readUnlock();
    }
  }

  private class SubTerracottaList implements List<E> {
    private final int startIndex;
    private int       size;
    private int       expectedModCount;

    public SubTerracottaList(int start, int end) {
      this.size = end - start;
      this.startIndex = start;
      expectedModCount = ToolkitListImpl.this.modCount;
    }

    private List localSubList() {
      return localList.subList(startIndex, startIndex + size);
    }

    @Override
    public void add(int index, E element) {
      writeLock();
      try {
        synchronized (localResolveLock) {
          checkRange(index);
          checkModification(expectedModCount);
          size++;
          ToolkitListImpl.this.add(startIndex + index, element);
          expectedModCount = ToolkitListImpl.this.modCount;
        }
      } finally {
        writeUnlock();
      }
    }

    private void checkRange(int index) {
      if (index < 0 || index >= size) { throw new IndexOutOfBoundsException(); }
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
      writeLock();
      try {
        synchronized (localResolveLock) {
          checkRange(index);
          checkModification(expectedModCount);
          boolean result = ToolkitListImpl.this.addAll(startIndex + index, c);
          if (result) {
            size += c.size();
          }
          expectedModCount = ToolkitListImpl.this.modCount;
          return result;
        }
      } finally {
        writeUnlock();
      }
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
      writeLock();
      try {
        synchronized (localResolveLock) {
          checkModification(expectedModCount);
          boolean result = ToolkitListImpl.this.addAll(startIndex + size, c);
          if (result) {
            size += c.size();
          }
          expectedModCount = ToolkitListImpl.this.modCount;
          return result;
        }
      } finally {
        writeUnlock();
      }
    }

    @Override
    public void clear() {
      writeLock();
      try {
        synchronized (localResolveLock) {
          checkModification(expectedModCount);
          removeRangeUnlocked(startIndex, startIndex + size);
          size = 0;
          expectedModCount = ToolkitListImpl.this.modCount;
        }
      } finally {
        writeUnlock();
      }
    }

    @Override
    public E get(int index) {
      readLock();
      try {
        synchronized (localResolveLock) {
          checkRange(index);
          checkModification(expectedModCount);
          return ToolkitListImpl.this.get(startIndex + index);
        }
      } finally {
        readUnlock();
      }
    }

    @Override
    public Iterator<E> iterator() {
      synchronized (localResolveLock) {
        checkModification(expectedModCount);
        return ToolkitListImpl.this.subListIterator(startIndex, startIndex + size);
      }
    }

    @Override
    public ListIterator<E> listIterator() {
      return listIterator(0);
    }

    @Override
    public ListIterator<E> listIterator(int start) {
      synchronized (localResolveLock) {
        checkModification(expectedModCount);
        checkRange(start);
        return ToolkitListImpl.this.listIterator(start + startIndex, startIndex + size);
      }
    }

    @Override
    public int size() {
      readLock();
      try {
        synchronized (localResolveLock) {
          checkModification(expectedModCount);
          return size;
        }
      } finally {
        readUnlock();
      }
    }

    @Override
    public boolean isEmpty() {
      readLock();
      try {
        synchronized (localResolveLock) {
          checkModification(expectedModCount);
          return size == 0;
        }
      } finally {
        readUnlock();
      }
    }

    @Override
    public boolean contains(Object o) {
      readLock();
      try {
        synchronized (localResolveLock) {
          checkModification(expectedModCount);
          return indexOf(o) != -1;
        }
      } finally {
        readUnlock();
      }
    }

    @Override
    public Object[] toArray() {
      readLock();
      try {
        synchronized (localResolveLock) {
          checkModification(expectedModCount);
          Object[] array = new Object[size];
          int index = 0;
          for (Object tempObj : localSubList()) {
            array[index] = getValueFromTCCompatibleObject(tempObj);
            index++;
          }
          return array;
        }
      } finally {
        readUnlock();
      }
    }

    @Override
    public <T> T[] toArray(T[] a) {
      readLock();
      try {
        synchronized (localResolveLock) {
          checkModification(expectedModCount);
          if (a.length < size) {
            return (T[]) toArray();
          } else {
            int index = 0;
            for (Object tempObj : localSubList()) {
              a[index] = (T) getValueFromTCCompatibleObject(tempObj);
              index++;
            }
            for (int i = localList.size(); i < a.length; i++) {
              a[i] = null;
            }

            return a;
          }
        }
      } finally {
        readUnlock();
      }
    }

    @Override
    public boolean add(E e) {
      writeLock();
      try {
        synchronized (localResolveLock) {
          checkModification(expectedModCount);
          size++;
          ToolkitListImpl.this.add(startIndex + size, e);
          expectedModCount = ToolkitListImpl.this.modCount;
          return true;
        }
      } finally {
        writeUnlock();
      }
    }

    @Override
    public boolean remove(Object o) {
      if (!(o instanceof Serializable)) { throw new ClassCastException("Object passed should be Serializable " + o); }
      writeLock();
      try {
        checkModification(expectedModCount);
        boolean rv = unlockedRemoveSubList(o);
        expectedModCount = ToolkitListImpl.this.modCount;
        return rv;
      } finally {
        writeUnlock();
      }
    }

    private boolean unlockedRemoveSubList(Object o) {
      synchronized (localResolveLock) {
        Iterator<SerializedClusterObject<E>> iterator = localSubList().iterator();
        int index = 0;
        while (iterator.hasNext()) {
          Object tempObject = iterator.next();
          if (o.equals(getValueFromTCCompatibleObject(tempObject))) {
            logicalInvoke(SerializationUtil.REMOVE_AT_SIGNATURE, new Object[] { Integer.valueOf(startIndex + index) });
            iterator.remove();
            size--;
            ToolkitListImpl.this.modCount++;
            return true;
          }
          index++;
        }
        return false;
      }
    }

    @Override
    public boolean containsAll(Collection<?> c) {
      readLock();
      try {
        synchronized (localResolveLock) {
          checkModification(expectedModCount);
          for (Object o : c) {
            if (!contains(o)) { return false; }
          }
          return true;
        }
      } finally {
        readUnlock();
      }
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      writeLock();
      try {
        checkModification(expectedModCount);
        boolean isRemoved = false;
        synchronized (localResolveLock) {
          for (Object o : c) {
            if (unlockedRemoveSubList(o)) {
              isRemoved = true;
            }
          }
        }
        expectedModCount = ToolkitListImpl.this.modCount;
        return isRemoved;
      } finally {
        writeUnlock();
      }
    }

    @Override
    public boolean retainAll(Collection<?> c) {
      writeLock();
      try {
        synchronized (localResolveLock) {
          boolean modified = false;
          checkModification(expectedModCount);
          Iterator iter = localSubList().iterator();
          while (iter.hasNext()) {
            Object tcCompatibleObj = iter.next();
            Object tempObject = getValueFromTCCompatibleObject(tcCompatibleObj);
            if (!c.contains(tempObject)) {
              logicalInvoke(SerializationUtil.REMOVE_SIGNATURE, new Object[] { tcCompatibleObj });
              iter.remove();
              size--;
              modified = true;
            }
          }
          if (modified) {
            ToolkitListImpl.this.modCount++;
            expectedModCount = ToolkitListImpl.this.modCount;
          }

          return modified;
        }
      } finally {
        writeUnlock();
      }
    }

    @Override
    public E set(int index, E element) {
      writeLock();
      try {
        synchronized (localResolveLock) {
          checkRange(index);
          checkModification(expectedModCount);
          return ToolkitListImpl.this.set(startIndex + index, element);
        }
      } finally {
        writeUnlock();
      }
    }

    @Override
    public E remove(int index) {
      writeLock();
      try {
        synchronized (localResolveLock) {
          checkRange(index);
          checkModification(expectedModCount);
          size--;
          E rv = ToolkitListImpl.this.remove(startIndex + index);
          expectedModCount = ToolkitListImpl.this.modCount;
          return rv;
        }
      } finally {
        writeUnlock();
      }
    }

    @Override
    public int indexOf(Object o) {
      if (o == null) { return -1; }

      readLock();
      try {
        synchronized (localResolveLock) {
          checkModification(expectedModCount);
          int index = 0;
          for (Object tempObj : localSubList()) {
            if (o.equals(getValueFromTCCompatibleObject(tempObj))) { return index; }
            index++;
          }

          return -1;
        }
      } finally {
        readUnlock();
      }
    }

    @Override
    public int lastIndexOf(Object o) {
      if (o == null) { return -1; }

      readLock();
      try {
        synchronized (localResolveLock) {
          checkModification(expectedModCount);
          int index = 0;
          int setIndex = -1;
          for (Object tempObj : localSubList()) {
            if (o.equals(getValueFromTCCompatibleObject(tempObj))) {
              setIndex = index;
            }
            index++;
          }

          return setIndex;
        }
      } finally {
        readUnlock();
      }
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {

      readLock();
      try {
        synchronized (localResolveLock) {
          checkModification(expectedModCount);
          if (fromIndex < 0 || toIndex > size || fromIndex > toIndex) { throw new IndexOutOfBoundsException(); }
          return new SubTerracottaList(fromIndex + startIndex, toIndex + startIndex);
        }
      } finally {
        readUnlock();
      }

    }
  }

  private void removeRangeUnlocked(int fromIndex, int toIndex) {
    if (fromIndex < 0 || toIndex > size() || fromIndex > toIndex) { throw new IndexOutOfBoundsException(); }
    logicalInvoke(SerializationUtil.REMOVE_RANGE_SIGNATURE,
                  new Object[] { Integer.valueOf(fromIndex), Integer.valueOf(toIndex) });
    for (int i = fromIndex; i < toIndex; i++) {
      localList.remove(fromIndex);
    }
    modCount += toIndex - fromIndex;

  }

  private class FullListIterator implements ListIterator<E> {
    private final ListIterator myListIterator;
    private Object             currentItem;
    private int                currentIndex;

    FullListIterator(ListIterator myListIterator) {
      this.myListIterator = myListIterator;
    }

    @Override
    public boolean hasNext() {
      readLock();
      try {
        synchronized (localResolveLock) {
          return myListIterator.hasNext();
        }
      } finally {
        readUnlock();
      }
    }

    @Override
    public E next() {
      readLock();
      try {
        synchronized (localResolveLock) {
          currentIndex = myListIterator.nextIndex();
          currentItem = myListIterator.next();
          return getValueFromTCCompatibleObject(currentItem);
        }
      } finally {
        readUnlock();
      }
    }

    @Override
    public boolean hasPrevious() {
      readLock();
      try {
        synchronized (localResolveLock) {
          return myListIterator.hasPrevious();
        }
      } finally {
        readUnlock();
      }
    }

    @Override
    public E previous() {
      readLock();
      try {
        synchronized (localResolveLock) {
          currentIndex = myListIterator.previousIndex();
          currentItem = myListIterator.previous();
          return getValueFromTCCompatibleObject(currentItem);
        }
      } finally {
        readUnlock();
      }
    }

    @Override
    public int nextIndex() {
      readLock();
      try {
        synchronized (localResolveLock) {
          return myListIterator.nextIndex();
        }
      } finally {
        readUnlock();
      }
    }

    @Override
    public int previousIndex() {
      readLock();
      try {
        synchronized (localResolveLock) {
          return myListIterator.previousIndex();
        }
      } finally {
        readUnlock();
      }
    }

    @Override
    public void remove() {
      writeLock();
      try {
        synchronized (localResolveLock) {
          logicalInvoke(SerializationUtil.REMOVE_SIGNATURE, new Object[] { currentItem });
          myListIterator.remove();
          modCount++;
        }
      } finally {
        writeUnlock();
      }
    }

    @Override
    public void set(E o) {
      writeLock();
      try {
        synchronized (localResolveLock) {
          Object objectToPut = createTCCompatibleObject(o);
          logicalInvoke(SerializationUtil.SET_SIGNATURE, new Object[] { Integer.valueOf(currentIndex), objectToPut });
          myListIterator.set(objectToPut);
        }
      } finally {
        writeUnlock();
      }
    }

    @Override
    public void add(E o) {
      writeLock();
      try {
        synchronized (localResolveLock) {
          Object objectToPut = createTCCompatibleObject(o);
          logicalInvoke(SerializationUtil.ADD_SIGNATURE, new Object[] { objectToPut });
          myListIterator.add(objectToPut);
          modCount++;
        }
      } finally {
        writeUnlock();
      }
    }

  }

  private class SimpleListIterator implements Iterator<E> {
    private final Iterator myLocalIterator;
    private Object         currentItem;

    SimpleListIterator(Iterator myLocalIterator) {
      this.myLocalIterator = myLocalIterator;
    }

    @Override
    public boolean hasNext() {
      readLock();
      try {
        synchronized (localResolveLock) {
          return myLocalIterator.hasNext();
        }
      } finally {
        readUnlock();
      }
    }

    @Override
    public E next() {
      readLock();
      try {
        synchronized (localResolveLock) {
          currentItem = myLocalIterator.next();
          return getValueFromTCCompatibleObject(currentItem);
        }
      } finally {
        readUnlock();
      }
    }

    @Override
    public void remove() {
      writeLock();
      try {
        synchronized (localResolveLock) {
          logicalInvoke(SerializationUtil.REMOVE_SIGNATURE, new Object[] { currentItem });
          myLocalIterator.remove();
        }
      } finally {
        writeUnlock();
      }
    }
  }

  /*
   * Internal methods - called by the applicator. No need to broadcast but only apply locally
   */

  /**
   * @param o is either {@link SerializedClusterObject} or Literal
   */
  public void internalAdd(Object o) {
    localList.add(o);
    modCount++;
  }

  /**
   * @param o is either {@link SerializedClusterObject} or Literal
   */
  public void internalAdd(int location, Object o) {
    localList.add(location, o);
    modCount++;
  }

  /**
   * @param o is either {@link SerializedClusterObject} or Literal
   */
  public void internalSet(int index, Object o) {
    localList.set(index, o);
  }

  /**
   * @param o is either {@link SerializedClusterObject} or Literal
   */
  public void internalRemove(Object o) {
    localList.remove(o);
    modCount++;
  }

  /**
   * @param o is either {@link SerializedClusterObject} or Literal
   */
  public void internalRemove(int index) {
    localList.remove(index);
    modCount++;
  }

  public void internalClear() {
    localList.clear();
    modCount++;
  }

  @Override
  public void cleanupOnDestroy() {
    this.localList.clear();
    modCount++;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((localList == null) ? 0 : localList.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    ToolkitListImpl other = (ToolkitListImpl) obj;
    if (localList == null) {
      if (other.localList != null) return false;
    } else if (!localList.equals(other.localList)) return false;
    return true;
  }

  @Override
  public boolean contains(Object o) {
    readLock();
    try {
      synchronized (localResolveLock) {

        return indexOf(o) != -1;
      }
    } finally {
      readUnlock();
    }
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    readLock();
    try {
      synchronized (localResolveLock) {

        for (Object o : c) {
          if (!contains(o)) { return false; }
        }
        return true;
      }
    } finally {
      readUnlock();
    }
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    writeLock();
    try {
      boolean isRemoved = false;
      synchronized (localResolveLock) {
        for (Object o : c) {
          if (unlockedRemove(o)) {
            isRemoved = true;
          }
        }
      }
      if (isRemoved) {
        modCount++;
      }
      return isRemoved;
    } finally {
      writeUnlock();
    }
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    writeLock();
    try {
      synchronized (localResolveLock) {
        boolean modified = false;
        Iterator iter = localList.iterator();
        while (iter.hasNext()) {
          Object tcCompatibleObj = iter.next();
          Object tempObject = getValueFromTCCompatibleObject(tcCompatibleObj);
          if (!c.contains(tempObject)) {
            logicalInvoke(SerializationUtil.REMOVE_SIGNATURE, new Object[] { tcCompatibleObj });
            iter.remove();
            modified = true;
          }
        }
        if (modified) {
          modCount++;
        }
        return modified;
      }
    } finally {
      writeUnlock();
    }
  }

  @Override
  public String toString() {
    return "ClusteredListImpl [localList=" + localList + "]";
  }

}
