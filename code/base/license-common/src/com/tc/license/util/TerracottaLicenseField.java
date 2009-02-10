package com.tc.license.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.regex.Pattern;

public class TerracottaLicenseField implements LicenseField {
  protected final String  name;
  protected Object        value;
  protected final String  type;
  protected final String  pattern;
  protected final boolean required;
  protected final Map     range;

  public TerracottaLicenseField(String name, String type, String pattern, boolean required, Map range) {
    this.name = name;
    this.type = type;
    this.pattern = pattern;
    this.required = required;
    this.range = range;
  }

  public void setRawValue(String rawValue) throws LicenseException {
    this.value = convertAndValidate(rawValue);
  }

  public Object getValue() {
    return value;
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public String getPattern() {
    return pattern;
  }

  public boolean isRequired() {
    return required;
  }

  public Map getRange() {
    return range;
  }

  private Object convertAndValidate(String rawValue) throws LicenseException {
    if (LicenseConstants.STRING.equals(type)) {
      return convertToString(rawValue);
    } else if (LicenseConstants.INTEGER.equals(type)) {
      return convertToInteger(rawValue);
    } else if (LicenseConstants.DATE.equals(type)) {
      return convertToDate(rawValue);
    } else {
      throw new LicenseException("Type '" + type + "' isn't recognized");
    }
  }

  private String convertToString(String rawValue) throws LicenseException {
    if (isBlank(rawValue)) {
      if (required) {
        throw new LicenseException("Field '" + name + "' is required");
      } else {
        return null;
      }
    } else {
      if (pattern != null && !Pattern.matches(pattern, rawValue)) {
        //
        throw new LicenseException("Field '" + name + "' doesn't match pattern '" + pattern + "'");
      }
      return rawValue;
    }
  }

  private static boolean isBlank(String s) {
    return s == null || s.trim().length() == 0;
  }

  private Integer convertToInteger(String rawValue) throws LicenseException {
    if (isBlank(rawValue)) {
      if (required) {
        throw new LicenseException("Field '" + name + "' is required");
      } else {
        return null;
      }
    } else {
      Integer intValue;
      try {
        intValue = Integer.valueOf(rawValue);
      } catch (Exception e) {
        throw new LicenseException("Field '" + name + "' requires integer value", e);
      }

      if (range != null) {
        int min = Integer.MIN_VALUE;
        int max = Integer.MAX_VALUE;

        String minS = (String) range.get("min");
        String maxS = (String) range.get("max");

        if (minS != null) min = Integer.valueOf(minS);
        if (maxS != null) max = Integer.valueOf(maxS);

        if (intValue < min || intValue > max) throw new LicenseException("Field '" + name + "' is not within range "
                                                                         + range);
      }
      return intValue;
    }
  }

  private Date convertToDate(String rawValue) throws LicenseException {
    if (isBlank(rawValue)) {
      if (required) {
        throw new LicenseException("Field '" + name + "' is required");
      } else {
        return null;
      }
    } else {
      DateFormat df = new SimpleDateFormat(LicenseConstants.DATE_FORMAT);
      df.setLenient(false);
      try {
        return df.parse(rawValue);
      } catch (ParseException e) {
        throw new LicenseException("Can't parse field '" + name + "' with pattern '"
                                   + LicenseConstants.DATE_FORMAT.toUpperCase() + "'");
      }
    }
  }

  public String toString() {
    return name + " = " + value;
  }
}
