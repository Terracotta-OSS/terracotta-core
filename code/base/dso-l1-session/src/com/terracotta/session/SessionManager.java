/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.session;

import com.terracotta.session.util.SessionCookieWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface SessionManager {

  SessionCookieWriter getCookieWriter();

  void postprocess(TerracottaRequest req);

  TerracottaRequest preprocess(HttpServletRequest valveReq, HttpServletResponse valveRes);

  Session getSession(SessionId requestedSessionId, HttpServletRequest req, HttpServletResponse res);

  Session getSessionIfExists(SessionId requestedSessionId, HttpServletRequest req, HttpServletResponse res);

  TerracottaResponse createResponse(TerracottaRequest req, HttpServletResponse response);

  void remove(Session data, boolean unlock);

}
