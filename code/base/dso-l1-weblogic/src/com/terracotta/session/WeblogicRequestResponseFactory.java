/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.terracotta.session;

import weblogic.servlet.internal.ServletResponseImpl;
import weblogic.servlet.internal.TerracottaServletResponseImpl;

import javax.servlet.http.HttpServletResponse;

public class WeblogicRequestResponseFactory extends BaseRequestResponseFactory {

  public TerracottaResponse createResponse(TerracottaRequest request, HttpServletResponse response) {
    return new TerracottaServletResponseImpl(request, (ServletResponseImpl) response);
  }

  // As some point we'll probably need to subclass Weblogic's request type too ;-)

}
