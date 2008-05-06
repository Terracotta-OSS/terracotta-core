/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.beanfactory;

import org.apache.xmlbeans.XmlException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Knows how to turn a stream containing XML into the appropriate XMLBean.
 */
public interface ConfigBeanFactory {

  BeanWithErrors createBean(InputStream in, String sourceDescription) throws IOException, SAXException,
      ParserConfigurationException, XmlException;

}
