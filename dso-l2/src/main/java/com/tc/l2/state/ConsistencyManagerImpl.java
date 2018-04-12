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

import com.tc.logging.TCLogging;
import com.tc.management.AbstractTerracottaMBean;
import com.tc.management.TerracottaManagement;
import com.tc.net.NodeID;
import com.tc.net.groups.GroupEventsListener;
import com.tc.util.Assert;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.management.ObjectName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsistencyManagerImpl implements ConsistencyManager, GroupEventsListener {
  
  private static final Logger CONSOLE = TCLogging.getConsoleLogger();
  private static final Logger LOGGER = LoggerFactory.getLogger(ConsistencyManagerImpl.class);
  private final int peerServers;
  private boolean activeVote = false;
  private boolean blocked = false;
  private Set<Transition> actions = EnumSet.noneOf(Transition.class);
  private long voteTerm = 0;
  private long blockedAt = Long.MAX_VALUE;
  private final ServerVoterManager voter;
  private final Set<NodeID> activePeers = Collections.synchronizedSet(new HashSet<>());
  private final Set<NodeID> passives = Collections.synchronizedSet(new HashSet<>());
  
  public ConsistencyManagerImpl(int knownPeers, int voters) {
    try {
      this.peerServers = knownPeers;
      this.voter = new ServerVoterManagerImpl(voters);
      initMBean();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  public synchronized long getVoteTerm() {
    return voteTerm;
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
        
  private void initMBean() {
    try {
      ObjectName MBEAN_NAME = TerracottaManagement.createObjectName(null, "ConsistencyManager", TerracottaManagement.MBeanDomain.PUBLIC);
      ManagementFactory.getPlatformMBeanServer().registerMBean(new ConsistencyMBeanImpl(), MBEAN_NAME);
    } catch (Exception e) {
      LOGGER.warn("Consistency MBean not initialized", e);
    }
  }

  @Override
  public boolean requestTransition(ServerMode mode, NodeID sourceNode, Transition newMode) throws IllegalStateException {
    if (newMode == Transition.ADD_PASSIVE) {
 //  starting passive sync to a new node, at this point the passive can be consisdered a
 //  vote for the current active and the passive sync rues will make sure all the data is replicated      
      passives.add(sourceNode);
      Assert.assertEquals(mode, ServerMode.ACTIVE);
      //  adding a passive to an active is always OK
      CONSOLE.info("Action:{} is always allowed", newMode);
      return true;
    }
    if (newMode == Transition.CONNECT_TO_ACTIVE) {
      endVoting(true, newMode);
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
    boolean allow = false;
    // activate voting to lock the voting members and return the number of server votes
    int serverVotes = activateVoting(mode, newMode);
    int threshold = voteThreshold(mode);
    if (serverVotes >= threshold || serverVotes == this.peerServers) {
    // if the threshold is achieved with just servers or all the servers are visible, transition is granted
      CONSOLE.info("Action:{} allowed because enough servers are connected", newMode);
      endVoting(true, newMode);
      return true;
    }
    if (voter.overrideVoteReceived()) {
      CONSOLE.info("Action:{} allowed because override received", newMode);
      endVoting(true, newMode);
      return true;
    }
    if (mode == ServerMode.START && newMode == Transition.MOVE_TO_ACTIVE) {
//  only other servers can be considered in this case.  any registered
//  external voters should not be considered because they were registered before 
//  when there should have been no cluster wide active
      CONSOLE.info("Action:{} not allowed because not enough servers are connected", newMode);
      endVoting(allow, newMode);
      return false;
    }

    long start = System.currentTimeMillis();
    try {
      if (voter.getRegisteredVoters() + serverVotes < threshold) {
        blocked = true;
        blockedAt = System.currentTimeMillis();
        CONSOLE.warn("Not enough registered voters.  Require override intervention or {} members of the stripe to be connected for action {}", this.peerServers + 1 > threshold ? threshold : "all", newMode);
      } else while (!allow && System.currentTimeMillis() - start < ServerVoterManagerImpl.VOTEBEAT_TIMEOUT) {
        try {
          //  servers connected + votes received
          if (serverVotes + voter.getVoteCount() < threshold) {
            TimeUnit.MILLISECONDS.sleep(100);
          } else {
            allow = true;
          }
        } catch (InterruptedException ie) {
          LOGGER.info("interrupted", ie);
          break;
        }
      }
    } finally {
      CONSOLE.info("Action:{} granted:{} vote tally servers:{} external:{} of total:{}", newMode, allow, serverVotes + 1, voter.getVoteCount(), peerServers + voter.getVoterLimit() + 1);
      endVoting(allow, newMode);
    }
    return allow;
  }
  
  private int voteThreshold(ServerMode mode) {
    //  peer servers plus extra votes plus self is the total votes available
    int voteCount = peerServers + voter.getVoterLimit() + 1;
    if (mode == ServerMode.ACTIVE) {
      // only half the votes needed to continue as active
      return voteCount - (voteCount>>1) - 1; // ceiling of half minus the vote for self
    } else {
      // need more than half to promote to active
      return (voteCount>>1);  // floor of half because the self vote will tip the scales
    }
  }
  
  private synchronized int activateVoting(ServerMode mode, Transition moveTo) {
    if (!activeVote) {
      activeVote = true;
      voter.startVoting(++voteTerm);
    }
    actions.add(moveTo);
    if (mode != ServerMode.ACTIVE) {
      return this.activePeers.size();
    } else {
      return this.passives.size();
    }
  }
  
  private synchronized void endVoting(boolean allowed, Transition moveTo) {
    if (activeVote) {
      if (allowed) {
        Assert.assertEquals(voteTerm, voter.stopVoting());
        activeVote = false;
        blocked = false;
        blockedAt = Long.MAX_VALUE;
        switch(moveTo) {
          case CONNECT_TO_ACTIVE:
          case MOVE_TO_ACTIVE:
            actions.remove(Transition.CONNECT_TO_ACTIVE);
            actions.remove(Transition.MOVE_TO_ACTIVE);
            break;
          default:
            actions.remove(moveTo);
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
  
  public class ConsistencyMBeanImpl extends AbstractTerracottaMBean implements com.tc.l2.state.ConsistencyMBean {

    public ConsistencyMBeanImpl() throws Exception {
      super(ConsistencyMBean.class, false);
    }
    
    @Override
    public boolean isBlocked() {
      return blocked;
    }
    
    
    @Override
    public boolean isStuck() {
      return activeVote ;
    }    

    @Override
    public Collection<Transition> requestedActions() {
      return getActions();
    }
    
    

    @Override
    public void reset() {
      //
    }
  }
}
