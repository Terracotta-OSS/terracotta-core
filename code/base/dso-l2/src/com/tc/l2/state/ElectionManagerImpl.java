/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.state;

import com.tc.l2.L2DebugLogging;
import com.tc.l2.L2DebugLogging.LogLevel;
import com.tc.l2.ha.WeightGeneratorFactory;
import com.tc.l2.msg.L2StateMessage;
import com.tc.l2.msg.L2StateMessageFactory;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.GroupMessage;
import com.tc.net.groups.GroupResponse;
import com.tc.util.Assert;
import com.tc.util.State;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ElectionManagerImpl implements ElectionManager {

  private static final TCLogger logger               = TCLogging.getLogger(ElectionManagerImpl.class);

  private static final State    INIT                 = new State("Initial-State");
  private static final State    ELECTION_COMPLETE    = new State("Election-Complete");
  private static final State    ELECTION_IN_PROGRESS = new State("Election-In-Progress");

  private final GroupManager    groupManager;
  private final Map             votes                = new HashMap();

  private State                 state                = INIT;

  // XXX::NOTE:: These variables are not reset until next election
  private Enrollment            myVote               = null;
  private Enrollment            winner;

  private final long            electionTime;

  public ElectionManagerImpl(GroupManager groupManager, StateManagerConfig stateManagerConfig) {
    this.groupManager = groupManager;
    electionTime = stateManagerConfig.getElectionTimeInSecs() * 1000;
  }

  public synchronized boolean handleStartElectionRequest(L2StateMessage msg) {
    Assert.assertEquals(L2StateMessage.START_ELECTION, msg.getType());
    if (state == ELECTION_IN_PROGRESS && (myVote.isANewCandidate() || !msg.getEnrollment().isANewCandidate())) {
      // Another node is also joining in the election process, Cast its vote and notify my vote
      // Note : WE dont want to do this for new candidates when we are not new.
      Enrollment vote = msg.getEnrollment();
      Enrollment old = (Enrollment) votes.put(vote.getNodeID(), vote);
      boolean sendResponse = msg.inResponseTo().isNull();
      if (old != null && !vote.equals(old)) {
        logger.warn("Received duplicate vote : Replacing with new one : " + vote + " old one : " + old);
        sendResponse = true;
      }
      if (sendResponse) {
        // This is either not a response to this node initiating election or a duplicate vote. Either case notify this
        // nodes vote
        GroupMessage response = createElectionStartedMessage(msg, myVote);
        logger.info("Casted vote from " + msg + " My Response : " + response);
        try {
          groupManager.sendTo(msg.messageFrom(), response);
        } catch (GroupException e) {
          logger.error("Error sending Votes to : " + msg.messageFrom(), e);
        }
      } else {
        logger.info("Casted vote from " + msg);
      }
      return true;
    } else {
      logger.info("Ignoring Start Election Request  : " + msg + " My state = " + state);
      return false;
    }
  }

  public synchronized void handleElectionAbort(L2StateMessage msg) {
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

  public synchronized void handleElectionResultMessage(L2StateMessage msg) {
    Assert.assertEquals(L2StateMessage.ELECTION_RESULT, msg.getType());
    debugInfo("Handling election result");
    if (state == ELECTION_COMPLETE && !this.winner.equals(msg.getEnrollment())) {
      // conflict
      GroupMessage resultConflict = L2StateMessageFactory.createResultConflictMessage(msg, this.winner);
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
      GroupMessage resultAgreed = L2StateMessageFactory.createResultAgreedMessage(msg, msg.getEnrollment());
      logger.info("Agreed with Election Result from " + msg.messageFrom() + " : " + resultAgreed);
      try {
        groupManager.sendTo(msg.messageFrom(), resultAgreed);
      } catch (GroupException e) {
        logger.error("Error sending Election result agreed message : " + resultAgreed);
      }
    }
  }

  private void basicAbort(L2StateMessage msg) {
    reset(msg.getEnrollment());
    logger.info("Aborted Election : Winner is : " + this.winner);
  }

  /**
   * This method is called by the winner of the election to announce to the world
   */
  public synchronized void declareWinner(NodeID myNodeId) {
    Assert.assertEquals(winner.getNodeID(), myNodeId);
    GroupMessage msg = createElectionWonMessage(this.winner);
    debugInfo("Announcing as winner: " + myNodeId);
    this.groupManager.sendAll(msg);
    logger.info("Declared as Winner: Winner is : " + this.winner);
    reset(winner);
  }

  public synchronized void reset(Enrollment winningEnrollment) {
    this.winner = winningEnrollment;
    this.state = INIT;
    this.votes.clear();
    this.myVote = null;
    notifyAll();
  }

  public NodeID runElection(NodeID myNodeId, boolean isNew, WeightGeneratorFactory weightsFactory) {
    NodeID winnerID = ServerID.NULL_ID;
    int count = 0;
    while (winnerID.isNull()) {
      if (count++ > 0) {
        logger.info("Requesting Re-election !!! count = " + count);
      }
      try {
        winnerID = doElection(myNodeId, isNew, weightsFactory);
      } catch (InterruptedException e) {
        logger.error("Interrupted during election : ", e);
        reset(null);
      } catch (GroupException e1) {
        logger.error("Error during election : ", e1);
        reset(null);
      }
    }
    return winnerID;
  }

  private synchronized void electionStarted(Enrollment e) {
    if (this.state == ELECTION_IN_PROGRESS) { throw new AssertionError("Election Already in Progress"); }
    this.state = ELECTION_IN_PROGRESS;
    this.myVote = e;
    this.winner = null;
    this.votes.clear();
    this.votes.put(e.getNodeID(), e); // Cast my vote
    logger.info("Election Started : " + e);
  }

  private NodeID doElection(NodeID myNodeId, boolean isNew, WeightGeneratorFactory weightsFactory)
      throws GroupException, InterruptedException {

    // Step 1: publish to cluster NodeID, weight and election start
    Enrollment e = EnrollmentFactory.createEnrollment(myNodeId, isNew, weightsFactory);
    electionStarted(e);

    GroupMessage msg = createElectionStartedMessage(e);
    debugInfo("Sending my election vote to all members");
    groupManager.sendAll(msg);

    // Step 2: Wait for election completion
    waitTillElectionComplete();

    // Step 3: Compute Winner
    Enrollment lWinner = computeResult();
    if (lWinner != e) {
      logger.info("Election lost : Winner is : " + lWinner);
      Assert.assertNotNull(lWinner);
      return lWinner.getNodeID();
    }
    // Step 4 : local host won the election, so notify world for acceptance
    msg = createElectionResultMessage(e);
    debugInfo("Won election, announcing to world and waiting for response...");
    GroupResponse responses = groupManager.sendAllAndWaitForResponse(msg);
    for (Iterator i = responses.getResponses().iterator(); i.hasNext();) {
      L2StateMessage response = (L2StateMessage) i.next();
      Assert.assertEquals(msg.getMessageID(), response.inResponseTo());
      if (response.getType() == L2StateMessage.RESULT_AGREED) {
        Assert.assertEquals(e, response.getEnrollment());
      } else if (response.getType() == L2StateMessage.RESULT_CONFLICT) {
        logger.info("Result Conflict: Local Result : " + e + " From : " + response.messageFrom() + " Result : "
                    + response.getEnrollment());
        return ServerID.NULL_ID;
      } else {
        throw new AssertionError("Node : " + response.messageFrom()
                                 + " responded neither with RESULT_AGREED or RESULT_CONFLICT :" + response);
      }
    }

    // Step 5 : result agreed - I am the winner
    return myNodeId;
  }

  private synchronized Enrollment computeResult() {
    if (state == ELECTION_IN_PROGRESS) {
      state = ELECTION_COMPLETE;
      logger.info("Election Complete : " + votes.values() + " : " + state);
      winner = countVotes();
    }
    return winner;
  }

  private Enrollment countVotes() {
    Enrollment computedWinner = null;
    for (Iterator i = votes.values().iterator(); i.hasNext();) {
      Enrollment e = (Enrollment) i.next();
      if (computedWinner == null) {
        computedWinner = e;
      } else if (e.wins(computedWinner)) {
        computedWinner = e;
      }
    }
    Assert.assertNotNull(computedWinner);
    return computedWinner;
  }

  private synchronized void waitTillElectionComplete() throws InterruptedException {
    long diff = electionTime;
    debugInfo("Waiting till election complete, electionTime=" + electionTime);
    while (state == ELECTION_IN_PROGRESS && diff > 0) {
      long start = System.currentTimeMillis();
      wait(diff);
      diff = diff - (System.currentTimeMillis() - start);
    }
  }

  private GroupMessage createElectionStartedMessage(Enrollment e) {
    return L2StateMessageFactory.createElectionStartedMessage(e);
  }

  private GroupMessage createElectionWonMessage(Enrollment e) {
    return L2StateMessageFactory.createElectionWonMessage(e);
  }

  private GroupMessage createElectionResultMessage(Enrollment e) {
    return L2StateMessageFactory.createElectionResultMessage(e);
  }

  private GroupMessage createElectionStartedMessage(L2StateMessage msg, Enrollment e) {
    return L2StateMessageFactory.createElectionStartedMessage(msg, e);
  }

  public long getElectionTime() {
    return electionTime;
  }

  private static void debugInfo(String message) {
    L2DebugLogging.log(logger, LogLevel.INFO, message, null);
  }

}
