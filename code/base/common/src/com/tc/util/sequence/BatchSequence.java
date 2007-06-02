/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.sequence;

import com.tc.exception.TCRuntimeException;
import com.tc.util.sequence.BatchSequenceProvider;

/**
 * This Sequence deals with batches. It keeps a next batch around to avoid
 * pauses and always requests a new next batch as soon as the old next batch is
 * promoted to current batch
 * 
 * @author steve, orion
 */
public final class BatchSequence implements BatchSequenceReceiver, Sequence {
  
  private static final SequenceBatch NULL_SEQUENCE_BATCH = new SequenceBatch(0, 0);
  
  private SequenceBatch               current   = NULL_SEQUENCE_BATCH;
  private SequenceBatch               nextBatch = NULL_SEQUENCE_BATCH;
  private boolean                     requestInProgress;
  private final BatchSequenceProvider remoteProvider;
  private final int                   batchSize;

  public BatchSequence(BatchSequenceProvider sequenceProvider, int batchSize) {
    this.remoteProvider = sequenceProvider;
    this.batchSize = batchSize;
  }

  public synchronized long next() {
    requestMoreIDsIfNecessary();
    return current.next();
  }

  public synchronized long current() {
    return current.current();
  }

  private void requestMoreIDsIfNecessary() {

    // This should only happen the first time
    while (!current.hasNext() && !nextBatch.hasNext()) {
      if (!requestInProgress) requestNextBatch();
      try {
        this.wait();
      } catch (InterruptedException ie) {
        throw new TCRuntimeException(ie);
      }
    }

    // This is the more normal case
    if (!current.hasNext()) {
      this.current = nextBatch;
      this.nextBatch = NULL_SEQUENCE_BATCH;
      requestNextBatch();
    }
  }

  private void requestNextBatch() {
    remoteProvider.requestBatch(this, batchSize);
    this.requestInProgress = true;
  }

  public synchronized void setNextBatch(long start, long end) {
    this.nextBatch = new SequenceBatch(start, end);
    this.requestInProgress = false;
    this.notifyAll();
  }

  // The currentBatch is not considered here as we want to greedily get the next set even if the
  // current set has some available.
  public synchronized boolean hasNext() {
    return nextBatch.hasNext();
  }
}