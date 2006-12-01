/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.util.Util;

import gnu.trove.TIntObjectHashMap;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Define all the various TC Message type numbers.
 */

public final class TCMessageType {
  // //////////////////////////////////////////////
  // NOTE: Never recycle these numbers, always add a new constant
  // //////////////////////////////////////////////
  public static final int           TYPE_PING_MESSAGE                             = 1;
  public static final int           TYPE_PONG_MESSAGE                             = 2;
  public static final int           TYPE_REQUEST_ROOT_MESSAGE                     = 8;
  public static final int           TYPE_LOCK_REQUEST_MESSAGE                     = 9;
  public static final int           TYPE_COMMIT_TRANSACTION_MESSAGE               = 10;
  public static final int           TYPE_REQUEST_ROOT_RESPONSE_MESSAGE            = 11;
  public static final int           TYPE_REQUEST_MANAGED_OBJECT_MESSAGE           = 12;
  public static final int           TYPE_REQUEST_MANAGED_OBJECT_RESPONSE_MESSAGE  = 13;
  public static final int           TYPE_BROADCAST_TRANSACTION_MESSAGE            = 14;
  public static final int           TYPE_OBJECT_ID_BATCH_REQUEST_MESSAGE          = 18;
  public static final int           TYPE_OBJECT_ID_BATCH_REQUEST_RESPONSE_MESSAGE = 19;
  public static final int           TYPE_ACKNOWLEDGE_TRANSACTION_MESSAGE          = 24;
  public static final int           TYPE_LOCK_RESPONSE_MESSAGE                    = 26;
  public static final int           TYPE_CLIENT_HANDSHAKE_MESSAGE                 = 28;
  public static final int           TYPE_BATCH_TRANSACTION_ACK_MESSAGE            = 29;
  public static final int           TYPE_CLIENT_HANDSHAKE_ACK_MESSAGE             = 30;
  public static final int           TYPE_CONFIG_PUSH_MESSAGE                      = 31;
  public static final int           TYPE_OVERRIDE_APPLICATION_CONFIG_MESSAGE      = 32;
  public static final int           TYPE_LOCK_RECALL_MESSAGE                      = 33;
  public static final int           TYPE_JMX_MESSAGE                              = 34;
  public static final int           TYPE_LOCK_QUERY_RESPONSE_MESSAGE              = 35;
  public static final int           TYPE_JMXREMOTE_MESSAGE_CONNECTION_MESSAGE     = 36;

  public static final TCMessageType PING_MESSAGE                                  = new TCMessageType();
  public static final TCMessageType PONG_MESSAGE                                  = new TCMessageType();
  public static final TCMessageType REQUEST_ROOT_MESSAGE                          = new TCMessageType();
  public static final TCMessageType LOCK_REQUEST_MESSAGE                          = new TCMessageType();
  public static final TCMessageType LOCK_RECALL_MESSAGE                           = new TCMessageType();
  public static final TCMessageType LOCK_RESPONSE_MESSAGE                         = new TCMessageType();
  public static final TCMessageType LOCK_QUERY_RESPONSE_MESSAGE                   = new TCMessageType();
  public static final TCMessageType COMMIT_TRANSACTION_MESSAGE                    = new TCMessageType();
  public static final TCMessageType REQUEST_ROOT_RESPONSE_MESSAGE                 = new TCMessageType();
  public static final TCMessageType REQUEST_MANAGED_OBJECT_MESSAGE                = new TCMessageType();
  public static final TCMessageType REQUEST_MANAGED_OBJECT_RESPONSE_MESSAGE       = new TCMessageType();
  public static final TCMessageType BROADCAST_TRANSACTION_MESSAGE                 = new TCMessageType();
  public static final TCMessageType OBJECT_ID_BATCH_REQUEST_MESSAGE               = new TCMessageType();
  public static final TCMessageType OBJECT_ID_BATCH_REQUEST_RESPONSE_MESSAGE      = new TCMessageType();
  public static final TCMessageType ACKNOWLEDGE_TRANSACTION_MESSAGE               = new TCMessageType();
  public static final TCMessageType CLIENT_HANDSHAKE_MESSAGE                      = new TCMessageType();
  public static final TCMessageType CLIENT_HANDSHAKE_ACK_MESSAGE                  = new TCMessageType();
  public static final TCMessageType BATCH_TRANSACTION_ACK_MESSAGE                 = new TCMessageType();
  public static final TCMessageType CONFIG_PUSH_MESSAGE                           = new TCMessageType();
  public static final TCMessageType OVERRIDE_APPLICATION_CONFIG_MESSAGE           = new TCMessageType();
  public static final TCMessageType JMX_MESSAGE                                   = new TCMessageType();
  public static final TCMessageType JMXREMOTE_MESSAGE_CONNECTION_MESSAGE          = new TCMessageType();

  public static TCMessageType getInstance(int i) {
    return (TCMessageType) typeMap.get(i);
  }

  public static TCMessageType[] getAllMessageTypes() {
    return (TCMessageType[]) allTypes.clone();
  }

  public int getType() {
    return type;
  }

  public String getTypeName() {
    return typeName;
  }

  public String toString() {
    return typeName + " (" + type + ")";
  }

  // //////////////////////////////////////////////////////
  //
  // ******** You need not modify anything below *********
  //
  // //////////////////////////////////////////////////////
  private static final TCLogger          logger     = TCLogging.getLogger(TCMessageType.class);
  private static final TIntObjectHashMap typeMap    = new TIntObjectHashMap();
  private static final TCMessageType[]   allTypes;
  private static final String            typePrefix = "TYPE_";

  private int                            type;
  private String                         typeName;

  private TCMessageType() {
    // type and name are set automagically in init()
  }

  private void setType(int type) {
    this.type = type;
  }

  private void setTypeName(String typeName) {
    this.typeName = typeName;
  }

  public int hashCode() {
    return this.typeName.hashCode() + type;
  }

  public boolean equals(Object obj) {
    if (obj instanceof TCMessageType) {
      TCMessageType other = (TCMessageType) obj;
      return this.typeName.equals(other.typeName) && (this.type == other.type);
    }
    return false;
  }

  /**
   * do some sanity checking on all the defined message types. Hopefully we'll catch duplicate message numbers early at
   * development time. If somehow a compiler introduces funny public static final fields in this class, then one can add
   * additional checks for naming conventions for the message types. Or maybe we shouldn't try to be smart about
   * validating the message type numbers dynamically. Maybe I need more some coffee
   */
  private static TCMessageType[] init() throws IllegalArgumentException, IllegalAccessException {
    Field[] fields = TCMessageType.class.getDeclaredFields();

    Map mtFields = new HashMap();
    Map intFields = new HashMap();

    for (int i = 0; i < fields.length; i++) {
      final Field field = fields[i];
      final String fName = field.getName();

      int modifiers = field.getModifiers();

      // disallow public non-final fields
      if (Modifier.isPublic(modifiers) && !Modifier.isFinal(modifiers)) { throw new RuntimeException(
                                                                                                     "TCMessageType: "
                                                                                                         + fName
                                                                                                         + " must be final if public"); }

      boolean shouldInspect = Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers)
                              && Modifier.isFinal(modifiers);

      if (!shouldInspect) {
        continue;
      }

      if (field.getType().equals(TCMessageType.class)) {
        if (!fName.toUpperCase().equals(fName)) {
          // make formatter sane
          throw new RuntimeException("TCMessageType: Message type names must be all UPPER CASE: " + fName);
        }

        if (fName.startsWith("_")) {
          // make formatter sane
          throw new RuntimeException("TCMessageType: Message type cannot start with underscore: " + fName);
        }

        mtFields.put(fName, field);
      } else if (field.getType().equals(Integer.TYPE)) {
        if (!fName.startsWith(typePrefix)) {
          // <Hi there> If this is being thrown, you probably added an oddly
          // named static final integer to this class
          throw new RuntimeException("TCMessageType: Illegal integer field name: " + fName);
        }

        Integer value = (Integer) field.get(TCMessageType.class);

        intFields.put(fName, value);
      }
    }

    for (final Iterator iter = mtFields.values().iterator(); iter.hasNext();) {
      final Field field = (Field) iter.next();
      final String name = field.getName();
      final TCMessageType type = (TCMessageType) field.get(TCMessageType.class);

      final String intName = typePrefix + name;
      if (!intFields.containsKey(intName)) {
        // make formatter sane
        throw new RuntimeException("TCMessageType: Missing " + intName + " integer constant");
      }

      final int val = ((Integer) intFields.remove(intName)).intValue();

      type.setType(val);
      type.setTypeName(name);

      Object prev = typeMap.put(type.getType(), type);
      if (prev != null) {
        // make formatter sane
        throw new RuntimeException("TCMessageType: Duplicate message types defined for message number: "
                                   + type.getType());
      }

      iter.remove();
    }

    if (!mtFields.isEmpty()) {
      // make formatter sane
      throw new RuntimeException("TCMessageType: internal error - not all message types filled in");
    }

    if (!intFields.isEmpty()) {
      String unused = Util.enumerateArray(intFields.keySet().toArray());
      throw new RuntimeException("TCMessageType: Unused integer constants (please remove): " + unused);
    }

    final TCMessageType[] rv = new TCMessageType[typeMap.size()];
    System.arraycopy(typeMap.getValues(), 0, rv, 0, rv.length);

    Arrays.sort(rv, new Comparator() {
      public int compare(Object o1, Object o2) {
        int i1 = ((TCMessageType) o1).getType();
        int i2 = ((TCMessageType) o2).getType();

        if (i1 < i2) {
          return -1;
        } else if (i1 == i2) {
          return 0;
        } else if (i1 > i2) {
          return 1;
        } else {
          throw new RuntimeException("internal error");
        }
      }
    });

    if (logger.isDebugEnabled()) {
      logger.debug(Util.enumerateArray(rv));
    }

    return rv;
  }

  static {
    try {
      allTypes = init();
    } catch (Exception e) {
      e.printStackTrace();
      throw new TCRuntimeException(e);
    }
  }

}
