/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.tomcat50.session;

import org.apache.catalina.Connector;
import org.apache.catalina.Context;
import org.apache.catalina.HttpResponse;
import org.apache.catalina.Request;
import org.apache.coyote.tomcat5.CoyoteResponse;

import com.terracotta.session.SessionResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;

public class SessionResponse50 extends SessionResponse implements HttpResponse {

  private final CoyoteResponse   valveRes;
  private final SessionRequest50 sessReq;

  public SessionResponse50(final CoyoteResponse valveRes, final SessionRequest50 sessReq) {
    super(sessReq, valveRes);
    this.valveRes = valveRes;
    this.sessReq = sessReq;
  }

  public Request getRequest() {
    return sessReq;
  }

  public ServletResponse getResponse() {
    return this;
  }

  // /////////////////////////////////////////////
  // the rest is just delegates...
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

  public Context getContext() {
    return valveRes.getContext();
  }

  public Cookie[] getCookies() {
    return valveRes.getCookies();
  }

  public String getHeader(String s) {
    return valveRes.getHeader(s);
  }

  public String[] getHeaderNames() {
    return valveRes.getHeaderNames();
  }

  public String[] getHeaderValues(String s) {
    return valveRes.getHeaderValues(s);
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

  public int getStatus() {
    return valveRes.getStatus();
  }

  public OutputStream getStream() {
    return valveRes.getStream();
  }

  public boolean isAppCommitted() {
    return valveRes.isAppCommitted();
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

  public void reset(int i, String s) {
    valveRes.reset(i, s);
  }

  public void sendAcknowledgement() throws IOException {
    valveRes.sendAcknowledgement();
  }

  public void setAppCommitted(boolean flag) {
    valveRes.setAppCommitted(flag);
  }

  public void setConnector(Connector connector) {
    valveRes.setConnector(connector);
  }

  public void setContext(Context context) {
    valveRes.setContext(context);
  }

  public void setError() {
    valveRes.setError();
  }

  public void setRequest(Request request) {
    valveRes.setRequest(request);
  }

  public void setStream(OutputStream outputstream) {
    valveRes.setStream(outputstream);
  }

  public void setSuspended(boolean flag) {
    valveRes.setSuspended(flag);
  }

  public String toString() {
    return valveRes.toString();
  }

  public void setIncluded(boolean flag) {
    valveRes.setIncluded(flag);
  }

}
