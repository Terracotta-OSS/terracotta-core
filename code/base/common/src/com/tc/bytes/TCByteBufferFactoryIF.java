/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.bytes;

/**
 * Interface for TCByteBufferFactory instances
 * 
 * @author teck
 */
interface TCByteBufferFactoryIF {
  TCByteBuffer getInstance(int capacity, boolean direct);
  
  TCByteBuffer wrap(byte[] data);
}