/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
