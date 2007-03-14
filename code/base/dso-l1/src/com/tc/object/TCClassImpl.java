/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object;

import sun.misc.Unsafe;

import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.applicator.ChangeApplicator;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.impl.ProxyInstance;
import com.tc.object.field.TCField;
import com.tc.object.field.TCFieldFactory;
import com.tc.object.loaders.Namespace;
import com.tc.object.tx.optimistic.OptimisticTransactionManager;
import com.tc.util.Assert;
import com.tc.util.ClassUtils;
import com.tc.util.ReflectionUtil;
import com.tc.util.UnsafeUtil;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

/**
 * Peer of a Class under management.
 * <p>
 * This is used to cache the fields of each class by type.
 *
 * @author orion
 */
public class TCClassImpl implements TCClass {
  private final static TCLogger          logger                 = TCLogging.getLogger(TCClassImpl.class);
  private final static Unsafe            unsafe                 = UnsafeUtil.getUnsafe();

  /**
   * Peer java class that this TCClass represents.
   */
  private final Class                    peer;

  private final TCClass                  superclazz;
  private final TCClassFactory           clazzFactory;
  private final TCField[]                portableFields;
  private final boolean                  indexed;
  private final boolean                  isNonStaticInner;
  private final boolean                  isLogical;
  private final boolean                  isCallConstructor;
  private final String                   onLoadScript;
  private final String                   onLoadMethod;
  private final ChangeApplicator         applicator;
  private final String                   parentFieldName;
  private final Map                      declaredTCFieldsByName = new HashMap();
  private final Map                      tcFieldsByName         = new HashMap();
  private final String                   loaderDesc;
  private Constructor                    constructor            = null;
  private final Field                    parentField;
  private final static SerializationUtil SERIALIZATION_UTIL     = new SerializationUtil();
  private final boolean                  useNonDefaultConstructor;
  private Map                            offsetToFields;
  private final ClientObjectManager      objectManager;
  private final boolean                  isProxyClass;
  private final boolean                  isEnum;

  private final String                   logicalExtendingClassName;
  private final Class                    logicalSuperClass;

  TCClassImpl(TCFieldFactory factory, TCClassFactory clazzFactory, ClientObjectManager objectManager, Class peer,
              Class logicalSuperClass, String loaderDesc, String logicalExtendingClassName, boolean isLogical,
              boolean isCallConstructor, String onLoadScript, String onLoadMethod, boolean useNonDefaultConstructor) {
    this.clazzFactory = clazzFactory;
    this.objectManager = objectManager;
    this.peer = peer;
    this.loaderDesc = loaderDesc;
    this.indexed = peer.isArray();

    boolean isStatic = Modifier.isStatic(peer.getModifiers());
    boolean mightBeInner = peer.getName().indexOf('$') != -1 && !isIndexed();
    this.parentField = mightBeInner && !isStatic ? findParentField() : null;
    this.isNonStaticInner = parentField != null;
    this.parentFieldName = parentField == null ? null : getName() + '.' + parentField.getName();

    this.isLogical = isLogical;
    this.isProxyClass = Proxy.isProxyClass(peer) || ProxyInstance.class.getName().equals(peer.getName());
    this.isCallConstructor = isCallConstructor;
    this.onLoadScript = onLoadScript;
    this.onLoadMethod = onLoadMethod;
    this.superclazz = findSuperClass(peer);
    this.isEnum = ClassUtils.isEnum(peer);
    this.logicalExtendingClassName = logicalExtendingClassName;

    this.applicator = createApplicator();

    introspectFields(peer, factory);
    this.portableFields = createPortableFields();
    this.useNonDefaultConstructor = isProxyClass || ClassUtils.isPortableReflectionClass(peer) || useNonDefaultConstructor;
    this.logicalSuperClass = logicalSuperClass;
  }

  public Field getParentField() {
    return parentField;
  }

  public boolean isNonStaticInner() {
    return this.isNonStaticInner;
  }

  public Class getPeerClass() {
    return this.peer;
  }

  private Field findParentField() {
    Field[] fields = peer.getDeclaredFields();
    for (int i = 0; i < fields.length; i++) {
      if (SERIALIZATION_UTIL.isParent(fields[i].getName())) return fields[i];
    }
    return null;
  }

  private TCClass findSuperClass(Class c) {
    Class superclass = c.getSuperclass();
    if (superclass != null) { return clazzFactory.getOrCreate(superclass, objectManager); }
    return null;
  }

  private ChangeApplicator createApplicator() {
    return clazzFactory.createApplicatorFor(this, indexed);
  }

  public void hydrate(TCObject tcObject, DNA dna, Object pojo, boolean force) throws IOException,
      ClassNotFoundException {
    // Okay...long story here The application of the DNA used to be a synchronized(applicator) block. As best as Steve
    // and I could tell, the synchronization was solely a memory boundary and not a mutual exlusion mechanism. For the
    // time being, we have resolved that we need no synchronization here (either for memory, or exclusion). The memory
    // barrier aspect isn't known to be a problem and the concurrency is handled by the server (ie. we won't get
    // concurrent updates). At some point it would be a good idea to detect (and error out) when updates are received
    // from L2 but local read/writes have been made on the target TCObject

    final long localVersion = tcObject.getVersion();
    final long dnaVersion = dna.getVersion();

    if (force || (localVersion < dnaVersion)) {
      tcObject.setVersion(dnaVersion);
      applicator.hydrate(objectManager, tcObject, dna, pojo);
    } else {
      if (logger.isDebugEnabled()) {
        logger.debug("IGNORING UPDATE, local object at version " + localVersion + ", dna update is version "
                     + dnaVersion);
      }
    }

  }

  public void dehydrate(TCObject tcObject, DNAWriter writer, Object pojo) {
    try {
      applicator.dehydrate(objectManager, tcObject, writer, pojo);
    } catch (ConcurrentModificationException cme) {
      // try to log some useful stuff about the pojo in question here.
      // This indicates improper locking, but is certainly possible
      String type = pojo == null ? "null" : pojo.getClass().getName();
      String toString = String.valueOf(pojo);
      int ihc = System.identityHashCode(pojo);
      logger.error("Shared object (presumably new) modified during dehydrate (type " + type + ", ihc " + ihc + "): "
                   + toString, cme);
      throw cme;
    }
  }

  public Class getComponentType() {
    return peer.getComponentType();
  }

  public boolean isEnum() {
    return isEnum;
  }

  public String getName() {
    if (isProxyClass) { return ProxyInstance.class.getName(); }
    if (isEnum) { return LiteralValues.ENUM_CLASS_DOTS; }
    return peer.getName();
  }

  public String getExtendingClassName() {
    String className = getName();
    if (this.logicalExtendingClassName != null) {
      className = Namespace.createLogicalExtendingClassName(className, logicalExtendingClassName);
    }
    return className;
  }

  public TCClass getSuperclass() {
    return superclazz;
  }

  public synchronized Constructor getConstructor() {
    if (constructor == null) {
      // As best as I can tell, the reason for the lazy initialization here is that we don't actually need the cstr
      // looked up for all of the TCClass instances we cook up. Additionally, the assertions in findConstructor will go
      // off for a fair number of abstract base classes (eg. java.util.AbstractMap, java.util.Dictionary, etc)
      constructor = findConstructor();
    }
    return constructor;
  }

  public boolean hasOnLoadExecuteScript() {
    return onLoadScript != null;
  }

  public String getOnLoadExecuteScript() {
    Assert.eval(hasOnLoadExecuteScript());
    return onLoadScript;
  }

  public String getOnLoadMethod() {
    Assert.eval(hasOnLoadMethod());
    return onLoadMethod;
  }

  private Constructor findConstructor() {
    Constructor rv = null;

    if (isCallConstructor || isLogical) {
      Constructor[] cons = peer.getDeclaredConstructors();
      for (int i = 0; i < cons.length; i++) {
        Class[] types = cons[i].getParameterTypes();
        if (types.length == 0) {
          rv = cons[i];
          rv.setAccessible(true);
          return rv;
        }
      }
    }

    if (rv == null) {
      rv = ReflectionUtil.newConstructor(peer, logicalSuperClass);
      rv.setAccessible(true);
    }
    return rv;
  }

  public String getParentFieldName() {
    return parentFieldName;
  }

  private void introspectFields(Class clazz, TCFieldFactory fieldFactory) {
    // Note: this gets us all of the fields declared in the class, static
    // as well as instance fields.
    Field[] fields = clazz.equals(Object.class) ? new Field[0] : clazz.getDeclaredFields();

    Field field;
    TCField tcField;
    for (int i = 0; i < fields.length; i++) {
      field = fields[i];
      // The factory does a bunch of callbacks based on the field type.
      tcField = fieldFactory.getInstance(this, field);
      declaredTCFieldsByName.put(field.getName(), tcField);
      tcFieldsByName.put(tcField.getName(), tcField);
    }
  }

  public String toString() {
    return peer.getName();
  }

  /**
   * Expects the field name in the format <classname>. <fieldname>(e.g. com.foo.Bar.baz)
   */
  public TCField getField(String name) {
    TCField rv = (TCField) tcFieldsByName.get(name);
    if (rv == null && superclazz != null) {
      rv = superclazz.getField(name);
    }
    return rv;
  }

  public TCField[] getPortableFields() {
    return portableFields;
  }

  public TraversedReferences getPortableObjects(Object pojo, TraversedReferences addTo) {
    return applicator.getPortableObjects(pojo, addTo);
  }

  private TCField[] createPortableFields() {
    if (isLogical || !objectManager.isPortableClass(this.peer)) { return new TCField[0]; }
    LinkedList l = new LinkedList();
    for (Iterator i = declaredTCFieldsByName.values().iterator(); i.hasNext();) {

      TCField f = (TCField) i.next();
      if (f.isPortable()) {
        l.add(f);
      }
    }
    return (TCField[]) l.toArray(new TCField[l.size()]);
  }

  public Map connectedCopy(Object source, Object dest, Map visited, OptimisticTransactionManager txManager) {
    return this.applicator.connectedCopy(source, dest, visited, objectManager, txManager);
  }

  public boolean isIndexed() {
    return indexed;
  }

  public String getDefiningLoaderDescription() {
    return loaderDesc;
  }

  public boolean isLogical() {
    return isLogical;
  }

  public ClientObjectManager getObjectManager() {
    return objectManager;
  }

  public TCObject createTCObject(ObjectID id, Object pojo) {
    if (isLogical) {
      return new TCObjectLogical(objectManager.getReferenceQueue(), id, pojo, this);
    } else {
      return new TCObjectPhysical(objectManager.getReferenceQueue(), id, pojo, this);
    }
  }

  public boolean hasOnLoadMethod() {
    return onLoadMethod != null;
  }

  public boolean isUseNonDefaultConstructor() {
    return useNonDefaultConstructor;
  }

  public Object getNewInstanceFromNonDefaultConstructor(DNA dna) throws IOException, ClassNotFoundException {
    Object o = applicator.getNewInstance(objectManager, dna);

    if (o == null) { throw new AssertionError("Can't find suitable constructor for class: " + getName() + "."); }
    return o;
  }

  public String getFieldNameByOffset(long fieldOffset) {
    Long fieldOffsetObj = new Long(fieldOffset);
    if (offsetToFields == null) {
      offsetToFields = new HashMap();
      if (unsafe != null) {
        try {
          Field[] fields = peer.equals(Object.class) ? new Field[0] : peer.getDeclaredFields();
          for (int i = 0; i < fields.length; i++) {
            try {
              if (!Modifier.isStatic(fields[i].getModifiers())) {
                fields[i].setAccessible(true);
                offsetToFields.put(new Long(unsafe.objectFieldOffset(fields[i])), fields[i]);
              }
            } catch (Exception e) {
              // Ignore those fields that throw an exception
            }
          }
        } catch (Exception e) {
          throw new TCRuntimeException(e);
        }
      }
    }
    Field field = (Field) this.offsetToFields.get(fieldOffsetObj);
    if (field == null) {
      if (superclazz != null) {
        return superclazz.getFieldNameByOffset(fieldOffset);
      } else {
        throw new AssertionError("Field does not exist for offset: " + fieldOffset);
      }
    } else {
      StringBuffer sb = new StringBuffer(field.getDeclaringClass().getName());
      sb.append(".");
      sb.append(field.getName());
      return sb.toString();
    }
  }

  public boolean isProxyClass() {
    return isProxyClass;
  }
}
