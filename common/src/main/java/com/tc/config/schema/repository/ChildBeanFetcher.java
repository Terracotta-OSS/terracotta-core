/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.repository;

/**
 * Knows how to fetch a child of a bean.
 */
public interface ChildBeanFetcher {

  Object getChild(Object parent);
  
}
