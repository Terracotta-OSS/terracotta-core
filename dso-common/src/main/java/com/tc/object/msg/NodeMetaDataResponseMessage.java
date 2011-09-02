/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.object.locks.ThreadID;

public interface NodeMetaDataResponseMessage extends ClusterMetaDataResponseMessage {

  public void initialize(ThreadID threadID, String ip, String hostname);

  public String getIp();

  public String getHostname();

}
