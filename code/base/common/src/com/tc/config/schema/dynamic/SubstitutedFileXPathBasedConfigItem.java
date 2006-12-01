/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.dynamic;

import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.context.ConfigContext;

import java.io.File;

/**
 * A {@link com.tc.config.schema.dynamic.FileXPathBasedConfigItem} that uses the
 * {@link com.tc.config.schema.dynamic.ParameterSubstituter} to substitute values before processing.
 */
public class SubstitutedFileXPathBasedConfigItem extends FileXPathBasedConfigItem {

  public SubstitutedFileXPathBasedConfigItem(ConfigContext context, String xpath, File relativeTo) {
    super(context, xpath, relativeTo);
  }

  public SubstitutedFileXPathBasedConfigItem(ConfigContext context, String xpath) {
    super(context, xpath);
  }

  protected Object fetchDataFromXmlObject(XmlObject xmlObject) {
    String theString = (String) super.fetchDataFromXmlObjectByReflection(xmlObject, "getStringValue");
    if (theString == null || theString.trim().length() == 0) return null;
    String substituted = ParameterSubstituter.substitute(theString);

    File out = new File(substituted);
    if (relativeTo() != null && !out.isAbsolute()) out = new File(relativeTo(), substituted);
    return out;
  }

}
