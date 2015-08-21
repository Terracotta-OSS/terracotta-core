package com.tc.entity;

import com.tc.net.protocol.tcm.TCMessage;

/**
 * @author twu
 */
public interface ServerEntityResponseMessage extends TCMessage {
  void setResponseId(long responseId);

  long getResponseId();
}
