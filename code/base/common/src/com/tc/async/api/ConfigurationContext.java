package com.tc.async.api;

import com.tc.logging.TCLogger;

public interface ConfigurationContext {

  public Stage getStage(String name);

  public TCLogger getLogger(Class clazz);

}