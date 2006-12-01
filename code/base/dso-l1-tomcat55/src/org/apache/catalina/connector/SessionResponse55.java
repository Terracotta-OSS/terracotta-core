/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.apache.catalina.connector;

import com.terracotta.session.SessionResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;

public class SessionResponse55 extends Response {
  private final Response         valveRes;
  private final SessionRequest55 sessReq;
  private final SessionResponse  sessRes;

  public SessionResponse55(Response valveRes, SessionRequest55 sessReq, SessionResponse sessRes) {
    this.valveRes = valveRes;
    this.sessRes = sessRes;
    this.sessReq = sessReq;
  }

  // //////////////////////////////////////////
  // url encoding methods -- START
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

  // url encoding methods -- STOP
  // //////////////////////////////////////////

  public void addCookie(Cookie arg0) {
    valveRes.addCookie(arg0);
  }

  public void addDateHeader(String arg0, long arg1) {
    valveRes.addDateHeader(arg0, arg1);
  }

  public void addHeader(String arg0, String arg1) {
    valveRes.addHeader(arg0, arg1);
  }

  public void addIntHeader(String arg0, int arg1) {
    valveRes.addIntHeader(arg0, arg1);
  }

  public boolean containsHeader(String arg0) {
    return valveRes.containsHeader(arg0);
  }

  public boolean equals(Object arg0) {
    return valveRes.equals(arg0);
  }

  public void flushBuffer() throws IOException {
    valveRes.flushBuffer();
  }

  public int getBufferSize() {
    return valveRes.getBufferSize();
  }

  public String getCharacterEncoding() {
    return valveRes.getCharacterEncoding();
  }

  public String getContentType() {
    return valveRes.getContentType();
  }

  public Locale getLocale() {
    return valveRes.getLocale();
  }

  public ServletOutputStream getOutputStream() throws IOException {
    return valveRes.getOutputStream();
  }

  public PrintWriter getWriter() throws IOException {
    return valveRes.getWriter();
  }

  public int hashCode() {
    return valveRes.hashCode();
  }

  public boolean isCommitted() {
    return valveRes.isCommitted();
  }

  public void reset() {
    valveRes.reset();
  }

  public void resetBuffer() {
    valveRes.resetBuffer();
  }

  public void sendError(int arg0, String arg1) throws IOException {
    valveRes.sendError(arg0, arg1);
  }

  public void sendError(int arg0) throws IOException {
    valveRes.sendError(arg0);
  }

  public void sendRedirect(String arg0) throws IOException {
    valveRes.sendRedirect(arg0);
  }

  public void setBufferSize(int arg0) {
    valveRes.setBufferSize(arg0);
  }

  public void setCharacterEncoding(String arg0) {
    valveRes.setCharacterEncoding(arg0);
  }

  public void setContentLength(int arg0) {
    valveRes.setContentLength(arg0);
  }

  public void setContentType(String arg0) {
    valveRes.setContentType(arg0);
  }

  public void setDateHeader(String arg0, long arg1) {
    valveRes.setDateHeader(arg0, arg1);
  }

  public void setHeader(String arg0, String arg1) {
    valveRes.setHeader(arg0, arg1);
  }

  public void setIntHeader(String arg0, int arg1) {
    valveRes.setIntHeader(arg0, arg1);
  }

  public void setLocale(Locale arg0) {
    valveRes.setLocale(arg0);
  }

  public void setStatus(int arg0, String arg1) {
    valveRes.setStatus(arg0, arg1);
  }

  public void setStatus(int arg0) {
    valveRes.setStatus(arg0);
  }

  public String toString() {
    return valveRes.toString();
  }

  public ServletOutputStream createOutputStream() throws IOException {
    return valveRes.createOutputStream();
  }

  public void finishResponse() throws IOException {
    valveRes.finishResponse();
  }

  public Connector getConnector() {
    return valveRes.getConnector();
  }

  public int getContentCount() {
    return valveRes.getContentCount();
  }

  public int getContentLength() {
    return valveRes.getContentLength();
  }

  public org.apache.catalina.Context getContext() {
    return valveRes.getContext();
  }

  public Cookie[] getCookies() {
    return valveRes.getCookies();
  }

  public String getHeader(String name) {
    return valveRes.getHeader(name);
  }

  public String[] getHeaderNames() {
    return valveRes.getHeaderNames();
  }

  public String[] getHeaderValues(String name) {
    return valveRes.getHeaderValues(name);
  }

  public boolean getIncluded() {
    return valveRes.getIncluded();
  }

  public String getInfo() {
    return valveRes.getInfo();
  }

  public String getMessage() {
    return valveRes.getMessage();
  }

  public PrintWriter getReporter() throws IOException {
    return valveRes.getReporter();
  }

  public Request getRequest() {
    return this.sessReq;
  }

  public javax.servlet.http.HttpServletResponse getResponse() {
    return this.sessRes;
  }

  public int getStatus() {
    return valveRes.getStatus();
  }

  public java.io.OutputStream getStream() {
    return valveRes.getStream();
  }

  public boolean isAppCommitted() {
    return valveRes.isAppCommitted();
  }

  protected boolean isEncodeable(String location) {
    return valveRes.isEncodeable(location);
  }

  public boolean isError() {
    return valveRes.isError();
  }

  public boolean isSuspended() {
    return valveRes.isSuspended();
  }

  public void recycle() {
    valveRes.recycle();
  }

  public void reset(int status, String message) {
    valveRes.reset(status, message);
  }

  public void sendAcknowledgement() throws IOException {
    valveRes.sendAcknowledgement();
  }

  public void setAppCommitted(boolean appCommitted) {
    valveRes.setAppCommitted(appCommitted);
  }

  public void setConnector(Connector connector) {
    valveRes.setConnector(connector);
  }

  public void setContext(org.apache.catalina.Context context) {
    valveRes.setContext(context);
  }

  public void setError() {
    valveRes.setError();
  }

  public void setIncluded(boolean included) {
    valveRes.setIncluded(included);
  }

  public void setRequest(Request request) {
    valveRes.setRequest(request);
  }

  public void setStream(java.io.OutputStream outputstream) {
    valveRes.setStream(outputstream);
  }

  public void setSuspended(boolean suspended) {
    valveRes.setSuspended(suspended);
  }

}
