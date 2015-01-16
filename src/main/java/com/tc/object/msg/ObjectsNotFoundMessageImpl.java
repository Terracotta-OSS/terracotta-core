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

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class ObjectsNotFoundMessageImpl extends DSOMessageBase implements ObjectsNotFoundMessage {

  private final static byte BATCH_ID    = 0;
  private final static byte MISSING_OID = 1;

  private Set<ObjectID>     missingOids;
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
  public void initialize(Set<ObjectID> missingObjectIDs, long batchId) {
    this.missingOids = missingObjectIDs;
    this.batchID = batchId;
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(BATCH_ID, batchID);
    for (ObjectID oid : missingOids) {
      putNVPair(MISSING_OID, oid.toLong());
    }
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case BATCH_ID:
        this.batchID = getLongValue();
        return true;
      case MISSING_OID:
        if (missingOids == null) {
          missingOids = new HashSet<ObjectID>();
        }
        this.missingOids.add(new ObjectID(getLongValue()));
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
  public Set<ObjectID> getMissingObjectIDs() {
    return missingOids;
  }

}
