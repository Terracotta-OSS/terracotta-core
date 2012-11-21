/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

import java.io.Serializable;

public interface OffheapStats extends Serializable {
  public static final OffheapStats NULL_OFFHEAP_STATS = new OffheapStats() {

                                                        public long getOffheapMaxSize() {
                                                          return 0;
                                                        }

                                                        public long getOffheapReservedSize() {
                                                          return 0;
                                                        }

                                                        public long getOffheapUsedSize() {
                                                          return 0;
                                                        }

                                                      };

  long getOffheapMaxSize();

  long getOffheapReservedSize();

  long getOffheapUsedSize();
}
