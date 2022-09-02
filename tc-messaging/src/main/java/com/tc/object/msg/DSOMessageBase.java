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
package com.tc.object.msg;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
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
  public TCNetworkMessage send() {
    if (!localSessionID.equals(getChannel().getSessionID())) {
      LOG.debug("not same connection {} != {}", localSessionID, getChannel().getSessionID());
    }
    return super.send();
  }



}
