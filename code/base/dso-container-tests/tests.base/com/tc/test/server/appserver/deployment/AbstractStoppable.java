/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.appserver.deployment;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public abstract class AbstractStoppable implements Stoppable {

  protected boolean stopped = true;
  protected Log     logger  = LogFactory.getLog(getClass());

  public void start() throws Exception {
    logger.info("### Starting "+this);
    long l1 = System.currentTimeMillis();
    doStart();
    stopped = false;
    long l2 = System.currentTimeMillis();
    logger.info("### Started " + this + "; " + (l2-l1)/1000f);
  }

  public void stop() throws Exception {
    logger.info("### Stopping " + this);
    long l1 = System.currentTimeMillis();
    stopped = true;
    doStop();
    long l2 = System.currentTimeMillis();
    logger.info("### Stopped " + this + "; " + (l2-l1)/1000f);
  }

  protected abstract void doStop() throws Exception;

  protected abstract void doStart() throws Exception;

  public boolean isStopped() {
    return stopped;
  }

  public void stopIgnoringExceptions() {
    try {
      stop();
    } catch (Exception e) {
      logger.error(e);
    }
  }

}
