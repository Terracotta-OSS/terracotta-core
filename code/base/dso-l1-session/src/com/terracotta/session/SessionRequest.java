/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.session;

import com.terracotta.session.util.Assert;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class SessionRequest extends HttpServletRequestWrapper implements TerracottaRequest {

  // Attribute name for storing the Session in the request
  private static final String       SESSION_ATTRIBUTE_NAME         = SessionRequest.class.getName() + ".session";

  // Attribute name for storing a Boolean.TRUE indicating the request has been forwarded
  public static final String        SESSION_FORWARD_ATTRIBUTE_NAME = SessionRequest.class.getName() + ".forward";

  private final HttpServletRequest  req;
  private final HttpServletResponse res;
  private SessionId                 requestedSessionId;
  private final long                requestStartMillis;
  private final boolean             isForwarded;
  private final SessionManager      mgr;
  private final String              rawRequestedSessionId;
  private final SessionIDSource     source;

  public SessionRequest(SessionId requestedSessionId, HttpServletRequest req, HttpServletResponse res,
                        SessionManager sessionManager, String rawRequestedSessionId, SessionIDSource source) {
    super(req);

    Assert.pre(req != null);
    Assert.pre(res != null);
    Assert.pre(sessionManager != null);
    Assert.pre(source != null);

    this.req = req;
    this.res = res;
    this.mgr = sessionManager;
    this.requestedSessionId = requestedSessionId;
    this.requestStartMillis = System.currentTimeMillis();
    this.rawRequestedSessionId = rawRequestedSessionId;
    this.source = source;

    // Added by TerracottaDispatcher when requests get forwarded through
    Object fwdFlag = req.getAttribute(SESSION_FORWARD_ATTRIBUTE_NAME);
    this.isForwarded = (fwdFlag != null && fwdFlag.equals(Boolean.TRUE));
  }

  @Override
  public HttpSession getSession() {
    HttpSession rv = getTerracottaSession(true);
    Assert.post(rv != null);
    return rv;
  }

  @Override
  public HttpSession getSession(boolean createNew) {
    return getTerracottaSession(createNew);
  }

  @Override
  public boolean isRequestedSessionIdValid() {
    if (requestedSessionId == null) return false;
    Session sess = getTerracottaSession(false);
    return (sess != null && requestedSessionId.getKey().equals(sess.getSessionId().getKey()));
  }

  @Override
  public String getRequestedSessionId() {
    return rawRequestedSessionId;
  }

  @Override
  public boolean isRequestedSessionIdFromCookie() {
    return source.isCookie();
  }

  @Override
  public boolean isRequestedSessionIdFromURL() {
    return source.isURL();
  }

  @Override
  public boolean isRequestedSessionIdFromUrl() {
    return isRequestedSessionIdFromURL();
  }

  public String encodeRedirectURL(String url) {
    return mgr.getCookieWriter().encodeRedirectURL(url, this);
  }

  public String encodeURL(String url) {
    return mgr.getCookieWriter().encodeURL(url, this);
  }

  public final Session getTerracottaSession(boolean createNew) {
    Session session = (Session) getAttribute(SESSION_ATTRIBUTE_NAME);
    if (session != null) return session;
    session = (createNew) ? mgr.getSession(requestedSessionId, req, res) : mgr.getSessionIfExists(requestedSessionId,
                                                                                                  req, res);
    setAttribute(SESSION_ATTRIBUTE_NAME, session);

    if (session != null) {
      session.associateRequest(this);
    }
    Assert.post(!createNew || session != null);
    return session;
  }

  public boolean isForwarded() {
    return isForwarded;
  }

  public long getRequestStartMillis() {
    return requestStartMillis;
  }

  @Override
  public RequestDispatcher getRequestDispatcher(String path) {
    return new TerracottaDispatcher(super.getRequestDispatcher(path));
  }

  void clearSession() {
    Assert.pre(getAttribute(SESSION_ATTRIBUTE_NAME) != null);
    removeAttribute(SESSION_ATTRIBUTE_NAME);

    requestedSessionId = null;
  }

}
