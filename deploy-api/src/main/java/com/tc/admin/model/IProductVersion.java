/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.admin.model;

public interface IProductVersion {
  String version();

  String mavenArtifactsVersion();

  String patchLevel();

  String patchVersion();

  String license();

  String copyright();

  String buildID();
}
