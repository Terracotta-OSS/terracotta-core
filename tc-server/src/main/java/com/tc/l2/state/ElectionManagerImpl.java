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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventHandler;
import com.tc.async.api.EventHandlerException;
import com.tc.l2.ha.WeightGeneratorFactory;
import com.tc.l2.msg.L2StateMessage;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.groups.GroupEventsListener;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.GroupResponse;
import com.tc.net.utils.L2Utils;
import com.tc.util.Assert;
import com.tc.util.State;
import java.util.Collections;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ElectionManagerImpl implements ElectionManager {

  private static final Logger logger = LoggerFactory.getLogger(ElectionManagerImpl.class);

  private static final State    INIT                 = new State("Initial-State");
  private static final State    ELECTION_COMPLETE    = new State("Election-Complete");
  private static final State    ELECTION_VOTED    = new State("Election-Votes-Are-In");
  private static final State    ELECTION_IN_PROGRESS = new State("Election-In-Progress");
  private static final State    ELECTION_SHUTDOWN = new State("Election-Shutdown");

  private final GroupManager<L2StateMessage> groupManager;
  private final Map<NodeID, Enrollment> votes        = new HashMap<>();

  private State                 state                = INIT;

  // XXX::NOTE:: These variables are not reset until next election
  private Enrollment            myVote               = null;
  private State                 serverState;
  private Enrollment            winner;
  private NodeID                active = ServerID.NULL_ID;
  private Set<ServerID>           passiveStandbys;

  private final long            electionTime;
  private int             expectedServers;

  public ElectionManagerImpl(GroupManager groupManager, int electionTimeInSec) {
    this.groupManager = groupManager;
    this.electionTime = electionTimeInSec * 1000;
    this.groupManager.registerForGroupEvents(new GroupEventsListener() {
      @Override
      public void nodeJoined(NodeID nodeID) {
        sendToNewMember(nodeID);
      }

      @Override
      public void nodeLeft(NodeID nodeID) {
        debugInfo("node left " + nodeID);
      }
    });
  }

  public synchronized void shutdown() {
    state = ELECTION_SHUTDOWN;
    notifyAll();
  }
  
  private synchronized boolean isShutdown() {
    return state == ELECTION_SHUTDOWN;
  }
  
  public EventHandler<ElectionContext> getEventHandler() {
    return new AbstractEventHandler<ElectionContext> () {
      @Override
      public void handleEvent(ElectionContext context) throws EventHandlerException {
          context.setWinner(runElection(context.getNode(), context.getServers(), context.isNew(), context.getFactory(),
                                        context.getCurrentState()));
      }

      @Override
      public void destroy() {
        super.destroy();
        reset(ServerID.NULL_ID, null);
      }
    };
  }
  
  public Set<ServerID> passiveStandbys() {
    return passiveStandbys;
  }

  @Override
  public synchronized boolean handleStartElectionRequest(L2StateMessage msg, State currentState) {
    Assert.assertEquals(L2StateMessage.START_ELECTION, msg.getType());
    if (state == ELECTION_IN_PROGRESS) {
      Enrollment vote = msg.getEnrollment();
      Enrollment old = votes.put(vote.getNodeID(), vote);
      if (votes.size() == expectedServers) {
        this.state = ELECTION_VOTED;
        notify();
      }
      if (myVote.isANewCandidate() || !msg.getEnrollment().isANewCandidate()) {
        // Another node is also joining in the election process, Cast its vote and notify my vote
        // Note : WE dont want to do this for new candidates when we are not new.
        boolean sendResponse = msg.inResponseTo().isNull();
        if (old != null && !vote.equals(old)) {
          logger.warn("Received duplicate vote : Replacing with new one : " + vote + " old one : " + old);
          sendResponse = true;
        }
        if (sendResponse) {
          // This is either not a response to this node initiating election or a duplicate vote. Either case notify this
          // nodes vote
          L2StateMessage response = L2StateMessage.createElectionStartedMessage(msg, myVote, currentState);
          logger.info("Cast vote from " + msg + " My Response : " + response);
          try {
            groupManager.sendTo(msg.messageFrom(), response);
          } catch (GroupException e) {
            logger.error("Error sending Votes to : " + msg.messageFrom(), e);
          }
        } else {
          logger.info("Cast vote from " + msg);
        }
        return true;
      }
    }
    logger.info("Ignoring Start Election Request  : " + msg + " My state = " + state + " " + myVote);
    return false;
  }

  @Override
  public synchronized void handleElectionAbort(L2StateMessage msg, State currentState) {
    Assert.assertEquals(L2StateMessage.ABORT_ELECTION, msg.getType());
    debugInfo("Handling election abort");
    if (state == ELECTION_IN_PROGRESS) {
      // An existing ACTIVE Node has forced election to quit
      Assert.assertNotNull(myVote);
      basicAbort(msg);
    } else {
      logger.warn("Ignoring Abort Election Request  : " + msg + " My state = " + state);
    }
  }

  @Override
  public synchronized void handleElectionResultMessage(L2StateMessage msg, State currentState) {
    Assert.assertEquals(L2StateMessage.ELECTION_RESULT, msg.getType());
    debugInfo("Handling election result");
    if (state == ELECTION_COMPLETE && !this.winner.equals(msg.getEnrollment())) {
      // conflict
      L2StateMessage resultConflict = L2StateMessage.createResultConflictMessage(msg, this.winner, currentState);
      logger.warn("WARNING :: Election result conflict : Winner local = " + this.winner + " :  remote winner = "
                  + msg.getEnrollment());
      try {
        groupManager.sendTo(msg.messageFrom(), resultConflict);
      } catch (GroupException e) {
        logger.error("Error sending Election result conflict message : " + resultConflict);
      }
    } else {
      // Agree to the result, abort the election if necessary
      if (state == ELECTION_IN_PROGRESS) {
        basicAbort(msg);
      }
      L2StateMessage resultAgreed = L2StateMessage.createResultAgreedMessage(msg, msg.getEnrollment(), currentState);
      logger.info("Agreed with Election Result from " + msg.messageFrom() + " : " + resultAgreed);
      try {
        groupManager.sendTo(msg.messageFrom(), resultAgreed);
      } catch (GroupException e) {
        logger.error("Error sending Election result agreed message : " + resultAgreed);
      }
    }
  }

  private void basicAbort(L2StateMessage msg) {
    reset(msg.messageFrom(), msg.getEnrollment());
    logger.info("Aborted Election : Winner is : " + this.winner);
  }

  /**
   * This method is called by the winner of the election to announce to the world
   */
  @Override
  public synchronized void declareWinner(Enrollment verify, State currentState) {
    L2StateMessage msg = L2StateMessage.createElectionWonMessage(verify, currentState);
    debugInfo("Announcing as winner: " + groupManager.getLocalNodeID());
    this.groupManager.sendAll(msg);
    logger.info("Declared as Winner: Winner is : " + this.winner);
    reset(groupManager.getLocalNodeID(), winner);
  }

  @Override
  public synchronized void reset(NodeID winnerNode, Enrollment winningEnrollment) {
    this.active = winnerNode;
    this.winner = winningEnrollment;
    this.state = INIT;
    this.votes.clear();
    this.myVote = null;
    notifyAll();
  }

  private NodeID runElection(NodeID myNodeId, Set<String> servers, boolean isNew, WeightGeneratorFactory weightsFactory, State currentState) {
    NodeID winnerID = ServerID.NULL_ID;
    int count = 0;
    while (winnerID.isNull() && !isShutdown()) {
      if (count++ > 0) {
        logger.info("Requesting Re-election !!! count = " + count);
      }
      try {
        winnerID = doElection(myNodeId, servers, isNew, weightsFactory, currentState);
      } catch (InterruptedException e) {
        L2Utils.handleInterrupted(logger, e);
        reset(ServerID.NULL_ID, null);
        return ServerID.NULL_ID;
      } catch (GroupException e1) {
        logger.error("Error during election : ", e1);
        reset(ServerID.NULL_ID, null);
      }
    }
    return winnerID;
  }
  
  private synchronized void sendToNewMember(NodeID node) {
    if (state == ELECTION_IN_PROGRESS) {
      L2StateMessage msg = L2StateMessage.createElectionStartedMessage(this.myVote, this.serverState);
      debugInfo("Sending my election vote to a new member " + node);
      try {
        groupManager.sendTo(node, msg);
      } catch (GroupException ge) {
        logger.error("Error during election : ", ge);
      }
    } else {
      debugInfo("no election in progress not sending to " + node);      
    }
  }

  private synchronized void electionStarted(Enrollment e, State serverState, int expectedServers) {
    if (this.state == ELECTION_IN_PROGRESS) { throw new AssertionError("Election Already in Progress"); }
    this.state = ELECTION_IN_PROGRESS;
    this.expectedServers = expectedServers;
    this.myVote = e;
    this.serverState = serverState;
    this.winner = null;
    this.passiveStandbys = null;
    this.votes.clear();
    this.votes.put(e.getNodeID(), e); // Cast my vote
    logger.info("Election Started : " + e);
  }

  private NodeID doElection(NodeID myNodeId, Set<String> servers, boolean isNew, WeightGeneratorFactory weightsFactory, State currentState)
      throws GroupException, InterruptedException {

    // Step 1: publish to cluster NodeID, weight and election start
    Enrollment e = EnrollmentFactory.createEnrollment(myNodeId, isNew, weightsFactory);
    electionStarted(e, currentState, servers.size());

    L2StateMessage msg = L2StateMessage.createElectionStartedMessage(e, currentState);
    debugInfo("Sending my election vote to all members");
    groupManager.sendTo(servers, msg);

    // Step 2: Wait for election completion
    long waited = waitTillElectionComplete();
    Assert.assertTrue(waited <= 0 || this.state == ELECTION_VOTED || this.state == INIT || this.state == ELECTION_SHUTDOWN);
    logger.info("Election took " + TimeUnit.MILLISECONDS.toSeconds(electionTime - waited) + " sec. ending in " + this.state);
    // Step 3: Compute Winner
    Enrollment lWinner = computeResult();
    if (lWinner == null) {
      return ServerID.NULL_ID;
    } else if (lWinner != e) {
      logger.info("Election lost : Winner is : " + lWinner);
      Assert.assertNotNull(lWinner);
      return active;
    }
    // Step 4 : local host won the election, so notify world for acceptance
    msg = L2StateMessage.createElectionResultMessage(e, currentState);
    debugInfo("Won election, announcing to world and waiting for response...");
    GroupResponse<L2StateMessage> responses = groupManager.sendToAndWaitForResponse(servers, msg);
    Set<ServerID> passives = new HashSet<>();
    for (L2StateMessage response : responses.getResponses()) {
      Assert.assertEquals(msg.getMessageID(), response.inResponseTo());
      if (response.getType() == L2StateMessage.RESULT_AGREED) {
        Assert.assertEquals(e, response.getEnrollment());
        if (StateManager.convert(response.getState()) == ServerMode.PASSIVE) {
          passives.add(response.messageFrom());
        }
      } else if (response.getType() == L2StateMessage.RESULT_CONFLICT) {
        logger.info("Result Conflict: Local Result : " + e + " From : " + response.messageFrom() + " Result : "
                    + response.getEnrollment());
        return ServerID.NULL_ID;
      } else {
        throw new AssertionError("Node : " + response.messageFrom()
                                 + " responded neither with RESULT_AGREED or RESULT_CONFLICT :" + response);
      }
    }
    passiveStandbys = Collections.unmodifiableSet(passives);
    // Step 5 : result agreed - I am the winner
    return myNodeId;
  }

  private synchronized Enrollment computeResult() {
    if (state == ELECTION_IN_PROGRESS || state == ELECTION_VOTED) {
      this.state = ELECTION_COMPLETE;
      logger.info("Election Complete : " + votes.values() + " : " + state);
      winner = countVotes();
      active = winner.getNodeID();
    }
    return winner;
  }

  private Enrollment countVotes() {
    Enrollment computedWinner = null;
    for (Enrollment e : votes.values()) {
      if (computedWinner == null) {
        computedWinner = e;
      } else if (e.wins(computedWinner)) {
        computedWinner = e;
      }
    }
    Assert.assertNotNull(computedWinner);
    return computedWinner;
  }

  private synchronized long waitTillElectionComplete() throws InterruptedException {
    long diff = electionTime;
    debugInfo("Waiting till election complete, electionTime=" + electionTime);
    while (state == ELECTION_IN_PROGRESS && diff > 0) {
      long start = System.currentTimeMillis();
      wait(diff);
      diff = diff - (System.currentTimeMillis() - start);
    }
    return diff;
  }

  @Override
  public long getElectionTime() {
    return electionTime;
  }

  private static void debugInfo(String message) {
    logger.debug(message);
  }

}
