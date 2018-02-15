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

import com.tc.net.core.ConnectionInfo;

/**
 * Any class implementing this interface can take appropriate action when a connection is not successfully established 
 * between client and server.
 *
 * An instance of this interface can be passed to transport (lower) layer and on getting an exception, {@code onError} 
 * can be invoked to store the exception against the connection information. Stored exceptions can be passed to upper
 * layer for inspection and analysis.
 */
public interface ClientConnectionErrorListener {

  /**
   * 
   * @param connInfo Contains host and port information where the connection was being established
   * @param e Exception that client got while establishing the connection
   */
  void onError(ConnectionInfo connInfo, Exception e);
}
