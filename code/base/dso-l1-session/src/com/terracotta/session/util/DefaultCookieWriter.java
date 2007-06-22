/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.session.util;

import com.terracotta.session.SessionId;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class DefaultCookieWriter implements SessionCookieWriter {

  protected static final int HTTP_PORT  = 80;
  protected static final int HTTPS_PORT = 443;

  protected final String     cookieName;
  protected final String     idTag;
  private final boolean      isTrackingEnabled;
  private final boolean      isCookieEnabled;
  private final boolean      isUrlRewriteEnabled;
  private final String       cookieDomain;
  private final String       cookiePath;
  private final String       cookieComment;
  private final int          cookieMaxAge;
  private final boolean      isCookieSecure;

  public static DefaultCookieWriter makeInstance(ConfigProperties cp) {
    Assert.pre(cp != null);
    return new DefaultCookieWriter(cp.getSessionTrackingEnabled(), cp.getCookiesEnabled(), cp.getUrlRewritingEnabled(),
                                   cp.getCookieName(), cp.getCookieDomain(), cp.getCookiePath(),
                                   cp.getCookieCoomment(), cp.getCookieMaxAgeSeconds(), cp.getCookieSecure());
  }

  protected DefaultCookieWriter(boolean isTrackingEnabled, boolean isCookieEnabled, boolean isUrlRewriteEnabled,
                                String cookieName, String cookieDomain, String cookiePath, String cookieComment,
                                int cookieMaxAge, boolean isCookieSecure) {
    Assert.pre(cookieName != null && cookieName.trim().length() > 0);
    Assert.pre(cookieMaxAge >= -1);
    this.isTrackingEnabled = isTrackingEnabled;
    this.isCookieEnabled = isCookieEnabled;
    this.isUrlRewriteEnabled = isUrlRewriteEnabled;
    this.cookieName = cookieName;
    this.cookiePath = cookiePath;
    this.cookieDomain = cookieDomain;
    this.cookieComment = cookieComment;
    this.cookieMaxAge = cookieMaxAge;
    this.isCookieSecure = isCookieSecure;
    this.idTag = ";" + this.cookieName.toLowerCase() + "=";
  }

  public void writeCookie(HttpServletRequest req, HttpServletResponse res, SessionId id) {
    Assert.pre(req != null);
    Assert.pre(res != null);
    Assert.pre(id != null);

    if (res.isCommitted()) { throw new IllegalStateException("response is already committed"); }

    if (isTrackingEnabled && isCookieEnabled) res.addCookie(createCookie(req, id));
  }

  public String encodeRedirectURL(String url, HttpServletRequest req) {
    Assert.pre(req != null);

    if (url == null || !isTrackingEnabled || !isUrlRewriteEnabled) return url;
    final String absolute = toAbsolute(url, req);
    if (isEncodeable(absolute, req)) {
      return toEncoded(url, req.getSession().getId(), idTag);
    } else {
      return url;
    }
  }

  public String encodeURL(String url, HttpServletRequest req) {
    Assert.pre(req != null);

    if (url == null || !isTrackingEnabled || !isUrlRewriteEnabled) return url;
    String absolute = toAbsolute(url, req);
    if (isEncodeable(absolute, req)) {
      // W3c spec clearly said
      if (url.equalsIgnoreCase("")) {
        url = absolute;
      }
      return toEncoded(url, req.getSession().getId(), idTag);
    } else {
      return url;
    }
  }

  private static String toEncoded(final String url, final String sessionId, final String idTag) {
    Assert.pre(idTag != null);

    if ((url == null) || (sessionId == null)) return url;

    String path = url;
    String query = "";
    String anchor = "";
    int question = url.indexOf('?');
    if (question >= 0) {
      path = url.substring(0, question);
      query = url.substring(question);
    }
    int pound = path.indexOf('#');
    if (pound >= 0) {
      anchor = path.substring(pound);
      path = path.substring(0, pound);
    }
    StringBuffer sb = new StringBuffer(path);
    if (sb.length() > 0) { // jsessionid can't be first.
      sb.append(idTag);
      sb.append(sessionId);
    }
    sb.append(anchor);
    sb.append(query);
    return sb.toString();
  }

  protected static boolean isEncodeable(final String location, final HttpServletRequest hreq) {

    Assert.pre(hreq != null);

    if (location == null) return false;

    // Is this an intra-document reference?
    if (location.startsWith("#")) return false;

    final HttpSession session = hreq.getSession(false);
    if (session == null) return false;
    if (hreq.isRequestedSessionIdFromCookie()) return false;

    return isEncodeable(hreq, session, location);
  }

  private static boolean isEncodeable(HttpServletRequest hreq, HttpSession session, String location) {
    Assert.pre(hreq != null);
    Assert.pre(session != null);

    // Is this a valid absolute URL?
    URL url = null;
    try {
      url = new URL(location);
    } catch (MalformedURLException e) {
      return false;
    }

    // Does this URL match down to (and including) the context path?
    if (!hreq.getScheme().equalsIgnoreCase(url.getProtocol())) return false;
    if (!hreq.getServerName().equalsIgnoreCase(url.getHost())) return false;
    final int serverPort = getPort(hreq);
    int urlPort = getPort(url);
    if (serverPort != urlPort) return false;

    String contextPath = hreq.getContextPath();
    if (contextPath != null) {
      String file = url.getFile();
      if ((file == null) || !file.startsWith(contextPath)) return false;
      if (file.indexOf(";jsessionid=" + session.getId()) >= 0) return false;
    }

    // This URL belongs to our web application, so it is encodeable
    return true;

  }

  private static int getPort(URL url) {
    Assert.pre(url != null);
    return getPort(url.getPort(), url.getProtocol());
  }

  private static int getPort(HttpServletRequest hreq) {
    Assert.pre(hreq != null);
    return getPort(hreq.getServerPort(), hreq.getScheme());
  }

  private static int getPort(final int port, final String scheme) {
    if (port == -1) {
      if ("https".equals(scheme)) return 443;
      else return 80;
    }
    return port;
  }

  private static String toAbsolute(String location, HttpServletRequest request) {
    Assert.pre(request != null);
    if (location == null) return location;

    final boolean leadingSlash = location.startsWith("/");

    if (leadingSlash || (!leadingSlash && (location.indexOf("://") == -1))) {
      final StringBuffer sb = new StringBuffer();

      final String scheme = request.getScheme();
      final String name = request.getServerName();
      final int port = request.getServerPort();

      sb.append(scheme);
      sb.append("://");
      sb.append(name);
      sb.append(getPortString(scheme, port));
      if (!leadingSlash) {
        final String relativePath = request.getRequestURI();
        final int pos = relativePath.lastIndexOf('/');
        final String frelativePath = relativePath.substring(0, pos);
        sb.append(encodeSafely(frelativePath));
        sb.append('/');
      }
      sb.append(location);
      return sb.toString();
    } else {
      return location;
    }
  }

  private static String getPortString(final String scheme, final int port) {
    if (("http".equals(scheme) && port != 80) || ("https".equals(scheme) && port != 443)) {
      return ":" + port;
    } else {
      return "";
    }
  }

  private static String encodeSafely(final String source) {
    try {
      return URLEncoder.encode(source, "UTF-8");
    } catch (IOException e) {
      return source;
    }
  }

  protected Cookie createCookie(HttpServletRequest req, SessionId id) {
    Assert.pre(req != null);
    Assert.pre(id != null);

    Cookie c = new Cookie(cookieName, id.getExternalId());
    c.setPath(getCookiePath(req));
    c.setMaxAge(cookieMaxAge);
    c.setSecure(isCookieSecure);
    if (cookieDomain != null) c.setDomain(cookieDomain);
    if (cookieComment != null) c.setComment(cookieComment);

    Assert.post(c != null);
    return c;
  }

  protected String getCookiePath(HttpServletRequest req) {
    Assert.pre(req != null);
    if (cookiePath == null) {
      // if nothing is specified, use request context path
      String rv = req.getContextPath();
      return rv == null || rv.trim().length() == 0 ? ConfigProperties.defaultCookiePath : rv.trim();
    } else {
      return cookiePath;
    }
  }
}
