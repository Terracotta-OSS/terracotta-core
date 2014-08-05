/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAWriter;
import com.tc.platform.PlatformService;

import java.io.IOException;
import java.lang.reflect.Constructor;

/**
 * Interface for peer to java.lang.Class. The Class of every object under management is represented by an instance of
 * TCClass. Keeping a peer of each Class object allows us to organize the class elements in useful ways and to cache
 * that organization so we only have to do it once per class.
 * <p>
 * <b>Important </b>-- It is likely that we will enforce the Serializable contract and only manage those classes which
 * implement Serializable.
 * <p>
 * TODO: Add support for using a serialized instance of classes with no nullary constructor to rehydrate into. <br>
 * 
 * @author Orion Letizi
 */
public interface TCClass {

  /**
   * Get the class this TCClass is a peer for
   * 
   * @return Peer class, never null
   */
  public Class getPeerClass();

  /**
   * Traverse a graph of objects to find the portable ones
   * 
   * @param pojo The object to walk
   * @param addTo The traversed references collected so far
   * @return The addTo collection
   */
  public TraversedReferences getPortableObjects(Object pojo, TraversedReferences addTo);

  /**
   * Get constructor for the class
   * 
   * @return The constructor
   * @throws NoSuchMethodException If there is no constructor
   * @throws SecurityException If the constructor cannot be accessed in the current security model
   */
  public Constructor getConstructor() throws NoSuchMethodException, SecurityException;

  /**
   * @return Name of peer or proxy class
   */
  public String getName();

  /**
   * @return The client object manager for this client
   */
  public ClientObjectManager getObjectManager();

  /**
   * @return True if should use a non-default constructor when creating new instances
   */
  public boolean isUseNonDefaultConstructor();

  /**
   * Construct a new instance from a DNA strand using a non-default constructor
   * 
   * @param dna The DNA with the data to use
   * @return The new instance
   * @throws IOException Reading DNA
   * @throws ClassNotFoundException Can't instantiate a class
   */
  public Object getNewInstanceFromNonDefaultConstructor(DNA dna, PlatformService platformService) throws IOException,
      ClassNotFoundException;

  /**
   * Reconstitute object from DNA
   * 
   * @param tcObject The object manager
   * @param dna The DNA to read
   * @param pojo The new instance of the pojo to reconstitute (will be modified)
   * @param force Set to true to force an update to the DNA version, regardless of the local version
   */
  public void hydrate(TCObject tcObject, DNA dna, Object pojo, boolean force) throws IOException,
      ClassNotFoundException;

  /**
   * Write an object to DNA
   * 
   * @param tcObject The object manager
   * @param writer The writer to write to
   * @param pojo The instance to write
   */
  public void dehydrate(TCObject tcObject, DNAWriter writer, Object pojo);

  /**
   * Create a new TCObject
   * 
   * @param id The object identifier
   * @param peer The object
   * @param isNew true whether this TCObject is for a newly shared pojo peer
   */
  public TCObject createTCObject(ObjectID id, Object peer, boolean isNew);

}
