/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors.xmlbeans;

import java.text.NumberFormat;
import java.text.ParseException;

public class XmlIntegerField extends XmlStringField {
  public XmlIntegerField() {
    super();
  }
  
  public void set() {
    try {
      int i = NumberFormat.getIntegerInstance().parse(getText()).intValue();
      setText(Integer.toString(i));
    } catch(ParseException nfe) {/**/}
    
    super.set();
  }
}
