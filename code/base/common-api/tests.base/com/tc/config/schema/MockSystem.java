/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema;

import org.apache.xmlbeans.XmlBoolean;

import com.terracottatech.config.ConfigurationModel;
import com.terracottatech.config.License;
import com.terracottatech.config.System;
import com.terracottatech.config.ConfigurationModel.Enum;

/**
 * A mock {@link System}, for use in tests.
 */
public class MockSystem extends MockXmlObject implements System {

  public MockSystem() {
    super();
  }

  public boolean isSetDsoEnabled() {
    return false;
  }

  public boolean isSetJdbcEnabled() {
    return false;
  }

  public boolean isSetHttpEnabled() {
    return false;
  }

  public boolean isSetJmxEnabled() {
    return false;
  }

  public boolean isSetJmxHttpEnabled() {
    return false;
  }

  public void unsetDsoEnabled() {
    // nothing here
  }

  public void unsetJdbcEnabled() {
    // nothing here
  }

  public void unsetHttpEnabled() {
    // nothing here
  }

  public void unsetJmxEnabled() {
    // nothing here
  }

  public void unsetJmxHttpEnabled() {
    // nothing here
  }

  public boolean getJdbcEnabled() {
    return false;
  }

  public XmlBoolean xgetJdbcEnabled() {
    return null;
  }

  public void setJdbcEnabled(boolean arg0) {
    // nothing here
  }

  public void xsetJdbcEnabled(XmlBoolean arg0) {
    // nothing here
  }

  public boolean getDsoEnabled() {
    return false;
  }

  public XmlBoolean xgetDsoEnabled() {
    return null;
  }

  public void setDsoEnabled(boolean arg0) {
    // nothing here
  }

  public void xsetDsoEnabled(XmlBoolean arg0) {
    // nothing here
  }

  public License getLicense() {
    return null;
  }

  public boolean isSetLicense() {
    return false;
  }

  public void setLicense(License arg0) {
    // nothing here
  }

  public License addNewLicense() {
    return null;
  }

  public void unsetLicense() {
    // nothing here
  }

  public Enum getConfigurationModel() {
    return null;
  }

  public ConfigurationModel xgetConfigurationModel() {
    return null;
  }

  public boolean isSetConfigurationModel() {
    return false;
  }

  public void setConfigurationModel(Enum arg0) {
    // nothing here
  }

  public void xsetConfigurationModel(ConfigurationModel arg0) {
    // nothing here
  }

  public void unsetConfigurationModel() {
    // nothing here
  }

  public boolean getHttpEnabled() {
    return false;
  }

  public boolean getJmxEnabled() {
    return false;
  }

  public boolean getJmxHttpEnabled() {
    return false;
  }

  public void setHttpEnabled(boolean arg0) {
    // nothing here
  }

  public void setJmxEnabled(boolean arg0) {
    // nothing here
  }

  public void setJmxHttpEnabled(boolean arg0) {
    // nothing here
  }

  public XmlBoolean xgetHttpEnabled() {
    return null;
  }

  public XmlBoolean xgetJmxEnabled() {
    return null;
  }

  public XmlBoolean xgetJmxHttpEnabled() {
    return null;
  }

  public void xsetHttpEnabled(XmlBoolean arg0) {
    // nothing here
  }

  public void xsetJmxEnabled(XmlBoolean arg0) {
    // nothing here
  }

  public void xsetJmxHttpEnabled(XmlBoolean arg0) {
    // nothing here
  }

}
