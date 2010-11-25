/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

import java.io.Serializable;

public interface OffheapStats extends Serializable {
  public static final OffheapStats NULL_OFFHEAP_STATS = new OffheapStats() {

                                                        public long getOffheapObjectCachedCount() {
                                                          return 0;
                                                        }

                                                        public long getOffheapMaxDataSize() {
                                                          return 0;
                                                        }

                                                        public long getOffHeapFlushRate() {
                                                          return 0;
                                                        }

                                                        public long getOffHeapFaultRate() {
                                                          return 0;
                                                        }

                                                        public long getExactOffheapObjectCachedCount() {
                                                          return 0;
                                                        }

                                                        public long getOffheapTotalOccupiedSize() {
                                                          return 0;
                                                        }

                                                        public long getOffheapMapOccupiedMemory() {
                                                          return 0;
                                                        }

                                                        public long getOffheapObjectOccupiedMemory() {
                                                          return 0;
                                                        }
                                                      };

  long getOffheapObjectCachedCount();

  /**
   * This will be locked
   */
  long getExactOffheapObjectCachedCount();

  long getOffHeapFaultRate();

  long getOffHeapFlushRate();

  long getOffheapMaxDataSize();

  long getOffheapTotalOccupiedSize();

  long getOffheapObjectOccupiedMemory();

  long getOffheapMapOccupiedMemory();
}
