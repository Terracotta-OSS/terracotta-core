/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.test;

import com.tc.config.schema.builder.SpringApplicationConfigBuilder;
import com.tc.config.schema.builder.SpringConfigBuilder;


public class SpringConfigBuilderImpl extends BaseConfigBuilder implements SpringConfigBuilder {

  protected SpringConfigBuilderImpl() {
    super(3, new String[]{"jee-application"});
  }

  public static SpringConfigBuilder newMinimalInstance() {
    SpringConfigBuilderImpl result = new SpringConfigBuilderImpl();

    result.setApplications(new SpringApplicationConfigBuilderImpl[] { SpringApplicationConfigBuilderImpl.newMinimalInstance() });
    return result;
  }


  public String toString() {
    return propertyAsString("jee-application");
  }

  public SpringApplicationConfigBuilder[] getApplications() {
    if (isSet("jee-application")) {
      Object rawProperty = getRawProperty("jee-application");
      SelfTaggingArray selfTaggingArray = (SelfTaggingArray) rawProperty;
      return (SpringApplicationConfigBuilder[]) (selfTaggingArray).values();
    }
    else return null;
  }

  private void setApplications(BaseConfigBuilder[] applications) {
    setProperty("jee-application", selfTaggingArray(applications));
  }
}
