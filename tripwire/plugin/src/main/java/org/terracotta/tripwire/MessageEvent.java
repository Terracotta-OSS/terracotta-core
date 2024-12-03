/*
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
