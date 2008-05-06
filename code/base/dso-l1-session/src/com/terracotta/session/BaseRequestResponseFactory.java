/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.session;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class BaseRequestResponseFactory implements RequestResponseFactory {

  public TerracottaRequest createRequest(SessionId id, HttpServletRequest request, HttpServletResponse response,
                                         SessionManager manager, String rawRequestedSessionId, SessionIDSource source) {
    return new SessionRequest(id, request, response, manager, rawRequestedSessionId, source);
  }

  public TerracottaResponse createResponse(TerracottaRequest request, HttpServletResponse response) {
    return new SessionResponse(request, response);
  }

}
