/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.session;

import com.tc.object.bytecode.ManagerUtil;
import com.tc.util.ReflectiveProxy;
import com.terracotta.session.util.Assert;
import com.terracotta.session.util.ConfigProperties;
import com.terracotta.session.util.ContextMgr;
import com.terracotta.session.util.DefaultContextMgr;
import com.terracotta.session.util.DefaultCookieWriter;
import com.terracotta.session.util.DefaultIdGenerator;
import com.terracotta.session.util.DefaultLifecycleEventMgr;
import com.terracotta.session.util.LifecycleEventMgr;
import com.terracotta.session.util.SessionCookieWriter;
import com.terracotta.session.util.SessionIdGenerator;
import com.terracotta.session.util.WebsphereIdGenerator;

import java.io.IOException;
import java.lang.reflect.Method;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SessionFilter implements Filter {
  public final static String APP_SERVER_PARAM_NAME = "app-server";
  public final static String BEA_WEBLOGIC          = "BEA-Weblogic";
  public final static String IBM_WEBSPHERE         = "IBM-Websphere";

  private SessionManager     mgr                   = null;
  private ServletContext     servletContext        = null;
  private String             appServer             = null;

  public static final String FILTER_CLASS          = SessionFilter.class.getName();
  public static final String FILTER_NAME           = "Terracotta Session Filter";

  public SessionFilter() {
    // nothing
  }

  public void init(FilterConfig config) {
    servletContext = config.getServletContext();
    appServer = config.getInitParameter(APP_SERVER_PARAM_NAME);
  }

  public void destroy() {
    // not used
  }

  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
      ServletException {
    if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
      doHttpFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
    } else {
      chain.doFilter(request, response);
    }
  }

  private void doHttpFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    Assert.pre(request != null);
    Assert.pre(response != null);
    Assert.pre(chain != null);

    SessionManager tcMgr = getManager(request);
    TerracottaRequest sReq = tcMgr.preprocess(request, response);
    TerracottaResponse sRes = tcMgr.createResponse(sReq, response);
    try {
      chain.doFilter(sReq, sRes);
    } finally {
      tcMgr.postprocess(sReq);
    }
  }

  protected synchronized SessionManager getManager(HttpServletRequest req) {
    if (mgr == null) {
      if (isWebsphere()) {
        mgr = createWebsphereManager(req);
      } else if (servletContext instanceof WebAppConfig) {
        mgr = createWebAppConfigManager(req, (WebAppConfig) servletContext, servletContext);
      } else {
        mgr = createDefaultManager(req, servletContext);
      }
    }
    Assert.post(mgr != null);
    return mgr;
  }

  private SessionManager createWebsphereManager(HttpServletRequest req) {
    Object sessionContext;
    Method getSessionContext;
    WebAppConfig webappconfig;

    try {
      getSessionContext = servletContext.getClass().getMethod("__tc_getSessionContext", new Class[] {});
    } catch (Exception e) {
      throw new RuntimeException("Missing method on " + servletContext.getClass(), e);
    }

    try {
      sessionContext = getSessionContext.invoke(servletContext, new Object[] {});
    } catch (Exception e) {
      throw new RuntimeException("Exception retrieving session context", e);
    }

    if (sessionContext == null) { throw new RuntimeException("got back null for session context from "
                                                             + servletContext.getClass()); }

    webappconfig = (WebAppConfig) ReflectiveProxy.createProxyIfPossible(WebAppConfig.class, sessionContext);
    if (webappconfig == null) { throw new RuntimeException(
                                                           "Cannot create proxy to WebAppConfig -- check methods defined on "
                                                               + sessionContext.getClass()); }

    return createWebAppConfigManager(req, webappconfig, servletContext);
  }

  private boolean isWebsphere() {
    return IBM_WEBSPHERE.equals(appServer);
  }

  protected SessionManager createDefaultManager(final HttpServletRequest req, final ServletContext sc) {
    final SessionManager rv = createManager(req, null, new BaseRequestResponseFactory(), sc);
    return rv;
  }

  protected SessionManager createWebAppConfigManager(final HttpServletRequest req, final WebAppConfig wac,
                                                     final ServletContext sc) {
    final SessionManager rv = createManager(req, wac, pickFactory(), sc);
    return rv;
  }

  protected SessionManager createManager(final HttpServletRequest req, final WebAppConfig wac,
                                         final RequestResponseFactory factory, final ServletContext sc) {
    final ConfigProperties cp = new ConfigProperties(wac);

    String appName = DefaultContextMgr.computeAppName(req);
    int lockType = ManagerUtil.getSessionLockType(appName);
    SessionIdGenerator sig = pickIdGenerator(cp, lockType);

    final SessionCookieWriter scw = DefaultCookieWriter.makeInstance(cp);
    final LifecycleEventMgr eventMgr = DefaultLifecycleEventMgr.makeInstance(cp);
    final ContextMgr contextMgr = DefaultContextMgr.makeInstance(req, sc);
    final SessionManager rv = new TerracottaSessionManager(sig, scw, eventMgr, contextMgr, factory, cp);
    return rv;
  }

  private RequestResponseFactory pickFactory() {
    try {
      if (BEA_WEBLOGIC.equals(appServer)) {
        return (RequestResponseFactory) Class.forName("com.terracotta.session.WeblogicRequestResponseFactory")
            .newInstance();
      } else {
        return new BaseRequestResponseFactory();
      }
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  private SessionIdGenerator pickIdGenerator(ConfigProperties cp, int lockType) {
    if (isWebsphere()) { return new WebsphereIdGenerator(cp.getSessionIdLength(), cp.getServerId(), lockType, cp
        .getDelimiter()); }
    return DefaultIdGenerator.makeInstance(cp, lockType);
  }

}
