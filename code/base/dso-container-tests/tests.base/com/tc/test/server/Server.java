/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server;

/**
 * Represents a generic server to be run as part of a unit test.
 */
public interface Server {

  ServerResult start(ServerParameters parameters) throws Exception;

  void stop() throws Exception;
}
