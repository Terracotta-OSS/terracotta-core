/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.async.api.EventContext;
import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ObjectID;
import com.tc.object.session.SessionID;

import java.io.IOException;

/**
 * @author steve
 */
public class RequestRootResponseMessage extends DSOMessageBase implements EventContext {
  private final static byte ROOT_NAME = 1;
  private final static byte ROOT_ID   = 2;

  private String            rootName;
  private ObjectID          rootID;

  public RequestRootResponseMessage(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out, MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public RequestRootResponseMessage(SessionID sessionID, MessageMonitor monitor, MessageChannel channel, TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID,  monitor, channel, header, data);
  }

  protected void dehydrateValues() {
    putNVPair(ROOT_ID, rootID.toLong());
    putNVPair(ROOT_NAME, rootName);
  }

  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case ROOT_ID:
        this.rootID = new ObjectID(getLongValue());
        return true;
      case ROOT_NAME:
        this.rootName = getStringValue();
        return true;
      default:
        return false;
    }
  }

  public String getRootName() {
    return rootName;
  }

  public ObjectID getRootID() {
    return rootID;
  }

  public void initialize(String name, ObjectID id) {
    this.rootID = id;
    this.rootName = name;
  }
}
