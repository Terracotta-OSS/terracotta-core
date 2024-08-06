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
 * The main interface for Connection HealthChecker. When CommunicationsManager are built, a HealthChecker is tied to it
 * to monitor all the connections it establishes. By default, an ECHO health checker is tied to all the communications
 * manager to respond to the peer probe signals if it ever receive any.
 * 
 * @author Manoj
 */
public interface ConnectionHealthChecker extends MessageTransportListener {

  public void start();

  public void stop();

}
