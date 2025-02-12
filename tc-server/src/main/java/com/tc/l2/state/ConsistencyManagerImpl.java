/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
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
package com.tc.l2.state;

import com.tc.l2.ha.WeightGeneratorFactory;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.groups.GroupEventsListener;
import com.tc.net.utils.L2Utils;
import com.tc.objectserver.impl.Topology;
import com.tc.objectserver.impl.TopologyManager;
import com.tc.util.Assert;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toSet;

public class ConsistencyManagerImpl implements ConsistencyManager, GroupEventsListener {
  
  private static final Logger CONSOLE = TCLogging.getConsoleLogger();
  private static final Logger LOGGER = LoggerFactory.getLogger(ConsistencyManagerImpl.class);
  private final TopologyManager topologyManager;
  private boolean activeVote = false;
  private boolean blocked = false;
  private Set<Transition> actions = EnumSet.noneOf(Transition.class);
  private long voteTerm = 1;
  private long blockedAt = Long.MAX_VALUE;
  private final ServerVoterManager voter;
  private final Set<NodeID> activePeers = Collections.synchronizedSet(new HashSet<>());
  private final Set<NodeID> passives = Collections.synchronizedSet(new HashSet<>());
  
  @Override
  public Map<String, ?> getStateMap() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("type", "Consistency");
    map.put("peerServers", this.topologyManager.getTopology().getServers().size() - 1);
    map.put("activeVote", activeVote);
    map.put("blocked", blocked);
    map.put("actions", new HashSet<>(actions).stream().map(Transition::toString).collect(Collectors.toList()));
    map.put("votingTerm", voteTerm);
    map.put("blockedAt", blockedAt);
    map.put("activePeers", new ArrayList<>(activePeers).stream().map(n->n.toString()).collect(Collectors.toList()));
    map.put("passives", new ArrayList<>(passives).stream().map(n->n.toString()).collect(Collectors.toList()));
    Map<String, Object> voteMap = new LinkedHashMap<>();
    voteMap.put("registered", voter.getRegisteredVoters());
    voteMap.put("count", voter.getVoteCount());
    voteMap.put("limit", this.topologyManager.getExternalVoters());
    voteMap.put("overridden", voter.overrideVoteReceived());
    map.put("voter", voteMap);
    return map;
  }
  
  public ConsistencyManagerImpl(Supplier<ServerMode> mode, TopologyManager topologyManager) {
    this.topologyManager = topologyManager;
    try {
      this.voter = new ServerVoterManagerImpl(mode, topologyManager::getExternalVoters);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  public synchronized long getBlockingTimestamp() {
    return blockedAt;
  }
        
  @Override
  public void nodeJoined(NodeID nodeID) {
    activePeers.add(nodeID);
  }

  @Override
  public void nodeLeft(NodeID nodeID) {
    activePeers.remove(nodeID);
  }
  
  @Override
  public long getCurrentTerm() {
    return voteTerm;
  }
  
  @Override
  public void setCurrentTerm(long term) {
    voteTerm = term;
  }
        
  @Override
  public boolean requestTransition(ServerMode mode, NodeID sourceNode, Topology topology, Transition newMode) throws IllegalStateException {
    if (topology == null) {
      topology = this.topologyManager.getTopology();
    }
    if (this.topologyManager.isAvailability()) {
      // availability mode
      return true;
    }

    if (newMode == Transition.ADD_PASSIVE) {
 //  starting passive sync to a new node, at this point the passive can be consisdered a
 //  vote for the current active and the passive sync rules will make sure all the data is replicated      
      passives.add(sourceNode);
      Assert.assertEquals(mode, ServerMode.ACTIVE);
      //  adding a passive to an active is always OK
      CONSOLE.info("Action:{} is always allowed", newMode);
      return true;
    }
    if (newMode == Transition.CONNECT_TO_ACTIVE) {
      endVoting(true, newMode, topology);
      CONSOLE.info("Action:{} is always allowed", newMode);
      return true;
    }
    if (newMode == Transition.REMOVE_PASSIVE) {
 //  try and remove the passive from the list of 
 //  votes for an active, need to check below if the 
 //  server can remove waiters for client transactions, can only do this
 //  with a quorum of votes
      passives.remove(sourceNode);
      Assert.assertEquals(mode, ServerMode.ACTIVE);
    }
    if (!newMode.isStateTransition()) {
      //  only reject a non-state transition if blocked on some other transition
      return !isBlocked();
    }
    boolean allow = false;
    
    // activate voting to lock the voting members and return the number of server votes
    int serverVotes = activateVoting(mode, newMode, topology);
    int peerServers = topology.getServers().size() - 1;

    int threshold = voteThreshold(mode, peerServers);
    if (serverVotes >= threshold || serverVotes == peerServers) {
    // if the threshold is achieved with just servers or all the servers are visible, transition is granted
      CONSOLE.info("Action:{} allowed because enough servers are connected", newMode);
      endVoting(true, newMode, topology);
      return true;
    }
    if (voter.overrideVoteReceived()) {
      CONSOLE.info("Action:{} allowed because override received", newMode);
      endVoting(true, newMode, topology);
      return true;
    }

    long start = System.currentTimeMillis();
    try {
      if (voter.getRegisteredVoters() + serverVotes < threshold) {
        CONSOLE.warn("Not enough registered voters.  Require override intervention or {} members of the stripe to be connected for action {}", peerServers + 1 > threshold ? threshold : "all", newMode);
      } else while (!allow && System.currentTimeMillis() - start < ServerVoterManagerImpl.VOTEBEAT_TIMEOUT) {
        try {
          //  servers connected + votes received
          if (serverVotes + voter.getVoteCount() < threshold) {
            TimeUnit.MILLISECONDS.sleep(100);
          } else {
            allow = true;
          }
        } catch (InterruptedException ie) {
          L2Utils.handleInterrupted(LOGGER, ie);
        }
      }
    } finally {
      CONSOLE.info("Action:{} granted:{} vote tally servers:{} external:{} of total:{}", newMode, allow, serverVotes + 1, voter.getVoteCount(), peerServers + topologyManager.getExternalVoters() + 1);
      endVoting(allow, newMode, topology);
    }
    return allow;
  }

  @Override
  public boolean lastTransitionSuspended() {
    return blocked;
  }

  @Override
  public void allowLastTransition() {
    this.voter.overrideVote("external");
  }

  @Override
  public Collection<Transition> requestedActions() {
    return Collections.unmodifiableSet(actions);
  }

  private int voteThreshold(ServerMode mode, int peerServers) {
    //  peer servers plus extra votes plus self is the total votes available
    int voteCount = peerServers + this.topologyManager.getExternalVoters() + 1;
    if (mode == ServerMode.ACTIVE) {
      // only half the votes needed to continue as active
      return voteCount - (voteCount>>1) - 1; // ceiling of half minus the vote for self
    } else {
      // need more than half to promote to active
      return (voteCount>>1);  // floor of half because the self vote will tip the scales
    }
  }
  
  private synchronized int activateVoting(ServerMode mode, Transition moveTo, Topology topology) {
    if (!activeVote) {
      blocked = true;
      blockedAt = System.currentTimeMillis();
      activeVote = true;
      boolean stateTransition = moveTo.isStateTransition();
      if (stateTransition) {
        voteTerm += 1;
      }
      voter.startVoting(voteTerm, stateTransition);
    }
    actions.add(moveTo);
    //  for zapping, only need to count the servers connected since they are 
    //  presumably participating in election
    if (mode != ServerMode.ACTIVE || moveTo == Transition.ZAP_NODE) {
      return filterActivePeers(activePeers, topology).size();
    } else {
      return this.passives.size();
    }
  }
  
  private void promotePeers(Set<NodeID> activePeers) {
    passives.addAll(activePeers);
  }

  private static Set<NodeID> filterActivePeers(Set<NodeID> activePeers, Topology topology) {
    return activePeers.stream().filter(p -> topology.getServers().contains(((ServerID)p).getName())).collect(toSet());
  }
  
  @SuppressWarnings("fallthrough")
  private synchronized void endVoting(boolean allowed, Transition moveTo, Topology topology) {
    Assert.assertTrue(moveTo.isStateTransition());
    if (activeVote) {
      if (allowed) {
        switch(moveTo) {
          case MOVE_TO_ACTIVE:
            promotePeers(filterActivePeers(activePeers, topology));
            //  fallthrough is expected here
          case CONNECT_TO_ACTIVE:
            actions.remove(Transition.CONNECT_TO_ACTIVE);
            actions.remove(Transition.MOVE_TO_ACTIVE);
            break;
          default:
            actions.remove(moveTo);
        }
        if (actions.isEmpty()) {
          Assert.assertEquals(voteTerm, voter.stopVoting());
          activeVote = false;
          blocked = false;
          blockedAt = Long.MAX_VALUE;
        }
      }
    }
  }
  
  public synchronized Collection<Transition> getActions() {
    return new ArrayList<>(actions);
  }
   
  public synchronized boolean isVoting() {
    return activeVote;
  }
  
  public synchronized boolean isBlocked() {
    return blocked;
  }
  
  @Override
  public Enrollment createVerificationEnrollment(NodeID lastActive, WeightGeneratorFactory weightFactory) {
    return EnrollmentFactory.createVerificationEnrollment(lastActive, weightFactory);
  }
}
