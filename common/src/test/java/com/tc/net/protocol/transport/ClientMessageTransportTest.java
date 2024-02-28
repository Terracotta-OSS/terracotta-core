/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.net.protocol.transport;

import com.tc.bytes.TCReference;
import com.tc.bytes.TCReferenceSupport;
import com.tc.net.core.TCConnection;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.protocol.TCProtocolAdaptor;
import com.tc.net.protocol.TCProtocolException;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class ClientMessageTransportTest {

  private ClientMessageTransport  client;
  private TransportHandshakeMessage   message;
  private TransportMessageFactoryImpl factory;
  private WireProtocolAdaptorFactory wiref;
  private WireProtocolAdaptorImpl adapt;
  private WireProtocolMessage seed;

  @Before
  public void setUp() throws Exception {
    factory = new TransportMessageFactoryImpl();
    TCConnectionManager cce = mock(TCConnectionManager.class);
    TransportHandshakeErrorHandler eh = new TransportHandshakeErrorNullHandler();
    wiref = new WireProtocolAdaptorFactoryImpl() {
      @Override
      public TCProtocolAdaptor newWireProtocolAdaptor(WireProtocolMessageSink sink) {
        adapt = new WireProtocolAdaptorImpl(sink) {
          @Override
          public void addReadData(TCConnection source, TCReference data) throws TCProtocolException {
            sink.putMessage(seed);
            seed.complete();
          }
          
        };
        return adapt;
      }
      
    };
    client = new ClientMessageTransport(cce, eh, factory, wiref, 0);
  }

  @Test
  public void testSendAndReceive() throws Exception {
    TCConnection connection = mock(TCConnection.class);
    client.setConnection(connection);
    TCProtocolAdaptor proto = client.getProtocolAdapter();

    seed = spy(factory.createAck(ConnectionID.NULL_ID, connection));
    proto.addReadData(connection, TCReferenceSupport.createGCReference(Collections.emptyList()));
    verify(seed).complete();
    
  }
}
