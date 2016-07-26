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

import com.tc.entity.MessageCodecSupplier;
import com.tc.async.api.Sink;
import com.tc.entity.NetworkVoltronEntityMessage;
import com.tc.entity.VoltronEntityMessage;
import com.tc.net.protocol.tcm.HydrateContext;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.net.protocol.tcm.TCMessageSink;

public class VoltronMessageSink implements TCMessageSink {
  private final Sink<VoltronEntityMessage> destSink;
  private final Sink<HydrateContext> hydrateSink;
  private final MessageCodecSupplier codecSupplier;
  

  public VoltronMessageSink(Sink<VoltronEntityMessage> destSink, Sink<HydrateContext> hydrateSink, MessageCodecSupplier codecSupplier) {
    this.destSink = destSink;
    this.hydrateSink = hydrateSink;
    this.codecSupplier = codecSupplier;
  }

  @Override
  public void putMessage(TCMessage message) { 
    if (message instanceof NetworkVoltronEntityMessage) {
      ((NetworkVoltronEntityMessage)message).setMessageCodecSupplier(codecSupplier);
    }
    HydrateContext<VoltronEntityMessage> context = new HydrateContext<>(message, destSink);
    hydrateSink.addMultiThreaded(context);
  }
  
    
  
}
