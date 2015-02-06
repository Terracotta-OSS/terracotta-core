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
import com.tc.object.EntityID;
import com.tc.object.ObjectID;
import com.tc.object.session.SessionID;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class ObjectsNotFoundMessageImpl extends DSOMessageBase implements ObjectsNotFoundMessage {

  private final static byte BATCH_ID    = 0;
  private final static byte MISSING_EID = 1;

  private Set<EntityID> missingEids;
  private long              batchID;

  public ObjectsNotFoundMessageImpl(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out,
                                    MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public ObjectsNotFoundMessageImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                                    TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  @Override
  public void initialize(Set<EntityID> missingEntityIDs, long batchId) {
    this.missingEids = missingEntityIDs;
    this.batchID = batchId;
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(BATCH_ID, batchID);
    for (EntityID eid : missingEids) {
      putNVPair(MISSING_EID, eid.getClassName());
      getOutputStream().writeString(eid.getEntityName());
    }
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case BATCH_ID:
        this.batchID = getLongValue();
        return true;
      case MISSING_EID:
        if (missingEids == null) {
          missingEids = new HashSet<>();
        }
        this.missingEids.add(new EntityID(getStringValue(), getStringValue()));
        return true;
      default:
        return false;
    }
  }

  @Override
  public long getBatchID() {
    return batchID;
  }

  @Override
  public Set<EntityID> getMissingEntityIDs() {
    return missingEids;
  }

}
