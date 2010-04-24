/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.applicator;

import com.tc.object.TCObjectExternal;
import com.tc.object.TraversedReferences;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAWriter;

import java.io.IOException;

/**
 * Applies a serialized change to an object.
 */
public interface ChangeApplicator {

  /**
   * Reconstitute the state of an object from DNA.
   * 
   * @param objectManager The client-side object manager
   * @param tcObject The manager for the object
   * @param dna The DNA, representing the state of the object
   * @param pojo A new instance of the object to reconstitute - this object will be modified with the values from the
   *        DNA
   */
  public void hydrate(ApplicatorObjectManager objectManager, TCObjectExternal tcObject, DNA dna, Object pojo)
      throws IOException, ClassNotFoundException;

  /**
   * Write an object's state to DNA
   * 
   * @param objectManager The client-side object manager
   * @param tcObject The manager for the object
   * @param writer The DNA writer for writing the DNA
   * @param pojo The object to write to writer
   */
  public void dehydrate(ApplicatorObjectManager objectManager, TCObjectExternal tcObject, DNAWriter writer, Object pojo);

  /**
   * Traverse an object and find all object references within it.
   * 
   * @param pojo The object instance
   * @param addTo A collection of traversed references found
   * @return The addTo collection
   */
  public TraversedReferences getPortableObjects(Object pojo, TraversedReferences addTo);

  /**
   * Instantiate a new instance of the object from DNA. May not be supported on all applicators.
   * 
   * @param objectManager The client-side object manager
   * @param dna The DNA for the new object
   * @return The new instance
   */
  public Object getNewInstance(ApplicatorObjectManager objectManager, DNA dna) throws IOException,
      ClassNotFoundException;
}
