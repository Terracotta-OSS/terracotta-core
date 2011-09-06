/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.tool.commands;

/**
 * Records well-known categories defined in the index for each artifact.  These may 
 * be recorded in either the Bundle-Category or Terracotta-Category.
 */
public enum ArtifactCategories {
  TIM("Terracotta Integration Module"),
  LIBRARY("Library"),
  PRODUCT("Product");
  
  private String category;
  
  ArtifactCategories(String category) {
    this.category = category;
  }
  
  public String category() {
    return this.category;
  }
}
