/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.weblogic;

import weblogic.j2ee.descriptor.FilterBean;
import weblogic.j2ee.descriptor.FilterMappingBean;
import weblogic.servlet.internal.WebAppServletContext;

import com.tc.aspectwerkz.joinpoint.StaticJoinPoint;
import com.tc.object.bytecode.hook.impl.ClassProcessorHelper;

public class SessionAspectWL9 {

  /**
   * Used in WL 9.2 to insert the Terracotta SessionFilter.
   * @param jp The Aspectwerkz join point
   * @param filterManager The Weblogic 9.2 FilterManager - does not exist in WL 8.
   * @return The FilterMBean[] expected to be returned from the original call.
   */
  public Object addFilterIfNeeded(StaticJoinPoint jp, weblogic.servlet.internal.FilterManager filterManager) 
  throws Throwable {
    
    FilterBean[] filters = (FilterBean[]) jp.proceed();
    if (filters == null) {
      filters = new FilterBean[] {};
    }

    WebAppServletContext context = filterManager.__tc_getContext();
    if (isDSOSessions(context)) {

      FilterBean filter = new DSOFilterBean();
      FilterBean[] withDSO = new FilterBean[filters.length + 1];
      System.arraycopy(filters, 0, withDSO, 1, filters.length);
      withDSO[0] = filter;
      filters = withDSO;
    }

    return filters;
  }
 
  /**
   * Used in WL 9.2 to insert the Terracotta SessionFilter.
   * @param jp The Aspectwerkz join point
   * @param filterManager The Weblogic 9.2 FilterManager - does not exist in WL 8.
   * @return The FilterMappingMBean[] expected to be returned from the original call.
   */
  public Object addFilterMappingIfNeeded(StaticJoinPoint jp, weblogic.servlet.internal.FilterManager filterManager) 
  throws Throwable {
    FilterMappingBean[] mappings = (FilterMappingBean[]) jp.proceed();
    if (mappings == null) {
      mappings = new FilterMappingBean[] {};
    }

    WebAppServletContext context = filterManager.__tc_getContext();
    if (isDSOSessions(context)) {
      FilterMappingBean[] withDSO = new FilterMappingBean[mappings.length + 1];
      System.arraycopy(mappings, 0, withDSO, 1, mappings.length);
      withDSO[0] = new DSOFilterMappingBean(new DSOFilterBean());
      mappings = withDSO;
    }
    return mappings;
  }
 
  private static boolean isDSOSessions(WebAppServletContext context) {
    // Get context path and strip leading /
    String appName = context.getContextPath();
    if(appName.startsWith("/")) {
      appName = appName.substring(1, appName.length());
    }
    
    boolean rv = ClassProcessorHelper.isDSOSessions(appName);
    return rv;
  }

}
