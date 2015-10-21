/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
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

  static Object deserialize(byte[] bytes, final ClassLoader classLoader) throws ClassNotFoundException {
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
