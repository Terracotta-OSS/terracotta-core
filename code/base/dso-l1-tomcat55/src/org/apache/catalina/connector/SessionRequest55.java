/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.apache.catalina.connector;

import org.apache.catalina.Realm;
import org.apache.catalina.Session;

import com.tc.object.util.OverrideCheck;
import com.tc.tomcat.session.SessionInternal;
import com.terracotta.session.TerracottaRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

// NOTE: A class adapter adds methods that delegate all override'able methods of tomcat's
// Request class to the "valveReq" instance
public class SessionRequest55 extends Request {

  static {
    OverrideCheck.check(Request.class, SessionRequest55.class);
  }

  private final Request           valveReq;
  private final TerracottaRequest sessionReq;
  private final Realm             realm;
  private SessionResponse55       sessionRes;
  private Session                 sessionInternal = null;

  /**
   * @param sessionReq SessionRequest must be a HttpServletRequest slice of Request
   * @param valveReq
   */
  public SessionRequest55(TerracottaRequest sessionReq, Request valveReq, Realm realm) {
    this.valveReq = valveReq;
    this.sessionReq = sessionReq;
    this.realm = realm;

    // silence compiler warning
    if (false && this.valveReq != this.valveReq) { throw new AssertionError(); }
  }

  public void setSessionResponse(SessionResponse55 sessionRes) {
    this.sessionRes = sessionRes;
  }

  public HttpSession getSession() {
    return sessionReq.getSession();
  }

  public HttpSession getSession(boolean arg0) {
    return sessionReq.getSession(arg0);
  }

  public String getRequestedSessionId() {
    return sessionReq.getRequestedSessionId();
  }

  public boolean isRequestedSessionIdFromCookie() {
    return sessionReq.isRequestedSessionIdFromCookie();
  }

  public boolean isRequestedSessionIdFromUrl() {
    return sessionReq.isRequestedSessionIdFromUrl();
  }

  public boolean isRequestedSessionIdFromURL() {
    return sessionReq.isRequestedSessionIdFromURL();
  }

  public boolean isRequestedSessionIdValid() {
    return sessionReq.isRequestedSessionIdValid();
  }

  public HttpServletRequest getRequest() {
    return sessionReq;
  }

  public Response getResponse() {
    return sessionRes;
  }

  public Session getSessionInternal() {
    return getSessionInternal(true);
  }

  public Session getSessionInternal(boolean create) {
    synchronized (this) {
      if (sessionInternal != null) { return sessionInternal; }

      com.terracotta.session.Session tcSession = (com.terracotta.session.Session) getSession(create);
      if (tcSession == null) { return null; }

      return sessionInternal = new SessionInternal(tcSession, realm);
    }

    // unreachable
  }

}
