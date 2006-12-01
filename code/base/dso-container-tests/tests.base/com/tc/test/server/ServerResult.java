/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server;

/**
 * Return values after a server has been initialized and started.
 */
public interface ServerResult {
  
  int serverPort();
  
  Server ref(); // reference to current server instance
}
