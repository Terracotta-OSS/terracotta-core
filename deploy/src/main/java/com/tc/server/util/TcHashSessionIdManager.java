/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.server.util;

import org.mortbay.jetty.SessionIdManager;
import org.mortbay.jetty.servlet.HashSessionIdManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Delegates to Jetty's HashSessionIdManager but initializes it lazily
 * with a background thread.
 */
public class TcHashSessionIdManager implements SessionIdManager {

  private volatile HashSessionIdManager delegate;

  @Override
  public void addSession(HttpSession session) {
    getDelegate().addSession(session);
  }

  @Override
  public String getClusterId(String nodeId) {
    return getDelegate().getClusterId(nodeId);
  }

  @Override
  public String getNodeId(String clusterId, HttpServletRequest request) {
    return getDelegate().getNodeId(clusterId, request);
  }

  @Override
  public String getWorkerName() {
    return getDelegate().getWorkerName();
  }

  @Override
  public boolean idInUse(String id) {
    return getDelegate().idInUse(id);
  }

  @Override
  public void invalidateAll(String id) {
    getDelegate().invalidateAll(id);
  }

  @Override
  public boolean isFailed() {
    if (delegate == null) { return false; }
    return getDelegate().isFailed();
  }

  public final void start() throws Exception {
    if (delegate != null) { return; }

    // Initialize delegate lazily to prevent the SecureRandom
    // it contains from blocking the jetty initialization.
    Thread thread = new Thread() {
      @Override
      public void run() {
        getDelegate();
      }
    };
    thread.setName("TcHashSessionIdManager initializer");
    thread.setDaemon(true);
    thread.start();
  }

  public final void stop() throws Exception {
    if (delegate == null) { return; }
    getDelegate().stop();
    delegate = null;
  }

  public boolean isRunning() {
    if (delegate == null) { return false; }
    return getDelegate().isRunning();
  }

  public boolean isStarted() {
    if (delegate == null) { return false; }
    return getDelegate().isStarted();
  }

  public boolean isStarting() {
    if (delegate == null) { return false; }
    return getDelegate().isStarting();
  }

  public boolean isStopping() {
    if (delegate == null) { return false; }
    return getDelegate().isStopping();
  }

  public boolean isStopped() {
    if (delegate == null) { return true; }
    return getDelegate().isStopped();
  }

  public void removeSession(HttpSession session) {
    getDelegate().removeSession(session);
  }

  public String newSessionId(HttpServletRequest request, long created) {
    return getDelegate().newSessionId(request, created);
  }

  private HashSessionIdManager getDelegate() {
    if (delegate != null) { return delegate; }
    synchronized (this) {
      if (delegate != null) { return delegate; }

      HashSessionIdManager realManager = new HashSessionIdManager();
      try {
        realManager.start();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      delegate = realManager;

      return delegate;
    }
  }
}
