/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.search;

import com.tc.io.TCSerializable;
import com.tc.object.ObjectID;
import com.tc.object.metadata.NVPair;

import java.util.List;

/**
 * Index search result.
 * 
 * @author Nabib El-Rahman
 */
public interface IndexQueryResult extends TCSerializable {

  /**
   * Entry key.
   * 
   * @return key
   */
  public String getKey();

  /**
   * Entry value.
   * 
   * @return value
   */
  public ObjectID getValue();

  /**
   * Entry attributes.
   * 
   * @return attributes
   */
  public List<NVPair> getAttributes();

  /**
   * Entry sortAttributes.
   * 
   * @return attributes
   */
  public List<NVPair> getSortAttributes();
}
