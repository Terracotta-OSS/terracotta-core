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
package com.tc.l2.msg;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.l2.state.Enrollment;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.MessageID;

import java.io.IOException;

public class L2StateMessage extends AbstractGroupMessage {

  public static final int START_ELECTION          = 0; // Sent during the start of an election by the initiator
  public static final int ELECTION_RESULT         = 1; // Sent at the end of an election by the initiator
  public static final int RESULT_AGREED           = 2; // Sent in response to ELECTION_RESULT/WON_ALREADY if no
  // conflict
  public static final int RESULT_CONFLICT         = 3; // Sent in response to ELECTION_RESULT/WON_ALREADY on conflict
  public static final int ABORT_ELECTION          = 4; // Sent in response to START_ELECTION by already elected ACTIVE
  public static final int ELECTION_WON            = 5; // Sent by the node that wins an election
  public static final int ELECTION_WON_ALREADY    = 6; // Sent to new nodes joining after the node wins an election and
  // turns ACTIVE
  public static final int MOVE_TO_PASSIVE_STANDBY = 7; // Sent by active to notify passive can become PASSIVE_STANDBY

  private Enrollment      enrollment;

  // To make serialization happy
  public L2StateMessage() {
    super(-1);
  }

  public L2StateMessage(int type, Enrollment e) {
    super(type);
    this.enrollment = e;
  }

  public L2StateMessage(MessageID requestID, int type, Enrollment e) {
    super(type, requestID);
    this.enrollment = e;
  }

  @Override
  protected void basicDeserializeFrom(TCByteBufferInput in) throws IOException {
    this.enrollment = new Enrollment();
    this.enrollment.deserializeFrom(in);
  }

  @Override
  protected void basicSerializeTo(TCByteBufferOutput out) {
    this.enrollment.serializeTo(out);
  }


  public Enrollment getEnrollment() {
    return enrollment;
  }

  @Override
  public String toString() {
    return "L2StateMessage [ " + messageFrom() + ", type = " + getTypeString() + ", " + enrollment + "]";
  }

  private String getTypeString() {
    switch (getType()) {
      case START_ELECTION:
        return "START_ELECTION";
      case ELECTION_RESULT:
        return "ELECTION_RESULT";
      case RESULT_AGREED:
        return "RESULT_AGREED";
      case RESULT_CONFLICT:
        return "RESULT_CONFLICT";
      case ABORT_ELECTION:
        return "ABORT_ELECTION";
      case ELECTION_WON:
        return "ELECTION_WON";
      case ELECTION_WON_ALREADY:
        return "ELECTION_WON_ALREADY";
      case MOVE_TO_PASSIVE_STANDBY:
        return "MOVE_TO_PASSIVE_STANDBY";
      default:
        throw new AssertionError("Unknow Type ! : " + getType());
    }
  }

  public static L2StateMessage createElectionStartedMessage(Enrollment e) {
    return new L2StateMessage(L2StateMessage.START_ELECTION, e);
  }

  public static L2StateMessage createElectionResultMessage(Enrollment e) {
    return new L2StateMessage(L2StateMessage.ELECTION_RESULT, e);
  }

  public static L2StateMessage createAbortElectionMessage(L2StateMessage initiatingMsg, Enrollment e) {
    return new L2StateMessage(initiatingMsg.getMessageID(), L2StateMessage.ABORT_ELECTION, e);
  }

  public static L2StateMessage createElectionStartedMessage(L2StateMessage initiatingMsg, Enrollment e) {
    return new L2StateMessage(initiatingMsg.getMessageID(), L2StateMessage.START_ELECTION, e);
  }

  public static L2StateMessage createResultConflictMessage(L2StateMessage initiatingMsg, Enrollment e) {
    return new L2StateMessage(initiatingMsg.getMessageID(), L2StateMessage.RESULT_CONFLICT, e);
  }

  public static L2StateMessage createResultAgreedMessage(L2StateMessage initiatingMsg, Enrollment e) {
    return new L2StateMessage(initiatingMsg.getMessageID(), L2StateMessage.RESULT_AGREED, e);
  }

  public static L2StateMessage createElectionWonMessage(Enrollment e) {
    return new L2StateMessage(L2StateMessage.ELECTION_WON, e);
  }

  public static L2StateMessage createElectionWonAlreadyMessage(Enrollment e) {
    return new L2StateMessage(L2StateMessage.ELECTION_WON_ALREADY, e);
  }

  public static L2StateMessage createMoveToPassiveStandbyMessage(Enrollment e) {
    return new L2StateMessage(L2StateMessage.MOVE_TO_PASSIVE_STANDBY, e);
  }
}
