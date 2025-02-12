/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
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
package com.tc.net.protocol.tcm;

import com.tc.net.core.ProductID;
import com.tc.net.protocol.transport.MessageTransportInitiator;
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
    
    ClientMessageChannelImpl instance = new ClientMessageChannelImpl(factory, router, ProductID.DIAGNOSTIC);
    instance.setMessageTransportInitiator(mock(MessageTransportInitiator.class));
    instance.close();
    instance.open(InetSocketAddress.createUnresolved("localhost", 9510));
  }
}
