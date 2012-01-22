/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
