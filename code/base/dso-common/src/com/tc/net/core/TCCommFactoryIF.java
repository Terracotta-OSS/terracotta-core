/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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