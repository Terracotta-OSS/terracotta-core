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
