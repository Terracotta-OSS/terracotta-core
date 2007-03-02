/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.session;

import com.tc.object.bytecode.ManagerUtil;
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

import java.io.IOException;

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

  private TerracottaSessionManager mgr            = null;
  private ServletContext           servletContext = null;
  public static final String       FILTER_CLASS   = SessionFilter.class.getName();
  public static final String       FILTER_NAME    = "Terracotta Session Filter";

  public SessionFilter() {
    // nothing
  }

  public void init(FilterConfig config) {
    servletContext = config.getServletContext();
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

    TerracottaSessionManager tcMgr = getManager(request);
    TerracottaRequest sReq = tcMgr.preprocess(request, response);
    TerracottaResponse sRes = tcMgr.createResponse(sReq, response);
    try {
      chain.doFilter(sReq, sRes);
    } finally {
      tcMgr.postprocess(sReq);
    }
  }

  protected synchronized TerracottaSessionManager getManager(HttpServletRequest req) {
    if (mgr == null) {
      if (servletContext instanceof WebAppConfig) mgr = createWebAppConfigManager(req, (WebAppConfig) servletContext,
                                                                                  servletContext);
      else mgr = createDefaultManager(req, servletContext);
    }
    Assert.post(mgr != null);
    return mgr;
  }

  protected static TerracottaSessionManager createDefaultManager(final HttpServletRequest req,
                                                                 final ServletContext servletContext) {
    final TerracottaSessionManager rv = createManager(req, null, new BaseRequestResponseFactory(), servletContext);
    return rv;
  }

  protected static TerracottaSessionManager createWebAppConfigManager(final HttpServletRequest req,
                                                                      final WebAppConfig wac,
                                                                      final ServletContext servletContext) {
    final TerracottaSessionManager rv = createManager(req, wac, pickFactory(), servletContext);
    return rv;
  }

  protected static TerracottaSessionManager createManager(final HttpServletRequest req, final WebAppConfig wac,
                                                          final RequestResponseFactory factory,
                                                          final ServletContext servletContext) {
    final ConfigProperties cp = new ConfigProperties(wac);

    String appName = DefaultContextMgr.computeAppName(req);
    int lockType = ManagerUtil.getSessionLockType(appName);
    final SessionIdGenerator sig = DefaultIdGenerator.makeInstance(cp, lockType);

    final SessionCookieWriter scw = DefaultCookieWriter.makeInstance(cp);
    final LifecycleEventMgr eventMgr = DefaultLifecycleEventMgr.makeInstance(cp);
    final ContextMgr contextMgr = DefaultContextMgr.makeInstance(req, servletContext);
    final TerracottaSessionManager rv = new TerracottaSessionManager(sig, scw, eventMgr, contextMgr, factory, cp);
    return rv;
  }

  private static RequestResponseFactory pickFactory() {
    // XXX: this is NOT a good way to implement the factory picking, but it will suffice for Lawton
    // XXX: It should probably be passed in through init(FilterConfig)

    try {
      ClassLoader.getSystemClassLoader().loadClass("weblogic.Server");
      return (RequestResponseFactory) Class.forName("com.terracotta.session.WeblogicRequestResponseFactory")
          .newInstance();
    } catch (Exception e) {
      return new BaseRequestResponseFactory();
    }
  }

}
