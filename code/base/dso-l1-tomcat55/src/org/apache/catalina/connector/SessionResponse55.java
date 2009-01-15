/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.apache.catalina.connector;

import com.tc.object.util.OverrideCheck;
import com.terracotta.session.SessionResponse;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

// NOTE: A class adapter adds methods that delegate all override'able methods of tomcat's
// Response class to the "valveRes" instance
public class SessionResponse55 extends Response {
  static {
    OverrideCheck.check(Response.class, SessionResponse55.class);
  }

  private final Response         valveRes;
  private final SessionRequest55 sessReq;
  private final SessionResponse  sessRes;

  public SessionResponse55(Response valveRes, SessionRequest55 sessReq, SessionResponse sessRes) {
    this.valveRes = valveRes;
    this.sessRes = sessRes;
    this.sessReq = sessReq;

    // silence compiler warning
    if (false && this.valveRes != this.valveRes) { throw new AssertionError(); }
  }

  public String encodeRedirectUrl(String url) {
    return sessRes.encodeRedirectUrl(url);
  }

  public String encodeRedirectURL(String url) {
    return sessRes.encodeRedirectUrl(url);
  }

  public String encodeUrl(String url) {
    return sessRes.encodeUrl(url);
  }

  public String encodeURL(String url) {
    return sessRes.encodeURL(url);
  }

  public Request getRequest() {
    return this.sessReq;
  }

  public javax.servlet.http.HttpServletResponse getResponse() {
    return new ResponseWrapper(this.sessRes, this);
  }

  public boolean isCommitted() {
    // see CDV-939
    return this.valveRes.isAppCommitted();
  }

  // This class delegates all HttpServletResponse methods to the delegate, except for isCommitted()
  private static class ResponseWrapper extends HttpServletResponseWrapper {
    private final SessionResponse55 sessResponse55;

    ResponseWrapper(HttpServletResponse  delegate, SessionResponse55 sessResponse55) {
      super(delegate);
      this.sessResponse55 = sessResponse55;
    }

    public boolean isCommitted() {
      return sessResponse55.isCommitted();
    }

    public ServletResponse getResponse() {
      return sessResponse55;
    }

  }

}
