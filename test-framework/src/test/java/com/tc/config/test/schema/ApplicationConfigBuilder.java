/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.test.schema;

import com.tc.config.schema.builder.DSOApplicationConfigBuilder;

public class ApplicationConfigBuilder extends BaseConfigBuilder {

  public ApplicationConfigBuilder() {
    super(1, ALL_PROPERTIES);
  }

  public void setDSO(String value) {
    setProperty("dso", value);
  }

  public void setDSO(DSOApplicationConfigBuilder value) {
    setProperty("dso", value);
  }

  public DSOApplicationConfigBuilder getDSO() {
    if (!isSet("dso")) setDSO(DSOApplicationConfigBuilderImpl.newMinimalInstance());
    return (DSOApplicationConfigBuilder) getRawProperty("dso");
  }

  public void setSpring(String value) {
    setProperty("spring", value);
  }

  private static final String[] ALL_PROPERTIES = new String[] { "dso", "spring" };

  @Override
  public String toString() {
    return elements(ALL_PROPERTIES);
  }

  public static ApplicationConfigBuilder newMinimalInstance() {
    return new ApplicationConfigBuilder();
  }

}
