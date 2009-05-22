/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import sun.misc.Unsafe;

import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.applicator.ChangeApplicator;
import com.tc.object.bytecode.NotClearable;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.impl.ProxyInstance;
import com.tc.object.field.TCField;
import com.tc.object.field.TCFieldFactory;
import com.tc.object.loaders.LoaderDescription;
import com.tc.object.loaders.Namespace;
import com.tc.util.Assert;
import com.tc.util.ClassUtils;
import com.tc.util.ReflectionUtil;
import com.tc.util.UnsafeUtil;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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
  private final static SerializationUtil SERIALIZATION_UTIL     = new SerializationUtil();

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
  private final boolean                  onLoadInjection;
  private final String                   onLoadScript;
  private final String                   onLoadMethod;
  private final ChangeApplicator         applicator;
  private final String                   parentFieldName;
  private final Map                      declaredTCFieldsByName = new HashMap();
  private final Map                      tcFieldsByName         = new HashMap();
  private final LoaderDescription        loaderDesc;
  private final Field                    parentField;
  private final boolean                  useNonDefaultConstructor;
  private final Map                      offsetToFieldNames;
  private final ClientObjectManager      objectManager;
  private final boolean                  isProxyClass;
  private final boolean                  isEnum;
  private final String                   logicalExtendingClassName;
  private final Class                    logicalSuperClass;
  private final boolean                  useResolveLockWhileClearing;
  private final boolean                  isNotClearable;
  private final List<Method>             postCreateMethods;
  private final List<Method>             preCreateMethods;
  private Constructor                    constructor            = null;

  TCClassImpl(final TCFieldFactory factory, final TCClassFactory clazzFactory, final ClientObjectManager objectManager,
              final Class peer, final Class logicalSuperClass, final LoaderDescription loaderDesc,
              final String logicalExtendingClassName, final boolean isLogical, final boolean isCallConstructor,
              final boolean onLoadInjection, final String onLoadScript, final String onLoadMethod,
              final boolean useNonDefaultConstructor, final boolean useResolveLockWhileClearing,
              String postCreateMethod, String preCreateMethod) {
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
    this.onLoadInjection = onLoadInjection;
    this.onLoadScript = onLoadScript;
    this.onLoadMethod = onLoadMethod;
    this.superclazz = findSuperClass(peer);
    this.isEnum = ClassUtils.isDsoEnum(peer);
    this.logicalExtendingClassName = logicalExtendingClassName;

    this.applicator = createApplicator();

    introspectFields(peer, factory);
    this.portableFields = createPortableFields();
    this.useNonDefaultConstructor = isProxyClass || ClassUtils.isPortableReflectionClass(peer)
                                    || useNonDefaultConstructor;
    this.logicalSuperClass = logicalSuperClass;
    this.offsetToFieldNames = getFieldOffsets(peer);
    this.useResolveLockWhileClearing = useResolveLockWhileClearing;
    this.isNotClearable = NotClearable.class.isAssignableFrom(peer);
    this.postCreateMethods = resolveCreateMethods(postCreateMethod, false);
    this.preCreateMethods = resolveCreateMethods(preCreateMethod, true);
  }

  private List<Method> resolveCreateMethods(String methodName, boolean preCreate) {
    List<Method> rv = new ArrayList<Method>();
    if (superclazz != null) {
      rv.addAll(preCreate ? superclazz.getPreCreateMethods() : superclazz.getPostCreateMethods());
    } else {
      Assert.assertEquals(Object.class, peer);
    }

    if (methodName != null) {
      try {
        Method method = peer.getDeclaredMethod(methodName);
        method.setAccessible(true);
        rv.add(method);
      } catch (Exception e) {
        logger.error("Exception resolving method '" + methodName + "' on " + peer, e);
      }
    }

    if (rv.isEmpty()) { return Collections.EMPTY_LIST; }

    return Collections.unmodifiableList(rv);
  }

  public Field getParentField() {
    return parentField;
  }

  public boolean isNotClearable() {
    return isNotClearable;
  }

  public boolean isNonStaticInner() {
    return this.isNonStaticInner;
  }

  public Class getPeerClass() {
    return this.peer;
  }

  private Field findParentField() {
    Field[] fields = peer.getDeclaredFields();
    for (Field field : fields) {
      if (SERIALIZATION_UTIL.isParent(field.getName())) return field;
    }
    return null;
  }

  private TCClass findSuperClass(final Class c) {
    Class superclass = c.getSuperclass();
    if (superclass != null) { return clazzFactory.getOrCreate(superclass, objectManager); }
    return null;
  }

  private ChangeApplicator createApplicator() {
    return clazzFactory.createApplicatorFor(this, indexed);
  }

  public void hydrate(final TCObject tcObject, final DNA dna, final Object pojo, final boolean force)
      throws IOException, ClassNotFoundException {
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
    } else if (logger.isDebugEnabled()) {
      logger
          .debug("IGNORING UPDATE, local object at version " + localVersion + ", dna update is version " + dnaVersion);
    }

  }

  public void dehydrate(final TCObject tcObject, final DNAWriter writer, final Object pojo) {
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

  public boolean hasOnLoadInjection() {
    return onLoadInjection;
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
      for (Constructor con : cons) {
        Class[] types = con.getParameterTypes();
        if (types.length == 0) {
          rv = con;
          rv.setAccessible(true);
          return rv;
        }
      }
    }

    rv = ReflectionUtil.newConstructor(peer, logicalSuperClass);
    rv.setAccessible(true);
    return rv;
  }

  public String getParentFieldName() {
    return parentFieldName;
  }

  private void introspectFields(final Class clazz, final TCFieldFactory fieldFactory) {
    // Note: this gets us all of the fields declared in the class, static
    // as well as instance fields.
    Field[] fields = clazz.equals(Object.class) ? new Field[0] : clazz.getDeclaredFields();

    Field field;
    TCField tcField;
    for (Field field2 : fields) {
      field = field2;
      // The factory does a bunch of callbacks based on the field type.
      tcField = fieldFactory.getInstance(this, field);
      declaredTCFieldsByName.put(field.getName(), tcField);
      tcFieldsByName.put(tcField.getName(), tcField);
    }
  }

  @Override
  public String toString() {
    return peer.getName();
  }

  /**
   * Expects the field name in the format <classname>. <fieldname>(e.g. com.foo.Bar.baz)
   */
  public TCField getField(final String name) {
    TCField rv = (TCField) tcFieldsByName.get(name);
    if (rv == null && superclazz != null) {
      rv = superclazz.getField(name);
    }
    return rv;
  }

  public TCField[] getPortableFields() {
    return portableFields;
  }

  public TraversedReferences getPortableObjects(final Object pojo, final TraversedReferences addTo) {
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

  public boolean isIndexed() {
    return indexed;
  }

  public LoaderDescription getDefiningLoaderDescription() {
    return loaderDesc;
  }

  public boolean isLogical() {
    return isLogical;
  }

  public ClientObjectManager getObjectManager() {
    return objectManager;
  }

  public TCObject createTCObject(final ObjectID id, final Object pojo, final boolean isNew) {
    if (isLogical) {
      return new TCObjectLogical(id, pojo, this, isNew);
    } else {
      return new TCObjectPhysical(id, pojo, this, isNew);
    }
  }

  public boolean hasOnLoadMethod() {
    return onLoadMethod != null;
  }

  public boolean isUseNonDefaultConstructor() {
    return useNonDefaultConstructor;
  }

  public Object getNewInstanceFromNonDefaultConstructor(final DNA dna) throws IOException, ClassNotFoundException {
    Object o = applicator.getNewInstance(objectManager, dna);

    if (o == null) { throw new AssertionError("Can't find suitable constructor for class: " + getName() + "."); }
    return o;
  }

  private static Map getFieldOffsets(final Class peer) {
    Map rv = new HashMap();
    if (unsafe != null) {
      try {
        Field[] fields = peer.equals(Object.class) ? new Field[0] : peer.getDeclaredFields();
        // System.err.println("Thread " + Thread.currentThread().getName() + ", class: " + getName() + ", # of field: "
        // + fields.length);
        for (int i = 0; i < fields.length; i++) {
          try {
            if (!Modifier.isStatic(fields[i].getModifiers())) {
              fields[i].setAccessible(true);
              rv.put(new Long(unsafe.objectFieldOffset(fields[i])), makeFieldName(fields[i]));
              // System.err.println("Thread " + Thread.currentThread().getName() + ", class: " + getName() + ", field: "
              // + fields[i].getName() + ", offset: " + unsafe.objectFieldOffset(fields[i]));
            }
          } catch (Exception e) {
            // Ignore those fields that throw an exception
          }
        }
      } catch (Exception e) {
        throw new TCRuntimeException(e);
      }
    }

    return rv;
  }

  private static String makeFieldName(final Field field) {
    StringBuffer sb = new StringBuffer(field.getDeclaringClass().getName());
    sb.append(".");
    sb.append(field.getName());
    return sb.toString();
  }

  public String getFieldNameByOffset(final long fieldOffset) {
    Long fieldOffsetObj = new Long(fieldOffset);

    String field = (String) this.offsetToFieldNames.get(fieldOffsetObj);
    if (field == null) {
      if (superclazz != null) {
        return superclazz.getFieldNameByOffset(fieldOffset);
      } else {
        throw new AssertionError("Field does not exist for offset: " + fieldOffset);
      }
    } else {
      return field;
    }
  }

  public boolean isPortableField(final long fieldOffset) {
    String fieldName = getFieldNameByOffset(fieldOffset);
    TCField tcField = getField(fieldName);

    return tcField.isPortable();
  }

  public boolean isProxyClass() {
    return isProxyClass;
  }

  public boolean useResolveLockWhileClearing() {
    return useResolveLockWhileClearing;
  }

  public List<Method> getPostCreateMethods() {
    return this.postCreateMethods;
  }

  public List<Method> getPreCreateMethods() {
    return this.preCreateMethods;
  }
}
