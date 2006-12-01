/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.terracotta.session;

import com.terracotta.session.util.Assert;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class SessionResponse extends HttpServletResponseWrapper implements TerracottaResponse {

  private final TerracottaRequest req;

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
}
