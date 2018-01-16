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

import com.tc.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.*;

public class TCVoterImpl implements TCVoter {

  private static final Logger LOGGER = LoggerFactory.getLogger(TCVoterImpl.class);

  private final String id;
  private final Map<String, VoterDaemon> registeredClusters = new ConcurrentHashMap<>();

  public TCVoterImpl() {
    this.id = UUID.getUUID().toString();
    LOGGER.info("Voter ID: {}", id);
  }

  @Override
  public boolean vetoVote(String hostPort) {
    ClientVoterManager voterManager = new ClientVoterManagerImpl(hostPort);
    voterManager.connect();
    String id = UUID.getUUID().toString();
    boolean veto = false;
    try {
      veto = voterManager.vetoVote(id);
    } catch (TimeoutException e) {
      LOGGER.error("Veto vote to {} timed-out", hostPort);
      return false;
    }

    if (veto) {
      LOGGER.info("Successfully cast a veto vote to {}", hostPort);
    } else {
      LOGGER.info("Veto vote rejected by {}", hostPort);
    }
    return veto;
  }

  @Override
  public void register(String clusterName, String... hostPorts) {
    if (registeredClusters.putIfAbsent(clusterName, new VoterDaemon(this.id, hostPorts)) != null) {
      throw new RuntimeException("Another cluster is already registered with the name: " + clusterName);
    }
  }

  @Override
  public void deregister(String clusterName) {
    VoterDaemon removed = registeredClusters.remove(clusterName);
    if (removed != null) {
      removed.stop();
    } else {
      throw new RuntimeException("A cluster with the given name: " + clusterName + " is not registered with this voter");
    }
  }

}
