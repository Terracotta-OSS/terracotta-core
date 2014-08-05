/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.internal.collections.ToolkitListInternal;
import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;

import com.tc.net.GroupID;
import com.tc.object.LiteralValues;
import com.tc.object.LogicalOperation;
import com.tc.object.TCObject;
import com.tc.platform.PlatformService;
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
  private final transient ToolkitLock             concurrentLock;

  public ToolkitListImpl(PlatformService platformService) {
    super(platformService);
    concurrentLock = ToolkitLockingApi.createConcurrentTransactionLock("CONCURRENT", platformService);
  }

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

    return ((SerializedClusterObject<E>) o).getValue(serStrategy, false, false);
  }

  @Override
  public boolean add(E e) {
    if (e == null) { throw new NullPointerException("Object passed in to add was null"); }

    writeLock();
    try {
      synchronized (localResolveLock) {
        Object o = createTCCompatibleObject(e);
        logicalInvoke(LogicalOperation.ADD, new Object[] { o });
        localList.add(o);
        return true;
      }
    } finally {
      writeUnlock();
    }
  }

  @Override
  public boolean unlockedAdd(E e) {
    if (e == null) { throw new NullPointerException("Object passed in to add was null"); }

    synchronized (localResolveLock) {
      Object o = createTCCompatibleObject(e);
      platformService.logicalInvoke(this, LogicalOperation.ADD, new Object[] { o });
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
        logicalInvoke(LogicalOperation.ADD_AT, new Object[] { Integer.valueOf(index), o });
        localList.add(index, o);
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
        logicalInvoke(LogicalOperation.ADD_ALL, new Object[] { collection });
        return localList.addAll(collection);

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
        logicalInvoke(LogicalOperation.ADD_ALL_AT, new Object[] { Integer.valueOf(index), collection });
        return localList.addAll(index, collection);

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
        logicalInvoke(LogicalOperation.CLEAR, new Object[] {});
        localList.clear();
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

  private void logicalInvoke(LogicalOperation method, Object[] params) {
    concurrentLock.lock();
    try {
      platformService.logicalInvoke(this, method, params);
    } finally {
      concurrentLock.unlock();
    }
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
      return new SimpleIterator(localList.iterator(), 0);
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
      return new SimpleListIterator(localList.listIterator(start), start);
    } finally {
      readUnlock();
    }
  }

  @Override
  public E remove(int index) {
    writeLock();
    try {
      synchronized (localResolveLock) {

        logicalInvoke(LogicalOperation.REMOVE_AT, new Object[] { Integer.valueOf(index) });
        return getValueFromTCCompatibleObject(localList.remove(index));
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
          logicalInvoke(LogicalOperation.REMOVE_AT, new Object[] { Integer.valueOf(index) });
          iterator.remove();
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
        logicalInvoke(LogicalOperation.SET, new Object[] { Integer.valueOf(index), o });
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
      return new SubTerracottaList(fromIndex, this.localList.subList(fromIndex, toIndex));
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
    private final int  startIndex;
    private final List subList;

    public SubTerracottaList(int start, List subList) {
      this.startIndex = start;
      this.subList = subList;
    }

    private void checkModification() throws ConcurrentModificationException {
      subList.size();
    }

    private void checkRange(int index) {
      if (index < 0 || index >= subList.size()) { throw new IndexOutOfBoundsException(); }
    }

    @Override
    public void add(int index, E element) {
      writeLock();
      try {
        synchronized (localResolveLock) {
          checkModification();
          checkRange(index);
          Object o = createTCCompatibleObject(element);
          logicalInvoke(LogicalOperation.ADD_AT, new Object[] { Integer.valueOf(startIndex + index), o });
          subList.add(index, o);
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
          checkModification();
          checkRange(index);
          Collection collection = createTCCompatiableCollection(c);
          logicalInvoke(LogicalOperation.ADD_ALL_AT, new Object[] { Integer.valueOf(startIndex + index),
              collection });
          return subList.addAll(index, c);
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
          checkModification();
          Collection collection = createTCCompatiableCollection(c);
          logicalInvoke(LogicalOperation.ADD_ALL_AT,
                        new Object[] { Integer.valueOf(startIndex + subList.size()), collection });
          return subList.addAll(collection);
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
          checkModification();
          logicalInvoke(LogicalOperation.REMOVE_RANGE,
                        new Object[] { Integer.valueOf(startIndex), Integer.valueOf(startIndex + subList.size()) });
          subList.clear();
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
          checkModification();
          checkRange(index);
          return getValueFromTCCompatibleObject(subList.get(index));
        }
      } finally {
        readUnlock();
      }
    }

    private Iterator<E> subIterator() {
      readLock();
      try {
        return new SimpleIterator(subList.iterator(), startIndex);
      } finally {
        readUnlock();
      }
    }

    private ListIterator<E> simpleListIterator(int index) {
      readLock();
      try {
        checkRange(index);
        return new SimpleListIterator(subList.listIterator(index), startIndex + index);
      } finally {
        readUnlock();
      }
    }

    @Override
    public Iterator<E> iterator() {
      synchronized (localResolveLock) {
        checkModification();
        return subIterator();
      }
    }

    @Override
    public ListIterator<E> listIterator() {
      return listIterator(0);
    }

    @Override
    public ListIterator<E> listIterator(int start) {
      synchronized (localResolveLock) {
        checkModification();
        checkRange(start);
        return simpleListIterator(start);
      }
    }

    @Override
    public int size() {
      readLock();
      try {
        synchronized (localResolveLock) {
          checkModification();
          return subList.size();
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
          checkModification();
          return subList.size() == 0;
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
          checkModification();
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
          checkModification();
          Object[] array = new Object[subList.size()];
          int index = 0;
          for (Object tempObj : subList) {
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
          checkModification();
          if (a.length < subList.size()) {
            return (T[]) toArray();
          } else {
            int index = 0;
            for (Object tempObj : subList) {
              a[index] = (T) getValueFromTCCompatibleObject(tempObj);
              index++;
            }
            for (int i = subList.size(); i < a.length; i++) {
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
          checkModification();
          Object o = createTCCompatibleObject(e);
          logicalInvoke(LogicalOperation.ADD_AT, new Object[] {
              Integer.valueOf(startIndex + subList.size()), o });
          subList.add(o);
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
        synchronized (localResolveLock) {
          checkModification();
        }
        return unlockedRemoveSubList(o);
      } finally {
        writeUnlock();
      }
    }

    private boolean unlockedRemoveSubList(Object o) {
      synchronized (localResolveLock) {
        Iterator iterator = this.iterator();
        while (iterator.hasNext()) {
          if (o.equals(iterator.next())) {
            iterator.remove();
            return true;
          }
        }
        return false;
      }
    }

    @Override
    public boolean containsAll(Collection<?> c) {
      readLock();
      try {
        synchronized (localResolveLock) {
          checkModification();
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
        synchronized (localResolveLock) {
          checkModification();
          boolean isRemoved = false;
          for (Object o : c) {
            if (unlockedRemoveSubList(o)) {
              isRemoved = true;
            }
          }
          return isRemoved;
        }
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
          checkModification();
          Iterator iter = subList.iterator();
          while (iter.hasNext()) {
            Object tcCompatibleObj = iter.next();
            Object tempObject = getValueFromTCCompatibleObject(tcCompatibleObj);
            if (!c.contains(tempObject)) {
              logicalInvoke(LogicalOperation.REMOVE, new Object[] { tcCompatibleObj });
              iter.remove();
              modified = true;
            }
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
          checkModification();
          checkRange(index);
          Object o = createTCCompatibleObject(element);
          logicalInvoke(LogicalOperation.SET, new Object[] { Integer.valueOf(startIndex + index), o });
          return getValueFromTCCompatibleObject(subList.set(index, o));
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
          checkModification();
          checkRange(index);
          logicalInvoke(LogicalOperation.REMOVE_AT, new Object[] { Integer.valueOf(startIndex + index) });
          return getValueFromTCCompatibleObject(subList.remove(index));
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
          checkModification();
          int index = 0;
          for (Object tempObj : subList) {
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
          checkModification();
          int index = 0;
          int setIndex = -1;
          for (Object tempObj : subList) {
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
          checkModification();
          if (fromIndex < 0 || toIndex > subList.size() || fromIndex > toIndex) { throw new IndexOutOfBoundsException(); }
          return new SubTerracottaList(fromIndex + startIndex, subList.subList(fromIndex, toIndex));
        }
      } finally {
        readUnlock();
      }

    }
  }

  /*
   * it is ListIterator used both by ToolkitList and SubTerracotsList
   */
  private class SimpleListIterator implements ListIterator<E> {
    private final ListIterator myListIterator;
    private int                currentIndex;

    SimpleListIterator(ListIterator myListIterator, int startIndex) {
      this.myListIterator = myListIterator;
      this.currentIndex = startIndex - 1;
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
          Object iter = myListIterator.next();
          currentIndex++;
          return getValueFromTCCompatibleObject(iter);
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
          Object iter = myListIterator.previous();
          currentIndex--;
          return getValueFromTCCompatibleObject(iter);
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
          logicalInvoke(LogicalOperation.REMOVE_AT, new Object[] { Integer.valueOf(currentIndex) });
          myListIterator.remove();
          currentIndex--;
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
          logicalInvoke(LogicalOperation.SET, new Object[] { Integer.valueOf(currentIndex),
              objectToPut });
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
          logicalInvoke(LogicalOperation.ADD, new Object[] { objectToPut });
          myListIterator.add(objectToPut);
          currentIndex++;
        }
      } finally {
        writeUnlock();
      }
    }

  }

  private class SimpleIterator implements Iterator<E> {
    private final Iterator myLocalIterator;
    private int            currentIndex;

    SimpleIterator(Iterator myLocalIterator, int startIndex) {
      this.myLocalIterator = myLocalIterator;
      this.currentIndex = startIndex - 1;
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
          Object element = myLocalIterator.next();
          currentIndex++;
          return getValueFromTCCompatibleObject(element);
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
          logicalInvoke(LogicalOperation.REMOVE_AT, new Object[] { Integer.valueOf(currentIndex) });
          myLocalIterator.remove();
          currentIndex--;
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
  }

  /**
   * @param o is either {@link SerializedClusterObject} or Literal
   */
  public void internalAdd(int location, Object o) {
    localList.add(location, o);
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
  }

  /**
   * @param o is either {@link SerializedClusterObject} or Literal
   */
  public void internalRemove(int index) {
    localList.remove(index);
  }

  public void internalClear() {
    localList.clear();
  }

  @Override
  public void cleanupOnDestroy() {
    this.localList.clear();
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
            logicalInvoke(LogicalOperation.REMOVE, new Object[] { tcCompatibleObj });
            iter.remove();
            modified = true;
          }
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
