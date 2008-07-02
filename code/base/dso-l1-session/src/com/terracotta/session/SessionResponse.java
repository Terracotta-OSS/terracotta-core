/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.session;

import com.terracotta.session.util.Assert;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class SessionResponse extends HttpServletResponseWrapper implements TerracottaResponse {

  private final TerracottaRequest req;
  private boolean                 isCommitted = false;

  public SessionResponse(TerracottaRequest req, HttpServletResponse res) {
    super(res);
    Assert.pre(req != null);
    Assert.pre(res != null);

    this.req = req;
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

  public boolean isCommitted() {
    return isCommitted || super.isCommitted();
  }

  public void sendError(int sc) throws IOException {
    checkCommitted();
    super.sendError(sc);
    isCommitted = true;
  }

  public void sendError(int sc, String msg) throws IOException {
    checkCommitted();
    super.sendError(sc, msg);
    isCommitted = true;
  }

  public void sendRedirect(String location) throws IOException {
    checkCommitted();
    super.sendRedirect(location);
    isCommitted = true;
  }

  private void checkCommitted() {
    if (isCommitted()) { throw new IllegalStateException("response already committed"); }
  }

}
