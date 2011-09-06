/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * This is a nifty little class that lets you 'profile' the serialization speed of a given object: it will serialize the
 * object in question, and each class underneath that object, printing the speed of each serialization at the end.
 */
public class SerializationSpeedTester {

  public static class SpeedEntry implements Comparable {
    private final Class  theClass;

    private final double millisecondsPerSerialization;

    public SpeedEntry(Class theClass, double millisecondsPerSerialization) {
      Assert.assertNotNull(theClass);

      this.theClass = theClass;
      this.millisecondsPerSerialization = millisecondsPerSerialization;
    }

    public String toString() {
      return "Class " + this.theClass.getName() + ": " + this.millisecondsPerSerialization
             + " ms per serialization, on average.";
    }

    public int compareTo(Object that) {
      double other = ((SpeedEntry) that).millisecondsPerSerialization;
      if (this.millisecondsPerSerialization > other) return 1;
      else if (this.millisecondsPerSerialization == other) return 0;
      else return -1;
    }
  }

  public SpeedEntry[] testSpeedOf(Serializable rootObject) {
    return testSpeedOf(rootObject, "com.tc");
  }

  public SpeedEntry[] testSpeedOf(Serializable rootObject, String requiredPrefix) {
    Assert.assertNotNull(rootObject);

    Map classMap = new HashMap();
    testSpeedOf(rootObject, classMap, new IdentityHashMap(), requiredPrefix, "root");

    SpeedEntry[] entries = (SpeedEntry[]) classMap.values().toArray(new SpeedEntry[classMap.size()]);
    Arrays.sort(entries);
    return entries;
  }

  private void testSpeedOf(Object object, Map classMap, Map processedObjects, String requiredPrefix, String fromWhere) {
    Assert.assertNotNull(object);

    if (object.getClass().isPrimitive()) return;
    // if (object.getClass().getName().equals("sun.reflect.UnsafeStaticObjectFieldAccessorImpl")) return;

    if (processedObjects.containsKey(object)) return;
    else {
      processedObjects.put(object, new Integer(1));
    }

    Class objectClass = object.getClass();

    if (objectClass.isArray()) {
      if (!objectClass.getComponentType().isPrimitive()) {
        int length = Array.getLength(object);
        for (int i = 0; i < length; ++i) {
          Object value = Array.get(object, i);
          if (value != null && (!value.getClass().isPrimitive())) {
            testSpeedOf(value, classMap, processedObjects, requiredPrefix, fromWhere + "[" + i + "]");
          }
        }
      }
    } else {
      if (Serializable.class.isAssignableFrom(object.getClass())
          && object.getClass().getName().startsWith(requiredPrefix) && (!classMap.containsKey(object.getClass()))) {
        classMap.put(objectClass, new SpeedEntry(objectClass, getSpeedOf(object)));
      }

      try {
        Field[] fields = getFields(objectClass);
        for (int i = 0; i < fields.length; ++i) {
          if (fields[i].getType().isPrimitive()) continue;
          if (Modifier.isStatic(fields[i].getModifiers())) continue;
          if (!fields[i].isAccessible()) fields[i].setAccessible(true);

          Object value = fields[i].get(object);
          if (value != null && (!value.getClass().isPrimitive())) {
            testSpeedOf(value, classMap, processedObjects, requiredPrefix, fields[i].toString());
          }
        }
      } catch (Exception e) {
        System.err.println("ERROR ON: " + object + " OF CLASS " + object.getClass());
        e.printStackTrace();
      }
    }
  }

  private Field[] getFields(Class objectClass) {
    List list = new ArrayList();
    addFields(objectClass, list);
    return (Field[]) list.toArray(new Field[list.size()]);
  }

  private void addFields(Class objectClass, List list) {
    Field[] fields = objectClass.getDeclaredFields();
    list.addAll(Arrays.asList(fields));
    Class superclass = objectClass.getSuperclass();
    if (superclass != null) addFields(superclass, list);
  }

  private static final int SERIALIZATION_COUNT = 1;

  private double getSpeedOf(Object object) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();

      System.err.println("Serializing object of class " + object.getClass() + "...");
      long startTime = System.currentTimeMillis();
      for (int i = 0; i < SERIALIZATION_COUNT; ++i) {
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(object);
        oos.close();
      }
      long endTime = System.currentTimeMillis();
      baos.reset();
      System.err.println("Took " + (endTime - startTime) + " ms for " + object.getClass());

      double out = (((double) (endTime - startTime)) / (double) SERIALIZATION_COUNT);
      // System.err.println("Class " + object.getClass() + ": " + out);
      return out;
    } catch (IOException ioe) {
      System.err.println("Can't get speed of: " + object.getClass().getName());
      return -1.0;
    }
  }
  
  public static void main(String[] args) throws Exception {
    // needs a good main
  }

}
