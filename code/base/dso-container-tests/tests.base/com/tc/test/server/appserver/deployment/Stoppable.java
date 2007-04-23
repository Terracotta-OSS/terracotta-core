/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.appserver.deployment;

public interface Stoppable {

  void start() throws Exception;

  void stop() throws Exception;

  public void stopIgnoringExceptions();

  boolean isStopped();

}
