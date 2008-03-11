/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.buffer.exceptions;

public class StatisticsBufferCaptureSessionCreationErrorException extends StatisticsBufferException {
  private final String clustersessionid;
  private final Long localsessionid;

  public StatisticsBufferCaptureSessionCreationErrorException(final String clustersessionid, final long localsessionid) {
    super("A new capture session could not be created with cluster-wide ID '" + clustersessionid + "' and local ID '" + localsessionid + "'.", null);
    this.clustersessionid = clustersessionid;
    this.localsessionid = new Long(localsessionid);
  }

  public StatisticsBufferCaptureSessionCreationErrorException(final String clustersessionid, final Throwable cause) {
    super("Unexpected error while creating a new capture session with cluster-wide ID '" + clustersessionid + "'.", cause);
    this.clustersessionid = clustersessionid;
    this.localsessionid = null;
  }

  public String getClusterSessionId() {
    return clustersessionid;
  }

  public Long getLocalSessionId() {
    return localsessionid;
  }
}
