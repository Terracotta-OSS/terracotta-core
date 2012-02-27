/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.server.util;

import org.mortbay.jetty.servlet.AbstractSessionManager;

import java.util.Collections;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 * Implementation of a Jetty 6 'no session' session manager
 */
public class NoSessionManager extends AbstractSessionManager {
  @Override
  public Map getSessionMap() {
    return Collections.emptyMap();
  }

  @Override
  public int getSessions() {
    return 0;
  }

  @Override
  protected void addSession(final Session session) {
    //
  }

  @Override
  public Session getSession(final String idInCluster) {
    return null;
  }

  @Override
  protected void invalidateSessions() {
    //
  }

  @Override
  protected Session newSession(final HttpServletRequest request) {
    return null;
  }

  @Override
  protected void removeSession(final String idInCluster) {
    //
  }
}
