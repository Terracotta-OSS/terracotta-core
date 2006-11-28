/**
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.util.sequence;

import com.tc.util.sequence.BatchSequenceReceiver;

/**
 * @author steve Responsible for retrieving a sequence segment asynchronously
 */
public interface BatchSequenceProvider {
  public void requestBatch(BatchSequenceReceiver receiver, int size);
}