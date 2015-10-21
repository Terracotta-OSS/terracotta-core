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

package com.tc.entity;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.DSOMessageBase;
import com.tc.object.session.SessionID;

import java.io.IOException;

/**
 * @author twu
 */
public class ServerEntityResponseMessageImpl extends DSOMessageBase implements ServerEntityResponseMessage {
  private static final byte RESPONSE_ID = 0;

  private long responseId;

  public ServerEntityResponseMessageImpl(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out, MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public ServerEntityResponseMessageImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel, TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  @Override
  public void setResponseId(long responseId) {
    this.responseId = responseId;
  }

  @Override
  public long getResponseId() {
    return responseId;
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(RESPONSE_ID, responseId);
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    if (name == RESPONSE_ID) {
      responseId = getLongValue();
      return true;
    } else {
      return false;
    }
  }
}
