/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema.migrate;

import org.apache.xmlbeans.XmlOptions;

public abstract class BaseConfigUpdate implements ConfigUpdate {

  public XmlOptions createDefaultXmlOptions() {
    XmlOptions opts = new XmlOptions();
    opts.setLoadLineNumbers();
    opts.setValidateOnSet();
    opts.setSavePrettyPrint();
    opts.setSavePrettyPrintIndent(3);
    opts.remove(XmlOptions.LOAD_STRIP_WHITESPACE);
    opts.remove(XmlOptions.LOAD_STRIP_COMMENTS);
    return opts;
  }
}
