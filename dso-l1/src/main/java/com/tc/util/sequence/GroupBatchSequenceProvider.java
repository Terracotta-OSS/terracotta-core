/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.sequence;

import java.util.LinkedList;

public class GroupBatchSequenceProvider implements BatchSequenceProvider, BatchSequenceReceiver {
  private final int                   requestBatchSize;
  private final BatchSequenceProvider remoteSequenceProvider;
  private final LinkedList<Request>   requests                 = new LinkedList<>();
  private SequenceBatch               currentBatchIdsAvailable = new SequenceBatch(0, 0);
  private boolean                     isRequestInProgress      = false;

  public GroupBatchSequenceProvider(BatchSequenceProvider sequenceProvider, int requestSize) {
    this.remoteSequenceProvider = sequenceProvider;
    this.requestBatchSize = requestSize;
  }

  @Override
  public synchronized boolean isBatchRequestPending() {
    int sizePresent = (int) (currentBatchIdsAvailable.end() - currentBatchIdsAvailable.current() - 1);

    return (sizePresent == 0 || isRequestInProgress);
  }

  @Override
  public synchronized void requestBatch(BatchSequenceReceiver receiver, int sizeRequested) {
    // check if there are presently object ids present to be sent to the client
    int sizePresent = (int) (currentBatchIdsAvailable.end() - currentBatchIdsAvailable.current() - 1);
    if (sizePresent >= sizeRequested) {
      long end = currentBatchIdsAvailable.current() + 1 + sizeRequested;
      receiver.setNextBatch(currentBatchIdsAvailable.current() + 1, end);
      currentBatchIdsAvailable = new SequenceBatch(end, currentBatchIdsAvailable.end());
    } else {
      // if not present and add to the list of requests
      requests.addLast(new Request(receiver, sizeRequested));

      if (isRequestInProgress) { return; }

      isRequestInProgress = true;
      remoteSequenceProvider.requestBatch(this, requestBatchSize);
    }
  }

  @Override
  public synchronized void setNextBatch(long start, long end) {
    isRequestInProgress = false;
    // add to the current batch
    currentBatchIdsAvailable = new SequenceBatch(start, end);

    // check if some request has to be served for the size else add to the current batch
    int sizeOfList = requests.size();
    while (sizeOfList > 0) {
      // Check for all the requests
      Request request = requests.removeFirst();
      requestBatch(request.getReciever(), request.getSize());
      sizeOfList--;
    }
  }

  private static class Request {
    private final BatchSequenceReceiver reciever;
    private final int                   size;

    public Request(BatchSequenceReceiver reciever, int size) {
      this.size = size;
      this.reciever = reciever;
    }

    public BatchSequenceReceiver getReciever() {
      return reciever;
    }

    public int getSize() {
      return size;
    }
  }
}
