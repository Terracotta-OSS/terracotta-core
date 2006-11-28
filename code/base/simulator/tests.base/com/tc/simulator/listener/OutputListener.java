/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.simulator.listener;

public interface OutputListener {
  public void println(Object o) throws InterruptedException;

  public void printerr(Object o) throws InterruptedException;
}