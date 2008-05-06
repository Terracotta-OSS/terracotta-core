/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.defaults;

import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;

/**
 * Knows how to tell you the default value for an element in the config.
 */
public interface DefaultValueProvider {

  boolean possibleForXPathToHaveDefault(String xpath);
  
  XmlObject defaultFor(SchemaType baseType, String xpath) throws XmlException;
  
  boolean hasDefault(SchemaType baseType, String xpath) throws XmlException;
  
  boolean isOptional(SchemaType baseType, String xpath) throws XmlException;

}
