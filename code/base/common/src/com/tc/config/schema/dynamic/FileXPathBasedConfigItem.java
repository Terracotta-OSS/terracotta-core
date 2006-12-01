/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.dynamic;

import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.context.ConfigContext;

import java.io.File;

/**
 * An {@link XPathBasedConfigItem} that returns its data as a {@link File}. The data should be expressed in XML as some
 * variant of an <code>xs:string</code>.
 */
public class FileXPathBasedConfigItem extends XPathBasedConfigItem implements FileConfigItem {

  private final File relativeTo;

  public FileXPathBasedConfigItem(ConfigContext context, String xpath, File relativeTo) {
    super(context, xpath);
    this.relativeTo = relativeTo;
  }

  public FileXPathBasedConfigItem(ConfigContext context, String xpath) {
    this(context, xpath, null);
  }

  protected Object fetchDataFromXmlObject(XmlObject xmlObject) {
    String theString = (String) super.fetchDataFromXmlObjectByReflection(xmlObject, "getStringValue");
    if (theString == null || theString.trim().length() == 0) return null;

    File out = new File(theString);
    if (this.relativeTo != null && !out.isAbsolute()) out = new File(this.relativeTo, theString);
    return out;
  }
  
  protected final File relativeTo() {
    return this.relativeTo;
  }

  public File getFile() {
    return (File) getObject();
  }

}
