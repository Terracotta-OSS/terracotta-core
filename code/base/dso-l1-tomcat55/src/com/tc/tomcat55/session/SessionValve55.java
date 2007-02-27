/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.tomcat55.session;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.connector.SessionRequest55;
import org.apache.catalina.connector.SessionResponse55;
import org.apache.catalina.valves.ValveBase;

import com.terracotta.session.BaseRequestResponseFactory;
import com.terracotta.session.SessionResponse;
import com.terracotta.session.TerracottaRequest;
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
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

public class SessionValve55 extends ValveBase {

  private final Map mgrs = new HashMap();

  public void invoke(Request valveReq, Response valveRes) throws IOException, ServletException {
    if (TerracottaSessionManager.isDsoSessionApp(valveReq)) {
      tcInvoke(valveReq, valveRes);
    } else {
      if (getNext() != null) getNext().invoke(valveReq, valveRes);
    }
  }

  private void tcInvoke(Request valveReq, Response valveRes) throws IOException, ServletException {
    TerracottaSessionManager mgr = findOrCreateManager(valveReq, valveReq.getContextPath());
    TerracottaRequest sReq = mgr.preprocess(valveReq, valveRes);
    SessionRequest55 sReq55 = new SessionRequest55(sReq, valveReq, valveReq.getContext().getRealm());
    SessionResponse sRes = new SessionResponse(sReq, valveRes);
    SessionResponse55 sRes55 = new SessionResponse55(valveRes, sReq55, sRes);
    try {
      if (getNext() != null) getNext().invoke(sReq55, sRes55);
    } finally {
      mgr.postprocess(sReq);
    }
  }

  private TerracottaSessionManager findOrCreateManager(Request valveReq, String contextPath) {
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

  private static TerracottaSessionManager createManager(Request valveReq, String contextPath) {
    final ConfigProperties cp = new ConfigProperties(makeWebAppConfig(valveReq.getContext()));
    final SessionIdGenerator sig = DefaultIdGenerator.makeInstance(cp);
    final SessionCookieWriter scw = DefaultCookieWriter.makeInstance(cp);
    final LifecycleEventMgr eventMgr = DefaultLifecycleEventMgr.makeInstance(cp);
    final ContextMgr contextMgr = DefaultContextMgr
        .makeInstance(contextPath, valveReq.getContext().getServletContext());
    final TerracottaSessionManager rv = new TerracottaSessionManager(sig, scw, eventMgr, contextMgr,
                                                                     new BaseRequestResponseFactory(), cp);
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
      Object o = listeners[i];
      if (o instanceof HttpSessionListener) sessionListeners.add(o);
      if (o instanceof HttpSessionAttributeListener) attributeListeners.add(o);
    }
  }

  public String getClassName() {
    return getClass().getName();
  }
}
