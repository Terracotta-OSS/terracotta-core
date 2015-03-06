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
public class GetEntityResponseMessageImpl extends DSOMessageBase implements GetEntityResponseMessage {
  private static final byte ENTITY_ID = 0;
  private static final byte MISSING = 1;
  private static final byte CONFIG = 2;
  
  private EntityID entityID;
  private boolean missing = false;
  private Optional<byte[]> config = Optional.empty();
  
  public GetEntityResponseMessageImpl(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out, MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public GetEntityResponseMessageImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel, TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  @Override
  public void setMissing(EntityID id) {
    entityID = id;
    missing = true;
  }

  @Override
  public void setEntity(EntityID id, byte[] config) {
    this.entityID = id;
    this.config = Optional.of(config);
  }

  @Override
  public boolean isMissing() {
    return missing;
  }

  @Override
  public EntityID getEntityID() {
    return entityID;
  }

  @Override
  public Optional<byte[]> getConfig() {
    return config;
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(ENTITY_ID, entityID);
    putNVPair(MISSING, missing);
    config.ifPresent(bytes -> {
      putNVPair(CONFIG, bytes.length);
      getOutputStream().write(bytes);
    });
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case ENTITY_ID:
        entityID = EntityID.readFrom(getInputStream());
        break;
      case MISSING:
        missing = getBooleanValue();
        break;
      case CONFIG:
        config = Optional.of(getBytesArray());
        break;
      default:
        return false;
    }
    return true;
  }
}
