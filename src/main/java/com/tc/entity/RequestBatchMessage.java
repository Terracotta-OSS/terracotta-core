package com.tc.entity;

import com.tc.net.protocol.tcm.TCMessage;

import java.util.List;

/**
 * @author twu
 */
public interface RequestBatchMessage extends TCMessage {
  void setRequestBatch(List<Request> requestBatch);
  
  List<Request> getRequests();
}
