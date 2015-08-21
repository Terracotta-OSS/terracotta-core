/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.tc.bytes;

/**
 *
 * @author cdennis
 */
public class TestTCByteBufferFactoryConfig implements TCByteBufferFactoryConfig {

  @Override
  public boolean isDisabled() {
    return false;
  }

  @Override
  public int getPoolMaxBufCount() {
    return 2000;
  }

  @Override
  public int getCommonPoolMaxBufCount() {
    return 3000;
  }
  
}
