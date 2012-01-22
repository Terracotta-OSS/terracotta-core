/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.object.locks.ThreadID;

public interface NodeMetaDataResponseMessage extends ClusterMetaDataResponseMessage {

  public void initialize(ThreadID threadID, String ip, String hostname);

  public String getIp();

  public String getHostname();

}
