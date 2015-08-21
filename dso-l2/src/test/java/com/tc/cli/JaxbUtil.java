/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.cli;

import org.terracotta.config.util.DefaultSubstitutor;
public class JaxbUtil {

  /**
   * Make a new instance of the given type and populate any defaults declared from the schema. NOTE: This method is expensive, invoke at your own risk
   */
  public static <T> T newInstanceWithDefaults(Class<T> type) {
    T instance;
    try {
      instance = type.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    DefaultSubstitutor.applyDefaults(instance);
    return instance;
  }

  private JaxbUtil() {
    //
  }

}
