package com.tc.entity;

import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.EntityID;

/**
 * @author twu
 */
public interface ServerEntityMessage extends TCMessage {
  void setMessage(EntityID entityID, byte[] payload);

  EntityID getEntityID();

  byte[] getMessage();
}
