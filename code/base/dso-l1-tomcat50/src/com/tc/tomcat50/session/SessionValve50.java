/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.tomcat50.session;

import org.apache.catalina.Context;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.ValveContext;
import org.apache.catalina.valves.ValveBase;
import org.apache.coyote.tomcat5.CoyoteRequest;
import org.apache.coyote.tomcat5.CoyoteResponse;

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

public class SessionValve50 extends ValveBase {

  private final Map mgrs = new HashMap();

  public void invoke(final Request valveReq, final Response valveRes, final ValveContext valveContext)
      throws IOException, ServletException {
    if (valveReq instanceof CoyoteRequest && valveRes instanceof CoyoteResponse
        && TerracottaSessionManager.isDsoSessionApp((HttpServletRequest) valveReq)) {
      tcInvoke((CoyoteRequest) valveReq, (CoyoteResponse) valveRes, valveContext);
    } else {
      valveContext.invokeNext(valveReq, valveRes);
    }
  }

  private void tcInvoke(final CoyoteRequest valveReq, final CoyoteResponse valveRes, final ValveContext valveContext)
      throws IOException, ServletException {
    TerracottaSessionManager mgr = null;
    SessionRequest50 sReq50 = null;
    SessionResponse50 sRes50 = null;
    try {
      mgr = findOrCreateManager(valveReq, valveReq.getContextPath());
      sReq50 = (SessionRequest50) mgr.preprocess(valveReq, valveRes);
      sRes50 = new SessionResponse50(valveRes, sReq50);
      sReq50.setSessionResposne50(sRes50);
      valveContext.invokeNext(sReq50, sRes50);
    } finally {
      mgr.postprocess(sReq50);
    }
  }

  private TerracottaSessionManager findOrCreateManager(CoyoteRequest valveReq, String contextPath) {
    TerracottaSessionManager rv = null;
    synchronized (mgrs) {
      rv = (TerracottaSessionManager) mgrs.get(contextPath);
      if (rv == null) {
        rv = createManager(valveReq, contextPath);
        mgrs.put(contextPath, rv);
      }
    }
    return rv;
  }

  private static TerracottaSessionManager createManager(CoyoteRequest valveReq, String contextPath) {
    final WebAppConfig webAppConfig = makeWebAppConfig(valveReq.getContext());
    final ConfigProperties cp = new ConfigProperties(webAppConfig);
    final SessionIdGenerator sig = DefaultIdGenerator.makeInstance(cp);
    final SessionCookieWriter scw = DefaultCookieWriter.makeInstance(cp);
    final LifecycleEventMgr eventMgr = DefaultLifecycleEventMgr.makeInstance(cp);
    final ContextMgr contextMgr = DefaultContextMgr
        .makeInstance(contextPath, valveReq.getContext().getServletContext());

    final TerracottaSessionManager rv = new TerracottaSessionManager(sig, scw, eventMgr, contextMgr, cp
        .getSessionTimeoutSeconds(), cp.getInvalidatorSleepSeconds(), cp.getRequestLogBenchEnabled(), cp
        .getInvalidatorLogBenchEnabled(), new Tomcat50RequestResponseFactory());
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
    return new DefaultWebAppConfig(context.getManager().getMaxInactiveInterval(), attrList, sessList);
  }

  private static void sortByType(Object[] listeners, ArrayList sessionListeners, ArrayList attributeListeners) {
    if (listeners == null) return;
    for (int i = 0; i < listeners.length; i++) {
      final Object o = listeners[i];
      if (o instanceof HttpSessionListener) sessionListeners.add(o);
      if (o instanceof HttpSessionAttributeListener) attributeListeners.add(o);
    }
  }

  public String getClassName() {
    return getClass().getName();
  }
}
