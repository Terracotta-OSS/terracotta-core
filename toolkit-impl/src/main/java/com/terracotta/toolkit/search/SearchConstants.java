/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.search;

public interface SearchConstants {

  public interface Meta {
    String KEY     = "KEY@";
    String COMMAND = "COMMAND@";
    String ATTR    = "ATTR@";
    String VALUE   = "VALUE@";
  }

  public interface Commands {
    String PUT                   = "PUT";
    String PUT_IF_ABSENT         = "PUT_IF_ABSENT";
    // String REPLACE = "REPLACE";
    Object REMOVE                = "REMOVE";
    Object CLEAR                 = "CLEAR";
    Object REMOVE_IF_VALUE_EQUAL = "REMOVE_IF_VALUE_EQUAL";
  }
}
