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
import com.tc.async.api.Stage;
import com.tc.entity.NetworkVoltronEntityMessage;
import com.tc.entity.VoltronEntityMessage;
import com.tc.net.core.TCComm;
import com.tc.net.protocol.tcm.HydrateContext;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.net.protocol.tcm.TCMessageHydrateSink;
import com.tc.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VoltronMessageSink extends TCMessageHydrateSink<VoltronEntityMessage> {
  private final MessageCodecSupplier codecSupplier;
  private final Stage<HydrateContext> helper;
  private final Sink<VoltronEntityMessage> dest;
  private static final Logger LOGGER = LoggerFactory.getLogger(VoltronMessageSink.class);

  public VoltronMessageSink(Stage<HydrateContext> helper, Sink<VoltronEntityMessage> destSink, MessageCodecSupplier codecSupplier) {
    super(destSink);
    this.helper = helper;
    this.codecSupplier = codecSupplier;
    this.dest = destSink;
  }

  @Override
  public void putMessage(TCMessage message) { 
    if (message instanceof NetworkVoltronEntityMessage) {
      ((NetworkVoltronEntityMessage)message).setMessageCodecSupplier(codecSupplier);
      if (TCComm.hasPendingRead() || !helper.isEmpty()) {
        helper.getSink().addToSink(new HydrateContext(message, this.dest));
      } else {
        super.putMessage(message);
      }
    } else {
      Assert.fail();
    }
  }
}
