/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.tomcat50.session;

import org.apache.coyote.tomcat5.CoyoteRequest;
import org.apache.coyote.tomcat5.CoyoteResponse;

import com.terracotta.session.BaseRequestResponseFactory;
import com.terracotta.session.SessionId;
import com.terracotta.session.TerracottaRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Tomcat50RequestResponseFactory extends BaseRequestResponseFactory {

  public TerracottaRequest createRequest(SessionId id, HttpServletRequest request, HttpServletResponse response) {
    return new SessionRequest50(id, (CoyoteRequest) request, (CoyoteResponse) response);
  }

}
