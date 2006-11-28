/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.config.schema.repository;

import org.apache.xmlbeans.XmlObject;

/**
 * Knows how to fetch a child of a bean.
 */
public interface ChildBeanFetcher {

  XmlObject getChild(XmlObject parent);
  
}
