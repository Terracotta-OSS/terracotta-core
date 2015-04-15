/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.server.util;

import org.eclipse.jetty.server.SessionIdManager;
import org.eclipse.jetty.server.session.HashSessionIdManager;

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

  @Override
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

  @Override
  public final void stop() throws Exception {
    if (delegate == null) { return; }
    getDelegate().stop();
    delegate = null;
  }

  @Override
  public boolean isRunning() {
    if (delegate == null) { return false; }
    return getDelegate().isRunning();
  }

  @Override
  public boolean isStarted() {
    if (delegate == null) { return false; }
    return getDelegate().isStarted();
  }

  @Override
  public boolean isStarting() {
    if (delegate == null) { return false; }
    return getDelegate().isStarting();
  }

  @Override
  public boolean isStopping() {
    if (delegate == null) { return false; }
    return getDelegate().isStopping();
  }

  @Override
  public boolean isStopped() {
    if (delegate == null) { return true; }
    return getDelegate().isStopped();
  }

  @Override
  public void removeSession(HttpSession session) {
    getDelegate().removeSession(session);
  }

  @Override
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

  @Override
  public void addLifeCycleListener(Listener arg0) {
    /**/
  }

  @Override
  public void removeLifeCycleListener(Listener arg0) {
    /**/
  }
}
