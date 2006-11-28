/*
 * Created on May 3, 2005 TODO To change the template for this generated file go to Window - Preferences - Java - Code
 * Style - Code Templates
 */
package com.tc.objectserver.persistence.api;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.transport.ConnectionIdFactory;
import com.tc.objectserver.l1.api.ClientState;
import com.tc.objectserver.persistence.impl.ClientNotFoundException;

import java.util.Iterator;
import java.util.Set;

public interface ClientStatePersistor {

  public ConnectionIdFactory getConnectionIDFactory();

  public Iterator loadClientIDs();

  public Set loadConnectionIDs();

  public boolean containsClient(ChannelID id);

  public void saveClientState(ClientState clientState);

  public void deleteClientState(ChannelID id) throws ClientNotFoundException;
}
