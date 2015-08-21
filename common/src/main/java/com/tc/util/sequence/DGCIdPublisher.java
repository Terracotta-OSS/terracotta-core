/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.sequence;

public interface DGCIdPublisher {
  void publishNextAvailableDGCID(long nextDGCId);
}
