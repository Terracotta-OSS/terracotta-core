/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.model;

import com.tc.admin.ConnectionContext;
import com.tc.object.ObjectID;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.objectserver.mgmt.MapEntryFacade;

public abstract class AbstractTcObject implements IObject {
  protected ManagedObjectFacadeProvider facadeProvider;
  protected String                      name;
  protected IObject                     parent;
  protected int                         batchSize;

  protected AbstractTcObject(ManagedObjectFacadeProvider facadeProvider, String name, IObject parent) {
    this.facadeProvider = facadeProvider;
    this.name = name;
    this.parent = parent;
    batchSize = ConnectionContext.DSO_SMALL_BATCH_SIZE;
  }

  public abstract Object getFacade();

  public ObjectID getObjectID() {
    return null;
  }

  public String getName() {
    return name;
  }

  public IObject getParent() {
    return parent;
  }

  public IObject getRoot() {
    IObject obj = this;
    while (obj != null) {
      if (obj.getParent() == null) { return obj; }
      obj = obj.getParent();
    }
    return null;
  }

  public void setBatchSize(int batchSize) {
    this.batchSize = batchSize;
  }

  public int getBatchSize() {
    return batchSize;
  }

  protected IObject newObject(String fieldName, Object value, String type) throws Exception {
    if (value instanceof MapEntryFacade) {
      return new TcMapEntryObject(facadeProvider, fieldName, (MapEntryFacade) value, this);
    } else if (value instanceof ObjectID) {
      ObjectID id = (ObjectID) value;

      if (!id.isNull()) {
        value = facadeProvider.lookupFacade(id, batchSize);
        type = ((ManagedObjectFacade) value).getClassName();
      } else {
        value = null;
        type = null;
      }
      if (type != null && (type.equals("java.util.Date") || type.equals("java.sql.Timestamp"))) {
        ManagedObjectFacade mof = (ManagedObjectFacade) value;
        value = mof.getFieldValue("date");
      }
    }
    type = convertTypeName(type);
    return new BasicTcObject(facadeProvider, fieldName, value, type, this);
  }

  private static final char C_ARRAY = '[';

  public static int getArrayCount(char[] typeSignature) throws IllegalArgumentException {
    try {
      int count = 0;
      while (typeSignature[count] == C_ARRAY) {
        ++count;
      }
      return count;
    } catch (ArrayIndexOutOfBoundsException e) {
      // signature is syntactically incorrect if last character is C_ARRAY
      throw new IllegalArgumentException();
    }
  }

  static String convertTypeName(String typeName) {
    if (typeName != null && typeName.length() > 0) {
      if (typeName.charAt(0) == C_ARRAY) {
        try {
          int arrayCount = getArrayCount(typeName.toCharArray());
          typeName = typeName.substring(arrayCount);
          if (typeName.charAt(0) == 'L') {
            int pos = 1;
            while (typeName.charAt(pos) != ';')
              pos++;
            typeName = typeName.substring(1, pos);
          } else {
            typeName = nativeTypeFor(typeName.charAt(0));
          }
          StringBuffer sb = new StringBuffer(typeName);
          for (int i = 0; i < arrayCount; i++) {
            sb.append("[]");
          }
          typeName = sb.toString();
        } catch (IllegalArgumentException iae) {/**/
        }
      } else if (typeName.length() == 1) {
        typeName = nativeTypeFor(typeName.charAt(0));
      }
    }
    return typeName;
  }

  private static String nativeTypeFor(char c) {
    switch (c) {
      case 'Z':
        return "boolean";
      case 'I':
        return "int";
      case 'F':
        return "float";
      case 'C':
        return "char";
      case 'D':
        return "double";
      case 'B':
        return "byte";
    }
    return String.valueOf(c);
  }

}
