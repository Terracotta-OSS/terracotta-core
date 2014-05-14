/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.security.impl;

import com.terracotta.management.security.SecurityContextService;

/**
 * @author Ludovic Orban
 */
public class DfltSecurityContextService implements SecurityContextService {

  private final ThreadLocal<SecurityContext> contextThreadLocal = new ThreadLocal<SecurityContext>();

  @Override
  public void setSecurityContext(SecurityContext securityContext) {
    contextThreadLocal.set(securityContext);
  }

  @Override
  public SecurityContext getSecurityContext() {
    return contextThreadLocal.get();
  }

  @Override
  public void clearSecurityContext() {
    contextThreadLocal.remove();
  }

}
