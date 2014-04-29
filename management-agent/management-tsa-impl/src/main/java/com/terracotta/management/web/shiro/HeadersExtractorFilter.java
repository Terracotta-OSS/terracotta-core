/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.management.web.shiro;

import org.terracotta.management.ServiceLocator;

import com.terracotta.management.security.IACredentials;
import com.terracotta.management.security.SecurityContextService;
import com.terracotta.management.service.TimeoutService;
import com.terracotta.management.web.utils.TSAConfig;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

/**
 * A servlet filter that extracts various headers from the request and populates Services with their values.

 * @author Ludovic Orban
 */
public class HeadersExtractorFilter implements Filter {

  private final TimeoutService timeoutService = ServiceLocator.locate(TimeoutService.class);
  private final SecurityContextService securityContextService = ServiceLocator.locate(SecurityContextService.class);

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

    if (TSAConfig.isSslEnabled()) {
      String reqTicket = httpServletRequest.getHeader(IACredentials.REQ_TICKET);
      String signature = httpServletRequest.getHeader(IACredentials.SIGNATURE);
      String alias = httpServletRequest.getHeader(IACredentials.ALIAS);
      String token = httpServletRequest.getHeader(IACredentials.TC_ID_TOKEN);
      securityContextService.setSecurityContext(new SecurityContextService.SecurityContext(reqTicket, signature, alias, token));
    }

    try {
      chain.doFilter(request, response);
    } finally {
      if (TSAConfig.isSslEnabled()) {
        securityContextService.clearSecurityContext();
      }
      timeoutService.clearCallTimeout();
    }
  }

}
