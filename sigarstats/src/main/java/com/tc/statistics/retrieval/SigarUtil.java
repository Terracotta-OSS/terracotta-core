/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.retrieval;

import org.hyperic.sigar.Sigar;

import com.tc.logging.CustomerLogging;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.Properties;

public class SigarUtil {

  private static final String  PATH_SEPARATOR        = System.getProperty("path.separator");
  private static final String  FILE_SEPARATOR        = System.getProperty("file.separator");
  private static final String  LIBRARY_PATH_PROPERTY = "org.hyperic.sigar.path";
  private static final boolean SIGAR_ENABLED         = TCPropertiesImpl.getProperties()
                                                         .getBoolean(TCPropertiesConsts.SIGAR_ENABLED);

  static {
    if (SIGAR_ENABLED) {
      sigarInit();
    }
  }

  public static Sigar createSigarIfEnabled() {
    return SIGAR_ENABLED ? new Sigar() : null;
  }

  /**
   * Attempts to ensure that Sigar native libraries are in the native library path. Don't call this in production code.
   * It's for testing purpose, use createSigarIfEnabled() instead
   */
  public static void sigarInit() {
    CustomerLogging.getConsoleLogger().warn("SIGAR is enabled. Its use is not recommended in production environment.");

    // If it's already there, nothing to do (this would be the case with tests run from tcbuild)
    if (isSigarInLibraryPath()) return;

    // Next try the kit installation directory
    /*
     * Since the native libraries are colocated with sigar.jar, this is unnecessary. String installRoot =
     * System.getProperty("tc.install-root"); if (installRoot != null) { File kitLibDirectory = new File(installRoot,
     * "lib"); if (isSigarInDirectory(kitLibDirectory)) { appendToLibraryPath(new File(installRoot)); return; } }
     */

    // Otherwise, add the appropriate Maven repository directories to the native library path
    addMavenRepositoryToLibraryPath();
  }

  private static boolean isSigarInLibraryPath() {
    String[] parts = getLibraryPath().split(PATH_SEPARATOR);
    for (int i = 0; i < parts.length; ++i) {
      File part = new File(parts[i]);
      if (isSigarInDirectory(part)) return true;
    }
    return false;
  }

  private static class SigarFilenameFilter implements FilenameFilter {
    public boolean accept(File dir, String name) {
      return name.toLowerCase().indexOf("sigar") > -1;
    }
  }

  private static final FilenameFilter sigarFilenameFilter = new SigarFilenameFilter();

  private static boolean isSigarInDirectory(File dir) {
    return dir.isDirectory() && dir.list(sigarFilenameFilter).length > 0;
  }

  private static String getLibraryPath() {
    String result = System.getProperty(LIBRARY_PATH_PROPERTY);
    if (result == null) result = "";
    return result;
  }

  private static void addMavenRepositoryToLibraryPath() {
    String baseLibraryName = baseLibraryName();
    if (baseLibraryName != null) {
      File m2Repository = new File(System.getProperty("user.home") + FILE_SEPARATOR + ".m2" + FILE_SEPARATOR
                                   + "repository");
      StringBuffer buf = new StringBuffer();
      buf.append(m2Repository);
      buf.append(FILE_SEPARATOR).append("org");
      buf.append(FILE_SEPARATOR).append("hyperic");
      buf.append(FILE_SEPARATOR).append(baseLibraryName);

      buf.append(FILE_SEPARATOR).append(getSigarVersion());

      appendToLibraryPath(new File(buf.toString()));
    }
  }

  private static String getSigarVersion() {
    URL propsUrl = SigarUtil.class.getResource("/sigarstats.properties");
    if (propsUrl == null) { return ""; }
    Properties props = new Properties();
    try {
      props.load(propsUrl.openStream());
    } catch (Exception e) {
      return "";
    }

    String result = props.getProperty("sigar.version");
    if (result == null) {
      result = System.getProperty("sigar.version");
      if (result /* still */== null) {
        result = "";
      }
    }
    return result;
  }

  private static void appendToLibraryPath(File directory) {
    if (directory != null && directory.isDirectory()) {
      String current = System.getProperty(LIBRARY_PATH_PROPERTY);
      String separator = PATH_SEPARATOR;
      if (current == null) {
        current = "";
        separator = "";
      }

      System.setProperty(LIBRARY_PATH_PROPERTY, current + separator + directory.getAbsolutePath());
    }
  }

  // Adapted from Sigar's ArchName.java
  private static String baseLibraryName() {
    String name = System.getProperty("os.name");
    String arch = System.getProperty("os.arch");
    String version = System.getProperty("os.version");
    String majorVersion = version.substring(0, 1); // 4.x, 5.x, etc.

    StringBuffer buf = new StringBuffer();

    if (arch.endsWith("86")) {
      arch = "x86";
    }

    if (name.equals("Linux")) {
      buf.append(arch).append("-linux");
    } else if (name.indexOf("Windows") > -1) {
      buf.append(arch).append("-winnt");
    } else if (name.equals("SunOS")) {
      if (arch.startsWith("sparcv") && "64".equals(System.getProperty("sun.arch.data.model"))) {
        arch = "sparc64";
      }
      buf.append(arch).append("-solaris");
    } else if (name.equals("HP-UX")) {
      if (arch.startsWith("IA64")) {
        arch = "ia64";
      } else {
        arch = "pa";
      }
      if (version.indexOf("11") > -1) {
        buf.append(arch).append("-hpux-11");
      }
    } else if (name.equals("AIX")) {
      buf.append("ppc-aix-").append(majorVersion);
    } else if (name.equals("Mac OS X")) {
      buf.append("universal");
      if ("64".equals(System.getProperty("sun.arch.data.model"))) {
        buf.append("64");
      }
      buf.append("-macosx");
    } else if (name.equals("FreeBSD")) {
      // none of the 4,5,6 major versions are binary compatible
      buf.append(arch).append("-freebsd-").append(majorVersion);
    } else if (name.equals("OpenBSD")) {
      buf.append(arch).append("-openbsd-").append(majorVersion);
    } else if (name.equals("NetBSD")) {
      buf.append(arch).append("-netbsd-").append(majorVersion);
    } else if (name.equals("OSF1")) {
      buf.append("alpha-osf1-").append(majorVersion);
    } else if (name.equals("NetWare")) {
      buf.append("x86-netware-").append(majorVersion);
    }

    if (buf.length() == 0) {
      return null;
    } else {
      String prefix = "libsigar-";
      if (name.startsWith("Windows")) {
        prefix = "sigar-";
      }
      return prefix + buf.toString();
    }
  }
}
