/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.util.TIMUtil;

import java.util.Date;

public class EhcacheGlobalEviction130Test extends EhcacheGlobalEvictionTestBase {

  public EhcacheGlobalEviction130Test() {
    disableAllUntil(new Date(Long.MAX_VALUE));
  }

  protected Class getApplicationClass() {
    return EhcacheGlobalEviction130TestApp.class;
  }

  protected String getEhcacheVersion() {
    return TIMUtil.EHCACHE_1_3;
  }

}
