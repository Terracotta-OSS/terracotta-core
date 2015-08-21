package com.tc.entity;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.EntityDescriptor;
import com.tc.object.msg.DSOMessageBase;
import com.tc.object.session.SessionID;

import java.io.IOException;
import java.util.Optional;

/**
 * @author twu
 */
public class ServerEntityMessageImpl extends DSOMessageBase implements ServerEntityMessage {
  private static final byte ENTITY_DESCRIPTOR = 0;
  private static final byte MESSAGE = 1;
  private static final byte RESPONSE_ID = 2;

  private byte[] message;
  private EntityDescriptor entityDescriptor;
  private Optional<Long> responseId = Optional.empty();

  public ServerEntityMessageImpl(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out, MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public ServerEntityMessageImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel, TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  @Override
  public void setMessage(EntityDescriptor entityDescriptor, byte[] message) {
    this.entityDescriptor = entityDescriptor;
    this.message = message;
  }

  @Override
  public void setMessage(EntityDescriptor entityDescriptor, byte[] payload, long responseId) {
    this.entityDescriptor = entityDescriptor;
    this.message = payload;
    this.responseId = Optional.of(responseId);
  }

  @Override
  public Optional<Long> getResponseId() {
    return responseId;
  }

  @Override
  public EntityDescriptor getEntityDescriptor() {
    return this.entityDescriptor;
  }

  @Override
  public byte[] getMessage() {
    return message;
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(ENTITY_DESCRIPTOR, this.entityDescriptor);
    responseId.ifPresent(id -> {
      putNVPair(RESPONSE_ID, id);
    });
    putNVPair(MESSAGE, message.length);
    getOutputStream().write(message);
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    boolean didMatch = false;
    switch (name) {
      case ENTITY_DESCRIPTOR:
        this.entityDescriptor = EntityDescriptor.readFrom(getInputStream());
        didMatch = true;
        break;
      case MESSAGE:
        message = getBytesArray();
        didMatch = true;
        break;
      case RESPONSE_ID:
        responseId = Optional.of(getLongValue());
        didMatch = true;
        break;
      default:
        // This must be malformed data so fail.
        didMatch = false;
    }
    return didMatch;
  }
}
