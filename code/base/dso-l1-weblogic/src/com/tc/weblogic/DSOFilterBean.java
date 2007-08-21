/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.weblogic;

import weblogic.j2ee.descriptor.FilterBean;
import weblogic.j2ee.descriptor.IconBean;
import weblogic.j2ee.descriptor.ParamValueBean;

import com.terracotta.session.SessionFilter;

public class DSOFilterBean implements FilterBean {

  public void addDescription(String s) {
    throw new AssertionError();
  }

  public void addDisplayName(String s) {
    throw new AssertionError();
  }

  public IconBean createIcon() {
    throw new AssertionError();
  }

  public ParamValueBean createInitParam() {
    return null;
  }

  public void destroyIcon(IconBean iconbean) {
    throw new AssertionError();
  }

  public void destroyInitParam(ParamValueBean paramvaluebean) {
    throw new AssertionError();
  }

  public String[] getDescriptions() {
    return new String[] {};
  }

  public String[] getDisplayNames() {
    return new String[] {};
  }

  public String getFilterClass() {
    return SessionFilter.FILTER_CLASS;
  }

  public String getFilterName() {
    return SessionFilter.FILTER_NAME;
  }

  public IconBean[] getIcons() {
    return new IconBean[] {};
  }

  public String getId() {
    throw new AssertionError();
  }

  public ParamValueBean[] getInitParams() {
    return new ParamValueBean[] { new DSOParamValueBean() };
  }

  public void removeDescription(String s) {
    throw new AssertionError();
  }

  public void removeDisplayName(String s) {
    throw new AssertionError();
  }

  public void setDescriptions(String[] as) {
    throw new AssertionError();
  }

  public void setDisplayNames(String[] as) {
    throw new AssertionError();
  }

  public void setFilterClass(String s) {
    throw new AssertionError();
  }

  public void setFilterName(String s) {
    throw new AssertionError();
  }

  public void setId(String s) {
    throw new AssertionError();
  }

}
