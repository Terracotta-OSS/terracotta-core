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
package com.tc.object.msg;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.NetworkRecall;
import com.tc.net.protocol.tcm.TCActionImpl;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.session.SessionID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for DSO network messages
 */
public abstract class DSOMessageBase extends TCActionImpl {

  private static final Logger LOG = LoggerFactory.getLogger(DSOMessageBase.class);
  private final SessionID localSessionID;

  public DSOMessageBase(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out, MessageChannel channel, TCMessageType type) {
    super(monitor, out, channel, type);
    this.localSessionID = sessionID;
  }

  public DSOMessageBase(SessionID sessionID, MessageMonitor monitor, MessageChannel channel, TCMessageHeader header,
                        TCByteBufferInputStream data) {
    super(monitor, channel, header, data);
    this.localSessionID = sessionID;
  }

  @Override
  public SessionID getLocalSessionID() {
    return localSessionID;
  }

  @Override
  public NetworkRecall send() {
    if (!localSessionID.equals(getChannel().getSessionID())) {
      LOG.debug("not same connection {} != {}", localSessionID, getChannel().getSessionID());
    }
    return super.send();
  }



}
