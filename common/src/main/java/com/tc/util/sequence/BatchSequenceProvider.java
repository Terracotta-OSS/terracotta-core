/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.sequence;

import com.tc.util.sequence.BatchSequenceReceiver;

/**
 * @author steve Responsible for retrieving a sequence segment asynchronously
 */
public interface BatchSequenceProvider {
  public void requestBatch(BatchSequenceReceiver receiver, int size);
}