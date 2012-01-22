/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema;

import com.terracottatech.config.Modules;

import java.io.File;

/**
 * Contains methods for L1 DSO.
 */
public interface CommonL1Config extends Config {

  File logsPath();

  Modules modules();

}
