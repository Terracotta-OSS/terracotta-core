/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
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
  private static final String SESSION_ATTRIBUTE_NAME = SessionRequest.class.getName() + ".session";
  
  // Attribute name for storing a Boolean.TRUE indicating the request has been forwarded
  public static final String SESSION_FORWARD_ATTRIBUTE_NAME = SessionRequest.class.getName() + ".forward";
  
  private final HttpServletRequest  req;
  private final HttpServletResponse res;
  private SessionId                 requestedSessionId;
  private final long                requestStartMillis;
  private final boolean             isForwarded;
  private final boolean             isSessionOwner;
  private final SessionManager      mgr;

  public SessionRequest(SessionId requestedSessionId, HttpServletRequest req, HttpServletResponse res,
                        SessionManager sessionManager) {
    super(req);

    Assert.pre(req != null);
    Assert.pre(res != null);
    Assert.pre(sessionManager != null);

    this.req = req;
    this.res = res;
    this.mgr = sessionManager;
    this.requestedSessionId = requestedSessionId;
    this.requestStartMillis = System.currentTimeMillis();
    // in some cases, we could be multi-wrapping native requests.
    // in this case, we need to check if TC session has already been created
    HttpSession nativeSess = req.getSession(false);
    if (nativeSess instanceof Session) {
      this.isForwarded = true;
      this.isSessionOwner = false;
      setAttribute(SESSION_ATTRIBUTE_NAME, nativeSess);
    } else {
      this.isSessionOwner = true;
      
      // Added by TerracottaDispatcher when requests get forwarded through
      Object fwdFlag = req.getAttribute(SESSION_FORWARD_ATTRIBUTE_NAME);
      this.isForwarded = (fwdFlag != null && fwdFlag.equals(Boolean.TRUE));
    }
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

  public boolean isSessionOwner() {
    return isSessionOwner && getAttribute(SESSION_ATTRIBUTE_NAME) != null;
  }

  public boolean isForwarded() {
    return isForwarded;
  }

  public long getRequestStartMillis() {
    return requestStartMillis;
  }

  public RequestDispatcher getRequestDispatcher(String path) {
    return new TerracottaDispatcher(super.getRequestDispatcher(path));
  }
  
  void clearSession() {
    Assert.pre(getAttribute(SESSION_ATTRIBUTE_NAME) != null);
    removeAttribute(SESSION_ATTRIBUTE_NAME);

    requestedSessionId = null;
  }

}

