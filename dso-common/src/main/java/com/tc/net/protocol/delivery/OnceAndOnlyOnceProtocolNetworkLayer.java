/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.net.protocol.delivery;

import com.tc.net.protocol.NetworkLayer;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.protocol.transport.MessageTransportListener;

import java.util.Timer;

/**
 * This is not a very interesting interface. It's here to allow testing of the once and only once network stack harness
 * with mock objects. The stack harness needs to treat the OOOP network layer as both a network layer and a transport
 * listener, hence this interface which combines the two.
 */
public interface OnceAndOnlyOnceProtocolNetworkLayer extends NetworkLayer, MessageTransport, MessageTransportListener {

  void startRestoringConnection();

  void connectionRestoreFailed();

  Timer getRestoreConnectTimer();

  boolean isClosed();

}
