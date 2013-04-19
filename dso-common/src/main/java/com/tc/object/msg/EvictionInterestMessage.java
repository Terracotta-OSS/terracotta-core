package com.tc.object.msg;

import com.tc.net.protocol.tcm.TCMessage;

/**
 * @author Eugene Shelestovich
 */
public interface EvictionInterestMessage extends TCMessage {

  Object getKey();

  void setKey(Object key);
}
