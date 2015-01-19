/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAWriter;

import java.lang.ref.WeakReference;

/**
 * Terracotta class attached to each shared instance Object
 */
public interface TCObject {
  /** Indicates null object identifier */
  public static final Long NULL_OBJECT_ID = Long.valueOf(-1);

  /** Indicates null field index */
  public static final int  NULL_INDEX     = -1;

  /**
   * Get the object identifier
   * 
   * @return Object identifier
   */
  public ObjectID getObjectID();

  /**
   * @return The Object that this TCObject is wrapping. This value will be null if the peer Object is null.
   */
  public Object getPeerObject();

  /**
   * Takes a DNA strand and hydrates the object with it.
   * 
   * @param force true if the DNA should be applied w/o any version checking
   * @param weakReference
   * @throws ClassNotFoundException If class not found
   */
  public void hydrate(DNA from, boolean force, WeakReference<Object> peer) throws ClassNotFoundException;

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
   * Invoke logical method
   * 
   * @param method Method indicator, as defined in {@link com.tc.object.SerializationUtil}
   * @param params The parameter values
   */
  public void logicalInvoke(LogicalOperation method, Object[] params);

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

  public Class<?> getPeerClass();
  
}
