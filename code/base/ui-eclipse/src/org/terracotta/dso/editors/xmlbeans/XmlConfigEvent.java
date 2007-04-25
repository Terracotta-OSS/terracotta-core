/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors.xmlbeans;

import org.apache.xmlbeans.XmlObject;

import com.tc.util.event.UpdateEvent;
import com.tc.util.event.UpdateEventListener;

public final class XmlConfigEvent extends UpdateEvent {

  // if the config xml structure is changed, rename the effected event type (producing errors) to locate it's listeners
  public static final int      ALT_RANGE_CONSTANT                  = 999999;
  public static final int      SERVER_NAME                         = 10;
  public static final int      SERVER_HOST                         = 15;
  public static final int      SERVER_DSO_PORT                     = 20;
  public static final int      SERVER_JMX_PORT                     = 25;
  public static final int      SERVER_DATA                         = 30;
  public static final int      SERVER_LOGS                         = 35;
  public static final int      SERVER_PERSIST                      = 40;
  public static final int      SERVER_GC                           = 45;
  public static final int      SERVER_GC_VERBOSE                   = 50;
  public static final int      SERVER_GC_INTERVAL                  = 55;
  public static final int      CLIENT_LOGS                         = 60;
  public static final int      CLIENT_CLASS                        = 65;
  public static final int      CLIENT_HIERARCHY                    = 70;
  public static final int      CLIENT_LOCKS                        = 75;
  public static final int      CLIENT_TRANSIENT_ROOT               = 80;
  public static final int      CLIENT_DISTRIBUTED_METHODS          = 85;
  public static final int      CLIENT_ROOTS                        = 90;
  public static final int      CLIENT_LOCK_DEBUG                   = 95;
  public static final int      CLIENT_DISTRIBUTED_METHOD_DEBUG     = 100;
  public static final int      CLIENT_FIELD_CHANGE_DEBUG           = 105;
  public static final int      CLIENT_NON_PORTABLE_DUMP            = 110;
  public static final int      CLIENT_WAIT_NOTIFY_DEBUG            = 115;
  public static final int      CLIENT_NEW_OBJECT_DEBUG             = 120;
  public static final int      CLIENT_AUTOLOCK_DETAILS             = 125;
  public static final int      CLIENT_CALLER                       = 130;
  public static final int      CLIENT_FULL_STACK                   = 135;
  public static final int      CLIENT_MODULE_LOCATION              = 140;
  public static final int      CLIENT_NAME                         = 145;
  public static final int      CLIENT_VERSION                      = 150;
  public static final int      CLIENT_FAULT_COUNT                  = 155;
  public static final int      ROOTS_FIELD                         = 160;
  public static final int      ROOTS_NAME                          = 165;
  public static final int      TRANSIENT_FIELD                     = 170;
  public static final int      DISTRIBUTED_METHOD                  = 175;
  public static final int      BOOT_CLASS                          = 180;
  public static final int      LOCKS_AUTO_METHOD                   = 185;
  public static final int      LOCKS_AUTO_LEVEL                    = 190;
  public static final int      LOCKS_NAMED_NAME                    = 195;
  public static final int      LOCKS_NAMED_METHOD                  = 200;
  public static final int      LOCKS_NAMED_LEVEL                   = 205;
  public static final int      INSTRUMENTED_CLASS_EXPRESSION       = 210;
  public static final int      INSTRUMENTED_CLASS_RULE             = 215;
  public static final int      INCLUDE_HONOR_TRANSIENT             = 220;
  public static final int      INCLUDE_BEHAVIOR                    = 225;
  public static final int      INCLUDE_ON_LOAD_EXECUTE             = 230;
  public static final int      INCLUDE_ON_LOAD_METHOD              = 235;
  public static final int      INSTRUMENTED_CLASS_ORDER_UP         = 240;
  public static final int      INSTRUMENTED_CLASS_ORDER_DOWN       = 245;
  // only this context may listen to "create" and "delete" events - corresponding "new" and "remove" events are
  // broadcast
  public static final int      NULL                                = -1;
  public static final int      CREATE_SERVER                       = -5;
  public static final int      DELETE_SERVER                       = -10;
  public static final int      CREATE_CLIENT_MODULE_REPO           = -15;
  public static final int      CREATE_CLIENT_MODULE                = -20;
  public static final int      DELETE_CLIENT_MODULE_REPO           = -25;
  public static final int      DELETE_CLIENT_MODULE                = -30;
  public static final int      CREATE_ROOT                         = -35;
  public static final int      DELETE_ROOT                         = -40;
  public static final int      CREATE_TRANSIENT_FIELD              = -45;
  public static final int      DELETE_TRANSIENT_FIELD              = -50;
  public static final int      CREATE_DISTRIBUTED_METHOD           = -55;
  public static final int      DELETE_DISTRIBUTED_METHOD           = -60;
  public static final int      CREATE_BOOT_CLASS                   = -65;
  public static final int      DELETE_BOOT_CLASS                   = -70;
  public static final int      CREATE_LOCK_AUTO                    = -75;
  public static final int      DELETE_LOCK_AUTO                    = -80;
  public static final int      CREATE_LOCK_NAMED                   = -85;
  public static final int      DELETE_LOCK_NAMED                   = -90;
  public static final int      CREATE_INSTRUMENTED_CLASS           = -95;
  public static final int      DELETE_INSTRUMENTED_CLASS           = -100;
  public static final int      DELETE_INCLUDE_ON_LOAD              = -105;
  // only this context may notify "new" and "remove" events after receiving a corresponding "create" or "delete" event
  public static final int      NEW_SERVER                          = ALT_RANGE_CONSTANT + 5;
  public static final int      REMOVE_SERVER                       = ALT_RANGE_CONSTANT + 10;
  public static final int      NEW_CLIENT_MODULE_REPO              = ALT_RANGE_CONSTANT + 15;
  public static final int      NEW_CLIENT_MODULE                   = ALT_RANGE_CONSTANT + 20;
  public static final int      REMOVE_CLIENT_MODULE_REPO           = ALT_RANGE_CONSTANT + 25;
  public static final int      REMOVE_CLIENT_MODULE                = ALT_RANGE_CONSTANT + 30;
  public static final int      NEW_ROOT                            = ALT_RANGE_CONSTANT + 35;
  public static final int      REMOVE_ROOT                         = ALT_RANGE_CONSTANT + 40;
  public static final int      NEW_TRANSIENT_FIELD                 = ALT_RANGE_CONSTANT + 45;
  public static final int      REMOVE_TRANSIENT_FIELD              = ALT_RANGE_CONSTANT + 50;
  public static final int      NEW_DISTRIBUTED_METHOD              = ALT_RANGE_CONSTANT + 55;
  public static final int      REMOVE_DISTRIBUTED_METHOD           = ALT_RANGE_CONSTANT + 60;
  public static final int      NEW_BOOT_CLASS                      = ALT_RANGE_CONSTANT + 65;
  public static final int      REMOVE_BOOT_CLASS                   = ALT_RANGE_CONSTANT + 70;
  public static final int      NEW_LOCK_AUTO                       = ALT_RANGE_CONSTANT + 75;
  public static final int      REMOVE_LOCK_AUTO                    = ALT_RANGE_CONSTANT + 80;
  public static final int      NEW_LOCK_NAMED                      = ALT_RANGE_CONSTANT + 85;
  public static final int      REMOVE_LOCK_NAMED                   = ALT_RANGE_CONSTANT + 90;
  public static final int      NEW_INSTRUMENTED_CLASS              = ALT_RANGE_CONSTANT + 95;
  public static final int      REMOVE_INSTRUMENTED_CLASS           = ALT_RANGE_CONSTANT + 100;
  public static final int      REMOVE_INCLUDE_ON_LOAD              = ALT_RANGE_CONSTANT + 105;
  // container elements with no associated events
  public static final String   PARENT_ELEM_APPLICATION             = "application";
  public static final String   PARENT_ELEM_DSO                     = "dso";
  public static final String   PARENT_ELEM_PERSIST                 = "persistence";
  public static final String   PARENT_ELEM_GC                      = "garbage-collection";
  public static final String   PARENT_ELEM_DEBUGGING               = "debugging";
  public static final String   PARENT_ELEM_INSTRUMENTATION_LOGGING = "instrumentation-logging";
  public static final String   PARENT_ELEM_RUNTIME_OUTPUT_OPTIONS  = "runtime-output-options";
  public static final String   PARENT_ELEM_RUNTIME_LOGGING         = "runtime-logging";
  public static final String   PARENT_ELEM_AUTOLOCK                = "autolock";
  public static final String   PARENT_ELEM_NAMED_LOCK              = "named-lock";
  public static final String   PARENT_ELEM_MODULES                 = "modules";
  public static final String   PARENT_ELEM_INCLUDE_ON_LOAD         = "on-load";
  // element names
  private static final String  ELEM_NAME                           = "name";
  private static final String  ELEM_HOST                           = "host";
  private static final String  ELEM_DSO_PORT                       = "dso-port";
  private static final String  ELEM_JMX_PORT                       = "jmx-port";
  private static final String  ELEM_DATA                           = "data";
  private static final String  ELEM_LOGS                           = "logs";
  private static final String  ELEM_PERSIST                        = "mode";
  private static final String  ELEM_GC                             = "enabled";
  private static final String  ELEM_GC_VERBOSE                     = "verbose";
  private static final String  ELEM_GC_INTERVAL                    = "interval";
  private static final String  ELEM_CLASS                          = "class";
  private static final String  ELEM_HIERARCHY                      = "hierarchy";
  private static final String  ELEM_LOCKS                          = "locks";
  private static final String  ELEM_TRANSIENT_ROOT                 = "transient-root";
  private static final String  ELEM_DISTRIBUTED_METHODS            = "distributed-methods";
  private static final String  ELEM_ROOTS                          = "roots";
  private static final String  ELEM_LOCK_DEBUG                     = "lock-debug";
  private static final String  ELEM_DISTRIBUTED_METHOD_DEBUG       = "distributed-method-debug";
  private static final String  ELEM_FIELD_CHANGE_DEBUG             = "field-change-debug";
  private static final String  ELEM_NON_PORTABLE_DUMP              = "non-portable-dump";
  private static final String  ELEM_WAIT_NOTIFY_DEBUG              = "wait-notify-debug";
  private static final String  ELEM_NEW_OBJECT_DEBUG               = "new-object-debug";
  private static final String  ELEM_AUTOLOCK_DETAILS               = "auto-lock-details";
  private static final String  ELEM_CALLER                         = "caller";
  private static final String  ELEM_FULL_STACK                     = "full-stack";
  private static final String  ELEM_FAULT_COUNT                    = "fault-count";
  private static final String  ELEM_FIELD_NAME                     = "field-name";
  private static final String  ELEM_ROOT_NAME                      = "root-name";
  private static final String  ELEM_LOCK_LEVEL                     = "lock-level";
  private static final String  ELEM_METHOD_EXPRESSION              = "method-expression";
  private static final String  ELEM_LOCK_NAME                      = "lock-name";
  private static final String  ELEM_INCLUDE                        = "include";
  private static final String  ELEM_INSTRUMENTED_CLASS_EXPRESSION  = "class-expression";
  private static final String  ELEM_INCLUDE_HONOR_TRANSIENT        = "honor-transient";
  private static final String  ELEM_INCLUDE_ON_LOAD_METHOD         = "method";
  private static final String  ELEM_INCLUDE_ON_LOAD_EXECUTE        = "execute";

  public static final String[] m_elementNames                      = new String[245 + 1];
  static {
    m_elementNames[SERVER_NAME] = ELEM_NAME;
    m_elementNames[SERVER_HOST] = ELEM_HOST;
    m_elementNames[SERVER_DSO_PORT] = ELEM_DSO_PORT;
    m_elementNames[SERVER_JMX_PORT] = ELEM_JMX_PORT;
    m_elementNames[SERVER_DATA] = ELEM_DATA;
    m_elementNames[SERVER_LOGS] = ELEM_LOGS;
    m_elementNames[SERVER_PERSIST] = ELEM_PERSIST;
    m_elementNames[SERVER_GC] = ELEM_GC;
    m_elementNames[SERVER_GC_VERBOSE] = ELEM_GC_VERBOSE;
    m_elementNames[SERVER_GC_INTERVAL] = ELEM_GC_INTERVAL;
    m_elementNames[CLIENT_LOGS] = ELEM_LOGS;
    m_elementNames[CLIENT_CLASS] = ELEM_CLASS;
    m_elementNames[CLIENT_HIERARCHY] = ELEM_HIERARCHY;
    m_elementNames[CLIENT_LOCKS] = ELEM_LOCKS;
    m_elementNames[CLIENT_TRANSIENT_ROOT] = ELEM_TRANSIENT_ROOT;
    m_elementNames[CLIENT_DISTRIBUTED_METHODS] = ELEM_DISTRIBUTED_METHODS;
    m_elementNames[CLIENT_ROOTS] = ELEM_ROOTS;
    m_elementNames[CLIENT_LOCK_DEBUG] = ELEM_LOCK_DEBUG;
    m_elementNames[CLIENT_DISTRIBUTED_METHOD_DEBUG] = ELEM_DISTRIBUTED_METHOD_DEBUG;
    m_elementNames[CLIENT_FIELD_CHANGE_DEBUG] = ELEM_FIELD_CHANGE_DEBUG;
    m_elementNames[CLIENT_NON_PORTABLE_DUMP] = ELEM_NON_PORTABLE_DUMP;
    m_elementNames[CLIENT_WAIT_NOTIFY_DEBUG] = ELEM_WAIT_NOTIFY_DEBUG;
    m_elementNames[CLIENT_NEW_OBJECT_DEBUG] = ELEM_NEW_OBJECT_DEBUG;
    m_elementNames[CLIENT_AUTOLOCK_DETAILS] = ELEM_AUTOLOCK_DETAILS;
    m_elementNames[CLIENT_CALLER] = ELEM_CALLER;
    m_elementNames[CLIENT_FULL_STACK] = ELEM_FULL_STACK;
    m_elementNames[CLIENT_FAULT_COUNT] = ELEM_FAULT_COUNT;
    m_elementNames[ROOTS_FIELD] = ELEM_FIELD_NAME;
    m_elementNames[ROOTS_NAME] = ELEM_ROOT_NAME;
    m_elementNames[LOCKS_AUTO_LEVEL] = ELEM_LOCK_LEVEL;
    m_elementNames[LOCKS_AUTO_METHOD] = ELEM_METHOD_EXPRESSION;
    m_elementNames[LOCKS_NAMED_LEVEL] = ELEM_LOCK_LEVEL;
    m_elementNames[LOCKS_NAMED_METHOD] = ELEM_METHOD_EXPRESSION;
    m_elementNames[LOCKS_NAMED_NAME] = ELEM_LOCK_NAME;
    m_elementNames[INSTRUMENTED_CLASS_RULE] = ELEM_INCLUDE;
    m_elementNames[INSTRUMENTED_CLASS_EXPRESSION] = ELEM_INSTRUMENTED_CLASS_EXPRESSION;
    m_elementNames[INCLUDE_HONOR_TRANSIENT] = ELEM_INCLUDE_HONOR_TRANSIENT;
    m_elementNames[INCLUDE_ON_LOAD_METHOD] = ELEM_INCLUDE_ON_LOAD_METHOD;
    m_elementNames[INCLUDE_ON_LOAD_EXECUTE] = ELEM_INCLUDE_ON_LOAD_EXECUTE;
  }

  public final int             type;
  public XmlObject             element;
  public Object                variable;
  public int                   index;

  public XmlConfigEvent(int type) {
    this(null, null, null, type);
  }

  public XmlConfigEvent(XmlObject element, int type) {
    this(null, null, element, type);
  }

  public XmlConfigEvent(Object data, UpdateEventListener source, XmlObject element, int type) {
    super(data);
    this.source = source; // may be null
    this.element = element;
    this.type = type;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("data=" + data + "\n");
    sb.append("source=" + source + "\n");
    sb.append("type=" + type + "\n");
    sb.append("element=" + element.getClass().getName() + "\n");
    sb.append("variable=" + variable + "\n");
    return sb.toString();
  }
}
