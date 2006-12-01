/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.appserver;

import com.tc.test.server.Server;

/**
 * Knows how to start and stop an application server. Application Servers must be started in serial to avoid race
 * conditions in allocating ports.
 */
public interface AppServer extends Server {
  // available for future feature enhancements
}
