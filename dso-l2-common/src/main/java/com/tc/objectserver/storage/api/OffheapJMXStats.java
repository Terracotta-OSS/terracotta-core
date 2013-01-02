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

                                                              @Override
                                                              public long getOffheapReservedSize() {
                                                                return 0;
                                                              }

                                                              @Override
                                                              public long getOffheapUsedSize() {
                                                                return 0;
                                                              }

                                                              @Override
                                                              public long getOffheapMaxSize() {
                                                                return 0;
                                                              }

                                                              @Override
                                                              public PrettyPrinter prettyPrint(PrettyPrinter out) {
                                                                return out;
                                                              }
                                                            };
}
