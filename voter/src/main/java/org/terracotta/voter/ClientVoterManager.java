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
package org.terracotta.voter;

import com.tc.voter.VoterManager;

import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeoutException;

public interface ClientVoterManager extends VoterManager {

  /**
   * The host and port information of the server that this client is connected to.
   *
   * @return host and port of the server separated by a ":"
   */
  String getTargetHostPort();

  /**
   * Establish a connection with the server at the given host and port
   */
  void connect(Optional<Properties> connectionProps);

  /**
   *
   * @return the current state of the server that this voter is connected to.
   */
  String getServerState() throws TimeoutException;

  /**
   *
   * @return the configuration of the server that this voter is connected to.
   * @throws java.util.concurrent.TimeoutException
   */
  String getServerConfig() throws TimeoutException;

  Set<String> getTopology() throws TimeoutException;

  /**
   * Close the connection with the server.
   */
  void close();
  
  boolean isVoting();
  
  void zombie();
  
  boolean isConnected();

  /**
   * @return the number of voters that have been registered with the server.
   */
  long getRegisteredVoterCount() throws TimeoutException;

  /**
   * @return the maximum number of voters that can be registered with the server.
   */
  long getRegisteredVoterLimit() throws TimeoutException;
}
