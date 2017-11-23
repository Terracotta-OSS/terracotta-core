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
import com.tc.net.protocol.TCProtocolAdaptor;
import com.tc.util.PortChooser;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.atomic.AtomicBoolean;
import static junit.framework.TestCase.fail;
import org.junit.Test;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 *
 */
public class TCConnectionImplTest {
  
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
    TCConnection conn = new TCConnectionImpl(listener, adaptor, mgr, nioServiceThread, socketParams, new ClearTextBufferManagerFactory());
    TCSocketAddress addr = new TCSocketAddress("localhost", port);
    conn.connect(addr, 0);
    verify(listener).connectEvent(any(TCConnectionEvent.class));
    
    conn = new TCConnectionImpl(listener, adaptor, mgr, nioServiceThread, socketParams, mock(BufferManagerFactory.class));
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
