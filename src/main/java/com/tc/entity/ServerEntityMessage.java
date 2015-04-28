package com.tc.entity;

import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.EntityID;

import java.util.Optional;

/**
 * @author twu
 */
public interface ServerEntityMessage extends TCMessage {

  void setMessage(EntityID entityID, byte[] payload);

  void setMessage(EntityID entityID, byte[] payload, long responseId);


  EntityID getEntityID();

  byte[] getMessage();

  Optional<Long> getResponseId();
}
