/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.weblogic;

import weblogic.management.descriptors.webapp.FilterMBean;
import weblogic.management.descriptors.webapp.FilterMappingMBean;
import weblogic.management.descriptors.webapp.ParameterMBean;
import weblogic.management.descriptors.webapp.ServletMBean;
import weblogic.management.descriptors.webapp.UIMBean;
import weblogic.servlet.internal.WebAppServletContext;

import com.tc.aspectwerkz.joinpoint.StaticJoinPoint;
import com.tc.object.bytecode.hook.impl.ClassProcessorHelper;
import com.terracotta.session.SessionFilter;

public class SessionAspect {

  private final ThreadLocal dsoFilter = new ThreadLocal();

  public Object addFilterIfNeeded(StaticJoinPoint jp, weblogic.servlet.internal.WebAppServletContext context)
      throws Throwable {
    FilterMBean[] filters = (FilterMBean[]) jp.proceed();
    if (filters == null) {
      filters = new FilterMBean[] {};
    }

    if (isDSOSesisons(context)) {
      FilterMBean filter = new DSOFilterMBean();
      setFilterInstance(context, filter);

      FilterMBean[] withDSO = new FilterMBean[filters.length + 1];
      System.arraycopy(filters, 0, withDSO, 1, filters.length);
      withDSO[0] = filter;
      filters = withDSO;
    }
    return filters;
  }

  public Object addFilterMappingIfNeeded(StaticJoinPoint jp, weblogic.servlet.internal.WebAppServletContext context)
      throws Throwable {
    FilterMappingMBean[] mappings = (FilterMappingMBean[]) jp.proceed();
    if (mappings == null) {
      mappings = new FilterMappingMBean[] {};
    }

    if (isDSOSesisons(context)) {
      FilterMappingMBean[] withDSO = new FilterMappingMBean[mappings.length + 1];
      System.arraycopy(mappings, 0, withDSO, 1, mappings.length);
      withDSO[0] = new DSOFilterMappingMBean(getFilterInstance(context));
      mappings = withDSO;
    }
    return mappings;
  }

  private FilterMBean getFilterInstance(WebAppServletContext context) {
    FilterMBean filter = (FilterMBean) dsoFilter.get();
    if (filter == null) { throw new AssertionError("No filter found for " + context.getName()); }
    dsoFilter.set(null);
    return filter;
  }

  private void setFilterInstance(WebAppServletContext context, FilterMBean filter) {
    if (dsoFilter.get() != null) { throw new AssertionError("Filter already present for " + context.getName()); }
    dsoFilter.set(filter);
  }

  private static boolean isDSOSesisons(WebAppServletContext context) {
    String appName = context.getName();
    boolean rv = ClassProcessorHelper.isDSOSessions(appName);
    return rv;
  }

  private static class DSOFilterMappingMBean implements FilterMappingMBean {

    private final FilterMBean filter;

    public DSOFilterMappingMBean(FilterMBean filter) {
      this.filter = filter;
    }

    public FilterMBean getFilter() {
      return filter;
    }

    public ServletMBean getServlet() {
      return null;
    }

    public String getUrlPattern() {
      return "/*";
    }

    public void setFilter(FilterMBean filtermbean) {
      throw new AssertionError();
    }

    public void setServlet(ServletMBean servletmbean) {
      throw new AssertionError();
    }

    public void setUrlPattern(String s) {
      throw new AssertionError();
    }

    public void addDescriptorError(String s) {
      throw new AssertionError();
    }

    public String[] getDescriptorErrors() {
      throw new AssertionError();
    }

    public boolean isValid() {
      throw new AssertionError();
    }

    public void removeDescriptorError(String s) {
      throw new AssertionError();
    }

    public void setDescriptorErrors(String[] as) {
      throw new AssertionError();
    }

    public String getName() {
      throw new AssertionError();
    }

    public void register() {
      throw new AssertionError();
    }

    public void setName(String s) {
      throw new AssertionError();
    }

    public String toXML(int i) {
      throw new AssertionError();
    }

    public void unregister() {
      throw new AssertionError();
    }

  }

  private static class DSOFilterMBean implements FilterMBean {

    public void addInitParam(ParameterMBean parametermbean) {
      //
    }

    public String getFilterClass() {
      return SessionFilter.FILTER_CLASS;
    }

    public String getFilterName() {
      return SessionFilter.FILTER_NAME;
    }

    public ParameterMBean[] getInitParams() {
      return new ParameterMBean[] {};
    }

    public UIMBean getUIData() {
      throw new AssertionError();
    }

    public void removeInitParam(ParameterMBean parametermbean) {
      throw new AssertionError();
    }

    public void setFilterClass(String s) {
      throw new AssertionError();
    }

    public void setFilterName(String s) {
      throw new AssertionError();
    }

    public void setInitParams(ParameterMBean[] aparametermbean) {
      throw new AssertionError();
    }

    public void setUIData(UIMBean uimbean) {
      throw new AssertionError();
    }

    public void addDescriptorError(String s) {
      throw new AssertionError();
    }

    public String[] getDescriptorErrors() {
      throw new AssertionError();
    }

    public boolean isValid() {
      throw new AssertionError();
    }

    public void removeDescriptorError(String s) {
      throw new AssertionError();
    }

    public void setDescriptorErrors(String[] as) {
      throw new AssertionError();
    }

    public String getName() {
      throw new AssertionError();
    }

    public void register() {
      throw new AssertionError();
    }

    public void setName(String s) {
      throw new AssertionError();
    }

    public String toXML(int i) {
      throw new AssertionError();
    }

    public void unregister() {
      throw new AssertionError();
    }

  }

}
