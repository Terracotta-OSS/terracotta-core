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

import java.net.InetSocketAddress;

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
   * @param serverAddress Contains host and port information where the connection was being established
   * @param e Exception that client got while establishing the connection
   */
  void onError(InetSocketAddress serverAddress, Exception e);
}
