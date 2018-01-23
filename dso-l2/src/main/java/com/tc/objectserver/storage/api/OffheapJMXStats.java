/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.objectserver.storage.api;

import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;

/**
 * Additional Offheap Stats which can be accessed vis JMX beans. These methods are not exposed in OffheapStats as they
 * can affect performance and don't want users like dev-consoles to invoke them frequently.
 */
public interface OffheapJMXStats extends OffheapStats {

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

                                                              
                                                            };
}
