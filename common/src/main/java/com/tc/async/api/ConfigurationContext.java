/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.async.api;

import com.tc.logging.TCLogger;

public interface ConfigurationContext {

  public Stage getStage(String name);

  public TCLogger getLogger(Class clazz);

}