/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.appserver.unit;

import java.util.Map;

import javax.servlet.Filter;

/**
 * Holder that combinedsa servlet filter class with the configuration that is
 * required to register the filter in a web.xml file.
 */
public class TCServletFilterHolder  {
  private Class filterClass;
  private String pattern;
  private Map initParams;
  
  public TCServletFilterHolder(Class filterClass, String pattern, Map initParams) {
    this.filterClass = filterClass;
    this.pattern = pattern;
    this.initParams = initParams;
  }

  public Class getFilterClass() {
    return filterClass;
  }
  
  public Map getInitParams() {
    return initParams;
  }
  
  public String getPattern() {
    return pattern;
  }
  
  public void setFilterClass(Class filterClass) {
    this.filterClass = filterClass;
  }
  
  public void setInitParams(Map initParams) {
    this.initParams = initParams;
  }
  
  public void setPattern(String pattern) {
    this.pattern = pattern;
  }

  public boolean isFilter() {
    return Filter.class.isAssignableFrom(filterClass);
  }
}
