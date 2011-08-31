/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
