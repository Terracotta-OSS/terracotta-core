/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.applicator;

import com.tc.object.TCClass;
import com.tc.object.TCObjectExternal;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAEncoding;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * This applicator handles transients in java.util.Calendar and subclasses (eg. GregorianCalendar)
 */
public class CalendarApplicator extends PhysicalApplicator {

  private static final Class       OBJECT_CLASS   = Object.class;
  private static final Object      NO_READ_OBJECT = new Object();

  private final Map<Class, Object> readObjects    = new WeakHashMap();

  public CalendarApplicator(TCClass clazz, DNAEncoding encoding) {
    super(clazz, encoding);
  }

  @Override
  public void hydrate(ApplicatorObjectManager objectManager, TCObjectExternal tcObject, DNA dna, Object po)
      throws IOException, ClassNotFoundException {
    super.hydrate(objectManager, tcObject, dna, po);

    Class c = po.getClass();
    while (c != OBJECT_CLASS) {
      runReadObjectIfPresent(po, c);
      c = c.getSuperclass();
    }
  }

  private void runReadObjectIfPresent(Object po, Class c) {
    Method readObject = getReadObjectOrNull(c);
    if (readObject == null) return;

    try {
      readObject.invoke(po, new Object[] { new NullObjectInputStream() });
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Method getReadObjectOrNull(Class c) {
    synchronized (readObjects) {
      Object o = readObjects.get(c);
      if (o == NO_READ_OBJECT) { return null; }
      if (o == null) {
        o = findReadObjectMethod(c);
        if (o == null) {
          readObjects.put(c, NO_READ_OBJECT);
        } else {
          readObjects.put(c, o);
        }
      }
      return (Method) o;
    }
  }

  private static Method findReadObjectMethod(Class c) {
    try {
      Method m = c.getDeclaredMethod("readObject", new Class[] { ObjectInputStream.class });
      m.setAccessible(true);
      return m;
    } catch (NoSuchMethodException nsme) {
      return null;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static class NullObjectInputStream extends ObjectInputStream {

    protected NullObjectInputStream() throws IOException, SecurityException {
      super();
    }

    @Override
    public void defaultReadObject() {
      //
    }

  }

}
