/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.config;

public interface ClassReplacementTest {
  boolean accepts(String origClassName, ClassLoader loader);
}
