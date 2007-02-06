/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;

import com.tc.config.schema.migrate.V1toV2;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Responsible for creating a TcConfigDocument of the current version from any
 * published configuration schema version.
 */

public class Loader {
  private V1toV2 v1toV2Converter;
  
  public Loader() {
    v1toV2Converter = new V1toV2();
  }
  
  public com.terracottatech.config.TcConfigDocument parse(File file)
    throws IOException, XmlException
  {
    return v1toV2Converter.parse(file);
  }
  
  public com.terracottatech.config.TcConfigDocument parse(File file, XmlOptions xmlOptions)
    throws IOException, XmlException
  {
    return v1toV2Converter.parse(file, xmlOptions);
  }

  public com.terracottatech.config.TcConfigDocument parse(String xmlText)
    throws IOException, XmlException
  {
    return v1toV2Converter.parse(xmlText);
  }

  public com.terracottatech.config.TcConfigDocument parse(String xmlText, XmlOptions xmlOptions)
    throws XmlException
  {
    return v1toV2Converter.parse(xmlText, xmlOptions);
  }

  public com.terracottatech.config.TcConfigDocument parse(InputStream stream)
    throws IOException, XmlException
  {
    return v1toV2Converter.parse(stream);
  }
  
  public com.terracottatech.config.TcConfigDocument parse(InputStream stream, XmlOptions xmlOptions)
    throws IOException, XmlException
  {
    return v1toV2Converter.parse(stream, xmlOptions);
  }

  public com.terracottatech.config.TcConfigDocument parse(URL url)
    throws IOException, XmlException
  {
    return v1toV2Converter.parse(url);
  }
  
  public com.terracottatech.config.TcConfigDocument parse(URL url, XmlOptions xmlOptions)
    throws IOException, XmlException
  {
    return v1toV2Converter.parse(url, xmlOptions);
  }

  public boolean testIsOld(File file) throws IOException, XmlException {
    try {
      return v1toV2Converter.testIsV1(file);
    } catch(XmlException xmle) {
      return false;
    }
  }
  
  public boolean testIsCurrent(File file) throws IOException, XmlException {
    try {
      v1toV2Converter.testIsV2(file);
      return true;
    } catch(XmlException xmle) {
      return false;
    }
  }

  public boolean updateToCurrent(File file) throws IOException, XmlException {
    try {
      v1toV2Converter.update(file);
      return true;
    } catch(XmlException xmle) {
      return false;
    }
  }
}
