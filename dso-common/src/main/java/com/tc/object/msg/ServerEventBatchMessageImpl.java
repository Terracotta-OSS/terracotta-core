package com.tc.object.msg;

import com.google.common.collect.Lists;
import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.session.SessionID;
import com.tc.server.ServerEvent;

import java.io.IOException;
import java.util.List;

/**
 * @author Eugene Shelestovich
 */
public class ServerEventBatchMessageImpl extends DSOMessageBase implements ServerEventBatchMessage {

  private static final byte SERVER_EVENT = 0;

  private List<ServerEvent> events = Lists.newArrayList();

  public ServerEventBatchMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                     final TCByteBufferOutputStream out, final MessageChannel channel,
                                     final TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public ServerEventBatchMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                     final MessageChannel channel, final TCMessageHeader header,
                                     final TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  @Override
  protected void dehydrateValues() {
    for (ServerEvent event : events) {
      putNVPair(SERVER_EVENT, new ServerEventSerializableContext(event));
    }
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case SERVER_EVENT:
        final ServerEventSerializableContext ctx = (ServerEventSerializableContext) getObject(new ServerEventSerializableContext());
        events.add(ctx.getEvent());
        return true;
      default:
        return false;
    }
  }

  @Override
  public void setEvents(final List<ServerEvent> events) {
    this.events = events;
  }

  @Override
  public List<ServerEvent> getEvents() {
    return events;
  }
}
