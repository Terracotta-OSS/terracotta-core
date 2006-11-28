/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.terracotta.session;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface RequestResponseFactory {

  TerracottaRequest createRequest(SessionId id, HttpServletRequest request, HttpServletResponse response);

  TerracottaResponse createResponse(TerracottaRequest request, HttpServletResponse response);

}
