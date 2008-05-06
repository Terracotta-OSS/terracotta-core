/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.DSOMessageBase;
import com.tc.object.session.SessionID;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * @author EY
 */
public class TCGroupMessageWrapper extends DSOMessageBase {
  private final static byte GROUP_MESSAGE_ID = 1;
  private GroupMessage      message;

  public TCGroupMessageWrapper(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out,
                               MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public TCGroupMessageWrapper(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                               TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  public void setGroupMessage(GroupMessage message) {
    this.message = message;
  }

  public GroupMessage getGroupMessage() {
    return this.message;
  }

  protected void dehydrateValues() {
    putNVPair(GROUP_MESSAGE_ID, 0); // to do TCMessageImpl nvCount++
    try {
      ObjectOutputStream stream = new ObjectOutputStream(getOutputStream());
      stream.writeObject(this.message);
    } catch (IOException e) {
      throw new RuntimeException();
    }
  }

  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case GROUP_MESSAGE_ID:
        TCByteBufferInputStream in = getInputStream();
        in.readInt(); // clear dummy int
        ObjectInputStream stream = new ObjectInputStream(in);
        try {
          this.message = (GroupMessage) stream.readObject();
        } catch (ClassNotFoundException e) {
          throw new RuntimeException();
        }
        return true;
      default:
        return false;
    }
  }
}