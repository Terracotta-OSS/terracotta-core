/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource.services.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Ludovic Orban
 */
public class AttachmentUtils {

  private final static ThreadLocal<SimpleDateFormat> DATE_FORMATTER = new ThreadLocal<SimpleDateFormat>() {
    @Override
    protected SimpleDateFormat initialValue() {
      return new SimpleDateFormat("yyyyMMddHHmmss");
    }
  };

  public static String createTimestampedZipFilename(String prefix) {
    StringBuilder sb = new StringBuilder(prefix);
    sb.append("-");
    sb.append(DATE_FORMATTER.get().format(new Date()));
    sb.append(".zip");
    return sb.toString();
  }

}
