/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.http.load;

import java.io.IOException;

public interface WorkItem {

  public void execute(StatsCollector c) throws IOException, NullPointerException, IllegalStateException;

  public boolean stop();

  public boolean expired(long currenttime);

  public void done();

}
