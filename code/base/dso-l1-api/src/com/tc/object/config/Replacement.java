package com.tc.object.config;

import java.net.URL;

public class Replacement {
  private final String               replacementClassName;
  private final URL                  replacementResource;
  private final ClassReplacementTest test;

  Replacement(String replacementClassName, URL resource, ClassReplacementTest test) {
    this.replacementClassName = replacementClassName;
    this.replacementResource = resource;
    this.test = test;
  }

  ClassReplacementTest getTest() {
    return test;
  }

  public String getReplacementClassName() {
    return replacementClassName;
  }

  public URL getReplacementResource() {
    return replacementResource;
  }

}