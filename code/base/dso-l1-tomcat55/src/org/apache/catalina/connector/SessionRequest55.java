/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.apache.catalina.connector;

import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.Session;
import org.apache.catalina.Wrapper;
import org.apache.tomcat.util.buf.B2CConverter;
import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.tomcat.util.http.mapper.MappingData;

import com.terracotta.session.TerracottaRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class SessionRequest55 extends Request {
  private final Request        valveReq;
  private final TerracottaRequest sessionReq;
  private SessionResponse55    sessionRes;

  /**
   * @param sessionReq SessionRequest must be a HttpServletRequest slice of Request
   * @param valveReq
   */
  public SessionRequest55(TerracottaRequest sessionReq, Request valveReq) {
    this.valveReq = valveReq;
    this.sessionReq = sessionReq;
  }

  public void setSessionResponse(SessionResponse55 sessionRes) {
    this.sessionRes = sessionRes;
  }

  // ////////////////////////////////////////////////////////
  // ------------ Interesting methods START ------------
  // ////////////////////////////////////////////////////////
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
    throw new UnsupportedOperationException();
  }

  // ////////////////////////////////////////////////////////
  // ------------ Interesting methods END ------------
  // ////////////////////////////////////////////////////////

  // /////////////////////////////////////////////////////////
  // the rest should be just delegating methods...
  public boolean equals(Object obj) {
    return valveReq.equals(obj);
  }

  public Object getAttribute(String arg0) {
    return valveReq.getAttribute(arg0);
  }

  public Enumeration getAttributeNames() {
    return valveReq.getAttributeNames();
  }

  public String getAuthType() {
    return valveReq.getAuthType();
  }

  public String getCharacterEncoding() {
    return valveReq.getCharacterEncoding();
  }

  public int getContentLength() {
    return valveReq.getContentLength();
  }

  public String getContentType() {
    return valveReq.getContentType();
  }

  public String getContextPath() {
    return valveReq.getContextPath();
  }

  public Cookie[] getCookies() {
    return valveReq.getCookies();
  }

  public long getDateHeader(String arg0) {
    return valveReq.getDateHeader(arg0);
  }

  public String getHeader(String arg0) {
    return valveReq.getHeader(arg0);
  }

  public Enumeration getHeaderNames() {
    return valveReq.getHeaderNames();
  }

  public Enumeration getHeaders(String arg0) {
    return valveReq.getHeaders(arg0);
  }

  public ServletInputStream getInputStream() throws IOException {
    return valveReq.getInputStream();
  }

  public int getIntHeader(String arg0) {
    return valveReq.getIntHeader(arg0);
  }

  public String getLocalAddr() {
    return valveReq.getLocalAddr();
  }

  public Locale getLocale() {
    return valveReq.getLocale();
  }

  public Enumeration getLocales() {
    return valveReq.getLocales();
  }

  public String getLocalName() {
    return valveReq.getLocalName();
  }

  public int getLocalPort() {
    return valveReq.getLocalPort();
  }

  public String getMethod() {
    return valveReq.getMethod();
  }

  public String getParameter(String arg0) {
    return valveReq.getParameter(arg0);
  }

  public Map getParameterMap() {
    return valveReq.getParameterMap();
  }

  public Enumeration getParameterNames() {
    return valveReq.getParameterNames();
  }

  public String[] getParameterValues(String arg0) {
    return valveReq.getParameterValues(arg0);
  }

  public String getPathInfo() {
    return valveReq.getPathInfo();
  }

  public String getPathTranslated() {
    return valveReq.getPathTranslated();
  }

  public String getProtocol() {
    return valveReq.getProtocol();
  }

  public String getQueryString() {
    return valveReq.getQueryString();
  }

  public BufferedReader getReader() throws IOException {
    return valveReq.getReader();
  }

  public String getRealPath(String arg0) {
    return valveReq.getRealPath(arg0);
  }

  public String getRemoteAddr() {
    return valveReq.getRemoteAddr();
  }

  public String getRemoteHost() {
    return valveReq.getRemoteHost();
  }

  public int getRemotePort() {
    return valveReq.getRemotePort();
  }

  public String getRemoteUser() {
    return valveReq.getRemoteUser();
  }

  public RequestDispatcher getRequestDispatcher(String arg0) {
    return valveReq.getRequestDispatcher(arg0);
  }

  public String getRequestURI() {
    return valveReq.getRequestURI();
  }

  public StringBuffer getRequestURL() {
    return valveReq.getRequestURL();
  }

  public String getScheme() {
    return valveReq.getScheme();
  }

  public String getServerName() {
    return valveReq.getServerName();
  }

  public int getServerPort() {
    return valveReq.getServerPort();
  }

  public String getServletPath() {
    return valveReq.getServletPath();
  }

  public Principal getUserPrincipal() {
    return valveReq.getUserPrincipal();
  }

  public int hashCode() {
    return valveReq.hashCode();
  }

  public boolean isSecure() {
    return valveReq.isSecure();
  }

  public boolean isUserInRole(String arg0) {
    return valveReq.isUserInRole(arg0);
  }

  public void removeAttribute(String arg0) {
    valveReq.removeAttribute(arg0);
  }

  public void setAttribute(String arg0, Object arg1) {
    valveReq.setAttribute(arg0, arg1);
  }

  public void setCharacterEncoding(String arg0) throws UnsupportedEncodingException {
    valveReq.setCharacterEncoding(arg0);
  }

  public void addCookie(Cookie cookie) {
    valveReq.addCookie(cookie);
  }

  public void addHeader(String s, String s1) {
    valveReq.addHeader(s, s1);
  }

  public void addLocale(Locale locale) {
    valveReq.addLocale(locale);
  }

  public void addParameter(String name, String[] values) {
    valveReq.addParameter(name, values);
  }

  public void clearCookies() {
    valveReq.clearCookies();
  }

  public void clearHeaders() {
    valveReq.clearHeaders();
  }

  public void clearLocales() {
    valveReq.clearLocales();
  }

  public void clearParameters() {
    valveReq.clearParameters();
  }

  protected void configureSessionCookie(Cookie cookie) {
    valveReq.configureSessionCookie(cookie);
  }

  public void finishRequest() throws IOException {
    valveReq.finishRequest();
  }

  public ServletInputStream createInputStream() throws IOException {
    return valveReq.createInputStream();
  }

  public Connector getConnector() {
    return valveReq.getConnector();
  }

  public Context getContext() {
    return valveReq.getContext();
  }

  public String getDecodedRequestURI() {
    return valveReq.getDecodedRequestURI();
  }

  public FilterChain getFilterChain() {
    return valveReq.getFilterChain();
  }

  public Host getHost() {
    return valveReq.getHost();
  }

  public String getInfo() {
    return valveReq.getInfo();
  }

  public Object getNote(String name) {
    return valveReq.getNote(name);
  }

  public java.util.Iterator getNoteNames() {
    return valveReq.getNoteNames();
  }

  public Principal getPrincipal() {
    return valveReq.getPrincipal();
  }

  public java.io.InputStream getStream() {
    return valveReq.getStream();
  }

  public Wrapper getWrapper() {
    return valveReq.getWrapper();
  }

  public void recycle() {
    valveReq.recycle();
  }

  public void removeNote(String name) {
    valveReq.removeNote(name);
  }

  public void setAuthType(String type) {
    valveReq.setAuthType(type);
  }

  public void setConnector(Connector connector) {
    valveReq.setConnector(connector);
  }

  public void setContentLength(int i) {
    valveReq.setContentLength(i);
  }

  public String toString() {
    return valveReq.toString();
  }

  public void setContentType(String s) {
    valveReq.setContentType(s);
  }

  public void setContext(Context context) {
    valveReq.setContext(context);
  }

  public void setContextPath(String path) {
    valveReq.setContextPath(path);
  }

  public void setCookies(Cookie[] cookies) {
    valveReq.setCookies(cookies);
  }

  public void setDecodedRequestURI(String s) {
    valveReq.setDecodedRequestURI(s);
  }

  public void setFilterChain(FilterChain filterChain) {
    valveReq.setFilterChain(filterChain);
  }

  public void setHost(Host host) {
    valveReq.setHost(host);
  }

  public void setMethod(String s) {
    valveReq.setMethod(s);
  }

  public void setNote(String name, Object value) {
    valveReq.setNote(name, value);
  }

  public void setPathInfo(String path) {
    valveReq.setPathInfo(path);
  }

  public void setProtocol(String s) {
    valveReq.setProtocol(s);
  }

  public void setQueryString(String s) {
    valveReq.setQueryString(s);
  }

  public void setRemoteAddr(String s) {
    valveReq.setRemoteAddr(s);
  }

  public void setRemoteHost(String s) {
    valveReq.setRemoteHost(s);
  }

  public void setRequestedSessionCookie(boolean flag) {
    valveReq.setRequestedSessionCookie(flag);
  }

  public void setRequestedSessionId(String id) {
    valveReq.setRequestedSessionId(id);
  }

  public void setRequestedSessionURL(boolean flag) {
    valveReq.setRequestedSessionURL(flag);
  }

  public void setRequestURI(String s) {
    valveReq.setRequestURI(s);
  }

  public void setResponse(Response response) {
    valveReq.setResponse(response);
  }

  public void setScheme(String s) {
    valveReq.setScheme(s);
  }

  public void setSecure(boolean secure) {
    valveReq.setSecure(secure);
  }

  public void setServerName(String name) {
    valveReq.setServerName(name);
  }

  public void setServerPort(int port) {
    valveReq.setServerPort(port);
  }

  public void setServletPath(String path) {
    valveReq.setServletPath(path);
  }

  public void setStream(InputStream inputstream) {
    valveReq.setStream(inputstream);
  }

  public void setUserPrincipal(Principal principal) {
    valveReq.setUserPrincipal(principal);
  }

  public void setWrapper(Wrapper wrapper) {
    valveReq.setWrapper(wrapper);
  }

  protected Session doGetSession(boolean create) {
    return valveReq.doGetSession(create);
  }

  protected void parseCookies() {
    valveReq.parseCookies();
  }

  protected void parseLocales() {
    valveReq.parseLocales();
  }

  protected void parseLocalesHeader(String value) {
    valveReq.parseLocalesHeader(value);
  }

  protected void parseParameters() {
    valveReq.parseParameters();
  }

  protected int readPostBody(byte[] body, int len) throws IOException {
    return valveReq.readPostBody(body, len);
  }

  public MessageBytes getContextPathMB() {
    return valveReq.getContextPathMB();
  }

  public org.apache.coyote.Request getCoyoteRequest() {
    return valveReq.getCoyoteRequest();
  }

  public MessageBytes getDecodedRequestURIMB() {
    return valveReq.getDecodedRequestURIMB();
  }

  public MappingData getMappingData() {
    return valveReq.getMappingData();
  }

  public MessageBytes getPathInfoMB() {
    return valveReq.getPathInfoMB();
  }

  public MessageBytes getRequestPathMB() {
    return valveReq.getRequestPathMB();
  }

  public MessageBytes getServletPathMB() {
    return valveReq.getServletPathMB();
  }

  protected B2CConverter getURIConverter() {
    return valveReq.getURIConverter();
  }

  public void setCoyoteRequest(org.apache.coyote.Request coyoteRequest) {
    valveReq.setCoyoteRequest(coyoteRequest);
  }

  protected void setURIConverter(B2CConverter URIConverter) {
    valveReq.setURIConverter(URIConverter);
  }

}
