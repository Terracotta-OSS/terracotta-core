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

/**
 * A Context per Transport takes care of sending and receiving the health checking probe signals to peer nodes. Also the
 * extra checks like socket connect to detect Long GC.
 *
 * @author Manoj
 */
public interface ConnectionHealthCheckerContext {

  /* Transport is lively */
  void refresh();

  /* Probe Message send and receive */
  boolean probeIfAlive();

  boolean receiveProbe(HealthCheckerProbeMessage message);

  void checkTime();
  
  void close();
}
