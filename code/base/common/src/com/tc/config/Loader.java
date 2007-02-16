/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config;

import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;

import com.tc.config.schema.migrate.ConfigUpdate;
import com.tc.config.schema.migrate.V1toV2;
import com.tc.config.schema.migrate.V2toV3;

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

  private final ConfigUpdate[] converters;

  public Loader() {
    // order newest to oldest -- don't ever hotswap converters
    converters = new ConfigUpdate[] { new V2toV3(), new V1toV2() };
  }

  private com.terracottatech.config.TcConfigDocument convert(InputStream in, XmlOptions xmlOptions) throws IOException,
      XmlException {
    byte[] data = new byte[in.available()];
    in.read(data);
    in.close();
    ByteArrayInputStream ain = new ByteArrayInputStream(data);
    SchemaType type = com.terracottatech.config.TcConfigDocument.type;
    if (xmlOptions == null) xmlOptions = new XmlOptions();
    xmlOptions.setDocumentType(type);
    try {
      return com.terracottatech.config.TcConfigDocument.Factory.parse(ain, xmlOptions);
    } catch (XmlException e) {
      ain.reset();
      return com.terracottatech.config.TcConfigDocument.Factory.parse(updateConfig(ain, 0, xmlOptions), xmlOptions);
    }
  }

  private synchronized InputStream updateConfig(InputStream in, int index, XmlOptions xmlOptions) throws IOException,
      XmlException {
    byte[] data = new byte[in.available()];
    in.read(data);
    in.close();
    ByteArrayInputStream ain = new ByteArrayInputStream(data);
    try {
      return converters[index].convert(ain, xmlOptions);
    } catch (XmlException e) {
      if (index == converters.length - 1) throw e;
      ain.reset();
      return converters[index].convert(updateConfig(ain, index + 1, xmlOptions), xmlOptions);
    }
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

  public boolean testIsOld(File file) throws IOException {
    return !testIsCurrent(file);
  }

  public boolean testIsCurrent(File file) throws IOException {
    try {
      com.terracottatech.config.TcConfigDocument.Factory.parse(new FileInputStream(file));
      return true;
    } catch (XmlException e) {
      return false;
    }
  }

  public void updateToCurrent(File file) throws IOException, XmlException {
    XmlOptions options = null;
    synchronized (converters) {
      options = converters[0].createDefaultXmlOptions();
    }
    com.terracottatech.config.TcConfigDocument doc = convert(new FileInputStream(file), options);
    doc.save(file);
  }
}
