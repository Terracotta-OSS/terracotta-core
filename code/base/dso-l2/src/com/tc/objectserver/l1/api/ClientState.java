/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.l1.api;

import java.util.Set;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.ObjectID;

public interface ClientState {

  public void addReference(ObjectID id);

  public boolean containsReference(ObjectID id);

  public void removeReferences(Set references);

  public void addReferencedIdsTo(Set ids);

  public ChannelID getClientID();

  public Set getReferences();

}