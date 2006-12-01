/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package javax.servlet.http;

import com.tc.exception.ImplementMe;

import java.io.BufferedReader;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;

public class MockHttpServletRequest implements HttpServletRequest {

  private final String contextPath;
  private String       requestUrl;
  private HttpSession  session;
  private String       requestedSessionId;
  private boolean      sidFromCookie;
  private boolean      sidValid;
  private String       scheme;
  private String       serverName;
  private int          serverPort;

  public MockHttpServletRequest() {
    this.contextPath = "/";
  }

  public MockHttpServletRequest(String contextPath, String requestedSessionId) {
    this.contextPath = contextPath;
    this.requestedSessionId = requestedSessionId;
  }

  public void setSession(HttpSession session) {
    this.session = session;
  }

  public void setRequestedSessionId(String requestedSessionId) {
    this.requestedSessionId = requestedSessionId;
  }

  public void setRequestUrl(String requestUrl) {
    this.requestUrl = requestUrl;
  }

  public void setSidFromCookie(boolean sidFromCookie) {
    this.sidFromCookie = sidFromCookie;
  }

  public void setSidValid(boolean sidValid) {
    this.sidValid = sidValid;
  }

  public void setScheme(String scheme) {
    this.scheme = scheme;
  }

  public void setServerName(String serverName) {
    this.serverName = serverName;
  }

  public void setServerPort(int serverPort) {
    this.serverPort = serverPort;
  }

  public String getAuthType() {
    throw new ImplementMe();
  }

  public String getContextPath() {
    return contextPath;
  }

  public Cookie[] getCookies() {
    throw new ImplementMe();
  }

  public long getDateHeader(String arg0) {
    throw new ImplementMe();
  }

  public String getHeader(String arg0) {
    throw new ImplementMe();
  }

  public Enumeration getHeaderNames() {
    throw new ImplementMe();
  }

  public Enumeration getHeaders(String arg0) {
    throw new ImplementMe();
  }

  public int getIntHeader(String arg0) {
    throw new ImplementMe();
  }

  public String getMethod() {
    throw new ImplementMe();
  }

  public String getPathInfo() {
    throw new ImplementMe();
  }

  public String getPathTranslated() {
    throw new ImplementMe();
  }

  public String getQueryString() {
    throw new ImplementMe();
  }

  public String getRemoteUser() {
    throw new ImplementMe();
  }

  public String getRequestURI() {
    throw new ImplementMe();
  }

  public StringBuffer getRequestURL() {
    return new StringBuffer(requestUrl);
  }

  public String getRequestedSessionId() {
    throw new ImplementMe();
  }

  public String getServletPath() {
    throw new ImplementMe();
  }

  public HttpSession getSession() {
    return getSession(true);
  }

  public HttpSession getSession(boolean create) {
    if (!create && requestedSessionId == null) return session;
    if (session == null) session = new MockHttpSession(requestedSessionId);
    return session;
  }

  public Principal getUserPrincipal() {
    throw new ImplementMe();
  }

  public boolean isRequestedSessionIdFromCookie() {
    return sidFromCookie;
  }

  public boolean isRequestedSessionIdFromURL() {
    return !sidFromCookie;
  }

  public boolean isRequestedSessionIdFromUrl() {
    return isRequestedSessionIdFromURL();
  }

  public boolean isRequestedSessionIdValid() {
    return sidValid;
  }

  public boolean isUserInRole(String arg0) {
    throw new ImplementMe();
  }

  public Object getAttribute(String arg0) {
    throw new ImplementMe();
  }

  public Enumeration getAttributeNames() {
    throw new ImplementMe();
  }

  public String getCharacterEncoding() {
    throw new ImplementMe();
  }

  public int getContentLength() {
    throw new ImplementMe();
  }

  public String getContentType() {
    throw new ImplementMe();
  }

  public ServletInputStream getInputStream() {
    throw new ImplementMe();
  }

  public String getLocalAddr() {
    throw new ImplementMe();
  }

  public String getLocalName() {
    throw new ImplementMe();
  }

  public int getLocalPort() {
    throw new ImplementMe();
  }

  public Locale getLocale() {
    throw new ImplementMe();
  }

  public Enumeration getLocales() {
    throw new ImplementMe();
  }

  public String getParameter(String arg0) {
    throw new ImplementMe();
  }

  public Map getParameterMap() {
    throw new ImplementMe();
  }

  public Enumeration getParameterNames() {
    throw new ImplementMe();
  }

  public String[] getParameterValues(String arg0) {
    throw new ImplementMe();
  }

  public String getProtocol() {
    throw new ImplementMe();
  }

  public BufferedReader getReader() {
    throw new ImplementMe();
  }

  public String getRealPath(String arg0) {
    throw new ImplementMe();
  }

  public String getRemoteAddr() {
    throw new ImplementMe();
  }

  public String getRemoteHost() {
    throw new ImplementMe();
  }

  public int getRemotePort() {
    throw new ImplementMe();
  }

  public RequestDispatcher getRequestDispatcher(String arg0) {
    throw new ImplementMe();
  }

  public String getScheme() {
    return scheme;
  }

  public String getServerName() {
    return serverName;
  }

  public int getServerPort() {
    return serverPort;
  }

  public boolean isSecure() {
    throw new ImplementMe();
  }

  public void removeAttribute(String arg0) {
    throw new ImplementMe();

  }

  public void setAttribute(String arg0, Object arg1) {
    throw new ImplementMe();

  }

  public void setCharacterEncoding(String arg0) {
    throw new ImplementMe();

  }

}
