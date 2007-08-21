/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.session;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class TerracottaDispatcher implements RequestDispatcher {

  private RequestDispatcher originalDispatcher;

  public TerracottaDispatcher(RequestDispatcher originalDispatcher) {
    this.originalDispatcher = originalDispatcher;
  }

  public void forward(ServletRequest servletrequest, ServletResponse servletresponse) throws ServletException,
      IOException {

    // Add our special forward attribute. WL 8.1 predates the official servlet attributes from Servlet 2.4 spec,
    // so we will key off this attribute in SessionRequest to determine whether the request is a forward
    servletrequest.setAttribute(SessionRequest.SESSION_FORWARD_ATTRIBUTE_NAME, Boolean.TRUE);

    originalDispatcher.forward(servletrequest, servletresponse);
  }

  public void include(ServletRequest servletrequest, ServletResponse servletresponse) throws ServletException,
      IOException {

    originalDispatcher.include(servletrequest, servletresponse);
  }

}
