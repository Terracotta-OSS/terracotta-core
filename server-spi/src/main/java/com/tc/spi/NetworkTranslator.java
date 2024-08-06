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
