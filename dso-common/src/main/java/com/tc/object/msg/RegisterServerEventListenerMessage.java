package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.server.ServerEventType;
import com.tc.object.session.SessionID;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;

/**
 * Client sends this message to start listening for server events.
 *
 * @author Eugene Shelestovich
 */
public class RegisterServerEventListenerMessage extends DSOMessageBase {

  private static final byte DESTINATION_NAME_ID = 0;
  private static final byte EVENT_TYPE_ID = 1;

  private String destination;
  private Set<ServerEventType> eventTypes = EnumSet.noneOf(ServerEventType.class);

  public RegisterServerEventListenerMessage(final SessionID sessionID, final MessageMonitor monitor,
                                            final TCByteBufferOutputStream out, final MessageChannel channel,
                                            final TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public RegisterServerEventListenerMessage(final SessionID sessionID, final MessageMonitor monitor,
                                            final MessageChannel channel, final TCMessageHeader header,
                                            final TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  protected void dehydrateValues() {
    putNVPair(DESTINATION_NAME_ID, destination);
    for (ServerEventType eventType : eventTypes) {
      putNVPair(EVENT_TYPE_ID, eventType.ordinal());
    }
  }

  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case DESTINATION_NAME_ID:
        destination = getStringValue();
        return true;
      case EVENT_TYPE_ID:
        eventTypes.add(ServerEventType.values()[getIntValue()]);
        return true;
      default:
        return false;
    }
  }

  public String getDestination() {
    return destination;
  }

  public void setDestination(final String destination) {
    this.destination = destination;
  }

  public void setEventTypes(final Set<ServerEventType> eventTypes) {
    this.eventTypes = eventTypes;
  }

  public Set<ServerEventType> getEventTypes() {
    return eventTypes;
  }
}
