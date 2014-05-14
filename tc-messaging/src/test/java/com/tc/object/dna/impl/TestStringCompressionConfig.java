/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.tc.object.dna.impl;

/**
 *
 * @author cdennis
 */
public class TestStringCompressionConfig implements StringCompressionConfig {

  @Override
  public boolean enabled() {
    return true;
  }

  @Override
  public boolean loggingEnabled() {
    return false;
  }

  @Override
  public int minSize() {
    return 512;
  }
}
