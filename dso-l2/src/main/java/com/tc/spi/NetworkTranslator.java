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
package com.tc.spi;

import java.net.InetSocketAddress;

/**
 */
public interface NetworkTranslator {
  /**
   * Translate a server host:port advertisement to something new.Used for 
 heterogeneous network environments where internal addresses can be translated to 
 external addresses.
   * 
   * @param srcOfRequest where the request is coming from 
   * @param serverHostPort the advertisement that will be sent
   * @return the address to advertise to the client
   */
  String redirectTo(InetSocketAddress srcOfRequest, String serverHostPort);
}
