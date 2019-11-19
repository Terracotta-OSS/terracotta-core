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
package org.terracotta.tripwire;

import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.StackTrace;
import jdk.jfr.Threshold;


@Category("Tripwire")
@StackTrace(false)
@Label("Message")
@Threshold("250 ms")
class MessageEvent extends Event implements org.terracotta.tripwire.Event {

  private final int concurrency;
  private final String action;
  private String description;
  private final long source;
  private final String instance;
  private final long transaction;
  private final String entity;
  private final String trace;

  MessageEvent(String eid, int concurrency, String action, long source, String instance, long transaction, String trace) {
    this.entity = eid;
    this.concurrency = concurrency;
    this.action = action;
    this.source = source;
    this.instance = instance;
    this.transaction = transaction;
    this.trace = trace;
  }

  @Override
  public void setDescription(String description) {
    this.description = description;
  }
}
