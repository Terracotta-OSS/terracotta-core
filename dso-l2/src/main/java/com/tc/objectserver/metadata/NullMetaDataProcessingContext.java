/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.metadata;


public class NullMetaDataProcessingContext extends MetaDataProcessingContext {

  public NullMetaDataProcessingContext() {
    super(null, null);
  }

  @Override
  public void processed() {
    // no-op
  }

}
