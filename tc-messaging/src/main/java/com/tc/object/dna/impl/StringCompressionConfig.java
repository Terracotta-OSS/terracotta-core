/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.dna.impl;

public interface StringCompressionConfig {

  boolean enabled();

  boolean loggingEnabled();

  int minSize();
}
