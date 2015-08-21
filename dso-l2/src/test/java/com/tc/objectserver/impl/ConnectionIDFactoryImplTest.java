package com.tc.objectserver.impl;

import com.tc.exception.TCRuntimeException;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.objectserver.persistence.ClientStatePersistor;
import com.tc.test.TCTestCase;
import com.tc.util.sequence.MutableSequence;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author tim
 */
public class ConnectionIDFactoryImplTest extends TCTestCase {
  private ConnectionIDFactoryImpl connectionIDFactory;
  private ClientStatePersistor persistor;
  private MutableSequence sequence;

  @Override
  public void setUp() throws Exception {
    sequence = createSequence();
    persistor = createPersistor(sequence);
    connectionIDFactory = new ConnectionIDFactoryImpl(persistor);
  }

  public void testNextID() throws Exception {
    uid("abc123");
    nextChannelId(0);
    ConnectionID id = connectionIDFactory.populateConnectionID(idWith("Mr. Bogus", -1L));
    assertEquals("Mr. Bogus", id.getJvmID());
    assertEquals(0, id.getChannelID());
    assertEquals("abc123", id.getServerID());
  }

  public void testCreateExistingID() throws Exception {
    setContainsId(0, true);
    try {
      connectionIDFactory.populateConnectionID(idWith("aaa", 0));
      fail();
    } catch (TCRuntimeException e) {
      // expected
    }
  }

  public void testRemoveId() throws Exception {
    connectionIDFactory.channelRemoved(channelWithId(0));
    verify(persistor).deleteClientState(new ChannelID(0));
  }

  private void uid(String uid) {
    when(sequence.getUID()).thenReturn(uid);
  }

  private void nextChannelId(long id) {
    when(sequence.next()).thenReturn(id);
  }

  private void setContainsId(long id, boolean contains) {
    when(persistor.containsClient(new ChannelID(id))).thenReturn(contains);
  }

  private static MutableSequence createSequence() {
    MutableSequence sequence = mock(MutableSequence.class);
    when(sequence.getUID()).thenReturn("abc123");
    return sequence;
  }

  private static ClientStatePersistor createPersistor(MutableSequence sequence) {
    ClientStatePersistor clientStatePersistor = mock(ClientStatePersistor.class);
    when(clientStatePersistor.getConnectionIDSequence()).thenReturn(sequence);
    return clientStatePersistor;
  }

  private static MessageChannel channelWithId(long id) {
    return when(mock(MessageChannel.class).getChannelID()).thenReturn(new ChannelID(id)).getMock();
  }

  private static ConnectionID idWith(String jvmId, long channelId) {
    return new ConnectionID(jvmId, channelId);
  }
}
