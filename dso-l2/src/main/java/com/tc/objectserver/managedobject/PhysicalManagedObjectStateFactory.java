/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.exception.TCRuntimeException;
import com.tc.io.serializer.TCObjectInputStream;
import com.tc.io.serializer.TCObjectOutputStream;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.objectserver.managedobject.bytecode.ClassSpec;
import com.tc.objectserver.managedobject.bytecode.FieldType;
import com.tc.objectserver.managedobject.bytecode.PhysicalStateClassLoader;
import com.tc.objectserver.persistence.api.ClassPersistor;
import com.tc.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public class PhysicalManagedObjectStateFactory {

  private static final TCLogger          logger                   = TCLogging
                                                                      .getLogger(PhysicalManagedObjectStateFactory.class);
  private static final Class[]           CONSTRUCTOR_PARAMS_CLASS = new Class[0];
  private static final Object[]          CONSTRUCTOR_PARAMS       = new Object[0];
  private final PhysicalStateClassLoader loader;
  private final Map<Object, String>      knownClasses;
  private final ClassPersistor           persistor;
  private int                            sequenceId               = 0;

  public PhysicalManagedObjectStateFactory(ClassPersistor persistor) {
    this.loader = new PhysicalStateClassLoader();
    this.knownClasses = new ConcurrentHashMap<Object, String>();
    this.persistor = persistor;
    loadAllClassesFromDB();
  }

  private synchronized void loadAllClassesFromDB() {
    SortedMap map = new TreeMap(persistor.retrieveAllClasses());
    for (Iterator i = map.entrySet().iterator(); i.hasNext();) {
      Map.Entry e = (Entry) i.next();
      Integer clazzId = (Integer) e.getKey();
      byte clazzBytes[] = (byte[]) e.getValue();
      int cid = clazzId.intValue();
      if (sequenceId < cid) {
        sequenceId = cid;
      }
      loadFromBytes(cid, clazzBytes);
    }
  }

  private void addKnownClasses(String classIdentifier, String genClassName, int cid) {
    knownClasses.put(classIdentifier, genClassName);
    knownClasses.put(Integer.valueOf(cid), genClassName);
  }

  private void addKnownClasses(ClassSpec cs) {
    addKnownClasses(cs.getClassIdentifier(), cs.getGeneratedClassName(), cs.getClassID());
  }

  private void loadFromBytes(int classId, byte[] clazzBytes) {
    try {
      ByteArrayInputStream bai = new ByteArrayInputStream(clazzBytes);
      TCObjectInputStream tci = new TCObjectInputStream(bai);

      String classIdentifier = tci.readString();
      String genClassName = tci.readString();

      loader.defineClassFromBytes(genClassName, classId, clazzBytes, clazzBytes.length - bai.available(),
                                  bai.available());
      addKnownClasses(classIdentifier, genClassName, classId);
    } catch (Exception ex) {
      throw new TCRuntimeException(ex);
    }
  }

  private void writeToDB(ClassSpec cs, byte[] data) {
    try {
      String classIdentifier = cs.getClassIdentifier();
      String genClassName = cs.getGeneratedClassName();

      ByteArrayOutputStream bao = new ByteArrayOutputStream(data.length + 1024);
      TCObjectOutputStream tco = new TCObjectOutputStream(bao);
      tco.writeString(classIdentifier);
      tco.writeString(genClassName);
      tco.write(data);
      tco.flush();

      persistor.storeClass(cs.getClassID(), bao.toByteArray());
    } catch (Exception ex) {
      throw new TCRuntimeException(ex);
    }
  }

  public PhysicalManagedObjectState create(long strIdx, ObjectID oid, ObjectID parentID, String className,
                                           DNACursor cursor) {
    ClassSpec cs = new ClassSpec(className, strIdx);
    cs.setGenerateParentIdStorage(!parentID.isNull());

    String classIdentifier = cs.getClassIdentifier();
    String generatedClassName = knownClasses.get(classIdentifier);
    if (generatedClassName == null) {
      Object lock = cs.getLock();
      synchronized (lock) {
        // Check again ! Double check locking is OK here as loader.load() is synchronized internally anyway
        generatedClassName = knownClasses.get(classIdentifier);
        if (generatedClassName == null) {
          PhysicalManagedObjectState po = createNewClassAndInitializeObject(oid, parentID, cs, cursor);
          return po;
        }
      }
    }
    return createNewObject(generatedClassName, parentID);
  }

  public PhysicalManagedObjectState create(ObjectID parentID, int classId) throws ClassNotFoundException {
    Integer cid = Integer.valueOf(classId);
    String className = knownClasses.get(cid);
    if (className == null) { throw new ClassNotFoundException("Unknown Class Id :" + classId + " Details : parent = "
                                                              + parentID); }
    return createNewObject(className, parentID);
  }

  public PhysicalManagedObjectState recreate(long classID, ObjectID pid, String className, DNACursor cursor,
                                             PhysicalManagedObjectState oldState) {
    ClassSpec cs = new ClassSpec(className, classID);
    String classIdentifier = cs.getClassIdentifier();
    String generatedClassName = knownClasses.get(classIdentifier);
    Assert.assertNotNull(generatedClassName);
    Object lock = cs.getLock();
    synchronized (lock) {
      PhysicalManagedObjectState newState = oldState;
      if (!oldState.getClass().getName().equals(generatedClassName)) {
        // There is already a new version generated for this class, first try using that.
        newState = createNewObject(generatedClassName, pid);
        initNewStateFromOld(newState, oldState);
      }
      List deltaFields = findDeltaFields(newState, cursor);
      if (deltaFields.isEmpty()) {
        // This class is sufficient
        return newState;
      } else {
        // This newly generated class subclasses the newState
        cs.setSuperClassName(newState.getClass().getName());
        cs.setGenerateParentIdStorage(!pid.isNull() && !HasParentIdStorage.class.isAssignableFrom(newState.getClass()));
        PhysicalManagedObjectState latestState = createNewClassAndInitializeObject(pid, cs, deltaFields);
        return initNewStateFromOld(latestState, oldState);
      }
    }
  }

  private PhysicalManagedObjectState initNewStateFromOld(PhysicalManagedObjectState newState,
                                                         PhysicalManagedObjectState oldState) {
    if (newState == oldState) { return newState; }
    Map fields2Vals = oldState.addValues(new HashMap());
    for (Iterator i = fields2Vals.entrySet().iterator(); i.hasNext();) {
      Map.Entry e = (Entry) i.next();
      newState.set((String) e.getKey(), e.getValue());
    }
    return newState;
  }

  private List findDeltaFields(PhysicalManagedObjectState state, DNACursor cursor) {
    try {
      List deltaFields = new ArrayList();
      Map fields2Values = new HashMap();
      state.addValues(fields2Values);
      cursor.reset();
      while (cursor.next()) {
        PhysicalAction action = cursor.getPhysicalAction();
        if (!fields2Values.containsKey(action.getFieldName())) {
          deltaFields.add(createFieldType(action, deltaFields.size()));
        }
      }
      return deltaFields;
    } catch (Exception ex) {
      throw new TCRuntimeException(ex);
    } finally {
      cursor.reset();
    }
  }

  /**
   * This method creates an instance of already loaded class. The fields are not initialized.
   */
  private PhysicalManagedObjectState createNewObject(String stateClassName, ObjectID parentID) {
    try {
      Class c = loader.loadClass(stateClassName);
      Constructor constructor = c.getConstructor(CONSTRUCTOR_PARAMS_CLASS);
      PhysicalManagedObjectState po = (PhysicalManagedObjectState) constructor.newInstance(CONSTRUCTOR_PARAMS);
      initializeManagedObjectState(po, parentID);
      return po;
    } catch (Exception e) {
      throw new TCRuntimeException(e);
    }
  }

  /**
   * The object returned by this method has the parent Id set
   * 
   * @param parentID2
   */
  private PhysicalManagedObjectState createNewClassAndInitializeObject(ObjectID oid, ObjectID parentID, ClassSpec cs,
                                                                       DNACursor cursor) {
    try {
      // Some client side applicators expect the order of fields in output DNA to be consistent
      // with the input DNA so use a LinkedHashMap here
      Map<String, FieldType> fields = new LinkedHashMap<String, FieldType>();
      cursor.reset();
      while (cursor.next()) {
        PhysicalAction action = cursor.getPhysicalAction();
        String fName = action.getFieldName();
        if (!fields.containsKey(fName)) {
          fields.put(fName, createFieldType(action, fields.size()));
        } else {
          logger.info("repeated field (" + fName + ") present in DNA for " + oid + ", type=" + cs.getClassName());
        }
      }
      return createNewClassAndInitializeObject(parentID, cs, fields.values());
    } catch (Exception ex) {
      throw new TCRuntimeException(ex);
    } finally {
      cursor.reset();
    }
  }

  private PhysicalManagedObjectState createNewClassAndInitializeObject(ObjectID parentID, ClassSpec cs,
                                                                       Collection<FieldType> fields) {
    try {
      int clazzId = getNextSequenceID();
      cs.setGeneratedClassID(clazzId);
      byte data[] = loader.createClassBytes(cs, fields);

      String generatedClassName = cs.getGeneratedClassName();
      Class c = loader.defineClassFromBytes(generatedClassName, clazzId, data, 0, data.length);
      addKnownClasses(cs);
      writeToDB(cs, data);
      Constructor constructor = c.getConstructor(CONSTRUCTOR_PARAMS_CLASS);
      PhysicalManagedObjectState mo = (PhysicalManagedObjectState) constructor.newInstance(CONSTRUCTOR_PARAMS);
      initializeManagedObjectState(mo, parentID);
      return mo;
    } catch (Exception e) {
      throw new TCRuntimeException(e);
    }
  }

  private void initializeManagedObjectState(PhysicalManagedObjectState po, ObjectID parentID) {
    po.setParentID(parentID);
  }

  private synchronized int getNextSequenceID() {
    return ++sequenceId;
  }

  private FieldType createFieldType(PhysicalAction action, int id) {
    String fieldName = action.getFieldName();
    Object value = action.getObject();
    return FieldType.create(fieldName, value, action.isReference(), id);
  }

}
