/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.l2.state.Enrollment;
import com.tc.net.groups.GroupMessage;

public class L2StateMessageFactory {

  public static GroupMessage createElectionStartedMessage(Enrollment e) {
    return new L2StateMessage(L2StateMessage.START_ELECTION, e);
  }

  public static GroupMessage createElectionResultMessage(Enrollment e) {
    return new L2StateMessage(L2StateMessage.ELECTION_RESULT, e);
  }

  public static GroupMessage createAbortElectionMessage(L2StateMessage initiatingMsg, Enrollment e) {
    return new L2StateMessage(initiatingMsg.getMessageID(), L2StateMessage.ABORT_ELECTION, e);
  }

  public static GroupMessage createElectionStartedMessage(L2StateMessage initiatingMsg, Enrollment e) {
    return new L2StateMessage(initiatingMsg.getMessageID(), L2StateMessage.START_ELECTION, e);
  }

  public static GroupMessage createResultConflictMessage(L2StateMessage initiatingMsg, Enrollment e) {
    return new L2StateMessage(initiatingMsg.getMessageID(), L2StateMessage.RESULT_CONFLICT, e);
  }

  public static GroupMessage createResultAgreedMessage(L2StateMessage initiatingMsg, Enrollment e) {
    return new L2StateMessage(initiatingMsg.getMessageID(), L2StateMessage.RESULT_AGREED, e);
  }

  public static GroupMessage createElectionWonMessage(Enrollment e) {
    return new L2StateMessage(L2StateMessage.ELECTION_WON, e);
  }

  public static GroupMessage createElectionWonAlreadyMessage(Enrollment e) {
    return new L2StateMessage(L2StateMessage.ELECTION_WON_ALREADY, e);
  }

  public static GroupMessage createMoveToPassiveStandbyMessage(Enrollment e) {
    return new L2StateMessage(L2StateMessage.MOVE_TO_PASSIVE_STANDBY, e);
  }
}
