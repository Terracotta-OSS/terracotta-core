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
package com.tc.net.protocol.tcm;

import com.tc.async.api.MultiThreadedEventContext;
import com.tc.async.api.Sink;

public class HydrateContext<T> implements MultiThreadedEventContext {

  private final Sink<T>      destSink;
  private final TCAction message;

  public HydrateContext(TCAction message, Sink<T> destSink) {
    this.message = message;
    this.destSink = destSink;
  }

  public Sink<T> getDestSink() {
    return destSink;
  }

  public TCAction getMessage() {
    return message;
  }

  @Override
  public Object getSchedulingKey() {
    return message.getSourceNodeID();
  }
  
  @Override
  public boolean flush() {
//  hydrate operations are independent and don't need a flush
    return false;
  }
}
