/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAWriter;

import java.io.IOException;

/**
 * Terracotta class attached to each shared instance Object
 */
public interface TCObject {

  EntityID getEntityID();

  /**
   * Takes a DNA strand and applies it
   * 
   * @throws ClassNotFoundException If class not found
   */
  public void apply(DNA dna) throws ClassNotFoundException, IOException;

  /**
   * Get version of this object instance
   * 
   * @return Version
   */
  public long getVersion();

  /**
   * Set a new version for this object
   * 
   * @param version New version
   */
  public void setVersion(long version);

  /**
   * @return True if new
   */
  public boolean isNew();

  /**
   * Unset the "is new" flag. This should only be done by one thread ever (namely the thread that first ever commits
   * this object)
   */
   public void setNotNew();

  /**
   * Dehydate the entire state of the peer object to the given writer
   */
  public void dehydrate(DNAWriter writer);

  public String getClassName();

}
