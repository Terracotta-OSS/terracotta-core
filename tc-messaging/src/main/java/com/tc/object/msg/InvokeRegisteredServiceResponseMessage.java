/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.management.ResponseHolder;
import com.tc.object.management.TCManagementSerializationException;
import com.tc.object.session.SessionID;

import java.io.IOException;

/**
 *
 */
public class InvokeRegisteredServiceResponseMessage extends AbstractManagementMessage {

  private static final byte RESPONSE = 1;

  private ResponseHolder responseHolder;

  public InvokeRegisteredServiceResponseMessage(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out, MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public InvokeRegisteredServiceResponseMessage(SessionID sessionID, MessageMonitor monitor, MessageChannel channel, TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  public ResponseHolder getResponseHolder() {
    return responseHolder;
  }

  public void setResponseHolder(ResponseHolder responseHolder) {
    this.responseHolder = responseHolder;
  }

  @Override
  protected void dehydrateValues() {
    super.dehydrateValues();
    putNVPair(RESPONSE, responseHolder);
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    if (!super.hydrateValue(name)) {
      switch (name) {
        case RESPONSE:
          try {
            responseHolder = (ResponseHolder)getObject(new ResponseHolder());
          } catch (TCManagementSerializationException se) {
            responseHolder = new ResponseHolder(se);
          }
          return true;

        default:
          return false;
      }
    } else {
      return true;
    }
  }

}
