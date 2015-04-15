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
package com.tc.util.concurrent;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class CircularLossyQueue<T> {
  private final AtomicReference<T>[] circularArray;
  private final int                  maxSize;

  private final AtomicLong           currentIndex = new AtomicLong(-1);

  public CircularLossyQueue(int size) {
    this.circularArray = new AtomicReference[size];
    for (int i = 0; i < size; i++) {
      this.circularArray[i] = new AtomicReference<T>();
    }
    this.maxSize = size;
  }

  public T push(T newVal) {
    int index = (int) (currentIndex.incrementAndGet() % maxSize);
    AtomicReference<T> ref = circularArray[index];
    T oldVal = ref.get();
    ref.set(newVal);
    return oldVal;
  }

  public T[] toArray(T[] type) {
    if (type.length > maxSize) { throw new IllegalArgumentException("Size of array passed in cannot be greater than "
                                                                    + maxSize); }

    int curIndex = getCurrentIndex();
    for (int k = 0; k < type.length; k++) {
      int index = getIndex(curIndex - k);
      type[k] = circularArray[index].get();
    }
    return type;
  }

  private int getIndex(int index) {
    index = index < 0 ? index + maxSize : index;
    return index;
  }

  public T peek() {
    if (depth() == 0) return null;
    return circularArray[getIndex(getCurrentIndex())].get();
  }

  public boolean isEmtpy() {
    return depth() == 0;
  }

  private int getCurrentIndex() {
    return (int) (currentIndex.get() % maxSize);
  }

  public int depth() {
    long currInd = currentIndex.get() + 1;
    return currInd >= maxSize ? maxSize : (int) currInd;
  }
}
