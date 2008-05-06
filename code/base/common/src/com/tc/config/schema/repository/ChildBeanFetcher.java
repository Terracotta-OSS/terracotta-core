/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.repository;

import org.apache.xmlbeans.XmlObject;

/**
 * Knows how to fetch a child of a bean.
 */
public interface ChildBeanFetcher {

  XmlObject getChild(XmlObject parent);
  
}
