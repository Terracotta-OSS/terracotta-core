/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema.context;

import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.IllegalConfigurationChangeHandler;
import com.tc.config.schema.dynamic.ConfigItem;

public class MockConfigContext implements ConfigContext {

  private int                               numIllegalConfigurationChangeHandlers;
  private IllegalConfigurationChangeHandler returnedIllegalConfigurationChangeHandler;

  private int                               numEnsureRepositoryProvides;
  private Class                             lastEnsureClass;
  private RuntimeException                  exceptionOnEnsureRepositoryProvides;

  private int                               numItemCreateds;
  private ConfigItem                        lastItemCreated;

  private int                               numHasDefaultFors;
  private String                            lastHasDefaultForXPath;
  private boolean                           returnedHasDefaultFor;

  private int                               numBeans;
  private XmlObject                         returnedBean;

  private int                               numIsOptionals;
  private String                            lastIsOptionalXPath;
  private boolean                           returnedIsOptional;

  private int                               numDefaultFors;
  private String                            lastDefaultForXPath;
  private XmlObject                         returnedDefaultFor;

  public MockConfigContext() {
    this.returnedIllegalConfigurationChangeHandler = null;
    this.exceptionOnEnsureRepositoryProvides = null;
    this.returnedBean = null;
    this.returnedHasDefaultFor = false;
    this.returnedIsOptional = false;
    this.returnedDefaultFor = null;

    reset();
  }

  public void reset() {
    this.numIllegalConfigurationChangeHandlers = 0;

    this.numEnsureRepositoryProvides = 0;
    this.lastEnsureClass = null;

    this.numItemCreateds = 0;
    this.lastItemCreated = null;

    this.numHasDefaultFors = 0;
    this.lastHasDefaultForXPath = null;

    this.numIsOptionals = 0;
    this.lastIsOptionalXPath = null;

    this.numDefaultFors = 0;
    this.lastDefaultForXPath = null;

    this.numBeans = 0;
  }

  public IllegalConfigurationChangeHandler illegalConfigurationChangeHandler() {
    ++this.numIllegalConfigurationChangeHandlers;
    return this.returnedIllegalConfigurationChangeHandler;
  }

  public void ensureRepositoryProvides(Class theClass) {
    ++this.numEnsureRepositoryProvides;
    this.lastEnsureClass = theClass;
    if (this.exceptionOnEnsureRepositoryProvides != null) throw this.exceptionOnEnsureRepositoryProvides;
  }

  public int getNumBeans() {
    return numBeans;
  }

  public int getNumEnsureRepositoryProvides() {
    return numEnsureRepositoryProvides;
  }

  public Class getLastEnsureClass() {
    return lastEnsureClass;
  }

  public boolean hasDefaultFor(String xpath) {
    ++this.numHasDefaultFors;
    this.lastHasDefaultForXPath = xpath;
    return this.returnedHasDefaultFor;
  }

  public XmlObject defaultFor(String xpath) {
    ++this.numDefaultFors;
    this.lastDefaultForXPath = xpath;
    return this.returnedDefaultFor;
  }

  public Object syncLockForBean() {
    return this;
  }

  public boolean isOptional(String xpath) {
    ++this.numIsOptionals;
    this.lastIsOptionalXPath = xpath;
    return this.returnedIsOptional;
  }

  public XmlObject bean() {
    ++this.numBeans;
    return this.returnedBean;
  }

  public void itemCreated(ConfigItem item) {
    ++this.numItemCreateds;
    this.lastItemCreated = item;
  }

  public ConfigItem getLastItemCreated() {
    return lastItemCreated;
  }

  public int getNumItemCreateds() {
    return numItemCreateds;
  }

  public String getLastHasDefaultForXPath() {
    return lastHasDefaultForXPath;
  }

  public int getNumHasDefaultFors() {
    return numHasDefaultFors;
  }

  public void setReturnedHasDefaultFor(boolean returnedHasDefaultFor) {
    this.returnedHasDefaultFor = returnedHasDefaultFor;
  }

  public void setReturnedBean(XmlObject returnedBean) {
    this.returnedBean = returnedBean;
  }

  public String getLastIsOptionalXPath() {
    return lastIsOptionalXPath;
  }

  public int getNumIsOptionals() {
    return numIsOptionals;
  }

  public void setReturnedIsOptional(boolean returnedIsOptional) {
    this.returnedIsOptional = returnedIsOptional;
  }

  public String getLastDefaultForXPath() {
    return lastDefaultForXPath;
  }

  public int getNumDefaultFors() {
    return numDefaultFors;
  }

  public void setReturnedDefaultFor(XmlObject returnedDefaultFor) {
    this.returnedDefaultFor = returnedDefaultFor;
  }

  public int getNumIllegalConfigurationChangeHandlers() {
    return numIllegalConfigurationChangeHandlers;
  }

  public void setReturnedIllegalConfigurationChangeHandler(
                                                           IllegalConfigurationChangeHandler returnedIllegalConfigurationChangeHandler) {
    this.returnedIllegalConfigurationChangeHandler = returnedIllegalConfigurationChangeHandler;
  }

}
