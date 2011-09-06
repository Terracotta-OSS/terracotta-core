/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema.test;

import com.tc.config.schema.builder.AppGroupConfigBuilder;
import com.tc.config.schema.builder.DSOApplicationConfigBuilder;
import com.tc.config.schema.builder.InstrumentedClassConfigBuilder;
import com.tc.config.schema.builder.LockConfigBuilder;
import com.tc.config.schema.builder.RootConfigBuilder;
import com.tc.config.schema.builder.WebApplicationConfigBuilder;

/**
 * Allows you to build valid config for the DSO part of an application. This class <strong>MUST NOT</strong> invoke the
 * actual XML beans to do its work; one of its purposes is, in fact, to test that those beans are set up correctly.
 */
public class DSOApplicationConfigBuilderImpl extends BaseConfigBuilder implements DSOApplicationConfigBuilder {

  public DSOApplicationConfigBuilderImpl() {
    super(4, ALL_PROPERTIES);

    setArrayPropertyTagName("transient-fields", "transient-field");
    setArrayPropertyTagName("roots", "root");
    setArrayPropertyTagName("distributed-methods", "distributed-method");
    setArrayPropertyTagName("additional-boot-jar-classes", "include");

    setProperty("instrumented-classes", "");
  }

  public void setInstrumentedClasses(String value) {
    setProperty("instrumented-classes", value);
  }

  public void setInstrumentedClasses(InstrumentedClassConfigBuilder[] value) {
    setProperty("instrumented-classes", value);
  }

  public void setDistributedMethods(String value) {
    setProperty("distributed-methods", value);
  }

  public void setDistributedMethods(String[] value) {
    setProperty("distributed-methods", value);
  }

  public void setAdditionalBootJarClasses(String value) {
    setProperty("additional-boot-jar-classes", value);
  }

  public void setAdditionalBootJarClasses(String[] value) {
    setProperty("additional-boot-jar-classes", value);
  }

  public void setAppGroups(AppGroupConfigBuilder[] value) {
    setProperty("app-groups", value);
  }

  public void setRoots(String value) {
    setProperty("roots", value);
  }

  public void setRoots(RootConfigBuilder[] value) {
    setProperty("roots", value);
  }

  public void setTransientFields(String value) {
    setProperty("transient-fields", value);
  }

  public void setTransientFields(String[] value) {
    setProperty("transient-fields", value);
  }

  public void setLocks(String value) {
    setProperty("locks", value);
  }

  public void setLocks(LockConfigBuilder[] value) {
    setProperty("locks", selfTaggingArray(value));
  }

  public void setWebApplications(WebApplicationConfigBuilder[] value) {
    setProperty("web-applications", selfTaggingArray(value));
  }

  private static final String[] ALL_PROPERTIES = new String[] { "instrumented-classes", "transient-fields", "locks",
      "roots", "distributed-methods", "additional-boot-jar-classes", "web-applications", "spring", "app-groups" };

  @Override
  public String toString() {
    String out = "";

    out += indent() + elements(ALL_PROPERTIES);

    return out;
  }

  public static DSOApplicationConfigBuilder newMinimalInstance() {
    return new DSOApplicationConfigBuilderImpl();
  }

}
