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
import com.tc.object.management.ManagementRequestID;
import com.tc.object.session.SessionID;

import java.io.IOException;

/**
 *
 */
public abstract class AbstractManagementMessage extends DSOMessageBase {

  protected static final byte REQUEST_ID = 0;

  private ManagementRequestID managementRequestID;

  public AbstractManagementMessage(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out, MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
    this.managementRequestID = generateId();
  }

  public AbstractManagementMessage(SessionID sessionID, MessageMonitor monitor, MessageChannel channel, TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
    this.managementRequestID = generateId();
  }

  private ManagementRequestID generateId() {
    return new ManagementRequestID();
  }

  public ManagementRequestID getManagementRequestID() {
    return managementRequestID;
  }

  /**
   * The ID of the request, used to correlate a request with a response. Can be null if the "response" did not come from a request, ie: when it is an event.
   */
  public void setManagementRequestID(ManagementRequestID managementRequestID) {
    this.managementRequestID = managementRequestID;
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(REQUEST_ID, managementRequestID == null ? null : "" + managementRequestID.getId());
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case REQUEST_ID:
        String stringValue = getStringValue();
        managementRequestID =  (stringValue == null) ? null : new ManagementRequestID(new Long(stringValue));
        return true;

      default:
        return false;
    }
  }

}
