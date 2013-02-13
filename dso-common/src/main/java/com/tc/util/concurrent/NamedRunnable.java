/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.util.concurrent;

/**
 * Convinient interface for custom named tasks.
 * {@link #toString()} could be used instead, but non-overridden {@link Object#toString()} looks ugly.
 */
public interface NamedRunnable extends Runnable {
  String getName();
}
