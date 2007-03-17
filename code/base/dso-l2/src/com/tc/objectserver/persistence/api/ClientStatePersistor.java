/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.api;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.objectserver.persistence.impl.ClientNotFoundException;

import java.util.Set;

public interface ClientStatePersistor {

  public PersistentSequence getConnectionIDSequence();

  public Set loadClientIDs();

  public boolean containsClient(ChannelID id);

  public void saveClientState(ChannelID channelID);

  public void deleteClientState(ChannelID id) throws ClientNotFoundException;
}
