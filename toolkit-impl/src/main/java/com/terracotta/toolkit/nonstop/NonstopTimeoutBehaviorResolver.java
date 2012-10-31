/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

public interface NonstopTimeoutBehaviorResolver<T> {

  T resolveTimeoutBehavior();
}