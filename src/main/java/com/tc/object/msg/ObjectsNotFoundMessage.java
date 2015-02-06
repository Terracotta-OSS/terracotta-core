/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.EntityID;

import java.util.Set;

public interface ObjectsNotFoundMessage extends TCMessage {

  public void initialize(Set<EntityID> missingEntityIDs, long batchId);

  public long getBatchID();

  public Set<EntityID> getMissingEntityIDs();

}
