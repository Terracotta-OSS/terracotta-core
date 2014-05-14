/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.locks.ThreadID;

public interface ClusterMetaDataMessage extends TCMessage {

  public ThreadID getThreadID();

  public void setThreadID(ThreadID threadID);

}
