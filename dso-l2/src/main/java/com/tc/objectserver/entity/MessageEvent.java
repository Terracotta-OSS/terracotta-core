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
package com.tc.objectserver.entity;

import com.tc.object.ClientInstanceID;
import com.tc.object.EntityID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ServerEntityAction;
import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.StackTrace;
import jdk.jfr.Threshold;


@Category("Java Application")
@StackTrace(false)
@Label("TC Message")
@Threshold("250 ms")
public class MessageEvent extends Event {

  private final int concurrency;
  private Class<?> type;
  private final ServerEntityAction action;
  private String debug;
  private final String source;
  private final long transaction;
  private final EntityID entity;
  private final String trace;

  public MessageEvent(EntityID eid, int concurrency, ServerEntityAction action, ClientInstanceID source, TransactionID transaction, String trace) {
    this.entity = eid;
    this.concurrency = concurrency;
    this.action = action;
    this.source = source.toString();
    this.transaction = transaction.toLong();
    this.trace = trace;
  }

  public void setType(Class<?> type) {
    this.type = type;
  }

  public void setDebug(String debug) {
    this.debug = debug;
  }
  
  
}
