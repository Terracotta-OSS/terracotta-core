/*
 * Copyright (c) 2006-2007 Terracotta, Inc. All rights reserved.
 */

package com.tc.admin;

import com.tc.admin.dso.DSOHelper;
import com.tc.admin.dso.DSORoot;
import com.tc.admin.dso.RootsHelper;
import com.tc.management.JMXConnectorProxy;
import com.tc.object.ObjectID;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.objectserver.mgmt.MapEntryFacade;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

/**
 * Utility to dump distributed object graph from the server
 * 
 * @author Eugene Kuleshov
 */
public class RootsTool {
  private ConnectionContext context;
  private JMXConnector      jmxc;

  public RootsTool(String host, int port) throws Exception {
    context = new ConnectionContext(host, port);
  }

  private void connect() throws IOException {
    jmxc = new JMXConnectorProxy(context.host, context.port);

    context.jmxc = jmxc;
    context.mbsc = jmxc.getMBeanServerConnection();
  }

  private void disconnect() throws IOException {
    jmxc.close();
  }

  private void print(PrintWriter w) throws Exception {
    connect();
    try {
      DSORoot[] roots = RootsHelper.getHelper().getRoots(context);
      for (int i = 0; i < roots.length; i++) {
        DSORoot root = roots[i];
        ObjectName objectName = root.getObjectName();
        String rootId = objectName.getKeyProperty("rootID");
        w.println(i + " " + root + " id=" + rootId);

        String[] fieldNames = root.getFieldNames();
        for (int j = 0; j < fieldNames.length; j++) {
          String fieldName = fieldNames[i];
          Object fieldValue = root.getFieldValue(fieldName);
          printValue(w, "  ", fieldName, root.getFieldType(fieldName), fieldValue, new HashSet());
        }
      }
    } finally {
      disconnect();
    }
  }

  private void printValue(PrintWriter w, String off, String name, String type, Object value, Set seen) throws Exception {
    ObjectID objectId;
    ManagedObjectFacade facade;
    if (value instanceof ObjectID) {
      objectId = (ObjectID) value;
      if (objectId.isNull()) {
        w.println(off + "  " + name + " = null");
        return;
      }
      facade = DSOHelper.getHelper().lookupFacade(context, objectId, 10);
      if (facade.isArray()) {
        int arrayLength = facade.getArrayLength();
        if (arrayLength > 0 && facade.isPrimitive("0")) {
          StringBuffer sb = new StringBuffer(" {");
          for (int i = 0; i < arrayLength; i++) {
            if (i > 0) sb.append(", ");
            sb.append("" + facade.getFieldValue("" + i));
          }
          sb.append("}");
          w.println(off + "- " + name + " (" + facade.getClassName() + ") " + sb.toString() + " " + objectId.toLong());
          return;
        }
      }
      w.println(off + "  " + name + " (" + facade.getClassName() + ") " + objectId.toLong() + " "
                + getSeen(seen, objectId));

    } else if (value instanceof ManagedObjectFacade) {
      facade = (ManagedObjectFacade) value;
      objectId = facade.getObjectId();
      w.println(off + "  " + name + " (" + facade.getClassName() + ") " + objectId.toLong() + " "
                + getSeen(seen, objectId));

    } else if (value instanceof MapEntryFacade) {
      MapEntryFacade entry = (MapEntryFacade) value;
      Object entryKey = entry.getKey();
      Object entryValue = entry.getValue();
      if (entryKey instanceof ObjectID) {
        w.println(off + "ENTRY");
        printValue(w, off + "  ", "key", null, entryKey, seen);
        printValue(w, off + "  ", "value", null, entryValue, seen);
      } else {
        w.println(off + "  ENTRY key = " + entryKey);
        printValue(w, off + "  ", "value", null, entryValue, seen);
      }
      return;

    } else {
      w.println(off + "- " + name + " = " + value + " (" + type + ")");
      return;

    }

    if (!seen.contains(objectId)) {
      seen.add(objectId);
      String[] fields = facade.getFields();
      for (int k = 0; k < fields.length; k++) {
        String fieldName = fields[k];
        String fieldType = facade.getFieldType(fieldName);
        Object fieldValue = facade.getFieldValue(fieldName);
        printValue(w, off + "  ", getShortFieldName(fieldName), fieldType, fieldValue, seen);
      }
    }
  }

  private String getSeen(Set seen, ObjectID objectId) {
    return seen.contains(objectId) ? "[SEEN BEFORE]" : "";
  }

  private static String getShortFieldName(String fieldName) {
    int n = fieldName.lastIndexOf('.');
    return n == -1 ? fieldName : fieldName.substring(n + 1);
  }

  public static void main(String[] args) throws Exception {
    PrintWriter w = new PrintWriter(System.out);

    String host;
    int port;
    if (args.length > 1) {
      host = args[0];
      port = Integer.parseInt(args[1]);
    } else {
      host = "localhost";
      port = 9520;
    }

    RootsTool rootsTool = new RootsTool(host, port);
    rootsTool.print(w);

    w.flush();
    w.close();
  }

}
