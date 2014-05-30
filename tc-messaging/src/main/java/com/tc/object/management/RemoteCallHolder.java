/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.management;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.NodeID;

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
public class RemoteCallHolder extends RemoteCallDescriptor {

  private byte[] serializedArgs;

  public RemoteCallHolder() {
  }

  public RemoteCallHolder(RemoteCallDescriptor remoteCallDescriptor, Object[] args) {
    super(remoteCallDescriptor.getL1Node(), remoteCallDescriptor.getServiceID(), remoteCallDescriptor.getMethodName(), remoteCallDescriptor.getArgTypeNames());
    this.serializedArgs = serialize(args);
  }

  protected RemoteCallHolder(NodeID l1Node, ServiceID serviceID, String methodName, String[] argTypeNames, Object[] args) {
    super(l1Node, serviceID, methodName, argTypeNames);
    this.serializedArgs = serialize(args);
  }

  public Object[] getArgs(final ClassLoader classLoader) throws ClassNotFoundException {
    return (Object[])deserialize(serializedArgs, classLoader);
  }

  public void setArgs(Object[] args) {
    this.serializedArgs = serialize(args);
  }

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
            Class<?> component;    // component class
            int dcount;          // dimension
            for (dcount = 1; cname.charAt(dcount) == '['; dcount++) ;
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

  @Override
  public void serializeTo(TCByteBufferOutput serialOutput) {
      super.serializeTo(serialOutput);
    if (serializedArgs == null) {
      serialOutput.writeInt(0);
    } else {
      serialOutput.writeInt(serializedArgs.length);
      serialOutput.write(serializedArgs);
    }
  }

  @Override
  public Object deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    super.deserializeFrom(serialInput);
    int arraySize = serialInput.readInt();
    serializedArgs = new byte[arraySize];
    serialInput.readFully(serializedArgs);
    return this;
  }
}
