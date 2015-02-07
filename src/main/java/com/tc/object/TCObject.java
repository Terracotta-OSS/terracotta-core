/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.object.dna.api.DNA;
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
  void apply(DNA dna) throws ClassNotFoundException, IOException;

  /**
   * Get version of this object instance
   * 
   * @return Version
   */
  long getVersion();

  /**
   * Set a new version for this object
   * 
   * @param version New version
   */
  void setVersion(long version);

  /**
   * @return True if new
   */
  boolean isNew();

  /**
   * Unset the "is new" flag. This should only be done by one thread ever (namely the thread that first ever commits
   * this object)
   */
  void setNotNew();

  String getClassName();
  
}
