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

                                                        public long getOffheapFlushObjectCount() {
                                                          return 0;
                                                        }

                                                        public long getOffheapFaultObjectCount() {
                                                          return 0;
                                                        }

                                                        public long getObjectMaxDataSize() {
                                                          return 0;
                                                        }

                                                        public long getObjectAllocatedSize() {
                                                          return 0;
                                                        }

                                                        public long getMapMaxDataSize() {
                                                          return 0;
                                                        }

                                                        public long getMapAllocatedSize() {
                                                          return 0;
                                                        }
                                                      };

  long getOffheapObjectCachedCount();

  long getOffheapFaultObjectCount();

  long getOffheapFlushObjectCount();

  long getOffheapMaxDataSize();

  long getMapMaxDataSize();

  long getObjectMaxDataSize();

  long getMapAllocatedSize();

  long getObjectAllocatedSize();
}
