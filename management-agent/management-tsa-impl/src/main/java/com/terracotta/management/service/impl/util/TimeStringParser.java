/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl.util;

/**
 * @author Ludovic Orban
 */
public class TimeStringParser {

  public static long parseTime(String timeString) throws NumberFormatException {
      if (timeString.endsWith("d")) {
        String days = timeString.substring(0, timeString.length() - 1);
        return System.currentTimeMillis() - Integer.parseInt(days) * 24 * 60 * 60 * 1000L;
      } else if (timeString.endsWith("h")) {
        String hours = timeString.substring(0, timeString.length() - 1);
        return System.currentTimeMillis() - Integer.parseInt(hours) * 60 * 60 * 1000L;
      } else if (timeString.endsWith("m")) {
        String minutes = timeString.substring(0, timeString.length() - 1);
        return System.currentTimeMillis() - Integer.parseInt(minutes) * 60 * 1000L;
      } else if (timeString.endsWith("s")) {
        String seconds = timeString.substring(0, timeString.length() - 1);
        return System.currentTimeMillis() - Integer.parseInt(seconds) * 1000L;
      } else {
        return Long.parseLong(timeString);
      }
  }

}
