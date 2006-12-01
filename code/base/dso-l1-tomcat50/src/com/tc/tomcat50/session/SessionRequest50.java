/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.tomcat50.session;

import org.apache.catalina.Connector;
import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.HttpRequest;
import org.apache.catalina.Response;
import org.apache.catalina.ValveContext;
import org.apache.catalina.Wrapper;
import org.apache.coyote.tomcat5.CoyoteRequest;
import org.apache.coyote.tomcat5.CoyoteResponse;
import org.apache.tomcat.util.buf.MessageBytes;

import com.terracotta.session.SessionId;
import com.terracotta.session.SessionRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.security.Principal;
import java.util.Iterator;
import java.util.Locale;

import javax.servlet.FilterChain;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;

public class SessionRequest50 extends SessionRequest implements HttpRequest {

  private final CoyoteRequest req;
  private SessionResponse50   sessRes50;

  public SessionRequest50(SessionId requestedSessionId, CoyoteRequest req,
                          CoyoteResponse res) {
    super(requestedSessionId, req, res);
    this.req = req;
  }

  public ServletRequest getRequest() {
    return this;
  }

  public void setSessionResposne50(SessionResponse50 sessRes50) {
    this.sessRes50 = sessRes50;
  }

  public Response getResponse() {
    return sessRes50;
  }

  // ///////////////////////////////////////////////
  // the rest is just delegates...
  public void addCookie(Cookie cookie) {
    req.addCookie(cookie);
  }

  public void addHeader(String s, String s1) {
    req.addHeader(s, s1);
  }

  public void addLocale(Locale locale) {
    req.addLocale(locale);
  }

  public void addParameter(String s, String[] as) {
    req.addParameter(s, as);
  }

  public void clearCookies() {
    req.clearCookies();
  }

  public void clearHeaders() {
    req.clearHeaders();
  }

  public void clearLocales() {
    req.clearLocales();
  }

  public void clearParameters() {
    req.clearParameters();
  }

  public ServletInputStream createInputStream() throws IOException {
    return req.createInputStream();
  }

  public void finishRequest() throws IOException {
    req.finishRequest();
  }

  public String getAuthorization() {
    return req.getAuthorization();
  }

  public Connector getConnector() {
    return req.getConnector();
  }

  public Context getContext() {
    return req.getContext();
  }

  public MessageBytes getContextPathMB() {
    return req.getContextPathMB();
  }

  public String getDecodedRequestURI() {
    return req.getDecodedRequestURI();
  }

  public MessageBytes getDecodedRequestURIMB() {
    return req.getDecodedRequestURIMB();
  }

  public FilterChain getFilterChain() {
    return req.getFilterChain();
  }

  public Host getHost() {
    return req.getHost();
  }

  public String getInfo() {
    return req.getInfo();
  }

  public Object getNote(String s) {
    return req.getNote(s);
  }

  public Iterator getNoteNames() {
    return req.getNoteNames();
  }

  public MessageBytes getPathInfoMB() {
    return req.getPathInfoMB();
  }

  public MessageBytes getRequestPathMB() {
    return req.getRequestPathMB();
  }

  public MessageBytes getServletPathMB() {
    return req.getServletPathMB();
  }

  public Socket getSocket() {
    return req.getSocket();
  }

  public InputStream getStream() {
    return req.getStream();
  }

  public ValveContext getValveContext() {
    return req.getValveContext();
  }

  public Wrapper getWrapper() {
    return req.getWrapper();
  }

  public void recycle() {
    req.recycle();
  }

  public void removeNote(String s) {
    req.removeNote(s);
  }

  public void setAuthorization(String s) {
    req.setAuthorization(s);
  }

  public void setAuthType(String s) {
    req.setAuthType(s);
  }

  public void setConnector(Connector connector) {
    req.setConnector(connector);
  }

  public void setContentLength(int i) {
    req.setContentLength(i);
  }

  public void setContentType(String s) {
    req.setContentType(s);
  }

  public void setContext(Context context) {
    req.setContext(context);
  }

  public void setContextPath(String s) {
    req.setContextPath(s);
  }

  public void setDecodedRequestURI(String s) {
    req.setDecodedRequestURI(s);
  }

  public void setFilterChain(FilterChain filterchain) {
    req.setFilterChain(filterchain);
  }

  public void setHost(Host host) {
    req.setHost(host);
  }

  public void setMethod(String s) {
    req.setMethod(s);
  }

  public void setNote(String s, Object obj) {
    req.setNote(s, obj);
  }

  public void setPathInfo(String s) {
    req.setPathInfo(s);
  }

  public void setProtocol(String s) {
    req.setProtocol(s);
  }

  public void setQueryString(String s) {
    req.setQueryString(s);
  }

  public void setRemoteAddr(String s) {
    req.setRemoteAddr(s);
  }

  public void setRequestedSessionCookie(boolean flag) {
    req.setRequestedSessionCookie(flag);
  }

  public void setRequestedSessionId(String s) {
    req.setRequestedSessionId(s);
  }

  public void setRequestedSessionURL(boolean flag) {
    req.setRequestedSessionURL(flag);
  }

  public void setRequestURI(String s) {
    req.setRequestURI(s);
  }

  public void setResponse(Response response) {
    req.setResponse(response);
  }

  public void setScheme(String s) {
    req.setScheme(s);
  }

  public void setSecure(boolean flag) {
    req.setSecure(flag);
  }

  public void setServerName(String s) {
    req.setServerName(s);
  }

  public void setServerPort(int i) {
    req.setServerPort(i);
  }

  public void setServletPath(String s) {
    req.setServletPath(s);
  }

  public void setSocket(Socket socket) {
    req.setSocket(socket);
  }

  public void setStream(InputStream inputstream) {
    req.setStream(inputstream);
  }

  public void setUserPrincipal(Principal principal) {
    req.setUserPrincipal(principal);
  }

  public void setValveContext(ValveContext valvecontext) {
    req.setValveContext(valvecontext);
  }

  public void setWrapper(Wrapper wrapper) {
    req.setWrapper(wrapper);
  }

}
