/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.field.TCField;
import com.tc.object.loaders.LoaderDescription;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

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
   * Determine whether this class has a BeanShell script to execute on class load
   *
   * @return True if has script
   */
  public boolean hasOnLoadExecuteScript();

  /**
   * Determine whether this class has a method to execute on class load
   *
   * @return True if has load method
   */
  public boolean hasOnLoadMethod();

  /**
   * Determine whether this class has a injection to execute on class load
   *
   * @return True if has injection on load
   */
  public boolean hasOnLoadInjection();

  /**
   * Get name of method to execute on load
   *
   * @return Method name
   */
  public String getOnLoadMethod();

  /**
   * Get script to execute on load
   *
   * @return Execute script
   */
  public String getOnLoadExecuteScript();

  /**
   * If the class is an inner class, get the field referring to the parent "this object.
   *
   * @return The field referring to the parent this
   */
  public Field getParentField();

  /**
   * If the class is an inner class, get the name of the field referring to the parent "this" object.
   *
   * @return The field name referring to the parent this
   */
  public String getParentFieldName();

  /**
   * Get all portable fields in the class
   *
   * @return Fields, never null
   */
  public TCField[] getPortableFields();

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
   * @return If this is an array, the type of array elements
   */
  public Class getComponentType();

  /**
   * @return True if this is a logically instrumented class
   */
  public boolean isLogical();

  /**
   * @return The client object manager for this client
   */
  public ClientObjectManager getObjectManager();

  /**
   * @return TCClass of the super class of the peer
   */
  public TCClass getSuperclass();

  /**
   * @return True if this is a non-static inner class and has a parent
   */
  public boolean isNonStaticInner();

  /**
   * @return True if this is an enum
   */
  public boolean isEnum();

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
  public Object getNewInstanceFromNonDefaultConstructor(DNA dna) throws IOException, ClassNotFoundException;

  /**
   * Get TCField for this class
   *
   * @param name Field name
   * @return TCField
   */
  public TCField getField(String name);

  /**
   * @return True if this is an array and indexed
   */
  public boolean isIndexed();

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
   * @return Descriptor of defining classloader
   */
  public LoaderDescription getDefiningLoaderDescription();

  /**
   * Create a new TCObject
   *
   * @param id The object identifier
   * @param peer The object
   * @param isNew true whether this TCObject is for a newly shared pojo peer
   */
  public TCObject createTCObject(ObjectID id, Object peer, boolean isNew);

  /**
   * Get a field name by offset into an index of fields
   *
   * @param fieldOffset The index
   * @return The fully-qualified field name at that index
   */
  public String getFieldNameByOffset(long fieldOffset);

  /**
   * @return True if this class uses a proxy class
   */
  public boolean isProxyClass();

  /**
   * Returns special generated name for classes extending logical classes
   *
   * @return Special generated logical extending class name or just the normal class name if not extending logical
   */
  public String getExtendingClassName();

  /**
   * Returns true if the field represented by the offset is a portable field, i.e., not static and not dso transient
   *
   * @param fieldOffset The index
   * @return true if the field is portable and false otherwise
   */
  public boolean isPortableField(long fieldOffset);

  /**
   * Returns true if the resolve lock should be held while clearing references
   */
  public boolean useResolveLockWhileClearing();

  /**
   * Returns true if instances of this type should NOT be cleared by the memory manager
   */
  public boolean isNotClearable();

  /**
   * List of method handles for the post create methods for this type. This list will include the post create methods
   * for all superclasses and may be empty (but never null)
   */
  public List<Method> getPostCreateMethods();

  /**
   * List of method handles for the pre create methods for this type. This list will include the post create methods for
   * all superclasses and may be empty (but never null)
   */
  public List<Method> getPreCreateMethods();
}
