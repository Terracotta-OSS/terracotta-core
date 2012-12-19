/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl.util;

import org.joda.time.DateTime;

/**
 * @author Ludovic Orban
 */
public class TimeStringParser {

  public static long parseTime(String timeString) throws NumberFormatException {
      if (timeString.endsWith("d")) {
        String days = timeString.substring(0, timeString.length() - 1);

        DateTime dateTime = new DateTime();
        return dateTime.minusDays(Integer.parseInt(days)).getMillis();
      } else if (timeString.endsWith("h")) {
        String hours = timeString.substring(0, timeString.length() - 1);

        DateTime dateTime = new DateTime();
        return dateTime.minusHours(Integer.parseInt(hours)).getMillis();
      } else if (timeString.endsWith("m")) {
        String minutes = timeString.substring(0, timeString.length() - 1);

        DateTime dateTime = new DateTime();
        return dateTime.minusMinutes(Integer.parseInt(minutes)).getMillis();
      } else if (timeString.endsWith("s")) {
        String seconds = timeString.substring(0, timeString.length() - 1);

        DateTime dateTime = new DateTime();
        return dateTime.minusSeconds(Integer.parseInt(seconds)).getMillis();
      } else {
        return new DateTime(Long.parseLong(timeString)).getMillis();
      }
  }

}
