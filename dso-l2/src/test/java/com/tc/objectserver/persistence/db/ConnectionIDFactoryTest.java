/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.objectserver.persistence.api.ClientStatePersistor;
import com.tc.objectserver.persistence.inmemory.InMemoryClientStatePersistor;
import com.tc.test.TCTestCase;

public class ConnectionIDFactoryTest extends TCTestCase {

  private ClientStatePersistor    clientStatePersistor;
  private ConnectionIDFactoryImpl idFactory;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    clientStatePersistor = new InMemoryClientStatePersistor();
    idFactory = new ConnectionIDFactoryImpl(clientStatePersistor);
  }

  public void testNextConnectionID() {
    ConnectionID cid = idFactory.nextConnectionId("jvm1");
    assertTrue(clientStatePersistor.containsClient(new ChannelID(cid.getChannelID())));
    assertEquals(cid.getChannelID(), clientStatePersistor.getConnectionIDSequence().current());

    cid = idFactory.nextConnectionId("jvm1");
    assertTrue(clientStatePersistor.containsClient(new ChannelID(cid.getChannelID())));
    assertEquals(cid.getChannelID(), clientStatePersistor.getConnectionIDSequence().current());

    cid = idFactory.nextConnectionId("jvm1");
    assertTrue(clientStatePersistor.containsClient(new ChannelID(cid.getChannelID())));
    assertEquals(cid.getChannelID(), clientStatePersistor.getConnectionIDSequence().current());

    assertEquals(3, idFactory.loadConnectionIDs().size());
  }

  public void testMakeConnectionId() {
    clientStatePersistor.getConnectionIDSequence().next();
    ConnectionID cid = idFactory.makeConnectionId("jvm1", clientStatePersistor.getConnectionIDSequence().current());
    assertTrue(clientStatePersistor.containsClient(new ChannelID(cid.getChannelID())));
    assertEquals(cid.getChannelID(), clientStatePersistor.getConnectionIDSequence().current());

    clientStatePersistor.getConnectionIDSequence().next();
    cid = idFactory.makeConnectionId("jvm1", clientStatePersistor.getConnectionIDSequence().current());
    assertTrue(clientStatePersistor.containsClient(new ChannelID(cid.getChannelID())));
    assertEquals(cid.getChannelID(), clientStatePersistor.getConnectionIDSequence().current());

    clientStatePersistor.getConnectionIDSequence().next();
    cid = idFactory.makeConnectionId("jvm1", clientStatePersistor.getConnectionIDSequence().current());
    assertTrue(clientStatePersistor.containsClient(new ChannelID(cid.getChannelID())));
    assertEquals(cid.getChannelID(), clientStatePersistor.getConnectionIDSequence().current());

    assertEquals(3, idFactory.loadConnectionIDs().size());
  }
}
