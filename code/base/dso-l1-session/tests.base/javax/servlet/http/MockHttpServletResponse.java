/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package javax.servlet.http;

import com.tc.exception.ImplementMe;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Locale;

import javax.servlet.ServletOutputStream;

public class MockHttpServletResponse implements HttpServletResponse {

  private ArrayList cookies   = new ArrayList();

  public void addCookie(Cookie arg0) {
    cookies.add(arg0);
  }

  public void addDateHeader(String arg0, long arg1) {
    throw new ImplementMe();

  }

  public void addHeader(String arg0, String arg1) {
    throw new ImplementMe();

  }

  public void addIntHeader(String arg0, int arg1) {
    throw new ImplementMe();

  }

  public boolean containsHeader(String arg0) {
    throw new ImplementMe();
  }

  public String encodeRedirectURL(String arg0) {
    throw new ImplementMe();
  }

  public String encodeRedirectUrl(String arg0) {
    throw new ImplementMe();
  }

  public String encodeURL(String arg0) {
    throw new ImplementMe();
  }

  public String encodeUrl(String arg0) {
    throw new ImplementMe();
  }

  public void sendError(int arg0) {
    throw new ImplementMe();

  }

  public void sendError(int arg0, String arg1) {
    throw new ImplementMe();

  }

  public void sendRedirect(String arg0) {
    throw new ImplementMe();

  }

  public void setDateHeader(String arg0, long arg1) {
    throw new ImplementMe();

  }

  public void setHeader(String arg0, String arg1) {
    throw new ImplementMe();

  }

  public void setIntHeader(String arg0, int arg1) {
    throw new ImplementMe();

  }

  public void setStatus(int arg0) {
    throw new ImplementMe();

  }

  public void setStatus(int arg0, String arg1) {
    throw new ImplementMe();

  }

  public void flushBuffer() {
    throw new ImplementMe();

  }

  public int getBufferSize() {
    throw new ImplementMe();
  }

  public String getCharacterEncoding() {
    throw new ImplementMe();
  }

  public String getContentType() {
    throw new ImplementMe();
  }

  public Locale getLocale() {
    throw new ImplementMe();
  }

  public ServletOutputStream getOutputStream() {
    throw new ImplementMe();
  }

  public PrintWriter getWriter() {
    throw new ImplementMe();
  }

  public boolean isCommitted() {
    throw new ImplementMe();
  }

  public void reset() {
    throw new ImplementMe();

  }

  public void resetBuffer() {
    throw new ImplementMe();

  }

  public void setBufferSize(int arg0) {
    throw new ImplementMe();

  }

  public void setCharacterEncoding(String arg0) {
    throw new ImplementMe();

  }

  public void setContentLength(int arg0) {
    throw new ImplementMe();

  }

  public void setContentType(String arg0) {
    throw new ImplementMe();

  }

  public void setLocale(Locale arg0) {
    throw new ImplementMe();

  }

  public Cookie[] getCookies() {
    return (Cookie[]) cookies.toArray(new Cookie[cookies.size()]);
  }

}
