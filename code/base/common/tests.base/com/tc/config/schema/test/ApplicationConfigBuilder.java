/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.test;

import com.tc.config.schema.builder.DSOApplicationConfigBuilder;
import com.tc.config.schema.builder.SpringConfigBuilder;

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
    if (! isSet("dso")) setDSO(DSOApplicationConfigBuilderImpl.newMinimalInstance());
    return (DSOApplicationConfigBuilder) getRawProperty("dso");
  }

  public void setSpring(String value) {
    setProperty("spring", value);
  }

  public void setSpring(SpringConfigBuilder value) {
    setProperty("spring", value);
  }
  
  public SpringConfigBuilder getSpring() {
    if (! isSet("spring")) setSpring(SpringConfigBuilderImpl.newMinimalInstance());
    return (SpringConfigBuilder) getRawProperty("spring");
  }

  private static final String[] ALL_PROPERTIES = new String[] { "dso", "spring" };

  public String toString() {
    return elements(ALL_PROPERTIES);
  }
  
  public static ApplicationConfigBuilder newMinimalInstance() {
    return new ApplicationConfigBuilder();
  }

}
