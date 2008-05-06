/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package weblogic.servlet.internal;

import com.tc.object.util.OverrideCheck;
import com.terracotta.session.TerracottaRequest;
import com.terracotta.session.TerracottaResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;

/**
 * The whole point of this class is to remain type compatible with weblogic's internal response types. There are places
 * in their container where the concrete type of the response needs to be instanceof compatible with their type. This
 * class also must reside in the weblogic package such that it can override and delegate package private methods. This
 * class is instrumented at runtime to override all non-private methods of ServletResponseImpl and delegate the
 * nativeResponse instance
 */
public final class TerracottaServletResponseImpl extends ServletResponseImpl implements TerracottaResponse {

  static {
    OverrideCheck.check(ServletResponseImpl.class, TerracottaServletResponseImpl.class);
  }

  private final TerracottaRequest   req;
  private final ServletResponseImpl nativeResponse;

  public TerracottaServletResponseImpl(TerracottaRequest req, ServletResponseImpl response) {
    this.req = req;
    this.nativeResponse = response;
  }

  public String encodeRedirectUrl(String url) {
    return encodeRedirectURL(url);
  }

  public String encodeUrl(String url) {
    return encodeURL(url);
  }

  public String encodeRedirectURL(final String url) {
    return req.encodeRedirectURL(url);
  }

  public String encodeURL(final String url) {
    return req.encodeURL(url);
  }

  public void addCookie(Cookie arg0) {
    nativeResponse.addCookie(arg0);
  }

  public void addDateHeader(String arg0, long arg1) {
    nativeResponse.addDateHeader(arg0, arg1);
  }

  public void addHeader(String arg0, String arg1) {
    nativeResponse.addHeader(arg0, arg1);
  }

  public void addIntHeader(String arg0, int arg1) {
    nativeResponse.addIntHeader(arg0, arg1);
  }

  public boolean containsHeader(String arg0) {
    return nativeResponse.containsHeader(arg0);
  }

  public void flushBuffer() throws IOException {
    nativeResponse.flushBuffer();
  }

  public int getBufferSize() {
    return nativeResponse.getBufferSize();
  }

  public String getCharacterEncoding() {
    return nativeResponse.getCharacterEncoding();
  }

  public String getContentType() {
    return nativeResponse.getContentType();
  }

  public Locale getLocale() {
    return nativeResponse.getLocale();
  }

  public ServletOutputStream getOutputStream() throws IOException {
    return nativeResponse.getOutputStream();
  }

  public PrintWriter getWriter() throws IOException {
    return nativeResponse.getWriter();
  }

  public boolean isCommitted() {
    return nativeResponse.isCommitted();
  }

  public void reset() {
    nativeResponse.reset();
  }

  public void resetBuffer() {
    nativeResponse.resetBuffer();
  }

  public void sendError(int arg0, String arg1) throws IOException {
    nativeResponse.sendError(arg0, arg1);
  }

  public void sendError(int arg0) throws IOException {
    nativeResponse.sendError(arg0);
  }

  public void sendRedirect(String arg0) throws IOException {
    nativeResponse.sendRedirect(arg0);
  }

  public void setBufferSize(int arg0) {
    nativeResponse.setBufferSize(arg0);
  }

  public void setCharacterEncoding(String arg0) {
    nativeResponse.setCharacterEncoding(arg0);
  }

  public void setContentLength(int arg0) {
    nativeResponse.setContentLength(arg0);
  }

  public void setContentType(String arg0) {
    nativeResponse.setContentType(arg0);
  }

  public void setDateHeader(String arg0, long arg1) {
    nativeResponse.setDateHeader(arg0, arg1);
  }

  public void setHeader(String arg0, String arg1) {
    nativeResponse.setHeader(arg0, arg1);
  }

  public void setIntHeader(String arg0, int arg1) {
    nativeResponse.setIntHeader(arg0, arg1);
  }

  public void setLocale(Locale arg0) {
    nativeResponse.setLocale(arg0);
  }

  public void setStatus(int arg0, String arg1) {
    nativeResponse.setStatus(arg0, arg1);
  }

  public void setStatus(int arg0) {
    nativeResponse.setStatus(arg0);
  }

}
