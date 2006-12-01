/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.logging;

import com.tc.logging.TCLogger;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Generic comma separated options parser
 */
public class Options {
  public static final String ALL     = "ALL";
  public static final String NONE    = "NONE";

  private final Map          options = new HashMap();

  public Options(String input, String[] keys, TCLogger logger, Map defaults) {
    if (keys == null) { throw new NullPointerException("keys is null"); }
    if (logger == null) { throw new NullPointerException("logger is null"); }
    checkKeys(keys);

    if (defaults == null) {
      defaults = Collections.EMPTY_MAP;
    }

    if (input != null) {
      input = input.replaceAll("\\s", "");
      if (input.length() > 0) {
        Set valid = new HashSet(Arrays.asList(keys));
        String[] configValues = input.split(",");

        for (int i = 0; i < configValues.length; i++) {
          String value = configValues[i];
          if (value.length() > 0) {
            boolean negate = value.startsWith("-");
            if (negate) {
              value = value.substring(1);
            }

            if (value.length() == 0) {
              continue;
            }

            if (valid.contains(value)) {
              options.put(value, negate ? Boolean.FALSE : Boolean.TRUE);
            } else if (ALL.equals(value)) {
              enableAll(keys);
            } else if (NONE.equals(value)) {
              disableAll(keys);
            } else {
              logger.warn("[" + value + "] is not a valid option, ignoring it");
            }
          }
        }
      }
    }

    defaultMissingOptions(keys, defaults);
  }

  private void disableAll(String[] keys) {
    for (int i = 0; i < keys.length; i++) {
      options.put(keys[i], Boolean.FALSE);
    }
  }

  private void checkKeys(String[] keys) {
    for (int i = 0; i < keys.length; i++) {
      String key = keys[i];
      if (ALL.equals(key) || NONE.equals(key)) { throw new IllegalArgumentException("Illegal key " + key
                                                                                    + " at position " + i); }
    }
  }

  public boolean getOption(String opt) {
    return ((Boolean) options.get(opt)).booleanValue();
  }

  private void enableAll(String[] keys) {
    for (int i = 0; i < keys.length; i++) {
      options.put(keys[i], Boolean.TRUE);
    }
  }

  private void defaultMissingOptions(String[] keys, Map defaults) {
    for (int i = 0; i < keys.length; i++) {
      String key = keys[i];
      if (defaults.containsKey(key) && !options.containsKey(key)) {
        Object value = defaults.get(key);
        if (value instanceof Boolean) {
          options.put(key, value);
        } else {
          throw new IllegalArgumentException("Invalid default value in map for " + key + ": " + value);
        }
      }

      // finally default option to false if no default present, and not explicitly set
      if (!options.containsKey(key)) {
        options.put(key, Boolean.FALSE);
      }
    }
  }

}
