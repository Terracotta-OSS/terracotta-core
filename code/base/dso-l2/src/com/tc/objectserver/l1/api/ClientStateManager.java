/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.l1.api;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.ObjectID;
import com.tc.objectserver.managedobject.BackReferences;
import com.tc.text.PrettyPrintable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author steve
 */
public interface ClientStateManager extends PrettyPrintable {

  public void stop();

  /**
   * Used to recover from client crashes this will acknowledge any waiter waiting for a downed client
   *
   * @param waitee
   */
  public void shutdownClient(ChannelID deadClient);

  /**
   * The the server representation of the client's state now knows that clientID has a reference to objectID
   */
  public void addReference(ChannelID clientID, ObjectID objectID);

  /**
   * For the local state of the l1 named clientID remove all the objectIDs that are references
   */
  public void removeReferences(ChannelID clientID, Set removed);

  public boolean hasReference(ChannelID clientID, ObjectID objectID);

  /**
   * Prunes the changes list down to include only changes for objects the given client has.
   * @param objectIDs TODO
   */
  public List createPrunedChangesAndAddObjectIDTo(Collection changes, BackReferences references, ChannelID clientID, Set objectIDs);
  
  public void addAllReferencedIdsTo(Set rescueIds);

  public void removeReferencedFrom(ChannelID channelID, Set secondPass);

  public Set addReferences(ChannelID channelID, Set oids);
}
