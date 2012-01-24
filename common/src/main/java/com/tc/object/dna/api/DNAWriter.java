/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.dna.api;

import com.tc.io.TCByteBufferOutput;
import com.tc.object.ObjectID;

/**
 * Interface for writing DNA. The Writer effectively defines the protocol for how DNA data is written to a stream.
 */
/**
 *
 */
public interface DNAWriter {

  /**
   * Add logical action to the writer
   * 
   * @param Method identifier, defined in {@link com.tc.object.SerializationUtil}
   * @param parameters Parameter values
   */
  void addLogicalAction(int method, Object[] parameters);

  /**
   * Add physical action to the writer representing field value, automatically determine whether value is a reference by
   * checking whether it is an ObjectID
   * 
   * @param fieldName The field name
   * @param value The field value
   */
  void addPhysicalAction(String fieldName, Object value);

  /**
   * Add physical action to the writer representing a field value, specify whether the value is a reference or not.
   * 
   * @param fieldName The field name
   * @param value The field value
   * @param canBeReference Is this a reference
   */
  void addPhysicalAction(String fieldName, Object value, boolean canBeReference);

  /**
   * Add physical action for array element change
   * 
   * @param index Index in the array
   * @param value New value
   */
  void addArrayElementAction(int index, Object value);

  /**
   * Add physical action for subarray change
   * 
   * @param start Start index in the array
   * @param array The array value
   * @param length The length of the subarray
   */
  void addSubArrayAction(int start, Object array, int length);

  /**
   * Add physical action for entire array
   * 
   * @param value Array value
   */
  void addEntireArray(Object value);

  /**
   * Add literal value
   * 
   * @param value Literal value
   */
  void addLiteralValue(Object value);

  /**
   * Finalize the DNA header fields
   */
  void finalizeHeader();

  /**
   * Set parent object ID for inner classes
   * 
   * @param id Parent object ID
   */
  void setParentObjectID(ObjectID id);

  /**
   * Set array length
   * 
   * @param length Length
   */
  void setArrayLength(int length);

  /**
   * Return the number of actions written so far in this writer
   */
  int getActionCount();

  /**
   * create a DNAWriter for appending more actions to this DNA
   */
  DNAWriter createAppender();

  /**
   * Is this DNA (including all appended actions) contiguous in memory
   * 
   * @return True if contiguous
   */
  boolean isContiguous();

  /**
   * Indicate to this writer that no more actions will be added (must be called)
   */
  void markSectionEnd();

  /**
   * Copy the written DNA data to the given output stream
   * 
   * @param dest The destination output stream
   */
  void copyTo(TCByteBufferOutput dest);

}
