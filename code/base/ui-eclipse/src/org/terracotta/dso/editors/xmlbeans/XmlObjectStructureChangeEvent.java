/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
