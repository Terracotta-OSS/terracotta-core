package com.tc.object.msg;

import com.tc.net.protocol.tcm.TCMessage;
import com.tc.server.ServerEvent;

import java.util.List;

/**
 * A message with batched server events.
 *
 * @author Eugene Shelestovich
 */
public interface ServerEventBatchMessage extends TCMessage {

  void setEvents(List<ServerEvent> events);

  List<ServerEvent> getEvents();
}
