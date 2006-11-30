/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.net.core;

/**
 * Interface for Connection manager factories
 * 
 * @author teck
 */
interface TCConnectionManagerFactoryIF {
  TCConnectionManager getInstance(TCComm comm);
}