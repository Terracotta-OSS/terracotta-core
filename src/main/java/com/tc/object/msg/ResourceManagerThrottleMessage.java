package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.GroupID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.session.SessionID;

import java.io.IOException;

/**
 * @author tim
 */
public class ResourceManagerThrottleMessage extends DSOMessageBase {
  private static final byte EXCEPTION_ID = 1;
  private static final byte THROTTLE_ID = 2;
  private static final byte GROUP_ID = 3;

  private boolean exception;
  private float throttle;
  private GroupID groupID;

  public ResourceManagerThrottleMessage(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out, MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public ResourceManagerThrottleMessage(SessionID sessionID, MessageMonitor monitor, MessageChannel channel, TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  public GroupID getGroupID() {
    return groupID;
  }

  public boolean getThrowException() {
    return exception;
  }

  public float getThrottle() {
    return throttle;
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(EXCEPTION_ID, exception);
    putNVPair(THROTTLE_ID, throttle);
    putNVPair(GROUP_ID, groupID.toInt());
  }

  @Override
  protected boolean hydrateValue(final byte name) throws IOException {
    switch (name) {
      case EXCEPTION_ID:
        exception = getBooleanValue();
        return true;
      case THROTTLE_ID:
        throttle = getFloatValue();
        return true;
      case GROUP_ID:
        groupID = new GroupID(getIntValue());
        return true;
      default:
        return false;
    }
  }

  public void initialize(GroupID groupId, boolean exceptionParam, float throttleParam) {
    this.exception = exceptionParam;
    this.throttle = throttleParam;
    this.groupID = groupId;
  }
}
