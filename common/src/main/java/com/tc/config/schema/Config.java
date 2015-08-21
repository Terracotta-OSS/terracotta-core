/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema;

/**
 * An interface implemented by all config objects.
 */
public interface Config<T> {
  
  T getBean();
  
}
