/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
import com.tc.net.protocol.tcm.TCMessageHydrateSink;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.tc.net.protocol.tcm.TCAction;

public class VoltronMessageSink extends TCMessageHydrateSink<VoltronEntityMessage> {
  private final MessageCodecSupplier codecSupplier;
  private final Stage<HydrateContext> helper;
  private final Sink<VoltronEntityMessage> dest;
  private static final Logger LOGGER = LoggerFactory.getLogger(VoltronMessageSink.class);
  private boolean always_hydrate = TCPropertiesImpl.getProperties()
                                                     .getBoolean(TCPropertiesConsts.L2_SEDA_STAGE_ALWAYS_HYDRATE, false);

  public VoltronMessageSink(Stage<HydrateContext> helper, Sink<VoltronEntityMessage> destSink, MessageCodecSupplier codecSupplier) {
    super(destSink);
    this.helper = helper;
    this.codecSupplier = codecSupplier;
    this.dest = destSink;
  }

  @Override
  public void putMessage(TCAction message) { 
    if (message instanceof NetworkVoltronEntityMessage) {
      ((NetworkVoltronEntityMessage)message).setMessageCodecSupplier(codecSupplier);
      if (always_hydrate || TCComm.hasPendingRead() || !helper.isEmpty()) {
        helper.getSink().addToSink(new HydrateContext(message, this.dest));
      } else {
        super.putMessage(message);
      }
    } else {
      Assert.fail();
    }
  }
  
  public void setAlwaysHydrate(boolean hydrate) {
    always_hydrate = hydrate;
  }
  
  public boolean isAlwaysHydrate() {
    return always_hydrate;
  }
}
