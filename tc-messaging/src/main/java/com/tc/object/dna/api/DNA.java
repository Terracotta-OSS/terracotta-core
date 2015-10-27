/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.object.dna.api;

import com.tc.object.EntityID;

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
  public static final byte IS_DELTA         = 1 << 1;
  public static final byte HAS_VERSION      = 1 << 2;
  public static final byte IGNORE_MISSING_OBJECT = 1 << 3;

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

  EntityID getEntityID();

  /**
   * Gets a DNACursor to spin through the field values.
   * 
   * @return The cursor
   */
  public DNACursor getCursor();

}
