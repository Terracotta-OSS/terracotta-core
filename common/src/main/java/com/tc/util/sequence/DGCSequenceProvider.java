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
package com.tc.util.sequence;

import com.tc.util.SequencePublisher;

import java.util.concurrent.CopyOnWriteArrayList;

public class DGCSequenceProvider implements SequencePublisher {

  private final MutableSequence                      dgcSequence;
  private final CopyOnWriteArrayList<DGCIdPublisher> dgcIdListeners = new CopyOnWriteArrayList<DGCIdPublisher>();

  public DGCSequenceProvider(MutableSequence dgcSequence) {
    this.dgcSequence = dgcSequence;
  }

  public long currentIDValue() {
    return this.dgcSequence.current();
  }

  public void setNextAvailableDGCId(long nextId) {
    this.dgcSequence.setNext(nextId);
  }

  public long getNextId() {
    long nexId = this.dgcSequence.next();
    publishNextId(nexId + 1);
    return nexId;
  }

  private void publishNextId(long nexId) {
    for (DGCIdPublisher dgcIdPublisher : dgcIdListeners) {
      dgcIdPublisher.publishNextAvailableDGCID(nexId);
    }
  }

  @Override
  public void registerSequecePublisher(DGCIdPublisher dgcIdPublisher) {
    this.dgcIdListeners.add(dgcIdPublisher);
  }

}
