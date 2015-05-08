package com.tc.entity;

import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.EntityDescriptor;
import java.util.Optional;

/**
 * @author twu
 */
public interface GetEntityResponseMessage extends TCMessage {

  void setMissing(EntityDescriptor entityDescriptor);

  void setEntity(EntityDescriptor entityDescriptor, byte[] config);
  
  boolean isMissing();
  
  EntityDescriptor getEntityDescriptor();
  
  Optional<byte[]> getConfig();
}
