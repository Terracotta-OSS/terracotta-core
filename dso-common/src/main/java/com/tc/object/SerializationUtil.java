/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntIterator;

import java.util.regex.Pattern;

/**
 * Some utility stuff for logical serialization
 */
public class SerializationUtil {

  // NOTE: DO NOT USE VALUE 0. A zero indicates a mapping that does exist
  public final static int         ADD                                  = 1;
  public final static int         ADD_AT                               = 2;
  public final static int         ADD_LAST                             = 3;
  public final static int         ADD_FIRST                            = 4;
  public final static int         PUT                                  = 5;
  public final static int         CLEAR                                = 6;
  public final static int         REMOVE                               = 7;
  public final static int         REMOVE_AT                            = 8;
  public final static int         SET                                  = 9;
  public final static int         SET_ELEMENT                          = 10;
  public final static int         SET_SIZE                             = 11;
  public final static int         TRIM_TO_SIZE                         = 12;
  public final static int         INSERT_AT                            = 13;
  public final static int         REMOVE_FIRST                         = 14;
  public final static int         REMOVE_LAST                          = 15;
  public final static int         REMOVE_RANGE                         = 16;
  public final static int         SET_TIME                             = 17;
  public final static int         SET_NANOS                            = 18;
  public final static int         REMOVE_ALL                           = 19;
  public final static int         VIEW_SET                             = 20;
  public final static int         GET                                  = 21;
  public final static int         TAKE                                 = 22;
  public final static int         REMOVE_FIRST_N                       = 23;
  public final static int         REPLACE                              = 24;
  public final static int         REPLACE_IF_VALUE_EQUAL               = 25;
  public final static int         PUT_IF_ABSENT                        = 26;
  public final static int         REMOVE_IF_VALUE_EQUAL                = 27;
  public final static int         URL_SET                              = 28;
  public final static int         CLEAR_LOCAL_CACHE                    = 29;
  public final static int         EVICTION_COMPLETED                   = 30;
  public final static int         SET_TARGET_MAX_TOTAL_COUNT           = 31;
  public final static int         SET_MAX_TTI                          = 32;
  public final static int         SET_MAX_TTL                          = 33;

  public final static String      PUSH_SIGNATURE                       = "push(Ljava/lang/Object;)java/lang/Object;";
  public final static String      POP_SIGNATURE                        = "pop()java/lang/Object;";
  public final static String      ADD_AT_SIGNATURE                     = "add(ILjava/lang/Object;)V";
  public final static String      INSERT_ELEMENT_AT_SIGNATURE          = "insertElementAt(Ljava/lang/Object;I)V";
  public final static String      ADD_ELEMENT_SIGNATURE                = "addElement(Ljava/lang/Object;)V";
  public final static String      ADD_ALL_AT_SIGNATURE                 = "addAll(ILjava/util/Collection;)Z";
  public final static String      ADD_SIGNATURE                        = "add(Ljava/lang/Object;)Z";
  public final static String      ADD_IF_ABSENT_SIGNATURE              = "addIfAbsent(Ljava/lang/Object;)Z";
  public final static String      ADD_ALL_ABSENT_SIGNATURE             = "addAllAbsent(Ljava/util/Collection;)I";
  public final static String      ADD_ALL_SIGNATURE                    = "addAll(Ljava/util/Collection;)Z";
  public final static String      ADD_LAST_SIGNATURE                   = "addLast(Ljava/lang/Object;)V";
  public final static String      ADD_FIRST_SIGNATURE                  = "addFirst(Ljava/lang/Object;)V";
  public final static String      CLEAR_SIGNATURE                      = "clear()V";
  public final static String      CLEAR_LOCAL_CACHE_SIGNATURE          = "clearLocalCache()V";
  public final static String      SET_TARGET_MAX_TOTAL_COUNT_SIGNATURE = "setTargetMaxTotalCount(I)V";
  public final static String      SET_MAX_TTI_SIGNATURE                = "setMaxTTI(I)V";
  public final static String      SET_MAX_TTL_SIGNATURE                = "setMaxTTL(I)V";
  public final static String      EVICTION_COMPLETED_SIGNATURE         = "evictionCompleted()V";
  public final static String      PUT_SIGNATURE                        = "put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";
  public final static String      GET_SIGNATURE                        = "get(Ljava/lang/Object;)Ljava/lang/Object;";
  public final static String      SET_SIGNATURE                        = "set(ILjava/lang/Object;)Ljava/lang/Object;";
  public final static String      SET_ELEMENT_SIGNATURE                = "setElementAt(Ljava/lang/Object;I)V";
  public final static String      TRIM_TO_SIZE_SIGNATURE               = "trimToSize()V";
  public final static String      SET_SIZE_SIGNATURE                   = "setSize(I)V";
  public final static String      REMOVE_AT_SIGNATURE                  = "remove(I)Ljava/lang/Object;";
  public final static String      REMOVE_SIGNATURE                     = "remove(Ljava/lang/Object;)Z";
  public final static String      REMOVE_KEY_SIGNATURE                 = "remove(Ljava/lang/Object;)Ljava/lang/Object;";
  public final static String      REMOVE_ENTRY_FOR_KEY_SIGNATURE       = "removeEntryForKey(Ljava/lang/Object;)Ljava/util/HashMap$Entry;";
  public final static String      REMOVE_ELEMENT_SIGNATURE             = "removeElement(Ljava/lang/Object;)Z";
  public final static String      REMOVE_ELEMENT_AT_SIGNATURE          = "removeElementAt(I)V";
  public final static String      REMOVE_ALL_ELEMENTS_SIGNATURE        = "removeAllElements()V";
  public final static String      REMOVE_ALL_SIGNATURE                 = "removeAll(Ljava/util/Collection;)Z";
  public final static String      RETAIN_ALL_SIGNATURE                 = "retainAll(Ljava/util/Collection;)Z";
  public final static String      ITERATOR_SIGNATURE                   = "iterator()Ljava/util/Iterator;";
  public final static String      TROVE_REMOVE_AT_SIGNATURE            = "removeAt(I)V";
  public final static String      REMOVE_FIRST_SIGNATURE               = "removeFirst()Ljava/lang/Object;";
  public final static String      REMOVE_LAST_SIGNATURE                = "removeLast()Ljava/lang/Object;";
  public final static String      REMOVE_RANGE_SIGNATURE               = "removeRange(II)V";
  public final static String      TO_ARRAY_SIGNATURE                   = "toArray([Ljava/lang/Object;)[Ljava/lang/Object;";
  public final static String      COPY_INTO_SIGNATURE                  = "copyInto([Ljava/lang/Object;)V";
  public final static String      SUBSET_SIGNATURE                     = "subSet(Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/SortedSet;";
  public final static String      HEADSET_SIGNATURE                    = "headSet(Ljava/lang/Object;)Ljava/util/SortedSet;";
  public final static String      TAILSET_SIGNATURE                    = "tailSet(Ljava/lang/Object;)Ljava/util/SortedSet;";
  public final static String      EXECUTE_SIGNATURE                    = "execute(Ljava/lang/Object;)Z";
  public final static String      ENTRY_SET_SIGNATURE                  = "entrySet()Ljava/util/Set;";
  public final static String      KEY_SET_SIGNATURE                    = "keySet()Ljava/util/Set;";
  public final static String      VALUES_SIGNATURE                     = "values()Ljava/util/Collection;";
  public final static String      SET_TIME_SIGNATURE                   = "setTime(J)V";
  public final static String      SET_YEAR_SIGNATURE                   = "setYear(I)V";
  public final static String      SET_MONTH_SIGNATURE                  = "setMonth(I)V";
  public final static String      SET_DATE_SIGNATURE                   = "setDate(I)V";
  public final static String      SET_HOURS_SIGNATURE                  = "setHours(I)V";
  public final static String      SET_MINUTES_SIGNATURE                = "setMinutes(I)V";
  public final static String      SET_SECONDS_SIGNATURE                = "setSeconds(I)V";
  public final static String      SET_NANOS_SIGNATURE                  = "setNanos(I)V";
  public final static String      ITERATOR_REMOVE_SIGNATURE            = "remove()V";
  public final static String      ELEMENTS_SIGNATURE                   = "elements()Ljava/util/Enumeration;";
  public final static String      QUEUE_PUT_SIGNATURE                  = "put(Ljava/lang/Object;)V";
  public final static String      OFFER_SIGNATURE                      = "offer(Ljava/lang/Object;)Z";
  public final static String      OFFER_TIMEOUT_SIGNATURE              = "offer(Ljava/lang/Object;JLjava/util/concurrent/TimeUnit;)Z";
  public final static String      TAKE_SIGNATURE                       = "take()Ljava/lang/Object;";
  public final static String      POLL_TIMEOUT_SIGNATURE               = "poll(JLjava/util/concurrent/TimeUnit;)Ljava/lang/Object;";
  public final static String      POLL_SIGNATURE                       = "poll()Ljava/lang/Object;";
  public final static String      DRAIN_TO_SIGNATURE                   = "drainTo(Ljava/util/Collection;)I";
  public final static String      DRAIN_TO_N_SIGNATURE                 = "drainTo(Ljava/util/Collection;I)I";
  public final static String      REMOVE_FIRST_N_SIGNATURE             = "removeFirst(I)V";
  public final static String      PUT_IF_ABSENT_SIGNATURE              = "putIfAbsent(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";
  public final static String      REMOVE_IF_VALUE_EQUAL_SIGNATURE      = "remove(Ljava/lang/Object;Ljava/lang/Object;)Z";
  public final static String      REPLACE_SIGNATURE                    = "replace(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";
  public final static String      REPLACE_IF_VALUE_EQUAL_SIGNATURE     = "replace(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Z";
  public final static String      SEGMENT_FOR_SIGNATURE                = "segmentFor(I)Ljava/util/concurrent/ConcurrentHashMap$Segment;";
  public final static String      CONTAINS_VALUE_SIGNATURE             = "containsValue(Ljava/lang/Object;)Z";
  public final static String      SIZE_SIGNATURE                       = "size()I";
  public final static String      IS_EMPTY_SIGNATURE                   = "isEmpty()Z";
  public final static String      SIGNAL_SIGNATURE                     = "signal()V";
  public final static String      SIGNAL_ALL_SIGNATURE                 = "signalAll()V";
  public final static String      TRANSFORM_VALUES_SIGNATURE           = "transformValues(Lgnu/trove/TObjectFunction;)V";
  public final static String      URL_SET_SIGNATURE                    = "set(Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V";

  private final TObjectIntHashMap mappings                             = new TObjectIntHashMap();

  public SerializationUtil() {
    mappings.put(SET_ELEMENT_SIGNATURE, SET_ELEMENT);
    mappings.put(SET_SIZE_SIGNATURE, SET_SIZE);
    mappings.put(TRIM_TO_SIZE_SIGNATURE, TRIM_TO_SIZE);
    mappings.put(REMOVE_ALL_ELEMENTS_SIGNATURE, CLEAR);
    mappings.put(REMOVE_ELEMENT_AT_SIGNATURE, REMOVE_AT);
    mappings.put(REMOVE_ELEMENT_SIGNATURE, REMOVE);
    mappings.put(ADD_ELEMENT_SIGNATURE, ADD);
    mappings.put(INSERT_ELEMENT_AT_SIGNATURE, INSERT_AT);
    mappings.put(ADD_AT_SIGNATURE, ADD_AT);
    mappings.put(ADD_ALL_AT_SIGNATURE, ADD_AT);
    mappings.put(ADD_SIGNATURE, ADD);
    mappings.put(ADD_ALL_SIGNATURE, ADD);
    mappings.put(ADD_LAST_SIGNATURE, ADD_LAST);
    mappings.put(ADD_FIRST_SIGNATURE, ADD_FIRST);
    mappings.put(CLEAR_SIGNATURE, CLEAR);
    mappings.put(PUT_SIGNATURE, PUT);
    mappings.put(GET_SIGNATURE, GET);
    mappings.put(SET_SIGNATURE, SET);
    mappings.put(REMOVE_AT_SIGNATURE, REMOVE_AT);
    mappings.put(REMOVE_SIGNATURE, REMOVE);
    mappings.put(REMOVE_IF_VALUE_EQUAL_SIGNATURE, REMOVE_IF_VALUE_EQUAL);
    mappings.put(REMOVE_KEY_SIGNATURE, REMOVE);
    mappings.put(TROVE_REMOVE_AT_SIGNATURE, REMOVE);
    mappings.put(REMOVE_ENTRY_FOR_KEY_SIGNATURE, REMOVE);
    mappings.put(REMOVE_FIRST_SIGNATURE, REMOVE_FIRST);
    mappings.put(REMOVE_LAST_SIGNATURE, REMOVE_LAST);
    mappings.put(REMOVE_RANGE_SIGNATURE, REMOVE_RANGE);
    mappings.put(SET_TIME_SIGNATURE, SET_TIME);
    mappings.put(SET_NANOS_SIGNATURE, SET_NANOS);
    mappings.put(REMOVE_ALL_SIGNATURE, REMOVE_ALL);
    mappings.put(QUEUE_PUT_SIGNATURE, PUT);
    mappings.put(TAKE_SIGNATURE, TAKE);
    mappings.put(REMOVE_FIRST_N_SIGNATURE, REMOVE_FIRST_N);
    mappings.put(PUT_IF_ABSENT_SIGNATURE, PUT_IF_ABSENT);
    mappings.put(REPLACE_SIGNATURE, REPLACE);
    mappings.put(REPLACE_IF_VALUE_EQUAL_SIGNATURE, REPLACE_IF_VALUE_EQUAL);
    mappings.put(URL_SET_SIGNATURE, URL_SET);
    mappings.put(CLEAR_LOCAL_CACHE_SIGNATURE, CLEAR_LOCAL_CACHE);
    mappings.put(EVICTION_COMPLETED_SIGNATURE, EVICTION_COMPLETED);
    mappings.put(SET_TARGET_MAX_TOTAL_COUNT_SIGNATURE, SET_TARGET_MAX_TOTAL_COUNT);
    mappings.put(SET_MAX_TTI_SIGNATURE, SET_MAX_TTI);
    mappings.put(SET_MAX_TTL_SIGNATURE, SET_MAX_TTL);
  }

  public String[] getSignatures() {
    String[] rv = new String[this.mappings.size()];
    int index = 0;
    for (TObjectIntIterator i = mappings.iterator(); i.hasNext(); index++) {
      i.advance();
      rv[index] = (String) i.key();
    }
    return rv;
  }

  public int methodToID(String name) {
    int i = mappings.get(name);
    if (i == 0) throw new AssertionError("Illegal method name:" + name);
    return i;
  }

  private static final Pattern PARENT_FIELD_PATTERN = Pattern.compile("^this\\$\\d+$");

  public boolean isParent(String fieldName) {
    return PARENT_FIELD_PATTERN.matcher(fieldName).matches();
  }
}
