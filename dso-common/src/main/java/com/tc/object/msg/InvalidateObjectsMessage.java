/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.invalidation.Invalidations;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.session.SessionID;

import java.io.IOException;

public class InvalidateObjectsMessage extends DSOMessageBase {

  private static final byte INVALIDATIONS = 0;

  private Invalidations     invalidations;

  public InvalidateObjectsMessage(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                                  TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  public InvalidateObjectsMessage(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out,
                                  MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public void initialize(Invalidations invalids) {
    this.invalidations = invalids;
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(INVALIDATIONS, invalidations);
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case INVALIDATIONS:
        if (invalidations == null) {
          invalidations = new Invalidations();
        }
        getObject(invalidations);
        return true;
      default:
        return false;
    }
  }

  public Invalidations getObjectIDsToInvalidate() {
    return this.invalidations;
  }
}
