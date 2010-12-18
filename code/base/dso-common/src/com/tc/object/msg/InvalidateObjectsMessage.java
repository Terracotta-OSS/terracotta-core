/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ObjectID;
import com.tc.object.session.SessionID;
import com.tc.util.ObjectIDSet;

import java.io.IOException;
import java.util.Set;

public class InvalidateObjectsMessage extends DSOMessageBase {

  private static final byte MANAGED_OBJECT_ID = 0;

  private Set<ObjectID>     invalidOids;

  public InvalidateObjectsMessage(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                                  TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  public InvalidateObjectsMessage(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out,
                                  MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public void initialize(Set<ObjectID> oids) {
    invalidOids = oids;
  }

  @Override
  protected void dehydrateValues() {
    // TODO::Optimize for ObjectIDSet
    for (ObjectID id : this.invalidOids) {
      putNVPair(MANAGED_OBJECT_ID, id.toLong());
    }
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case MANAGED_OBJECT_ID:
        if (invalidOids == null) {
          invalidOids = new ObjectIDSet();
        }
        this.invalidOids.add(new ObjectID(getLongValue()));
        return true;
      default:
        return false;
    }
  }

  public Set<ObjectID> getObjectIDsToInvalidate() {
    return this.invalidOids;
  }
}
