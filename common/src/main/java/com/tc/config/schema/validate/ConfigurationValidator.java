/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.validate;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;

/**
 * An object that knows how to validate some property of an {@link XmlObject} bean. (The bean it will be given is the
 * top-level bean of whatever repository it's attached to}.
 */
public interface ConfigurationValidator {

  void validate(XmlObject bean) throws XmlException;
  
}
