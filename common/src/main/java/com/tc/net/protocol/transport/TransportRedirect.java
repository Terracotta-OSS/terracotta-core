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

public class TransportRedirect extends TransportHandshakeException implements TransportHandshakeError {
  private final String    hostname;
  private final short     errorType;
  private final int port;

  public TransportRedirect(String activeHost) {
    super(activeHost);
    int index = activeHost.indexOf(':');
    hostname = activeHost.substring(0, index);
    port = Integer.parseInt(activeHost.substring(index + 1));          
    this.errorType = ERROR_REDIRECT_CONNECTION;
  }

  @Override
  public short getErrorType() {
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
