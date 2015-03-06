package com.tc.entity;

import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.EntityID;

import java.util.Optional;

/**
 * @author twu
 */
public interface GetEntityResponseMessage extends TCMessage {

  void setMissing(EntityID id);

  void setEntity(EntityID id, byte[] config);
  
  boolean isMissing();
  
  EntityID getEntityID();
  
  Optional<byte[]> getConfig();
}
