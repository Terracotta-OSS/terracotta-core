/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object;

import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.field.TCField;
import com.tc.object.tx.optimistic.OptimisticTransactionManager;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Map;

/**
 * Interface for peer to java.lang.Class. The Class of every object under management is represented by an instance of
 * TCClass. Keeping a peer of each Class object allows us to organize the class elements in useful ways and to cache
 * that organization so we only have to do it once per class.
 * <p>
 * <b>Important </b>-- It is likely that we will enforce the Serializable contract and only manage those classes which
 * implement Serializable.
 * <p>
 * TODO: Add support for using a serialized instance of classes with no nullary constructor to rehydrate into. <br>
 * TODO:
 *
 * @author Orion Letizi
 */
public interface TCClass {

  public Class getPeerClass();

  public boolean hasOnLoadExecuteScript();

  public boolean hasOnLoadMethod();

  public String getOnLoadMethod();

  public String getOnLoadExecuteScript();

  public Field getParentField();

  public String getParentFieldName();

  public TCField[] getPortableFields();

  /**
   * Connects the original object to the copy object and creates new copies of referened objects but leaves them
   * unconnected (mostly this is about doing a deep connected clone without recurrsion so that we don't get stack
   * overflows
   */
  public Map connectedCopy(Object source, Object dest, Map visited, OptimisticTransactionManager txManager);

  public TraversedReferences getPortableObjects(Object pojo, TraversedReferences addTo);

  public Constructor getConstructor() throws NoSuchMethodException, SecurityException;

  public String getName();

  public Class getComponentType();

  public boolean isLogical();

  public ClientObjectManager getObjectManager();

  public TCClass getSuperclass();

  public boolean isNonStaticInner();

  public boolean isEnum();

  public boolean isUseNonDefaultConstructor();

  public Object getNewInstanceFromNonDefaultConstructor(DNA dna) throws IOException, ClassNotFoundException;

  public TCField getField(String name);

  public boolean isIndexed();

  public void hydrate(TCObject tcObject, DNA dna, Object pojo, boolean force) throws IOException,
      ClassNotFoundException;

  public void dehydrate(TCObject tcObject, DNAWriter writer, Object pojo);

  public String getDefiningLoaderDescription();

  public TCObject createTCObject(ObjectID id, Object peer);

  public String getFieldNameByOffset(long fieldOffset);

  public boolean isProxyClass();

  public String getExtendingClassName();
}
