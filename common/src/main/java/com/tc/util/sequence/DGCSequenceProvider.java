/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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

  public void registerSequecePublisher(DGCIdPublisher dgcIdPublisher) {
    this.dgcIdListeners.add(dgcIdPublisher);
  }

}
