/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config;

public class MethodSpec {
  public final static int ALWAYS_LOG                             = 1;
  public final static int NONE                                   = 2;
  public final static int HASHMAP_REMOVE_LOG                     = 3;
  public final static int HASHMAP_PUT_LOG                        = 4;
  public final static int HASHTABLE_REMOVE_LOG                   = 5;
  public final static int HASHTABLE_PUT_LOG                      = 6;
  public final static int HASHTABLE_CLEAR_LOG                    = 7;
  public final static int LIST_REMOVE_LOG                        = 8;
  public final static int IF_TRUE_LOG                            = 9;
  public final static int SET_ITERATOR_WRAPPER_LOG               = 10;
  public final static int SORTED_SET_VIEW_WRAPPER_LOG            = 11;
  public final static int THASHMAP_PUT_LOG                       = 12;
  public final static int TOBJECTHASH_REMOVE_AT_LOG              = 13;
  // public final static int THASHSET_ADD_LOG = 14;
  // public final static int THASHSET_REMOVE_LOG = 15;
  public final static int ENTRY_SET_WRAPPER_LOG                  = 16;
  public final static int KEY_SET_WRAPPER_LOG                    = 17;
  public final static int VALUES_WRAPPER_LOG                     = 18;
  public final static int DATE_ADD_SET_TIME_WRAPPER_LOG          = 19;
  public final static int TIMESTAMP_SET_TIME_METHOD_WRAPPER_LOG  = 20;
  public final static int LINKED_HASH_MAP_GET_METHOD_WRAPPER_LOG = 21;

  private final String    name;
  private final int       instrumentationType;

  public MethodSpec(String name, int instrumentationType) {
    this.name = name;
    this.instrumentationType = instrumentationType;
  }

  public String getName() {
    return name;
  }

  public int getInstrumentationType() {
    return instrumentationType;
  }
}
