/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.management;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Array;

/**
 *
 */
class SerializationHelper {

  static byte[] serialize(Object obj) {
    if (obj == null) {
      return new byte[0];
    }

    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(obj);
      oos.close();
      return baos.toByteArray();
    } catch (IOException ioe) {
      throw new TCManagementSerializationException("Error serializing object", ioe);
    }
  }

  static Object deserialize(byte[] bytes, ClassLoader classLoader) throws ClassNotFoundException {
    if (bytes.length == 0) {
      return null;
    }

    try {
      ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes)) {
        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
          String cname = desc.getName();

          if (cname.startsWith("[")) {
            // An array
            Class<?> component;
            int dcount;
            for (dcount = 1; cname.charAt(dcount) == '['; dcount++) { }
            if (cname.charAt(dcount) == 'L') {
              component = lookupClass(cname.substring(dcount + 1, cname.length() - 1));
            } else {
              // primitive array
              return super.resolveClass(desc);
            }
            int dim[] = new int[dcount];
            for (int i = 0; i < dcount; i++) {
              dim[i] = 0;
            }
            return Array.newInstance(component, dim).getClass();
          } else {
            return lookupClass(cname);
          }
        }

        private Class<?> lookupClass(String s) throws ClassNotFoundException {
          return classLoader.loadClass(s);
        }
      };
      return ois.readObject();
    } catch (IOException ioe) {
      throw new TCManagementSerializationException("Error deserializing object", ioe);
    }
  }


}
