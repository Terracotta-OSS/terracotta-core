/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config;

import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Responsible for creating a TcConfigDocument of the current version from any published configuration schema version.
 */
public final class Loader {

  private com.terracottatech.config.TcConfigDocument convert(InputStream in, XmlOptions xmlOptions) throws IOException,
      XmlException {
    byte[] data = new byte[in.available()];
    in.read(data);
    in.close();
    ByteArrayInputStream ain = new ByteArrayInputStream(data);
    SchemaType type = com.terracottatech.config.TcConfigDocument.type;
    if (xmlOptions == null) xmlOptions = new XmlOptions();
    xmlOptions.setDocumentType(type);
    return com.terracottatech.config.TcConfigDocument.Factory.parse(ain, xmlOptions);
  }

  public com.terracottatech.config.TcConfigDocument parse(File file) throws IOException, XmlException {
    return convert(new FileInputStream(file), null);
  }

  public com.terracottatech.config.TcConfigDocument parse(File file, XmlOptions xmlOptions) throws IOException,
      XmlException {
    return convert(new FileInputStream(file), xmlOptions);
  }

  public com.terracottatech.config.TcConfigDocument parse(String xmlText) throws IOException, XmlException {
    return convert(new ByteArrayInputStream(xmlText.getBytes()), null);
  }

  public com.terracottatech.config.TcConfigDocument parse(String xmlText, XmlOptions xmlOptions) throws IOException,
      XmlException {
    return convert(new ByteArrayInputStream(xmlText.getBytes()), xmlOptions);
  }

  public com.terracottatech.config.TcConfigDocument parse(InputStream stream) throws IOException, XmlException {
    return convert(stream, null);
  }

  public com.terracottatech.config.TcConfigDocument parse(InputStream stream, XmlOptions xmlOptions)
      throws IOException, XmlException {
    return convert(stream, xmlOptions);
  }

  public com.terracottatech.config.TcConfigDocument parse(URL url) throws IOException, XmlException {
    return convert(url.openStream(), null);
  }

  public com.terracottatech.config.TcConfigDocument parse(URL url, XmlOptions xmlOptions) throws IOException,
      XmlException {
    return convert(url.openStream(), xmlOptions);
  }

}
