/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.terracotta.session;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface RequestResponseFactory {

  TerracottaRequest createRequest(SessionId id, HttpServletRequest request, HttpServletResponse response, SessionManager manager);

  TerracottaResponse createResponse(TerracottaRequest request, HttpServletResponse response);

}
