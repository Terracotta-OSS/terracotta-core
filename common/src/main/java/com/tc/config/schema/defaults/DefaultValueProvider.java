/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
