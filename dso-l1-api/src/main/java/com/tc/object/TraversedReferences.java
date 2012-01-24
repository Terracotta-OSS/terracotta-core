/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object;

import java.util.Iterator;

/**
 * Track references that were traversed
 */
public interface TraversedReferences {
  /**
   * Add an anonymous reference traversal, usually in a logical applicator
   * @param o The object traversed
   */
  public void addAnonymousReference(Object o);
  
  /**
   * Add a named reference traversal, usually in a physical applicator
   * @param className Class name
   * @param fieldName Field name
   * @param value Object value in field
   */
  public void addNamedReference(String className, String fieldName, Object value);
  
  /**
   * Add a named reference traversal, usually in a physical applicator
   * @param fullyQualifiedFieldName Full field name, which can be split into class and field
   * @param value Object value in field
   */
  public void addNamedReference(String fullyQualifiedFieldname, Object value);
  
  /**
   * Walk through the references traversed
   * @return Iterator<TraversedReference>
   */
  public Iterator iterator();
}
