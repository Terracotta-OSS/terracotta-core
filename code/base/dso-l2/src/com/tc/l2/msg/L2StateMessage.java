/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.async.api.EventContext;
import com.tc.l2.state.Enrollment;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.MessageID;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class L2StateMessage extends AbstractGroupMessage implements EventContext {

  public static final int START_ELECTION          = 0; // Sent during the start of an election by the initiator
  public static final int ELECTION_RESULT         = 1; // Sen at the end of an election by the initiator
  public static final int RESULT_AGREED           = 2; // Sent in response to ELECTION_WON if no conflict
  public static final int RESULT_CONFLICT         = 3; // Sent in response to ELECTION_WON on conflict
  public static final int ABORT_ELECTION          = 4; // Sent in response to START_ELECTION by already elected ACTIVE
  public static final int ELECTION_WON            = 5; // Sent by the node that wins an election
  public static final int MOVE_TO_PASSIVE_STANDBY = 6; // Sent by active to notify passive can become PASSIVE_STANDBY

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

  protected void basicReadExternal(int type, ObjectInput in) throws IOException, ClassNotFoundException {
    this.enrollment = (Enrollment) in.readObject();
  }

  protected void basicWriteExternal(int type, ObjectOutput out) throws IOException {
    out.writeObject(enrollment);
  }

  public Enrollment getEnrollment() {
    return enrollment;
  }

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
      case MOVE_TO_PASSIVE_STANDBY:
        return "MOVE_TO_PASSIVE_STANDBY";
      default:
        throw new AssertionError("Unknow Type ! : " + getType());
    }
  }

}
