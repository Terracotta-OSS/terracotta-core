/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.session;

import com.tc.logging.TCLogger;
import com.tc.management.beans.sessions.SessionMonitor;
import com.tc.management.beans.sessions.SessionMonitor.SessionsComptroller;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.bytecode.hook.impl.ClassProcessorHelper;
import com.tc.properties.TCPropertiesConsts;
import com.terracotta.session.util.Assert;
import com.terracotta.session.util.ConfigProperties;
import com.terracotta.session.util.ContextMgr;
import com.terracotta.session.util.DefaultContextMgr;
import com.terracotta.session.util.LifecycleEventMgr;
import com.terracotta.session.util.Lock;
import com.terracotta.session.util.SessionCookieWriter;
import com.terracotta.session.util.SessionIdGenerator;
import com.terracotta.session.util.Timestamp;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TerracottaSessionManager implements SessionManager {

  private static final String          WRAP_COUNT_ATTRIBUTE = TerracottaSessionManager.class.getName() + ".WRAP_COUNT";

  private final SessionMonitor         sessionMonitor;
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
  private final boolean                debugInvalidate;
  private final boolean                debugSessions;
  private final String                 sessionCookieName;
  private final String                 sessionUrlPathParamTag;
  private final boolean                usesStandardUrlPathParam;
  private int                          serverHopsDetected   = 0;
  private final boolean                sessionLocking;

  private static final Set             excludedVHosts       = loadExcludedVHosts();

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

    String appName = contextMgr.getHostName() + "/" + contextMgr.getAppName();
    this.store = new SessionDataStore(appName, cp.getSessionTimeoutSeconds(), eventMgr, contextMgr, this);
    this.logger = ManagerUtil.getLogger("com.tc.tcsession." + contextMgr.getAppName());
    this.reqeustLogEnabled = cp.getRequestLogBenchEnabled();
    this.invalidatorLogEnabled = cp.getInvalidatorLogBenchEnabled();
    this.sessionLocking = ManagerUtil.isApplicationSessionLocked(contextMgr.getAppName());
    idGenerator.initialize(sessionLocking);
    logger.info("Clustered HTTP sessions with session-locking=" + sessionLocking + " for [" + contextMgr.getAppName()
                + "]");

    // XXX: If reasonable, we should move this out of the constructor -- leaking a reference to "this" to another thread
    // within a constructor is a bad practice (note: although "this" isn't explicitly based as arg, it is available and
    // accessed by the non-static inner class)
    Thread invalidator = new Thread(new SessionInvalidator(cp.getInvalidatorSleepSeconds()), "SessionInvalidator - "
                                                                                             + contextMgr.getAppName());
    invalidator.setDaemon(true);
    invalidator.start();
    Assert.post(invalidator.isAlive());

    // This is disgusting, but right now we have to do this because we don't have an event
    // management infrastructure to boot stuff up
    sessionMonitor = ManagerUtil.getHttpSessionMonitor();

    sessionMonitor.registerSessionsController(new SessionsComptroller() {
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
    this.debugInvalidate = cp.isDebugSessionInvalidate();
    this.debugSessions = cp.isDebugSessions();
    this.sessionCookieName = this.cookieWriter.getCookieName();
    this.sessionUrlPathParamTag = this.cookieWriter.getPathParameterTag();
    this.usesStandardUrlPathParam = this.sessionUrlPathParamTag.equalsIgnoreCase(";"
                                                                                 + ConfigProperties.defaultCookieName
                                                                                 + "=");
  }

  public boolean isSessionLockingEnabled() {
    return sessionLocking;
  }

  private static Set loadExcludedVHosts() {
    String list = ManagerUtil.getTCProperties().getProperty(TCPropertiesConsts.SESSION_VHOSTS_EXCLUDED, true);
    list = (list == null) ? "" : list.replaceAll("\\s", "");

    Set set = new TreeSet();
    String[] vhosts = list.split(",");
    for (String vhost : vhosts) {
      if (vhost != null && vhost.length() > 0) {
        set.add(vhost);
      }
    }

    if (set.size() > 0) {
      ManagerUtil.getLogger("com.tc.TerracottaSessionManager").warn("Excluded vhosts for sessions: " + set);
    }

    return Collections.unmodifiableSet(set);
  }

  private void incrementWrapCount(HttpServletRequest req) {
    Integer count = (Integer) req.getAttribute(WRAP_COUNT_ATTRIBUTE);
    if (count == null) {
      count = Integer.valueOf(1);
    } else {
      count = Integer.valueOf(count.intValue() + 1);
    }
    req.setAttribute(WRAP_COUNT_ATTRIBUTE, count);
  }

  private int decrementWrapCount(HttpServletRequest req) {
    Integer count = (Integer) req.getAttribute(WRAP_COUNT_ATTRIBUTE);
    if (count == null) { throw new AssertionError("request did not contain expected attribute"); }
    if (count.intValue() < 1) { throw new AssertionError("unexpected count value: " + count); }

    int rv = count.intValue() - 1;

    if (rv == 0) {
      req.removeAttribute(WRAP_COUNT_ATTRIBUTE);
    } else {
      req.setAttribute(WRAP_COUNT_ATTRIBUTE, Integer.valueOf(rv));
    }

    return rv;
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

    incrementWrapCount(req);

    SessionIDSource source = SessionIDSource.NONE;

    String requestedSessionId = findSessionCookie(req);

    // cookies take precedence over URLs
    if (requestedSessionId == null) {
      requestedSessionId = findSessionInURL(req);
      if (requestedSessionId != null) {
        source = SessionIDSource.URL;
      }
    } else {
      source = SessionIDSource.COOKIE;
    }

    if (requestedSessionId == null) {
      if (debugSessions) {
        logger.info("no requested session id found in request");
      }
    } else {
      if (debugSessions) {
        logger.info("requested session ID from http request (" + source + "): " + requestedSessionId);
      }
    }

    SessionId sessionId = requestedSessionId == null ? null : idGenerator.makeInstanceFromBrowserId(requestedSessionId);
    if (debugSessions) {
      logger.info("session ID generator returned " + sessionId);
    }

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

    TerracottaRequest rw = wrapRequest(sessionId, req, res, requestedSessionId, source);

    Assert.post(rw != null);
    return rw;
  }

  private String findSessionInURL(HttpServletRequest req) {
    if (usesStandardUrlPathParam && req.isRequestedSessionIdFromURL()) {
      // let the container take care of finding the sessionid in this case
      return req.getRequestedSessionId();
    }

    String rv = null;

    String requestURI = req.getRequestURI();
    int start = requestURI.indexOf(sessionUrlPathParamTag);
    if (start >= 0) {
      int nextSemi = requestURI.indexOf(";", start + 1);
      if (nextSemi < 0) {
        rv = requestURI.substring(start);
      } else {
        rv = requestURI.substring(start, nextSemi);
      }

      rv = rv.substring(sessionUrlPathParamTag.length());

      if (debugSessions) {
        logger.info("requested session id (from URL): " + rv);
      }
    }

    return rv;
  }

  private String findSessionCookie(HttpServletRequest req) {
    String rv = null;

    Cookie[] cookies = req.getCookies();
    if (cookies != null) {
      for (int i = 0, count = 1; i < cookies.length; i++) {
        Cookie cookie = cookies[i];
        if (sessionCookieName.equals(cookie.getName())) {
          rv = cookie.getValue();
          // NOTE: we do not "break" here, the least specific cookie (by path) should be last
          // and is the one that should be obeyed

          if (debugSessions) {
            logger.info("found a sessionID cookie (" + count++ + ") in request: " + rv);
          }
        }
      }
    }

    return rv;
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

    int count = decrementWrapCount(req);
    if (count != 0) return;

    // don't do anything for forwarded requests
    if (req.isForwarded()) return;

    Assert.inv(!req.isForwarded());

    sessionMonitor.requestProcessed();

    try {
      postprocessSession(req);
    } finally {
      if (reqeustLogEnabled) {
        logRequestBench(req);
      }
    }
  }

  private void logRequestBench(TerracottaRequest req) {
    final String msgPrefix = "REQUEST BENCH: url=[" + req.getRequestURL() + "]";
    String sessionInfo = "";

    Session tcSession = req.getTerracottaSession(false);
    if (tcSession != null) {
      final SessionId id = tcSession.getSessionId();
      sessionInfo = " sid=[" + id.getKey() + "]";
    }
    final String msg = msgPrefix + sessionInfo + " -> " + (System.currentTimeMillis() - req.getRequestStartMillis());
    logger.info(msg);
  }

  private void postprocessSession(TerracottaRequest req) {
    Assert.pre(req != null);
    Assert.pre(!req.isForwarded());
    final Session session = req.getTerracottaSession(false);

    // session was not accessed on this request
    if (session == null) return;

    session.clearRequest();
    final SessionId id = session.getSessionId();
    final SessionData sd = session.getSessionData();
    if (!isSessionLockingEnabled()) id.getWriteLock();
    try {
      if (!session.isValid()) store.remove(id);
      else {
        sd.finishRequest();
        store.updateTimestampIfNeeded(sd);
      }
    } finally {
      id.commitLock();
      id.commitSessionInvalidatorLock();
    }
  }

  /**
   * The only use for this method [currently] is by Struts' Include Tag, which can generate a nested request. In this
   * case we have to release session lock, so that nested request (running, potentially, in another JVM) can acquire it.
   * {@link TerracottaSessionManager#resumeRequest(Session)} method will re-aquire the lock.
   * <p />
   * This method does not do anything when session-locking is <tt>false</tt>
   */
  public static void pauseRequest(final Session sess) {
    Assert.pre(sess != null);
    if (!sess.isSessionLockingEnabled()) return;
    final SessionId id = sess.getSessionId();
    final SessionData sd = sess.getSessionData();
    sd.finishRequest();
    id.commitLock();
  }

  /**
   * See {@link TerracottaSessionManager#resumeRequest(Session)} for details.
   * <p />
   * This method does not do anything when session-locking is <tt>false</tt>
   */
  public static void resumeRequest(final Session sess) {
    Assert.pre(sess != null);
    if (!sess.isSessionLockingEnabled()) return;
    final SessionId id = sess.getSessionId();
    final SessionData sd = sess.getSessionData();
    id.getWriteLock();
    sd.startRequest();
  }

  private TerracottaRequest wrapRequest(SessionId sessionId, HttpServletRequest req, HttpServletResponse res,
                                        String requestedSessionId, SessionIDSource source) {
    return factory.createRequest(sessionId, req, res, this, requestedSessionId, source);
  }

  /**
   * This method always returns a valid session. If data for the requestedSessionId found and is valid, it is returned.
   * Otherwise, we must create a new session id, a new session data, a new session, and cookie the response.
   */
  public Session getSession(final SessionId requestedSessionId, final HttpServletRequest req,
                            final HttpServletResponse res) {
    Assert.pre(req != null);
    Assert.pre(res != null);
    Session rv = doGetSession(requestedSessionId, req, res);
    Assert.post(rv != null);
    return rv;
  }

  public Session getSessionIfExists(SessionId requestedSessionId, HttpServletRequest req, HttpServletResponse res) {
    if (debugSessions) {
      logger.info("getSessionIfExists called for " + requestedSessionId);
    }

    if (requestedSessionId == null) return null;
    SessionData sd = store.find(requestedSessionId);
    if (sd == null) {
      if (debugSessions) {
        logger.info("No session found in store for " + requestedSessionId);
      }
      return null;
    }

    Assert.inv(sd.isValid());
    writeCookieIfHop(req, res, requestedSessionId);

    return sd;
  }

  private void writeCookieIfHop(HttpServletRequest req, HttpServletResponse res, SessionId id) {
    if (id.isServerHop()) {
      Cookie cookie = cookieWriter.writeCookie(req, res, id);
      if (debugSessions) {
        logger.info("writing new cookie for hopped request: " + getCookieDetails(cookie));
      }
    }
  }

  private String getCookieDetails(Cookie c) {
    if (c == null) { return "<null cookie>"; }

    StringBuffer buf = new StringBuffer("Cookie(");
    buf.append(c.getName()).append("=").append(c.getValue());
    buf.append(", path=").append(c.getPath()).append(", maxAge=").append(c.getMaxAge()).append(", domain=")
        .append(c.getDomain());
    buf.append(", secure=").append(c.getSecure()).append(", comment=").append(c.getComment()).append(", version=")
        .append(c.getVersion());

    buf.append(")");
    return buf.toString();
  }

  public SessionCookieWriter getCookieWriter() {
    return this.cookieWriter;
  }

  private void expire(SessionId id) {
    SessionData sd = null;
    boolean locked = false;
    try {
      sd = store.find(id);
      if (sd != null) {
        if (!isSessionLockingEnabled()) id.getWriteLock();
        locked = true;
        expire(id, sd);
      }
    } finally {
      if (sd != null && locked) id.commitLock();
    }
  }

  public void remove(Session data, boolean unlock) {
    if (debugInvalidate) {
      logger.info("Session id: " + data.getSessionId().getKey() + " being removed, unlock: " + unlock);
    }
    if (unlock && !isSessionLockingEnabled()) {
      data.getSessionId().getWriteLock();
    }
    try {
      store.remove(data.getSessionId());
      sessionMonitor.sessionDestroyed();
    } finally {
      if (unlock) {
        data.getSessionId().commitLock();
      }
    }
  }

  private void expire(SessionId id, SessionData sd) {
    try {
      sd.invalidateIfNecessary();
    } catch (Throwable t) {
      logger.error("unhandled exception during invalidate() for session " + id.getKey(), t);
    }
  }

  private Session doGetSession(final SessionId requestedSessionId, final HttpServletRequest req,
                               final HttpServletResponse res) {
    Assert.pre(req != null);
    Assert.pre(res != null);

    if (requestedSessionId == null) {
      if (debugSessions) {
        logger.info("creating new session since requested id is null");
      }
      return createNewSession(req, res);
    }
    final SessionData sd = store.find(requestedSessionId);

    if (sd == null) {
      if (debugSessions) {
        logger.info("creating new session since requested id is not in store: " + requestedSessionId);
      }
      return createNewSession(req, res);
    }

    if (debugSessions) {
      logger.info("requested id found in store: " + requestedSessionId);
    }

    Assert.inv(sd.isValid());

    writeCookieIfHop(req, res, requestedSessionId);

    return sd;
  }

  private Session createNewSession(HttpServletRequest req, HttpServletResponse res) {
    Assert.pre(req != null);
    Assert.pre(res != null);

    SessionId id = idGenerator.generateNewId();
    SessionData sd = store.createSessionData(id);
    Cookie cookie = cookieWriter.writeCookie(req, res, id);
    eventMgr.fireSessionCreatedEvent(sd);
    sessionMonitor.sessionCreated();
    Assert.post(sd != null);

    if (debugSessions) {
      logger.info("new session created: " + id + " with cookie " + getCookieDetails(cookie));
    }

    return sd;
  }

  private class SessionInvalidator implements Runnable {

    private final long sleepMillis;

    public SessionInvalidator(final long sleepSeconds) {
      this.sleepMillis = sleepSeconds * 1000L;
    }

    public void run() {
      final String invalidatorLock = "tc:session_invalidator_lock_" + contextMgr.getAppName();

      while (true) {
        sleep(sleepMillis);
        if (Thread.interrupted()) {
          logger.warn("invalidator thread interrupted -- exiting");
          break;
        } else {
          try {
            final Lock lock = new Lock(invalidatorLock);
            if (!lock.tryWriteLock()) {
              if (debugInvalidate) {
                logger.info("did not obtain the invalidator lock (" + invalidatorLock + ")");
              }
              continue;
            }
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

      for (final String key : keys) {
        try {
          final SessionId id = idGenerator.makeInstanceFromInternalKey(key);
          final Timestamp dtm = store.findTimestampUnlocked(id);
          if (dtm == null) {
            if (debugInvalidate) {
              logger.info("null timestamp for " + key);
            }
            continue;
          }
          totalCnt++;

          final long dtmMillis = dtm.getMillis();
          final long now = System.currentTimeMillis();

          if (dtmMillis < now) {
            if (debugInvalidate) {
              logger.info("evaluating session " + key + " with timestamp " + dtmMillis);
            }
            evaled++;
            if (evaluateSession(dtm, id)) invalCnt++;
          } else {
            if (debugInvalidate) {
              logger.info("not evaluting session " + key + " with timestamp " + dtmMillis + ", now=" + now);
            }
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

      if (debugInvalidate) {
        logger.info("starting trySessionInvalidatorWriteLock() for " + id.getKey());
      }
      if (!id.trySessionInvalidatorWriteLock()) {
        if (debugInvalidate) {
          logger.info("trySessionInvalidatorWriteLock() returned false for " + id.getKey());
        }
        return rv;
      }

      if (debugInvalidate) {
        logger.info("trySessionInvalidatorWriteLock() obtained for " + id.getKey());
      }

      try {
        if (debugInvalidate) {
          logger.info("starting tryWriteLock() for " + id.getKey());
        }
        if (!id.tryWriteLock()) {
          if (debugInvalidate) {
            logger.info("tryWriteLock() returned false for " + id.getKey());
          }
          return rv;
        }

        if (debugInvalidate) {
          logger.info("tryWriteLock() obtained for " + id.getKey());
        }
        try {
          final SessionData sd = store.findSessionDataUnlocked(id);
          if (sd == null) {
            if (debugInvalidate) {
              logger.info("null session data for " + id.getKey());
            }
            return rv;
          }
          if (!sd.isValid(debugInvalidate, logger)) {
            if (debugInvalidate) {
              logger.info(id.getKey() + " IS invalid");
            }
            expire(id, sd);
            rv = true;
          } else {
            if (debugInvalidate) {
              logger.info(id.getKey() + " IS NOT invalid, updating timestamp");
            }
            store.updateTimestampIfNeeded(sd);
          }
        } finally {
          id.commitLock();
        }
      } finally {
        id.commitSessionInvalidatorLock();
      }
      return rv;
    }

    private void sleep(long time) {
      String prevName = Thread.currentThread().getName();
      Thread.currentThread().setName(
                                     prevName + " (sleeping for " + time + " milliseconds, starting from " + new Date()
                                         + ")");
      try {
        Thread.sleep(time);
      } catch (InterruptedException ignore) {
        // nothing to do
      } finally {
        Thread.currentThread().setName(prevName);
      }
    }
  }

  private static final Map<String, Boolean> clusteredApps = new ConcurrentHashMap<String, Boolean>();

  // XXX: move this method? And the static cache!
  public static boolean isDsoSessionApp(HttpServletRequest request) {
    Assert.pre(request != null);

    if (excludedVHosts.contains(request.getServerName())) { return false; }

    String hostHeader = request.getHeader("Host");
    if (hostHeader != null && excludedVHosts.contains(hostHeader)) { return false; }

    final String appName = DefaultContextMgr.computeAppName(request);

    Boolean clustered = clusteredApps.get(appName);
    if (clustered != null) { return clustered.booleanValue(); }

    clustered = Boolean.valueOf(ClassProcessorHelper.isDSOSessions(appName));
    Boolean prevValue = clusteredApps.put(appName, clustered);
    if (prevValue != null && !clustered.equals(prevValue)) {
      // This shouldn't happen
      throw new AssertionError("Got differing response for [" + appName + "], was " + prevValue);
    }
    return clustered.booleanValue();
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
