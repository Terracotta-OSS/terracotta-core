/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.terracotta.session;

import com.terracotta.session.util.Assert;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class SessionRequest extends HttpServletRequestWrapper implements TerracottaRequest {
  private final HttpServletRequest  req;
  private final HttpServletResponse res;
  private final SessionId           requestedSessionId;
  private final long                requestStartMillis;
  private boolean                   unlockSesssionId = true;
  private Session                   session;
  private TerracottaSessionManager  mgr;

  public SessionRequest(SessionId requestedSessionId, HttpServletRequest req, HttpServletResponse res) {
    super(req);
    Assert.pre(req != null);
    Assert.pre(res != null);

    this.req = req;
    this.res = res;
    this.requestedSessionId = requestedSessionId;
    this.requestStartMillis = System.currentTimeMillis();
  }

  public HttpSession getSession() {
    HttpSession rv = getTerracottaSession(true);
    Assert.post(rv != null);
    return rv;
  }

  public HttpSession getSession(boolean createNew) {
    return getTerracottaSession(createNew);
  }

  public boolean isRequestedSessionIdValid() {
    if (requestedSessionId == null) return false;
    Session sess = getTerracottaSession(false);
    return (sess != null && requestedSessionId.getKey().equals(sess.getSessionId().getKey()));
  }

  public String encodeRedirectURL(String url) {
    return mgr.getCookieWriter().encodeRedirectURL(url, this);
  }

  public String encodeURL(String url) {
    return mgr.getCookieWriter().encodeURL(url, this);
  }

  protected Session getTerracottaSession(boolean createNew) {
    if (session != null) return session;
    // we need to make this check in case of a RequestDispatcher.forward call...
    final HttpSession s = req.getSession(false);
    if (s != null && s instanceof Session) {
      session = (Session) s;
      unlockSesssionId = false;
    } else {
      session = (createNew) ? mgr.getSession(requestedSessionId, req, res) : mgr.getSessionIfExists(requestedSessionId,
                                                                                                    req, res);
    }
    Assert.post(!createNew || session != null);
    return session;
  }

  public Session getSessionIfAny() {
    return session;
  }

  public long getRequestStartMillis() {
    return requestStartMillis;
  }

  public boolean isUnlockSesssionId() {
    return unlockSesssionId;
  }

  public void setSessionManager(TerracottaSessionManager sessionManager) {
    this.mgr = sessionManager;
  }
}
