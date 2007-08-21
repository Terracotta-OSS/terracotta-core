/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.weblogic;

import weblogic.j2ee.descriptor.FilterBean;
import weblogic.j2ee.descriptor.FilterMappingBean;

public class DSOFilterMappingBean implements FilterMappingBean {

  private final FilterBean filter;
  
  public DSOFilterMappingBean(FilterBean filter) {
    this.filter = filter;
  }
  
  public FilterBean getFilter() {
    return filter;
  }
  
  public void addDispatcher(String s) {
    throw new AssertionError();
  }

  public String[] getDispatchers() {
    return new String[0];
  }

  public String getFilterName() {
    return "Terracotta Session Filter";
  }

  public String getId() {
    throw new AssertionError();
  }

  public String getServletName() {
    return "Terracotta";
  }

  public String getUrlPattern() {
    return "/*";
  }

  public void removeDispatcher(String s) {
    throw new AssertionError();
  }

  public void setDispatchers(String[] as) {
    throw new AssertionError();
  }

  public void setFilterName(String s) {
    throw new AssertionError();
  }

  public void setId(String s) {
    throw new AssertionError();
  }

  public void setServletName(String s) {
    throw new AssertionError();
  }

  public void setUrlPattern(String s) {
    throw new AssertionError();
  }

}
