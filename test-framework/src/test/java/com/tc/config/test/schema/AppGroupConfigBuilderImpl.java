/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.test.schema;

import com.tc.config.schema.builder.AppGroupConfigBuilder;

/**
 * Used to create an app-group config element. Either setNamedClassloaders(),
 * setWebApplications(), or both may be used.
 */
public class AppGroupConfigBuilderImpl extends BaseConfigBuilder implements AppGroupConfigBuilder {

  private static final String TAG_NAME = "app-group";
  private static final String NAME_ATTR = "name";
  private static final String NAMED_CLASSLOADER = "named-classloader";
  private static final String WEB_APPLICATION = "web-application";
  
  private String name;
  private String[] namedClassLoaders;
  private String[] webApplications;

  public AppGroupConfigBuilderImpl() {
    super(5, new String[] { TAG_NAME });
  }

  // We generate our own XML here, rather than using the base class functionality, because the base class
  // doesn't understand the idea of properties that are a String[] but that are supposed to generate a
  // series of tagged elements.  The alternative would be to create NamedClassloaderConfigBuilder and
  // WebApplicationConfigBuilder classes, but that seems overly complex and also there's already a class
  // named WebApplicationConfigBuilder.
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(indent()).append("<").append(TAG_NAME).append(" ").append(NAME_ATTR).append("=\"").append(name).append("\">");
    ++currentIndentLevel;
    if (namedClassLoaders != null) {
      for (String ncl : namedClassLoaders) {
        sb.append(indent()).append("<").append(NAMED_CLASSLOADER).append(">");
        sb.append(ncl).append("</").append(NAMED_CLASSLOADER).append(">");
      }
    }
    if (webApplications != null) {
      for (String wa : webApplications) {
        sb.append(indent()).append("<").append(WEB_APPLICATION).append(">");
        sb.append(wa).append("</").append(WEB_APPLICATION).append(">");
      }
    }
    --currentIndentLevel;
    sb.append(indent()).append("</").append(TAG_NAME).append(">");
    return sb.toString();
  }

  public void setAppGroupName(String name) {
    this.name = name;
  }

  public void setNamedClassLoaders(String[] namedClassLoaders) {
    this.namedClassLoaders = namedClassLoaders;
  }

  public void setWebApplications(String[] webApplications) {
    this.webApplications = webApplications;
  }

}
