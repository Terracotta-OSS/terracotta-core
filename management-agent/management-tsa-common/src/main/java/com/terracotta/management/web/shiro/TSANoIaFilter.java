/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.management.web.shiro;

import org.terracotta.management.embedded.NoIaFilter;

import com.terracotta.management.web.utils.TSAConfig;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * A servlet filter that prevents secure requests from being processed by unsecure agents. On secure agents,
 * it's a no-op.
 *
 * @author Ludovic Orban
 */
public class TSANoIaFilter extends NoIaFilter {

  public static final boolean SSL_ENABLED = TSAConfig.isSslEnabled();

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    if (!SSL_ENABLED) {
      super.doFilter(request, response, chain);
    } else {
      chain.doFilter(request, response);
    }
  }

}
