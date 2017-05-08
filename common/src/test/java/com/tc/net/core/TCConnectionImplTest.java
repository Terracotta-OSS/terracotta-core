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
package com.tc.net.core;

import com.tc.net.TCSocketAddress;
import com.tc.net.core.event.TCConnectionEvent;
import com.tc.net.core.event.TCConnectionEventListener;
import com.tc.net.core.security.TCSecurityManager;
import com.tc.net.protocol.TCProtocolAdaptor;
import com.tc.util.PortChooser;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;
import static junit.framework.TestCase.fail;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 */
public class TCConnectionImplTest {
  
  public TCConnectionImplTest() {
  }
  
  @BeforeClass
  public static void setUpClass() {
  }
  
  @AfterClass
  public static void tearDownClass() {
  }
  
  @Before
  public void setUp() {
  }
  
  @After
  public void tearDown() {
  }

  @Test
  public void testFinishConnection() throws Exception {
    int port = new PortChooser().chooseRandomPort();
    ServerSocket socket = new ServerSocket(port);
    final AtomicBoolean createManager = new AtomicBoolean(true);
    TCConnectionEventListener listener = mock(TCConnectionEventListener.class);
    TCProtocolAdaptor adaptor = mock(TCProtocolAdaptor.class);
    TCConnectionManagerImpl mgr = new TCConnectionManagerImpl();
    CoreNIOServices nioServiceThread = mock(CoreNIOServices.class);
    SocketParams socketParams = new SocketParams();
    TCSecurityManager securityManager = mock(TCSecurityManager.class);
    when(securityManager.getBufferManagerFactory()).thenReturn(new BufferManagerFactory() {
      @Override
      public BufferManager createBufferManager(SocketChannel socketChannel, boolean client) {
        if (createManager.get()) {
          return new ClearTextBufferManager(socketChannel);
        } else {
          return null;
        }
      }
    });
    TCConnection conn = new TCConnectionImpl(listener, adaptor, mgr, nioServiceThread, socketParams, securityManager);
    TCSocketAddress addr = new TCSocketAddress("localhost", port);
    conn.connect(addr, 0);
    verify(listener).connectEvent(any(TCConnectionEvent.class));
    
    conn = new TCConnectionImpl(listener, adaptor, mgr, nioServiceThread, socketParams, securityManager);
    createManager.set(false);
    
    try {
      conn.connect(addr, 0);
      fail();
    } catch (IOException ioe) {
  // expected;
    }
    
    socket.close();
  }

  
}
