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

public class TransportRedirect extends TransportHandshakeException {
  private final String    hostname;
  private final TransportHandshakeError     errorType;
  private final int port;

  public TransportRedirect(String activeHost) {
    super(activeHost);
    int index = activeHost.lastIndexOf(':');
    hostname = activeHost.substring(0, index);
    port = Integer.parseInt(activeHost.substring(index + 1));
    this.errorType = TransportHandshakeError.ERROR_REDIRECT_CONNECTION;
  }

  public TransportHandshakeError getErrorType() {
    return errorType;
  }

  public int getPort() {
    return port;
  }

  public String getHostname() {
    return hostname;
  }

  @Override
  public String toString() {
    return hostname + ":" + port;
  }
  
  
}
