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
package com.tc.l2.state;

import com.tc.management.AbstractTerracottaMBean;
import com.tc.management.TerracottaManagement;
import com.tc.services.TimeSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ServerVoterManagerImpl extends AbstractTerracottaMBean implements ServerVoterManager {

  private final static Logger logger = LoggerFactory.getLogger(ServerVoterManagerImpl.class);

  public static final String MBEAN_NAME = "VoterManager";
  static final long VOTEBEAT_TIMEOUT = 5000;  // In milliseconds

  private final int voterLimit;
  final Map<String, Long> voters = new ConcurrentHashMap<>();
  final Set<String> candidates = new HashSet<>();
  private final TimeSource timeSource;

  private volatile boolean electionInProgress = false;
  private volatile long electionTerm;
  private final Set<String> votes = ConcurrentHashMap.newKeySet();

  public ServerVoterManagerImpl(int voterLimit) throws Exception {
    this(voterLimit, TimeSource.SYSTEM_TIME_SOURCE);
  }

  ServerVoterManagerImpl(int voterLimit, TimeSource timeSource) throws Exception {
    super(ServerVoterManager.class, false);
    ManagementFactory.getPlatformMBeanServer().registerMBean(this,
        TerracottaManagement.createObjectName(null, MBEAN_NAME, TerracottaManagement.MBeanDomain.PUBLIC));
    this.voterLimit = voterLimit;
    this.timeSource = timeSource;
  }

  @Override
  public int getVoterLimit() {
    return voterLimit;
  }

  @Override
  public synchronized boolean registerVoter(String id) {
    if (!canAcceptVoter()) {
      logger.info("Voter id: " + id + " could not be registered as there is no voter vacancy available");
      return false;
    }

    candidates.add(id);
    logger.info("Voter interest registered for voter id: " + id);
    return true;
  }

  boolean canAcceptVoter() {
    return voters.entrySet().stream()
        .filter((entry) -> {
          if (timeSource.currentTimeMillis() - entry.getValue() < VOTEBEAT_TIMEOUT) {
            return true;
          }
          voters.remove(entry.getKey());
          return false;
        })
        .count() < voterLimit;
  }

  @Override
  public synchronized boolean confirmVoter(String id) {
    if (!canAcceptVoter()) {
      logger.info("Registration of voter id: " + id + " could not be confirmed as there is no voter vacancy available.");
    }

    if (candidates.contains(id)) {
      voters.put(id, timeSource.currentTimeMillis());
      logger.info("Registration of voter id: " + id + " confirmed.");
      candidates.remove(id);
      return true;
    } else {
      logger.info("Registration of voter id: " + id + " could not be confirmed as it did not register before attempting to confirm.");
      return false;
    }
  }

  @Override
  public long heartbeat(String id) {
    Long val = voters.computeIfPresent(id, (key, timeStamp) -> timeSource.currentTimeMillis());
    if (val == null) {
      return INVALID_VOTER_RESPONSE;
    }

    if (electionInProgress) {
      return electionTerm;
    }

    return HEARTBEAT_RESPONSE;
  }

  @Override
  public void startElection(long electionTerm) {
    this.electionTerm = electionTerm;
    votes.clear();
    electionInProgress = true;
  }

  @Override
  public long vote(String id, long electionTerm) {
    if (electionTerm == this.electionTerm) {
      votes.add(id);
    }
    return heartbeat(id);
  }

  @Override
  public int getVoteCount() {
    return votes.size();
  }

  @Override
  public void endElection() {
    electionInProgress = false;
  }

  @Override
  public boolean reconnect(String id) {
    if (voters.containsKey(id)) {
      return true;
    }

    if (canAcceptVoter()) {
      voters.put(id, timeSource.currentTimeMillis());
      return true;
    }

    return false;
  }

  @Override
  public boolean deregisterVoter(String id) {
    System.out.println("Deregister " + id);
    return voters.remove(id) != null;
  }

  @Override
  public void reset() {
    //
  }

}
