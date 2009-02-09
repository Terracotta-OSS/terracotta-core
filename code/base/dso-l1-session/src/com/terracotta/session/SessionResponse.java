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

  public SessionResponse(final TerracottaRequest req, final HttpServletResponse res) {
    super(res);
    Assert.pre(req != null);
    Assert.pre(res != null);

    this.req = req;
  }

  @Override
  public String encodeRedirectUrl(final String url) {
    return encodeRedirectURL(url);
  }

  @Override
  public String encodeUrl(final String url) {
    return encodeURL(url);
  }

  @Override
  public String encodeRedirectURL(final String url) {
    return req.encodeRedirectURL(url);
  }

  @Override
  public String encodeURL(final String url) {
    return req.encodeURL(url);
  }

  @Override
  public boolean isCommitted() {
    return isCommitted || super.isCommitted();
  }

  @Override
  public void sendRedirect(final String location) throws IOException {
    checkCommitted();
    super.sendRedirect(location);
    isCommitted = true;
  }

  private void checkCommitted() {
    if (isCommitted()) { throw new IllegalStateException("response already committed"); }
  }

}
