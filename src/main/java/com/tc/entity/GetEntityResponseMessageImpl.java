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
public class GetEntityResponseMessageImpl extends DSOMessageBase implements GetEntityResponseMessage {
  private static final byte ENTITY_DESCRIPTOR = 0;
  private static final byte MISSING = 1;
  private static final byte CONFIG = 2;
  
  private EntityDescriptor entityDescriptor;
  private boolean missing = false;
  private Optional<byte[]> config = Optional.empty();
  
  public GetEntityResponseMessageImpl(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out, MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public GetEntityResponseMessageImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel, TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  @Override
  public void setMissing(EntityDescriptor entityDescriptor) {
    this.entityDescriptor = entityDescriptor;
    this.missing = true;
  }

  @Override
  public void setEntity(EntityDescriptor entityDescriptor, byte[] config) {
    this.entityDescriptor = entityDescriptor;
    this.config = Optional.of(config);
  }

  @Override
  public boolean isMissing() {
    return missing;
  }

  @Override
  public EntityDescriptor getEntityDescriptor() {
    return this.entityDescriptor;
  }

  @Override
  public Optional<byte[]> getConfig() {
    return config;
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(ENTITY_DESCRIPTOR, this.entityDescriptor);
    putNVPair(MISSING, missing);
    config.ifPresent(bytes -> {
      putNVPair(CONFIG, bytes.length);
      getOutputStream().write(bytes);
    });
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    boolean didMatch = false;
    switch (name) {
      case ENTITY_DESCRIPTOR:
        this.entityDescriptor = EntityDescriptor.readFrom(getInputStream());
        didMatch = true;
        break;
      case MISSING:
        missing = getBooleanValue();
        didMatch = true;
        break;
      case CONFIG:
        config = Optional.of(getBytesArray());
        didMatch = true;
        break;
      default:
        // This must be malformed data so fail.
        didMatch = false;
    }
    return didMatch;
  }
}
