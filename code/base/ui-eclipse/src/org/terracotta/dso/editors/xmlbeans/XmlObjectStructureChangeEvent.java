/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package org.terracotta.dso.editors.xmlbeans;

import org.apache.xmlbeans.XmlObject;

import java.util.EventObject;

public class XmlObjectStructureChangeEvent extends EventObject {
  public XmlObjectStructureChangeEvent() {
    super(new Object());
  }

  public XmlObjectStructureChangeEvent(XmlObject source) {
    super(source);
  }
  
  public void setXmlObject(XmlObject source) {
    this.source = source;
  }
  
  public XmlObject getXmlObject() {
    return (XmlObject)getSource();
  }
}
