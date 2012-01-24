/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.server;

public interface ServerConnectionValidator {
  boolean isAlive(String name);
}
