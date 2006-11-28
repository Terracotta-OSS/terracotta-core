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