/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.license.util;

import java.util.Map;

public interface LicenseField {

  public void setRawValue(String value) throws LicenseException;

  public Object getValue();

  public String getName();

  public String getType();

  public String getPattern();

  public boolean isRequired();

  public Map getRange();

}