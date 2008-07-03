/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.glassfish;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.core.StandardPipeline;
import org.apache.coyote.tomcat5.CoyoteRequest;
import org.apache.coyote.tomcat5.CoyoteResponse;

import com.tc.object.bytecode.ManagerUtil;
import com.tc.tomcat50.session.SessionRequest50;
import com.tc.tomcat50.session.SessionResponse50;
import com.tc.tomcat50.session.Tomcat50RequestResponseFactory;
import com.terracotta.session.SessionManager;
import com.terracotta.session.TerracottaSessionManager;
import com.terracotta.session.WebAppConfig;
import com.terracotta.session.util.Assert;
import com.terracotta.session.util.ConfigProperties;
import com.terracotta.session.util.ContextMgr;
import com.terracotta.session.util.DefaultContextMgr;
import com.terracotta.session.util.DefaultCookieWriter;
import com.terracotta.session.util.DefaultIdGenerator;
import com.terracotta.session.util.DefaultLifecycleEventMgr;
import com.terracotta.session.util.DefaultWebAppConfig;
import com.terracotta.session.util.LifecycleEventMgr;
import com.terracotta.session.util.SessionCookieWriter;
import com.terracotta.session.util.SessionIdGenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

/**
 * XXX: This class belongs in the glassfish module but cannot until the generic session code is made into a TIM as well
 * XXX: When this class is moved, it will need to be exported from the module
 */
public class TerracottaPipeline extends StandardPipeline {

  private final Map mgrs = new HashMap();

  public TerracottaPipeline(Container container) {
    super(container);
  }

  public void invoke(Request request, Response response) throws IOException, ServletException {
    if (request.getContext() != null && request instanceof CoyoteRequest && response instanceof CoyoteResponse
        && TerracottaSessionManager.isDsoSessionApp((HttpServletRequest) request)) {

      CoyoteRequest cReq = (CoyoteRequest) request;
      CoyoteResponse cRes = (CoyoteResponse) response;

      SessionManager mgr = findOrCreateManager(cReq, cReq.getContextPath());
      SessionRequest50 sReq50 = (SessionRequest50) mgr.preprocess(cReq, cRes);
      SessionResponse50 sRes50 = new SessionResponse50(cRes, sReq50);
      sReq50.setSessionResposne50(sRes50);

      try {
        super.invoke(sReq50, sRes50);
      } finally {
        mgr.postprocess(sReq50);
      }
    } else {
      super.invoke(request, response);
    }
  }

  private SessionManager findOrCreateManager(CoyoteRequest request, String contextPath) {
    SessionManager rv = null;
    synchronized (mgrs) {
      rv = (SessionManager) mgrs.get(contextPath);
      if (rv == null) {
        rv = createManager(request, contextPath);
        mgrs.put(contextPath, rv);
      }
    }
    return rv;
  }

  private static SessionManager createManager(CoyoteRequest request, String contextPath) {
    final WebAppConfig webAppConfig = makeWebAppConfig(request.getContext());
    final ClassLoader loader = request.getContext().getLoader().getClassLoader();
    final ConfigProperties cp = new ConfigProperties(webAppConfig, loader);

    String appName = DefaultContextMgr.computeAppName(request);
    int lockType = ManagerUtil.getSessionLockType(appName);
    final SessionIdGenerator sig = DefaultIdGenerator.makeInstance(cp, lockType);

    final SessionCookieWriter scw = DefaultCookieWriter.makeInstance(cp);
    final LifecycleEventMgr eventMgr = DefaultLifecycleEventMgr.makeInstance(cp);
    final ContextMgr contextMgr = DefaultContextMgr.makeInstance(contextPath, request.getContext().getServletContext());

    final SessionManager rv = new TerracottaSessionManager(sig, scw, eventMgr, contextMgr,
                                                           new Tomcat50RequestResponseFactory(), cp);
    return rv;
  }

  private static WebAppConfig makeWebAppConfig(Context context) {
    Assert.pre(context != null);
    final ArrayList sessionListeners = new ArrayList();
    final ArrayList attributeListeners = new ArrayList();
    sortByType(context.getApplicationEventListeners(), sessionListeners, attributeListeners);
    sortByType(context.getApplicationLifecycleListeners(), sessionListeners, attributeListeners);
    HttpSessionAttributeListener[] attrList = (HttpSessionAttributeListener[]) attributeListeners
        .toArray(new HttpSessionAttributeListener[attributeListeners.size()]);
    HttpSessionListener[] sessList = (HttpSessionListener[]) sessionListeners
        .toArray(new HttpSessionListener[sessionListeners.size()]);

    String jvmRoute = null;
    for (Container c = context; c != null; c = c.getParent()) {
      if (c instanceof Engine) {
        jvmRoute = ((Engine) c).getJvmRoute();
        break;
      }
    }

    return new DefaultWebAppConfig(context.getManager().getMaxInactiveInterval(), attrList, sessList, ".", jvmRoute,
                                   context.getCookies());
  }

  private static void sortByType(Object[] listeners, ArrayList sessionListeners, ArrayList attributeListeners) {
    if (listeners == null) return;
    for (int i = 0; i < listeners.length; i++) {
      final Object o = listeners[i];
      if (o instanceof HttpSessionListener) sessionListeners.add(o);
      if (o instanceof HttpSessionAttributeListener) attributeListeners.add(o);
    }
  }

}
