/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.terracotta.toolkit.config.cache;

public final class Validator {

  private Validator() {
    // private
  }

  public static Object notNull(String name, Object value) {
    if (value != null) {
      return value;
    } else {
      throw new IllegalArgumentException("Illegal value for '" + name + "' - cannot be null");
    }
  }

  public static boolean bool(String name, Object value) {
    if (value instanceof Boolean) {
      return ((Boolean) value).booleanValue();
    } else {
      throw new IllegalArgumentException("Illegal type for '" + name + "' - should be boolean: "
                                         + value.getClass().getName());
    }
  }

  public static <T> T instance(String name, Object value, Class<T> clazz) {
    if (clazz.isInstance(value)) {
      return clazz.cast(value);
    } else {
      throw new IllegalArgumentException(String.format("Value %s of '%s' is not an instance of expected type %s",
                                                       value, name, clazz.getName()));
    }
  }

  public static int integer(String name, Object value) {
    if (value instanceof Integer) {
      return ((Integer) value).intValue();
    } else {
      throw new IllegalArgumentException("Illegal type for '" + name + "' - should be int: "
                                         + value.getClass().getName());
    }
  }

  public static long integerOrLong(String name, Object value) {
    if (value instanceof Integer || value instanceof Long) {
      return ((Number) value).longValue();
    } else {
      throw new IllegalArgumentException("Illegal type for '" + name + "' - should be int or long: "
                                         + value.getClass().getName());
    }
  }

  public static String string(String name, Object value) {
    if (value instanceof String) {
      return (String) value;
    } else {
      throw new IllegalArgumentException("Illegal type for '" + name + "' - should be String: "
                                         + value.getClass().getName());
    }
  }

  public static int greaterThan(String name, int value, int compareWith) {
    if (value > compareWith) {
      return value;
    } else {
      throw new IllegalArgumentException("Illegal value for '" + name + "' - should be greater than " + compareWith
                                         + ", actual value: " + value);
    }
  }

  public static int greaterThanOrEqualTo(String name, int value, int compareWith) {
    if (value >= compareWith) {
      return value;
    } else {
      throw new IllegalArgumentException("Illegal value for '" + name + "', should be greater than or equal to "
                                         + compareWith + ", actual value: " + value);
    }
  }

  public static String notBlank(String name, String value) {
    if (!value.trim().isEmpty()) {
      return value;
    } else {
      throw new IllegalArgumentException("Illegal value for '" + name + "' - should be non-blank, actual value: '"
                                         + value + "'");
    }
  }

  public static <T extends Enum<T>> T enumInstanceIn(String name, String value, Class<T> enumType) {
    try {
      return Enum.valueOf(enumType, value.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Illegal value for '" + name + "' - should be name in enum '"
                                         + enumType.getName() + "', actual value: '" + value + "'");
    }
  }

  public static long greaterThanOrEqualTo(String name, long value, long compareWith) {
    if (value < compareWith) {
      throw new IllegalArgumentException("Illegal value for '" + name + "' - should be greater than or equal to "
                                         + compareWith + ", actual value: " + value);
    } else {
      return value;
    }
  }
}
