/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.test;

import com.tc.config.schema.builder.SpringApplicationConfigBuilder;
import com.tc.config.schema.builder.SpringApplicationContextConfigBuilder;

import java.util.Collections;

public class SpringApplicationConfigBuilderImpl extends BaseConfigBuilder implements SpringApplicationConfigBuilder {

  private String name;

  protected SpringApplicationConfigBuilderImpl() {
    super(4, new String[] { "application-contexts" });
  }

  public static SpringApplicationConfigBuilderImpl newMinimalInstance() {
    SpringApplicationConfigBuilderImpl result = new SpringApplicationConfigBuilderImpl();

    result.setApplicationContexts(new SpringApplicationContextConfigBuilder[] { SpringApplicationContextConfigBuilderImpl.newMinimalInstance() });
    return result;
  }

  public String toString() {
    return openElement("jee-application", Collections.singletonMap("name", name))
            + openElement("application-contexts")
           + propertyAsString("application-contexts") 
           + closeElement("application-contexts")
           + closeElement("jee-application");
  }

  public SpringApplicationContextConfigBuilder[] getApplicationContexts() {
    if (isSet("application-contexts")) return (SpringApplicationContextConfigBuilder[]) ((SelfTaggingArray) getRawProperty("application-contexts"))
        .values();
    else return null;
  }

  public void setApplicationContexts(SpringApplicationContextConfigBuilder[] applications) {
    setProperty("application-contexts", selfTaggingArray(applications));
  }

  public void setName(String name) {
    this.name = name;
  }
}
