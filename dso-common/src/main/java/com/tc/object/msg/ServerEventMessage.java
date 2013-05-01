package com.tc.object.msg;

import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.ServerEventType;

/**
 * Server event notification.
 *
 * @author Eugene Shelestovich
 */
public interface ServerEventMessage extends TCMessage {

  ServerEventType getType();

  void setType(ServerEventType type);

  Object getKey();

  void setKey(Object key);

  String getDestinationName();

  void setDestinationName(String cacheName);
}
