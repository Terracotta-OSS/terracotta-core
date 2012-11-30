/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package com.terracotta.management.web.shiro;

import org.apache.shiro.web.servlet.ShiroFilter;

import com.terracotta.management.web.utils.TSAConfig;

/**
 * @author brandony
 */
public final class TSASecurityFilter extends ShiroFilter {
  @Override
  public void init() throws Exception {
    if (TSAConfig.isSslEnabled()) {
      super.init();
    }
  }
}
