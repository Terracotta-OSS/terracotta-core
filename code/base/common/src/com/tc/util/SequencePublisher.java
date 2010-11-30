/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

import com.tc.util.sequence.DGCIdPublisher;

public interface SequencePublisher {
  void registerSequecePublisher(DGCIdPublisher dgcIdPublisher);
}
