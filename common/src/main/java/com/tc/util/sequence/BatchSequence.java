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

import com.tc.exception.TCRuntimeException;

/**
 * This Sequence deals with batches. It keeps a next batch around to avoid pauses and always requests a new next batch
 * as soon as the old next batch is promoted to current batch
 * 
 * @author steve, orion
 */
public final class BatchSequence implements BatchSequenceReceiver, Sequence {

  private static final SequenceBatch  NULL_SEQUENCE_BATCH = new SequenceBatch(0, 0);

  private SequenceBatch               current             = NULL_SEQUENCE_BATCH;
  private SequenceBatch               nextBatch           = NULL_SEQUENCE_BATCH;
  private boolean                     requestInProgress;
  private final BatchSequenceProvider remoteProvider;
  private final int                   batchSize;

  public BatchSequence(BatchSequenceProvider sequenceProvider, int batchSize) {
    this.remoteProvider = sequenceProvider;
    this.batchSize = batchSize;
  }

  @Override
  public synchronized long next() {
    requestMoreIDsIfNecessary();
    return this.current.next();
  }

  @Override
  public synchronized long current() {
    return this.current.current();
  }

  private void requestMoreIDsIfNecessary() {

    // This should only happen the first time
    while (!this.current.hasNext() && !this.nextBatch.hasNext()) {
      if (!this.requestInProgress) {
        requestNextBatch();
      }
      try {
        if (!this.current.hasNext() && !this.nextBatch.hasNext()) {
          this.wait();
        }
      } catch (InterruptedException ie) {
        throw new TCRuntimeException(ie);
      }
    }

    // This is the more normal case
    if (!this.current.hasNext()) {
      this.current = this.nextBatch;
      this.nextBatch = NULL_SEQUENCE_BATCH;
      requestNextBatch();
    }
  }

  private void requestNextBatch() {
    this.remoteProvider.requestBatch(this, this.batchSize);
    this.requestInProgress = true;
  }

  @Override
  public synchronized void setNextBatch(long start, long end) {
    this.nextBatch = new SequenceBatch(start, end);
    this.requestInProgress = false;
    this.notifyAll();
  }

  // The currentBatch is not considered here as we want to greedily get the next set even if the
  // current set has some available.
  @Override
  public synchronized boolean isBatchRequestPending() {
    return !this.nextBatch.hasNext();
  }

  public synchronized BatchSequenceProvider getProvider() {
    return this.remoteProvider;
  }
}
