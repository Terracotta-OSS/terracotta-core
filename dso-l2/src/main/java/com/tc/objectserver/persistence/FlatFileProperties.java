/*
 * All content copyright (c) 2014 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */
package com.tc.objectserver.persistence;

import java.io.Serializable;
import java.util.HashMap;

/**
 *
 */
public class FlatFileProperties extends HashMap<String, String> implements Serializable {
  
  private transient FlatFileWrite write;

  public FlatFileProperties(FlatFileWrite write) {
    this.write = write;
  }
  
  public void setWriter(FlatFileWrite write) {
    this.write = write;
  }

  @Override
  public synchronized String put(String key, String value) {
    return write.run(()->FlatFileProperties.super.put(key, value));
  }
  
}
