/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.session;

import com.tc.exception.ImplementMe;
import com.terracotta.session.util.SessionCookieWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MockSessionManager implements SessionManager {

  public TerracottaResponse createResponse(TerracottaRequest req, HttpServletResponse response) {
    throw new ImplementMe();
  }

  public SessionCookieWriter getCookieWriter() {
    throw new ImplementMe();
  }

  public Session getSession(SessionId requestedSessionId, HttpServletRequest req, HttpServletResponse res) {
    throw new ImplementMe();
  }

  public Session getSessionIfExists(SessionId requestedSessionId, HttpServletRequest req, HttpServletResponse res) {
    throw new ImplementMe();
  }

  public void postprocess(TerracottaRequest req) {
    throw new ImplementMe();
  }

  public TerracottaRequest preprocess(HttpServletRequest valveReq, HttpServletResponse valveRes) {
    throw new ImplementMe();
  }

  public void remove(Session data, boolean unlock) {
    //
  }

}
