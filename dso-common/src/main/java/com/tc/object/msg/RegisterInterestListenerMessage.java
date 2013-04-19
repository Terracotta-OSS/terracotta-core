package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.InterestType;
import com.tc.object.session.SessionID;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;

/**
 * This message is used when client wants to listen for specific L2 events (interests).
 *
 * @author Eugene Shelestovich
 */
public class RegisterInterestListenerMessage extends DSOMessageBase {

  private static final byte DESTINATION_NAME_ID = 0;
  private static final byte INTEREST_TYPE_ID = 1;

  private String destinationName;
  private Set<InterestType> interestTypes = EnumSet.noneOf(InterestType.class);

  public RegisterInterestListenerMessage(final SessionID sessionID, final MessageMonitor monitor,
                                         final TCByteBufferOutputStream out, final MessageChannel channel,
                                         final TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public RegisterInterestListenerMessage(final SessionID sessionID, final MessageMonitor monitor,
                                         final MessageChannel channel, final TCMessageHeader header,
                                         final TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  protected void dehydrateValues() {
    putNVPair(DESTINATION_NAME_ID, destinationName);
    for (InterestType interestType : interestTypes) {
      putNVPair(INTEREST_TYPE_ID, interestType.toInt());
    }
  }

  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case DESTINATION_NAME_ID:
        destinationName = getStringValue();
        return true;
      case INTEREST_TYPE_ID:
        interestTypes.add(InterestType.fromInt(getIntValue()));
        return true;
      default:
        return false;
    }
  }

  public String getDestinationName() {
    return destinationName;
  }

  public void setDestinationName(final String destinationName) {
    this.destinationName = destinationName;
  }

  public void setInterestTypes(final Set<InterestType> interestTypes) {
    this.interestTypes = interestTypes;
  }

  public Set<InterestType> getInterestTypes() {
    return interestTypes;
  }
}
