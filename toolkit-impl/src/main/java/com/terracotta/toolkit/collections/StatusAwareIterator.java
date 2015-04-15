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

import org.terracotta.toolkit.rejoin.RejoinException;

import com.terracotta.toolkit.util.ToolkitObjectStatus;

import java.util.Iterator;

public class StatusAwareIterator<E> implements Iterator<E> {

  private final Iterator<E>         iterator;
  private final ToolkitObjectStatus status;
  private final int                 currentRejoinCount;

  public StatusAwareIterator(Iterator<E> iterator, ToolkitObjectStatus status) {
    this.iterator = iterator;
    this.status = status;
    this.currentRejoinCount = this.status.getCurrentRejoinCount();
  }

  private void assertStatus() {
    if (status.isDestroyed()) { throw new IllegalStateException(
                                                                "Can not perform operation because object has been destroyed"); }
    if (this.currentRejoinCount != status.getCurrentRejoinCount()) { throw new RejoinException(
                                                                                               "Can not performe operation because rejoin happened."); }
  }

  @Override
  public boolean hasNext() {
    assertStatus();
    return iterator.hasNext();
  }

  @Override
  public E next() {
    assertStatus();
    return iterator.next();
  }

  @Override
  public void remove() {
    assertStatus();
    iterator.remove();
  }

}
