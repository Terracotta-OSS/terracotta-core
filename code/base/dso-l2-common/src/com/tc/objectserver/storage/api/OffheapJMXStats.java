/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;

/**
 * Additional Offheap Stats which can be accessed vis JMX beans. These methods are not exposed in OffheapStats as they
 * can affect performance and don't want users like dev-consoles to invoke them frequently.
 */
public interface OffheapJMXStats extends OffheapStats, PrettyPrintable {

  public static final OffheapJMXStats NULL_OFFHEAP_JMXSTATS = new OffheapJMXStats() {

                                                              public long getOffheapTotalAllocatedSize() {
                                                                return 0;
                                                              }

                                                              public long getOffheapObjectCachedCount() {
                                                                return 0;
                                                              }

                                                              public long getOffheapObjectAllocatedMemory() {
                                                                return 0;
                                                              }

                                                              public long getOffheapMaxDataSize() {
                                                                return 0;
                                                              }

                                                              public long getOffheapMapAllocatedMemory() {
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

                                                              public long getOffheapObjectOccupiedSize() {
                                                                return 0;
                                                              }

                                                              public long getOffheapMapOccupiedSize() {
                                                                return 0;
                                                              }

                                                              public PrettyPrinter prettyPrint(PrettyPrinter out) {
                                                                return out;
                                                              }
                                                            };

  long getOffheapTotalOccupiedSize();

  long getOffheapObjectOccupiedSize();

  long getOffheapMapOccupiedSize();

}
