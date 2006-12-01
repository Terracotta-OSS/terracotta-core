/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.dynamic;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Substitutes parameters into strings &mdash; for example, '%h' becomes the host name, and so on.
 */
public class ParameterSubstituter {

  private static final TCLogger logger = TCLogging.getLogger(ParameterSubstituter.class);

  public static String substitute(String source) {
    if (source == null) return null;

    StringBuffer out = new StringBuffer();
    char[] sourceChars = source.toCharArray();

    for (int i = 0; i < sourceChars.length; ++i) {
      if (sourceChars[i] == '%') {
        char nextChar = sourceChars[++i];
        String value = "" + nextChar;

        switch (nextChar) {
          case 'd':
            value = getUniqueTempDirectory();
            break;

          case 'h':
            value = getHostname();
            break;

          case 'i':
            value = getIpAddress();
            break;

          case 'H':
            value = System.getProperty("user.home");
            break;

          case 'n':
            value = System.getProperty("user.name");
            break;

          case 'o':
            value = System.getProperty("os.name");
            break;

          case 'a':
            value = System.getProperty("os.arch");
            break;

          case 'v':
            value = System.getProperty("os.version");
            break;

          case 't':
            value = System.getProperty("java.io.tmpdir");
            break;

          case '(':
            StringBuffer propertyName = new StringBuffer();
            boolean foundEnd = false;

            while (++i < sourceChars.length) {
              if (sourceChars[i] == ')') {
                foundEnd = true;
                break;
              }
              propertyName.append(sourceChars[i]);
            }

            if (foundEnd) {
              String prop = propertyName.toString();
              String defaultValue = "";
              int index = prop.lastIndexOf(":");

              if (index > 0) {
                prop = prop.substring(0, index);
                defaultValue = prop.substring(index + 1);
              }

              value = System.getProperty(prop);
              if (value == null) value = defaultValue;
            } else {
              value = "%(" + propertyName.toString();
            }
            break;

          default:
            // nothing here; don't do any substitution
            break;
        }

        out.append(value);
      } else {
        out.append(sourceChars[i]);
      }
    }

    return out.toString();
  }

  private static String uniqueTempDirectory = null;

  private static synchronized String getUniqueTempDirectory() {
    if (uniqueTempDirectory == null) {
      try {
        File theFile = File.createTempFile("terracotta", "data");
        theFile.delete();
        if (!theFile.mkdir()) {
          logger.warn("We were unable to create the directory '" + theFile.getAbsolutePath()
                      + "' as a temporary directory "
                      + "for Terracotta data; we will use the raw temporary directory, '"
                      + System.getProperty("java.io.tmpdir") + "', instead.");
          uniqueTempDirectory = System.getProperty("java.io.tmpdir");
        } else {
          logger.info("Using directory '" + theFile.getAbsolutePath() + "' for data from this Terracotta process.");
          uniqueTempDirectory = theFile.getAbsolutePath();
        }
      } catch (IOException ioe) {
        logger.warn("We were unable to create a new, empty temporary directory for Terracotta data; we will use the "
                    + "raw temporary directory, '" + System.getProperty("java.io.tmpdir") + "', instead.");
        uniqueTempDirectory = System.getProperty("java.io.tmpdir");
      }
    }

    return uniqueTempDirectory;
  }

  private static String getHostname() {
    try {
      return InetAddress.getLocalHost().getCanonicalHostName();
    } catch (UnknownHostException uhe) {
      return "unknown-host";
    }
  }

  private static String getIpAddress() {
    try {
      return InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException uhe) {
      return "unknown-ip-address";
    }
  }

}
