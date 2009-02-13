/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.validate;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;

public class MockConfigurationValidator implements ConfigurationValidator {

  private int          numValidates;
  private XmlObject    lastBean;
  private XmlException thrownException;

  public MockConfigurationValidator() {
    this.thrownException = null;

    reset();
  }

  public void reset() {
    this.numValidates = 0;
    this.lastBean = null;
  }

  public void validate(XmlObject bean) throws XmlException {
    ++this.numValidates;
    this.lastBean = bean;
    if (this.thrownException != null) throw this.thrownException;
  }

  public XmlObject getLastBean() {
    return lastBean;
  }

  public int getNumValidates() {
    return numValidates;
  }

  public void setThrownException(XmlException thrownException) {
    this.thrownException = thrownException;
  }

}
