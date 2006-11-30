/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.net.core;

/**
 * Factory interface for comm instances
 * 
 * @author teck
 */
interface TCCommFactoryIF {
  public TCComm getInstance();
}