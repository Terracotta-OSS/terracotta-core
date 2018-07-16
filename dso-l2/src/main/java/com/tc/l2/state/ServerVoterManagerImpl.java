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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ServerVoterManagerImpl extends AbstractTerracottaMBean implements ServerVoterManager {

  private final static Logger logger = LoggerFactory.getLogger(ServerVoterManagerImpl.class);

  static final long VOTEBEAT_TIMEOUT = 5000;  // In milliseconds

  private final int voterLimit;
  final Map<String, Long> voters = new ConcurrentHashMap<>();
  private final TimeSource timeSource;

  private volatile boolean votingInProgress = false;
  private volatile long electionTerm;
  private final Set<String> votes = ConcurrentHashMap.newKeySet();

  private volatile boolean overrideVote = false;

  public ServerVoterManagerImpl(int voterLimit) throws Exception {
    this(voterLimit, TimeSource.SYSTEM_TIME_SOURCE, true);
  }

  ServerVoterManagerImpl(int voterLimit, TimeSource timeSource, boolean initMBean) throws Exception {
    super(ServerVoterManager.class, false);
    if (initMBean) {
      try {
        ManagementFactory.getPlatformMBeanServer().registerMBean(this,
          TerracottaManagement.createObjectName(null, MBEAN_NAME, TerracottaManagement.MBeanDomain.PUBLIC));
      } catch (Exception e) {
        logger.warn("problem registering MBean", e);
      }
    }
    this.voterLimit = voterLimit;
    this.timeSource = timeSource;
    this.electionTerm = 0;
  }

  @Override
  public int getVoterLimit() {
    return voterLimit;
  }

  @Override
  public synchronized long registerVoter(String id) {
    if (voters.containsKey(id)) {
      //  already registered.  double register is not supported
      return HEARTBEAT_RESPONSE;
    }

    if (!canAcceptVoter()) {
      logger.info("Voter id: " + id + " could not be registered as there is no voter vacancy available");
      return INVALID_VOTER_RESPONSE;
    }

    voters.put(id, timeSource.currentTimeMillis());
    logger.info("Registration of voter id: " + id + " confirmed.");
    return electionTerm;
  }

  boolean canAcceptVoter() {
    return !votingInProgress && getRegisteredVoters() < voterLimit;
  }

  @Override
  public long heartbeat(String id) {
    Long val = voters.computeIfPresent(id, (key, timeStamp) -> {
      long currentTime = timeSource.currentTimeMillis();
      // make sure some crazy time lapse didn't happen since last heartbeat
      if (currentTime - timeStamp < VOTEBEAT_TIMEOUT) {
        return currentTime;
      } else {
        votes.remove(key);
        voters.remove(key);
        return null;
      }
    });
    
    if (val == null) {
      return INVALID_VOTER_RESPONSE;
    }

    if (votingInProgress) {
      return electionTerm;
    }

    return HEARTBEAT_RESPONSE;
  }

  @Override
  public void startVoting(long electionTerm) {
    this.electionTerm = electionTerm;
    votes.clear();
    overrideVote = false;
    votingInProgress = true;
  }

  @Override
  public long vote(String id, long electionTerm) {
    long response = heartbeat(id);
    if (response > 0 && electionTerm == this.electionTerm) {
      votes.add(id);
      return HEARTBEAT_RESPONSE;
    } else {
      return response;
    }
  }

  public long vote(String idTerm) {
    String[] split = idTerm.split(":");
    return vote(split[0], Long.parseLong(split[1]));
  }
  
  @Override
  public int getRegisteredVoters() {
    return (int)voters.entrySet().stream()
        .filter((entry) -> {
          if (timeSource.currentTimeMillis() - entry.getValue() < VOTEBEAT_TIMEOUT) {
            return true;
          } else {
            String id = entry.getKey();
            votes.remove(id);
            voters.remove(id);
            return false;
          }
        })
        .count();
    }

  @Override
  public int getVoteCount() {
    if (overrideVote) {
      return voterLimit;
    }
    return votes.size();
  }

  @Override
  public boolean overrideVote(String id) {
    if (votingInProgress) {
      logger.info("Override vote received from {}", id);
      this.overrideVote = true;
      return true;
    } else {
      logger.info("Override vote from {} ignored as the server is not in the middle of an election", id);
      return false;
    }
  }

  @Override
  public boolean overrideVoteReceived() {
    return this.overrideVote;
  }

  @Override
  public long stopVoting() {
    votingInProgress = false;
    this.overrideVote = false;
    return this.electionTerm;
  }

  @Override
  public boolean deregisterVoter(String id) {
    System.out.println("Deregister " + id);
    return !votingInProgress ? voters.remove(id) != null : false;
  }

  @Override
  public void reset() {
    //
  }

}
