package com.tc.entity;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.EntityID;
import com.tc.object.msg.DSOMessageBase;
import com.tc.object.session.SessionID;

import java.io.IOException;
import java.util.Optional;

/**
 * @author twu
 */
public class ServerEntityMessageImpl extends DSOMessageBase implements ServerEntityMessage {
  private static final byte ID = 0;
  private static final byte MESSAGE = 1;
  private static final byte RESPONSE_ID = 2;

  private byte[] message;
  private EntityID entityID;
  private Optional<Long> responseId = Optional.empty();

  public ServerEntityMessageImpl(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out, MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public ServerEntityMessageImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel, TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  @Override
  public void setMessage(EntityID entityID, byte[] message) {
    this.entityID = entityID;
    this.message = message;
  }

  @Override
  public void setMessage(EntityID entityID, byte[] payload, long responseId) {
    this.entityID = entityID;
    this.message = payload;
    this.responseId = Optional.of(responseId);
  }

  @Override
  public Optional<Long> getResponseId() {
    return responseId;
  }

  @Override
  public EntityID getEntityID() {
    return entityID;
  }

  @Override
  public byte[] getMessage() {
    return message;
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(ID, entityID);
    responseId.ifPresent(id -> {
      putNVPair(RESPONSE_ID, id);
    });
    putNVPair(MESSAGE, message.length);
    getOutputStream().write(message);
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case ID:
        entityID = EntityID.readFrom(getInputStream());
        return true;
      case MESSAGE:
        message = getBytesArray();
        return true;
      case RESPONSE_ID:
        responseId = Optional.of(getLongValue());
        return true;
    }
    return false;
  }
}
