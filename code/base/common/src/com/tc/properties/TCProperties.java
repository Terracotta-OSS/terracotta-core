/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.properties;

public interface TCProperties {

  int getInt(String key);

  long getLong(String key);

  boolean getBoolean(String key);

  String getProperty(String key);

  TCProperties getPropertiesFor(String key);

  String getProperty(String key, boolean missingOkay);

}
