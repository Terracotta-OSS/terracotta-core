/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ObjectID;
import com.tc.object.session.SessionID;
import com.tc.util.BasicObjectIDSet;
import com.tc.util.ObjectIDSet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class InvalidateObjectsMessage extends DSOMessageBase {

  private static final byte INVALIDATIONS = 0;

  private Map<ObjectID, ObjectIDSet> invalidations;

  public InvalidateObjectsMessage(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                                  TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  public InvalidateObjectsMessage(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out,
                                  MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public void initialize(Map<ObjectID, ObjectIDSet> invalids) {
    this.invalidations = invalids;
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(INVALIDATIONS, new InvalidationSerializer(invalidations));
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case INVALIDATIONS:
        invalidations = (Map<ObjectID, ObjectIDSet>) getObject(new InvalidationSerializer());
        return true;
      default:
        return false;
    }
  }

  public Map<ObjectID, ObjectIDSet> getObjectIDsToInvalidate() {
    return this.invalidations;
  }

  /**
   * @author tim
   */
  private static class InvalidationSerializer extends MapSerializer<ObjectID, ObjectIDSet> {
    public InvalidationSerializer() {
      this(new HashMap<ObjectID, ObjectIDSet>());
    }

    private InvalidationSerializer(final Map<ObjectID, ObjectIDSet> map) {
      super(map);
    }

    @Override
    protected void serializeKey(final ObjectID key, final TCByteBufferOutput serialOutput) {
      serialOutput.writeLong(key.toLong());
    }

    @Override
    protected void serializeValue(final ObjectIDSet value, final TCByteBufferOutput serialOutput) {
      value.serializeTo(serialOutput);
    }

    @Override
    protected ObjectID deserializeKey(final TCByteBufferInput serialInput) throws IOException {
      return new ObjectID(serialInput.readLong());
    }

    @Override
    protected ObjectIDSet deserializeValue(final TCByteBufferInput serialInput) throws IOException {
      return (ObjectIDSet) new BasicObjectIDSet().deserializeFrom(serialInput);
    }
  }
}
