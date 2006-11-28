/*
 * Created on May 3, 2005 TODO To change the template for this generated file go to Window - Preferences - Java - Code
 * Style - Code Templates
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