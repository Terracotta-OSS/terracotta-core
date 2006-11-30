package org.springframework.web.context.request;

import javax.servlet.http.HttpServletRequest;

public class HttpRequestAccessor {
  public static HttpServletRequest getRequest(ServletRequestAttributes attributes) {
    return attributes.getRequest();
  }
}
