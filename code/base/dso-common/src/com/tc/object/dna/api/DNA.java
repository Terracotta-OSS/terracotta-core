/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.dna.api;

import com.tc.object.ObjectID;

/**
 * Represents the data of an object. Implementations of this interface are used to extract data from and apply
 * (new/updated) data to objects. It's like serialization data, but it allows deltas to be extracted and applied.
 *
 * @author Orion Letizi
 */
public interface DNA {
  public static final int NULL_ARRAY_SIZE    = -1;
  public static final int NULL_VERSION       = -1;

  public long getVersion();

  public boolean hasLength();

  public int getArraySize();

  /**
   * returns true if the DNA represents a change of an object and
   * returns false if the DNA represents the entire object.
   */
  public boolean isDelta();

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

  public ObjectID getParentObjectID() throws DNAException;

  /**
   * Gets a DNACursor to spin through the field values.
   *
   * @return
   */
  public DNACursor getCursor();

  public String getDefiningLoaderDescription();
}