/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import com.tc.logging.TCLogger;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.lang.reflect.Array;

/**
 * Generic utility methods.
 */
public class Util {
  private static final Error FATAL_ERROR = new Error(
                                                     "Fatal error -- Please refer to console output and Terracotta log files for more information");

  public static boolean copyFile(File src, File dest) {
      if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
         File destpath = dest;
         if (dest.isDirectory()) {
            destpath = new File(dest, src.getName());
         }
      
   		InputStream in   = null;  
   		OutputStream out = null;
   		try {
      		in  = new FileInputStream(src);
      		out = new FileOutputStream(destpath);
   		
      		byte[] buffer = new byte[4096];
      		int bytesRead;
   		
      		while ((bytesRead = in.read(buffer)) >= 0) {
      			out.write(buffer, 0, bytesRead);
      		}
   	   }
   	   catch (FileNotFoundException fnfex) {
   	      System.err.println(fnfex.getMessage());
   	      return false;
   	   }
   	   catch (IOException ioex) {
   	      System.err.println(ioex.getMessage());
   	      return false;
   	   }
   	   finally {
   	      try {
      		   if (out != null) out.close();
      		   if (in  != null) in.close();
   	      } catch (IOException ioex) {
      	      System.err.println(ioex.getMessage());
      	      ioex.printStackTrace();
            }
   	   }
   	   return true;
      } else {
        return src.renameTo(dest);
      }
  }
  
  
  /**
   * Enumerates the argument, provided that it is an array, in a nice, human-readable format.
   * 
   * @param array
   * @return
   */
  public static String enumerateArray(Object array) {
    StringBuffer buf = new StringBuffer();
    if (array != null) {
      if (array.getClass().isArray()) {
        for (int i = 0, n = Array.getLength(array); i < n; i++) {
          if (i > 0) {
            buf.append(", ");
          }
          buf.append("<<" + Array.get(array, i) + ">>");
        }
      } else {
        buf.append("<").append(array.getClass()).append(" is not an array>");
      }
    } else {
      buf.append("null");
    }
    return buf.toString();
  }

  public static void printLogAndRethrowError(Throwable t, TCLogger logger) {
    printLogAndMaybeRethrowError(t, true, logger);
  }

  public static void printLogAndMaybeRethrowError(final Throwable t, final boolean rethrow, final TCLogger logger) {
    // if (t instanceof ReadOnlyException) { throw (ReadOnlyException) t; }
    // if (t instanceof UnlockedSharedObjectException) { throw (UnlockedSharedObjectException) t; }
    // if (t instanceof TCNonPortableObjectError) { throw (TCNonPortableObjectError) t; }

    try {
      if (t != null) t.printStackTrace();
      logger.error(t);
    } catch (Throwable err) {
      try {
        err.printStackTrace();
      } catch (Throwable err2) {
        // sorry, game over, stop trying
      }
    } finally {
      if (rethrow) {
        // don't wrap existing Runtime and Error
        if (t instanceof RuntimeException) { throw (RuntimeException) t; }
        if (t instanceof Error) { throw (Error) t; }

        // Try to new up a RuntimeException to throw
        final RuntimeException re;
        try {
          re = new RuntimeException("Unexpected Error " + t.getMessage(), t);
        } catch (Throwable err3) {
          try {
            err3.printStackTrace();
          } catch (Throwable err4) {
            // sorry, game over, stop trying
          }
          throw FATAL_ERROR;
        }

        throw re;
      }
    }
  }

}