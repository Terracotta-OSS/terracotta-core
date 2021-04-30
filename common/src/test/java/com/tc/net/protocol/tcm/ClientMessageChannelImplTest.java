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
package com.tc.net.protocol.tcm;

import com.tc.net.core.ProductID;
import com.tc.net.protocol.transport.MessageTransportInitiator;
import com.tc.object.session.SessionProvider;
import java.io.IOException;
import java.net.InetSocketAddress;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.mock;

/**
 *
 */
public class ClientMessageChannelImplTest {

  public ClientMessageChannelImplTest() {
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  /**
   * Test channel closed before opened
   */
  @Test(expected = IOException.class)
  public void testCloseThenOpen() throws Exception {
    System.out.println("setMessageTransportInitiator");
    TCMessageFactory factory = mock(TCMessageFactory.class);
    TCMessageRouter router = mock(TCMessageRouter.class);
    SessionProvider provider = mock(SessionProvider.class);
    
    ClientMessageChannelImpl instance = new ClientMessageChannelImpl(factory, router, provider, ProductID.DIAGNOSTIC);
    instance.setMessageTransportInitiator(mock(MessageTransportInitiator.class));
    instance.close();
    instance.open(InetSocketAddress.createUnresolved("localhost", 9510));
  }
}
