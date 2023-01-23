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
package com.tc.net.groups;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.NetworkRecall;
import com.tc.net.protocol.tcm.TCActionNetworkMessage;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.DSOMessageBase;
import com.tc.object.session.SessionID;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * @author EY
 */
public class TCGroupMessageWrapper extends DSOMessageBase {
  private final static byte GROUP_MESSAGE_ID = 1;
  private AbstractGroupMessage message;
  private Runnable sentCallback;

  public TCGroupMessageWrapper(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out,
                               MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public TCGroupMessageWrapper(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                               TCMessageHeader header, TCByteBufferInputStream data) {
    super(sessionID, monitor, channel, header, data);
  }

  public void setGroupMessage(AbstractGroupMessage message) {
    this.message = message;
  }

  public AbstractGroupMessage getGroupMessage() {
    return this.message;
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(GROUP_MESSAGE_ID, this.message.getClass().getName());
    this.message.serializeTo(getOutputStream());
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case GROUP_MESSAGE_ID:
        TCByteBufferInputStream in = getInputStream();
        try {
          this.message = (AbstractGroupMessage) Class.forName(in.readString()).getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | InvocationTargetException | NoSuchMethodException e) {
          throw new RuntimeException(e);
        }
        this.message.deserializeFrom(in);
        return true;

      default:
        return false;
    }
  }
  
  public NetworkRecall send(Runnable callback) {
    sentCallback = callback;
    return super.send();
  }

  @Override
  public TCActionNetworkMessage convertToNetworkMessage() {
    TCActionNetworkMessage msg = super.convertToNetworkMessage();
    if (sentCallback != null) {
      msg.addCompleteCallback(sentCallback);
    }
    return msg;
  }
  
  
}