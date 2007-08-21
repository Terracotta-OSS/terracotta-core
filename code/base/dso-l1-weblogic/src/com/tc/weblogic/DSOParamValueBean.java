/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.weblogic;

import com.terracotta.session.SessionFilter;

import weblogic.j2ee.descriptor.ParamValueBean;

public class DSOParamValueBean implements ParamValueBean {

  public void addDescription(String s) {
    throw new AssertionError();
  }

  public String[] getDescriptions() {
    return new String[] {"Session Filter parameters for BEA Weblogic"};
  }

  public String getParamName() {
    return SessionFilter.APP_SERVER_PARAM_NAME;
  }

  public String getParamValue() {
    return SessionFilter.BEA_WEBLOGIC;
  }

  public String getId() {
    return "Session Filter App Server Type";
  }

  public void removeDescription(String s) {
    throw new AssertionError();
  }

  public void setDescriptions(String[] as) {
    throw new AssertionError();
  }

  public void setId(String s) {
    throw new AssertionError();
  }

  public void setParamName(String s) {
    throw new AssertionError();
  }

  public void setParamValue(String s) {
    throw new AssertionError();
  }

}
