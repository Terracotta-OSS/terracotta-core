/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.apache.catalina.connector;

import com.tc.object.util.OverrideCheck;
import com.terracotta.session.SessionResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

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
    return this.valveRes.isCommitted();
  }

  // This class delegates all HttpServletResponse methods to the delegate, except for isCommitted()
  private static class ResponseWrapper implements HttpServletResponse {
    private final HttpServletResponse  delegate;
    private final SessionResponse55 sessResponse55;

    ResponseWrapper(HttpServletResponse  delegate, SessionResponse55 sessResponse55) {
      this.delegate = delegate;
      this.sessResponse55 = sessResponse55;
    }

    public void addCookie(Cookie cookie) {
      delegate.addCookie(cookie);
    }

    public void addDateHeader(String name, long date) {
      delegate.addDateHeader(name, date);
    }

    public void addHeader(String name, String value) {
      delegate.addHeader(name, value);
    }

    public void addIntHeader(String name, int value) {
      delegate.addIntHeader(name, value);
    }

    public boolean containsHeader(String name) {
      return delegate.containsHeader(name);
    }

    public String encodeRedirectUrl(String url) {
      return delegate.encodeRedirectUrl(url);
    }

    public String encodeRedirectURL(String url) {
      return delegate.encodeRedirectURL(url);
    }

    public String encodeUrl(String url) {
      return delegate.encodeUrl(url);
    }

    public String encodeURL(String url) {
      return delegate.encodeURL(url);
    }

    public void flushBuffer() throws IOException {
      delegate.flushBuffer();
    }

    public int getBufferSize() {
      return delegate.getBufferSize();
    }

    public String getCharacterEncoding() {
      return delegate.getCharacterEncoding();
    }

    public String getContentType() {
      return delegate.getContentType();
    }

    public Locale getLocale() {
      return delegate.getLocale();
    }

    public ServletOutputStream getOutputStream() throws IOException {
      return delegate.getOutputStream();
    }

    public PrintWriter getWriter() throws IOException {
      return delegate.getWriter();
    }

    public boolean isCommitted() {
      return sessResponse55.isCommitted();
    }

    public void reset() {
      delegate.reset();
    }

    public void resetBuffer() {
      delegate.resetBuffer();
    }

    public void sendError(int sc, String msg) throws IOException {
      delegate.sendError(sc, msg);
    }

    public void sendError(int sc) throws IOException {
      delegate.sendError(sc);
    }

    public void sendRedirect(String location) throws IOException {
      delegate.sendRedirect(location);
    }

    public void setBufferSize(int size) {
      delegate.setBufferSize(size);
    }

    public void setCharacterEncoding(String charset) {
      delegate.setCharacterEncoding(charset);
    }

    public void setContentLength(int len) {
      delegate.setContentLength(len);
    }

    public void setContentType(String type) {
      delegate.setContentType(type);
    }

    public void setDateHeader(String name, long date) {
      delegate.setDateHeader(name, date);
    }

    public void setHeader(String name, String value) {
      delegate.setHeader(name, value);
    }

    public void setIntHeader(String name, int value) {
      delegate.setIntHeader(name, value);
    }

    public void setLocale(Locale loc) {
      delegate.setLocale(loc);
    }

    public void setStatus(int sc, String sm) {
      delegate.setStatus(sc, sm);
    }

    public void setStatus(int sc) {
      delegate.setStatus(sc);
    }



  }

}
