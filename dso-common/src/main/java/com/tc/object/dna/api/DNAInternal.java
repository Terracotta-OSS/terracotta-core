/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.dna.api;

public interface DNAInternal extends DNA {

  MetaDataReader getMetaDataReader();

  boolean hasMetaData();

}
