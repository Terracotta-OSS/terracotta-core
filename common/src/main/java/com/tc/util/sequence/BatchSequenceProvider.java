/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.sequence;

import com.tc.util.sequence.BatchSequenceReceiver;

/**
 * @author steve Responsible for retrieving a sequence segment asynchronously
 */
public interface BatchSequenceProvider {
  public void requestBatch(BatchSequenceReceiver receiver, int size);
}
