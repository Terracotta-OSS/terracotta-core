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
package com.terracotta.toolkit.collections;

import org.terracotta.toolkit.collections.ToolkitSet;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.object.Destroyable;
import org.terracotta.toolkit.object.ToolkitLockedObject;
import org.terracotta.toolkit.object.ToolkitObject;

import com.tc.platform.PlatformService;
import com.terracotta.toolkit.rejoin.RejoinAwareToolkitMap;
import com.terracotta.toolkit.rejoin.RejoinAwareToolkitObject;
import com.terracotta.toolkit.util.ToolkitObjectStatusImpl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

public class ToolkitSetImpl<E> implements ToolkitSet<E>, RejoinAwareToolkitObject {
  static final Integer                            DUMMY_VALUE = 0;
  private final RejoinAwareToolkitMap<E, Integer> toolkitMap;
  protected final ToolkitObjectStatusImpl         status;

  public ToolkitSetImpl(RejoinAwareToolkitMap<E, Integer> toolkitMap, PlatformService plaformService) {
    this.toolkitMap = toolkitMap;
    this.status = new ToolkitObjectStatusImpl(plaformService);
  }

  @Override
  public void rejoinStarted() {
    toolkitMap.rejoinStarted();
  }

  @Override
  public void rejoinCompleted() {
    toolkitMap.rejoinCompleted();
  }

  @Override
  public boolean add(E element) {
    return toolkitMap.put(element, DUMMY_VALUE) == null;
  }

  @Override
  public boolean addAll(Collection<? extends E> c) {
    ReadWriteLock lock = getReadWriteLock();

    lock.writeLock().lock();
    int size = toolkitMap.size();
    try {
      Map<E, Integer> m = new HashMap<E, Integer>();
      for (E e : c) {
        m.put(e, DUMMY_VALUE);
      }
      toolkitMap.putAll(m);
      return size < toolkitMap.size();
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public void clear() {
    toolkitMap.clear();
  }

  @Override
  public boolean contains(Object element) {
    return toolkitMap.containsKey(element);
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return toolkitMap.keySet().containsAll(c);
  }

  @Override
  public boolean isEmpty() {
    return toolkitMap.isEmpty();
  }

  @Override
  public Iterator<E> iterator() {
    return new StatusAwareIterator(toolkitMap.keySet().iterator(), status);
  }

  @Override
  public boolean remove(Object element) {
    return toolkitMap.remove(element) != null;
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    ReadWriteLock lock = getReadWriteLock();
    lock.writeLock().lock();

    try {
      int size = toolkitMap.size();

      for (Object o : c) {
        remove(o);
      }

      return size > toolkitMap.size();
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    ReadWriteLock lock = getReadWriteLock();
    lock.writeLock().lock();

    try {
      int size = toolkitMap.size();

      for (Iterator iter = iterator(); iter.hasNext();) {
        if (!c.contains(iter.next())) {
          iter.remove();
        }
      }

      return size > toolkitMap.size();
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public int size() {
    return toolkitMap.size();
  }

  @Override
  public Object[] toArray() {
    return toolkitMap.keySet().toArray();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    return toolkitMap.keySet().toArray(a);
  }

  @Override
  public ToolkitReadWriteLock getReadWriteLock() {
    return ((ToolkitLockedObject) toolkitMap).getReadWriteLock();
  }

  @Override
  public String getName() {
    return ((ToolkitObject) toolkitMap).getName();
  }

  @Override
  public boolean isDestroyed() {
    return ((Destroyable) toolkitMap).isDestroyed();
  }

  @Override
  public void destroy() {
    ((Destroyable) toolkitMap).destroy();
  }

}
