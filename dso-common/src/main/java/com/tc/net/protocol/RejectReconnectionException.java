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
package com.tc.net.protocol;

import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.transport.ConnectionID;

/**
 * Thrown by Stack providers when reconnection attempt is rejected (meaning subsequent reconnects will also be rejected)
 */
public class RejectReconnectionException extends Exception {

  public RejectReconnectionException(String reason, TCSocketAddress socketAddress) {
    super("Connection attempts from the Terracotta node at " + socketAddress
          + " are being rejected by the Terracotta server array. Reason: " + reason);
  }
}
