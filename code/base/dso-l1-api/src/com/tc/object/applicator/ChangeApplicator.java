/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.applicator;

import com.tc.object.ClientObjectManager;
import com.tc.object.TCObject;
import com.tc.object.TraversedReferences;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.tx.optimistic.OptimisticTransactionManager;

import java.io.IOException;
import java.util.Map;

/**
 * Applies a serialized change to an object.
 */
public interface ChangeApplicator {

  /**
   * Reconstitute the state of an object from DNA.
   * @param objectManager The client-side object manager
   * @param tcObject The manager for the object
   * @param dna The DNA, representing the state of the object
   * @param pojo A new instance of the object to reconstitute - this object will be modified with the values from the DNA
   */
  public void hydrate(ClientObjectManager objectManager, TCObject tcObject, DNA dna, Object pojo) throws IOException,
      ClassNotFoundException;

  /**
   * Write an object's state to DNA
   * @param objectManager The client-side object manager
   * @param tcObject The manager for the object
   * @param writer The DNA writer for writing the DNA
   * @param pojo The object to write to writer
   */
  public void dehydrate(ClientObjectManager objectManager, TCObject tcObject, DNAWriter writer, Object pojo);

  /**
   * Create a new copy of the source object in the dest object, replacing connected objects with a new or
   * existing clone as necessary.  New clones that are created are returned so they can be properly
   * updated from their originals.
   * @param source The source object
   * @param dest The destination copy object
   * @param visited A Map of already visited objects and their clones (key=obj, value=clone)
   * @param objectManager Client-side object manager
   * @param txManager Transaction manager
   * @return Newly cloned objects, key=obj, value=clone
   */
  public Map connectedCopy(Object source, Object dest, Map visited, ClientObjectManager objectManager,
                           OptimisticTransactionManager txManager);

  /**
   * Traverse an object and find all object references within it.
   * @param pojo The object instance
   * @param addTo A collection of traversed references found
   * @return The addTo collection
   */
  public TraversedReferences getPortableObjects(Object pojo, TraversedReferences addTo);

  /**
   * Instantiate a new instance of the object from DNA.  May not be supported on all applicators.
   * @param objectManager The client-side object manager
   * @param dna The DNA for the new object
   * @return The new instance
   */
  public Object getNewInstance(ClientObjectManager objectManager, DNA dna) throws IOException, ClassNotFoundException;
}