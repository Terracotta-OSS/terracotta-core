/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package org.springframework.web.context.request;

import javax.servlet.http.HttpServletRequest;

public class HttpRequestAccessor {
  public static HttpServletRequest getRequest(ServletRequestAttributes attributes) {
    return attributes.getRequest();
  }
}
