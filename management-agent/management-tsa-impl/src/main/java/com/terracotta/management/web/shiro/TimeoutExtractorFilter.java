/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.management.web.shiro;

import org.terracotta.management.ServiceLocator;

import com.terracotta.management.service.TimeoutService;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

/**
 * A servlet filter that extracts the timeout from the request and sets it on the TimeoutService.

 * @author Ludovic Orban
 */
public class TimeoutExtractorFilter implements Filter {

  private final TimeoutService timeoutService = ServiceLocator.locate(TimeoutService.class);

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void destroy() {
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest httpServletRequest = (HttpServletRequest)request;

    String readTimeoutHeader = httpServletRequest.getHeader("X-Terracotta-Read-Timeout");
    if (readTimeoutHeader != null) {
      try {
        long readTimeout = Long.parseLong(readTimeoutHeader);
        timeoutService.setCallTimeout(readTimeout);
      } catch (NumberFormatException nfe) {
        //
      }
    }

    try {
      chain.doFilter(request, response);
    } finally {
      timeoutService.clearCallTimeout();
    }
  }

}
