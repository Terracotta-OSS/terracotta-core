/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.session;

import com.tc.logging.TCLogger;
import com.tc.management.beans.sessions.SessionMonitorMBean;
import com.tc.management.beans.sessions.SessionMonitorMBean.SessionsComptroller;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.bytecode.hook.impl.ClassProcessorHelper;
import com.terracotta.session.util.Assert;
import com.terracotta.session.util.ConfigProperties;
import com.terracotta.session.util.ContextMgr;
import com.terracotta.session.util.DefaultContextMgr;
import com.terracotta.session.util.LifecycleEventMgr;
import com.terracotta.session.util.Lock;
import com.terracotta.session.util.SessionCookieWriter;
import com.terracotta.session.util.SessionIdGenerator;
import com.terracotta.session.util.Timestamp;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TerracottaSessionManager {

  private final SessionMonitorMBean    mBean;
  private final SessionIdGenerator     idGenerator;
  private final SessionCookieWriter    cookieWriter;
  private final SessionDataStore       store;
  private final LifecycleEventMgr      eventMgr;
  private final ContextMgr             contextMgr;
  private final boolean                reqeustLogEnabled;
  private final boolean                invalidatorLogEnabled;
  private final TCLogger               logger;
  private final RequestResponseFactory factory;
  private final RequestTracker         tracker;
  private final boolean                debugServerHops;
  private final int                    debugServerHopsInterval;
  private int                          serverHopsDetected = 0;

  public TerracottaSessionManager(SessionIdGenerator sig, SessionCookieWriter scw, LifecycleEventMgr eventMgr,
                                  ContextMgr contextMgr, RequestResponseFactory factory, ConfigProperties cp) {

    Assert.pre(sig != null);
    Assert.pre(scw != null);
    Assert.pre(eventMgr != null);
    Assert.pre(contextMgr != null);

    this.idGenerator = sig;
    this.cookieWriter = scw;
    this.eventMgr = eventMgr;
    this.contextMgr = contextMgr;
    this.factory = factory;
    this.store = new SessionDataStore(contextMgr.getAppName(), cp.getSessionTimeoutSeconds(), eventMgr, contextMgr);
    this.logger = ManagerUtil.getLogger("com.tc.tcsession." + contextMgr.getAppName());
    this.reqeustLogEnabled = cp.getRequestLogBenchEnabled();
    this.invalidatorLogEnabled = cp.getInvalidatorLogBenchEnabled();

    // XXX: If reasonable, we should move this out of the constructor -- leaking a reference to "this" to another thread
    // within a constructor is a bad practice (note: althought "this" isn't explicitly based as arg, it is available and
    // acessed by the non-static inner class)
    Thread invalidator = new Thread(new SessionInvalidator(store, cp.getInvalidatorSleepSeconds(), cp
        .getSessionTimeoutSeconds()), "SessionInvalidator - " + contextMgr.getAppName());
    invalidator.setDaemon(true);
    invalidator.start();
    Assert.post(invalidator.isAlive());

    // This is disgusting, but right now we have to do this because we don't have an event
    // management infrastructure to boot stuff up
    mBean = ManagerUtil.getSessionMonitorMBean();

    mBean.registerSessionsController(new SessionsComptroller() {
      public boolean killSession(final String browserSessionId) {
        SessionId id = idGenerator.makeInstanceFromBrowserId(browserSessionId);
        if (id == null) {
          // that, potentially, was not *browser* id, try to recover...
          id = idGenerator.makeInstanceFromInternalKey(browserSessionId);
        }
        expire(id);
        return true;
      }
    });

    if (cp.isRequestTrackingEnabled()) {
      tracker = new StuckRequestTracker(cp.getRequestTrackerSleepMillis(), cp.getRequestTrackerStuckThresholdMillis(),
                                        cp.isDumpThreadsOnStuckRequests());
      ((StuckRequestTracker) tracker).start();
    } else {
      tracker = new NullRequestTracker();
    }

    this.debugServerHops = cp.isDebugServerHops();
    this.debugServerHopsInterval = cp.getDebugServerHopsInterval();
  }

  public TerracottaRequest preprocess(HttpServletRequest req, HttpServletResponse res) {
    tracker.begin(req);
    TerracottaRequest terracottaRequest = basicPreprocess(req, res);
    tracker.recordSessionId(terracottaRequest);
    return terracottaRequest;
  }

  private TerracottaRequest basicPreprocess(HttpServletRequest req, HttpServletResponse res) {
    Assert.pre(req != null);
    Assert.pre(res != null);

    SessionId sessionId = findSessionId(req);

    if (debugServerHops) {
      if (sessionId != null && sessionId.isServerHop()) {
        synchronized (this) {
          serverHopsDetected++;
          if ((serverHopsDetected % debugServerHopsInterval) == 0) {
            logger.info(serverHopsDetected + " server hops detected");
          }
        }
      }
    }

    TerracottaRequest rw = wrapRequest(sessionId, req, res);

    Assert.post(rw != null);
    return rw;
  }

  public TerracottaResponse createResponse(TerracottaRequest req, HttpServletResponse res) {
    return factory.createResponse(req, res);
  }

  public void postprocess(TerracottaRequest req) {
    try {
      basicPostprocess(req);
    } finally {
      tracker.end();
    }
  }

  private void basicPostprocess(TerracottaRequest req) {
    Assert.pre(req != null);

    // don't do anything for forwarded requests
    if (req.isForwarded()) return;

    Assert.inv(!req.isForwarded());

    mBean.requestProcessed();

    try {
      if (req.isSessionOwner()) postprocessSession(req);
    } finally {
      if (reqeustLogEnabled) {
        logRequestBench(req);
      }
    }
  }

  private void logRequestBench(TerracottaRequest req) {
    final String msgPrefix = "REQUEST BENCH: url=[" + req.getRequestURL() + "]";
    String sessionInfo = "";
    if (req.isSessionOwner()) {
      final SessionId id = req.getTerracottaSession(false).getSessionId();
      sessionInfo = " sid=[" + id.getKey() + "] -> [" + id.getLock().getLockTimer().elapsed() + ":"
                    + id.getLock().getUnlockTimer().elapsed() + "]";
    }
    final String msg = msgPrefix + sessionInfo + " -> " + (System.currentTimeMillis() - req.getRequestStartMillis());
    logger.info(msg);
  }

  private void postprocessSession(TerracottaRequest req) {
    Assert.pre(req != null);
    Assert.pre(!req.isForwarded());
    Assert.pre(req.isSessionOwner());
    final Session session = req.getTerracottaSession(false);
    Assert.inv(session != null);
    final SessionId id = session.getSessionId();
    final SessionData sd = session.getSessionData();
    try {
      if (!session.isValid()) store.remove(id);
      else {
        sd.finishRequest();
        store.updateTimestampIfNeeded(sd);
      }
    } finally {
      id.commitLock();
    }
  }

  /**
   * The only use for this method [currently] is by Struts' Include Tag, which can generate a nested request. In this
   * case we have to release session lock, so that nested request (running, potentially, in another JVM) can acquire it.
   * {@link TerracottaSessionManager#resumeRequest(Session)} method will re-aquire the lock.
   */
  public static void pauseRequest(final Session sess) {
    Assert.pre(sess != null);
    final SessionId id = sess.getSessionId();
    final SessionData sd = sess.getSessionData();
    sd.finishRequest();
    id.commitLock();
  }

  /**
   * See {@link TerracottaSessionManager#resumeRequest(Session)} for details
   */
  public static void resumeRequest(final Session sess) {
    Assert.pre(sess != null);
    final SessionId id = sess.getSessionId();
    final SessionData sd = sess.getSessionData();
    id.getWriteLock();
    sd.startRequest();
  }

  private TerracottaRequest wrapRequest(SessionId sessionId, HttpServletRequest req, HttpServletResponse res) {
    TerracottaRequest request = factory.createRequest(sessionId, req, res);
    request.setSessionManager(this);
    return request;
  }

  /**
   * This method always returns a valid session. If data for the requestedSessionId found and is valid, it is returned.
   * Otherwise, we must create a new session id, a new session data, a new sessiono, and cookie the response.
   */
  protected Session getSession(final SessionId requestedSessionId, final HttpServletRequest req,
                               final HttpServletResponse res) {
    Assert.pre(req != null);
    Assert.pre(res != null);
    Session rv = doGetSession(requestedSessionId, req, res);
    Assert.post(rv != null);
    return rv;
  }

  protected Session getSessionIfExists(SessionId requestedSessionId, HttpServletRequest req, HttpServletResponse res) {
    if (requestedSessionId == null) return null;
    SessionData sd = store.find(requestedSessionId);
    if (sd == null) return null;
    Assert.inv(sd.isValid());
    if (requestedSessionId.isServerHop()) cookieWriter.writeCookie(req, res, requestedSessionId);
    return sd;
  }

  protected SessionCookieWriter getCookieWriter() {
    return this.cookieWriter;
  }

  private void expire(SessionId id) {
    SessionData sd = null;
    try {
      sd = store.find(id);
      if (sd != null) {
        expire(id, sd);
      }
    } finally {
      if (sd != null) id.commitLock();
    }
  }

  private void expire(SessionId id, SessionData sd) {
    try {
      if (!sd.isInvalidated()) sd.invalidate();
    } catch (Throwable t) {
      logger.error("unhandled exception during invalidate() for session " + id.getKey());
    }
    store.remove(id);
    mBean.sessionDestroyed();
  }

  private Session doGetSession(final SessionId requestedSessionId, final HttpServletRequest req,
                               final HttpServletResponse res) {
    Assert.pre(req != null);
    Assert.pre(res != null);

    if (requestedSessionId == null) { return createNewSession(req, res); }
    final SessionData sd = store.find(requestedSessionId);
    if (sd == null) { return createNewSession(req, res); }
    Assert.inv(sd.isValid());
    if (requestedSessionId.isServerHop()) cookieWriter.writeCookie(req, res, requestedSessionId);

    return sd;
  }

  private Session createNewSession(HttpServletRequest req, HttpServletResponse res) {
    Assert.pre(req != null);
    Assert.pre(res != null);

    SessionId id = idGenerator.generateNewId();
    SessionData sd = store.createSessionData(id);
    cookieWriter.writeCookie(req, res, id);
    eventMgr.fireSessionCreatedEvent(sd);
    mBean.sessionCreated();
    Assert.post(sd != null);
    return sd;
  }

  private SessionId findSessionId(HttpServletRequest httpRequest) {
    Assert.pre(httpRequest != null);

    String requestedSessionId = httpRequest.getRequestedSessionId();
    if (requestedSessionId == null) return null;
    else return idGenerator.makeInstanceFromBrowserId(requestedSessionId);
  }

  private class SessionInvalidator implements Runnable {

    private final long sleepMillis;

    public SessionInvalidator(final SessionDataStore store, final long sleepSeconds,
                              final long defaultSessionIdleSeconds) {
      this.sleepMillis = sleepSeconds * 1000;
    }

    public void run() {
      final String invalidatorLock = "tc:session_invalidator_lock_" + contextMgr.getAppName();

      while (true) {
        sleep(sleepMillis);
        if (Thread.interrupted()) {
          break;
        } else {
          try {
            final Lock lock = new Lock(invalidatorLock);
            lock.tryWriteLock();
            if (!lock.isLocked()) continue;
            try {
              invalidateSessions();
            } finally {
              lock.commitLock();
            }
          } catch (Throwable t) {
            logger.error("Unhandled exception occurred during session invalidation", t);
          }
        }
      }
    }

    private void invalidateSessions() {
      final long startMillis = System.currentTimeMillis();
      final String keys[] = store.getAllKeys();
      int totalCnt = 0;
      int invalCnt = 0;
      int evaled = 0;
      int notEvaled = 0;
      int errors = 0;

      if (invalidatorLogEnabled) {
        logger.info("SESSION INVALIDATOR: started");
      }

      for (int i = 0, n = keys.length; i < n; i++) {
        final String key = keys[i];
        try {
          final SessionId id = idGenerator.makeInstanceFromInternalKey(key);
          final Timestamp dtm = store.findTimestampUnlocked(id);
          if (dtm == null) continue;
          totalCnt++;
          if (dtm.getMillis() < System.currentTimeMillis()) {
            evaled++;
            if (evaluateSession(dtm, id)) invalCnt++;
          } else {
            notEvaled++;
          }
        } catch (Throwable t) {
          errors++;
          logger.error("Unhandled exception inspecting session " + key + " for invalidation", t);
        }
      }
      if (invalidatorLogEnabled) {
        final String msg = "SESSION INVALIDATOR BENCH: " + " -> total=" + totalCnt + ", evaled=" + evaled
                           + ", notEvaled=" + notEvaled + ", errors=" + errors + ", invalidated=" + invalCnt
                           + " -> elapsed=" + (System.currentTimeMillis() - startMillis);
        logger.info(msg);
      }
    }

    private boolean evaluateSession(final Timestamp dtm, final SessionId id) {
      Assert.pre(id != null);

      boolean rv = false;
      id.tryWriteLock();
      if (!id.getLock().isLocked()) { return rv; }

      try {
        final SessionData sd = store.findSessionDataUnlocked(id);
        if (sd == null) return rv;
        if (!sd.isValid()) {
          expire(id, sd);
          rv = true;
        } else {
          store.updateTimestampIfNeeded(sd);
        }
      } finally {
        id.commitLock();
      }
      return rv;
    }

    private void sleep(long l) {
      try {
        Thread.sleep(l);
      } catch (InterruptedException ignore) {
        // nothing to do
      }
    }
  }

  public static boolean isDsoSessionApp(HttpServletRequest request) {
    Assert.pre(request != null);
    final String appName = DefaultContextMgr.computeAppName(request);
    return ClassProcessorHelper.isDSOSessions(appName);
  }

  private static final class NullRequestTracker implements RequestTracker {

    public final boolean end() {
      return true;
    }

    public final void begin(HttpServletRequest req) {
      //
    }

    public final void recordSessionId(TerracottaRequest terracottaRequest) {
      //
    }

  }

}
