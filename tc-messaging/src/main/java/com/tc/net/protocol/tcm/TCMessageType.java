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
package com.tc.net.protocol.tcm;


import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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
  public static final int           TYPE_PING_MESSAGE                                 = 1;
  public static final int           TYPE_CLIENT_HANDSHAKE_MESSAGE                     = 2;
  public static final int           TYPE_CLIENT_HANDSHAKE_ACK_MESSAGE                 = 3;
  public static final int           TYPE_CLIENT_HANDSHAKE_REDIRECT_MESSAGE                 = 4;
  
  public static final int           TYPE_CLUSTER_MEMBERSHIP_EVENT_MESSAGE             = 6;
  public static final int           TYPE_GROUP_WRAPPER_MESSAGE                        = 7;
  public static final int           TYPE_GROUP_HANDSHAKE_MESSAGE                      = 8;
  public static final int           TYPE_CLIENT_HANDSHAKE_REFUSED_MESSAGE             = 9;
  public static final int           TYPE_LIST_REGISTERED_SERVICES_MESSAGE             = 10;
  public static final int           TYPE_LIST_REGISTERED_SERVICES_RESPONSE_MESSAGE    = 11;
  public static final int           TYPE_INVOKE_REGISTERED_SERVICE_MESSAGE            = 12;
  public static final int           TYPE_INVOKE_REGISTERED_SERVICE_RESPONSE_MESSAGE   = 13;
  public static final int           TYPE_VOLTRON_ENTITY_RECEIVED_RESPONSE                          = 14;
  public static final int           TYPE_SERVER_ENTITY_MESSAGE                        = 15;
  public static final int           TYPE_SERVER_ENTITY_RESPONSE_MESSAGE               = 16;
  public static final int           TYPE_VOLTRON_ENTITY_MESSAGE                       = 17;
  public static final int           TYPE_VOLTRON_ENTITY_APPLIED_RESPONSE              = 18;
  public static final int           TYPE_VOLTRON_ENTITY_RETIRED_RESPONSE              = 19;
  public static final int           TYPE_VOLTRON_ENTITY_MULTI_RESPONSE              = 20;
  public static final int           TYPE_NOOP_MESSAGE              = 21;
  public static final int           TYPE_DIAGNOSTIC_REQUEST                 = 22;
  public static final int           TYPE_DIAGNOSTIC_RESPONSE                 = 23;
  public static final int           TYPE_LAST_MESSAGE_DO_NOT_USE              = 24;

  public static final TCMessageType PING_MESSAGE                                      = new TCMessageType();
  public static final TCMessageType CLIENT_HANDSHAKE_MESSAGE                          = new TCMessageType();
  public static final TCMessageType CLIENT_HANDSHAKE_ACK_MESSAGE                      = new TCMessageType();
  public static final TCMessageType CLIENT_HANDSHAKE_REFUSED_MESSAGE                  = new TCMessageType();
  public static final TCMessageType CLIENT_HANDSHAKE_REDIRECT_MESSAGE                  = new TCMessageType();
  public static final TCMessageType CLUSTER_MEMBERSHIP_EVENT_MESSAGE                  = new TCMessageType();
  public static final TCMessageType GROUP_WRAPPER_MESSAGE                             = new TCMessageType();
  public static final TCMessageType GROUP_HANDSHAKE_MESSAGE                           = new TCMessageType();
  public static final TCMessageType LIST_REGISTERED_SERVICES_MESSAGE                  = new TCMessageType();
  public static final TCMessageType LIST_REGISTERED_SERVICES_RESPONSE_MESSAGE         = new TCMessageType();
  public static final TCMessageType INVOKE_REGISTERED_SERVICE_MESSAGE                 = new TCMessageType();
  public static final TCMessageType INVOKE_REGISTERED_SERVICE_RESPONSE_MESSAGE        = new TCMessageType();
  public static final TCMessageType VOLTRON_ENTITY_RECEIVED_RESPONSE                  = new TCMessageType();
  public static final TCMessageType SERVER_ENTITY_MESSAGE                             = new TCMessageType();
  public static final TCMessageType SERVER_ENTITY_RESPONSE_MESSAGE                    = new TCMessageType();
  public static final TCMessageType VOLTRON_ENTITY_MESSAGE                            = new TCMessageType();
  public static final TCMessageType VOLTRON_ENTITY_APPLIED_RESPONSE                   = new TCMessageType();
  public static final TCMessageType VOLTRON_ENTITY_RETIRED_RESPONSE                   = new TCMessageType();
  public static final TCMessageType VOLTRON_ENTITY_MULTI_RESPONSE                   = new TCMessageType();
  public static final TCMessageType NOOP_MESSAGE                   = new TCMessageType();  
  public static final TCMessageType DIAGNOSTIC_REQUEST                   = new TCMessageType();  
  public static final TCMessageType DIAGNOSTIC_RESPONSE                   = new TCMessageType();  
  
  public static final TCMessageType LAST_MESSAGE_DO_NOT_USE                   = new TCMessageType();  // this one must always be the last

  public static TCMessageType getInstance(int i) {
    return typeArray[i-1];
  }

  public int getType() {
    return this.type;
  }

  public String getTypeName() {
    return this.typeName;
  }

  @Override
  public String toString() {
    return this.typeName + " (" + this.type + ")";
  }

  // //////////////////////////////////////////////////////
  //
  // ******** You need not modify anything below *********
  //
  // //////////////////////////////////////////////////////
  private static final Map<Integer, TCMessageType> typeMap    = new HashMap<Integer, TCMessageType>();
  private static final TCMessageType[] typeArray    = safeInit();
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

  @Override
  public int hashCode() {
    return this.typeName.hashCode() + this.type;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof TCMessageType) {
      final TCMessageType other = (TCMessageType) obj;
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
    final Field[] fields = TCMessageType.class.getDeclaredFields();

    final Map<String, Field> mtFields = new HashMap<String, Field>();
    final Map<String, Integer> intFields = new HashMap<String, Integer>();

    for (final Field field : fields) {
      final String fName = field.getName();

      final int modifiers = field.getModifiers();

      // disallow public non-final fields
      if (Modifier.isPublic(modifiers) && !Modifier.isFinal(modifiers)) { throw new RuntimeException(
                                                                                                     "TCMessageType: "
                                                                                                         + fName
                                                                                                         + " must be final if public"); }

      final boolean shouldInspect = Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers)
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

        final Integer value = (Integer) field.get(TCMessageType.class);

        intFields.put(fName, value);
      }
    }

    for (final Iterator<Field> iter = mtFields.values().iterator(); iter.hasNext();) {
      final Field field = iter.next();
      final String name = field.getName();
      final TCMessageType type = (TCMessageType) field.get(TCMessageType.class);

      final String intName = typePrefix + name;
      if (!intFields.containsKey(intName)) {
        // make formatter sane
        throw new RuntimeException("TCMessageType: Missing " + intName + " integer constant");
      }

      final int val = intFields.remove(intName).intValue();

      type.setType(val);
      type.setTypeName(name);

      final Object prev = typeMap.put(type.getType(), type);
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
      final String unused = intFields.keySet().toString();
      throw new RuntimeException("TCMessageType: Unused integer constants (please remove): " + unused);
    }

    final TCMessageType[] rv = new TCMessageType[TYPE_LAST_MESSAGE_DO_NOT_USE];
    for (TCMessageType tc : typeMap.values()) {
      rv[tc.getType()-1] = tc;
    }

    return rv;
  }
  
  private static TCMessageType[] safeInit() {
    try {
      return init();
    } catch (Exception i) {
      throw new RuntimeException(i);
    }
  }
}
