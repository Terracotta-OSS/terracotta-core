/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.net.core;

/**
 * The common interface for TCComm instances. A TCComm instance is used for managing the lowest level network details
 * (ie. reading/writing bytes, and opening/closing connections)
 * 
 * @author teck
 */
public interface TCComm {
  public void stop();

  public void start();

  public boolean isStarted();

  public boolean isStopped();
}