package com.tc.object.msg;

import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.InterestType;

/**
 * @author Eugene Shelestovich
 */
public interface ServerInterestMessage extends TCMessage {

  InterestType getInterestType();

  void setInterestType(InterestType type);

  Object getKey();

  void setKey(Object key);

  String getCacheName();

  void setCacheName(String cacheName);
}
