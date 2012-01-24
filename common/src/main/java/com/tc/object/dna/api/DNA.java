/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.dna.api;

import com.tc.object.ObjectID;

/**
 * Represents the data of an object. Implementations of this interface are used to extract data from and apply
 * (new/updated) data to objects. It's like serialization data, but it allows deltas to be extracted and applied.
 */
public interface DNA {

  enum DNAType {
    L2_SYNC, L1_FAULT
  }

  /** Array size constant indicating no array size */
  public static final int  NULL_ARRAY_SIZE  = -1;

  /** Version constant indicating no version */
  public static final long NULL_VERSION     = -1;

  public static final byte HAS_ARRAY_LENGTH = 1 << 0;
  public static final byte HAS_PARENT_ID    = 1 << 1;
  public static final byte IS_DELTA         = 1 << 2;
  public static final byte HAS_VERSION      = 1 << 3;

  /**
   * Get the version of this DNA based on the global transaction ID, may be {@link #NULL_VERSION}.
   */
  public long getVersion();

  /**
   * Determine whether this DNA has an array length
   * 
   * @return True if has length
   */
  public boolean hasLength();

  /**
   * Get length of the array in the DNA or {@link #NULL_ARRAY_SIZE} if it has no array length.
   * 
   * @return Array length or {@link #NULL_ARRAY_SIZE}
   */
  public int getArraySize();

  /**
   * Determine whether this DNA is a whole object or just a delta
   * 
   * @return true if the DNA represents a change of an object and false if the DNA represents the entire object.
   */
  public boolean isDelta();

  /**
   * Get the class name of the type for this piece of DNA
   * 
   * @return Class name
   */
  public String getTypeName();

  /**
   * Gets the id of the object represented by this DNA strand. The id is globally unique.
   * <p>
   * TODO: Potentially change the type from long to something which can be a composite key. We want to be able to
   * generate new ids in the local VM without making a round trip to the object service.
   * 
   * @return The id in question
   * @throws DNAException Exception thrown if the id cannot be resolved from the DNA strand.
   */
  public ObjectID getObjectID() throws DNAException;

  /**
   * If this object is an inner class, get the parent object identifier
   * 
   * @return The parent object if one exists
   * @throws DNAException Exception thrown if the id cannot be resolved
   */
  public ObjectID getParentObjectID() throws DNAException;

  /**
   * Gets a DNACursor to spin through the field values.
   * 
   * @return The cursor
   */
  public DNACursor getCursor();

}
