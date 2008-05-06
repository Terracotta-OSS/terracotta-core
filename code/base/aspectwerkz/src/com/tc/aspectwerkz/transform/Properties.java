/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */

package com.tc.aspectwerkz.transform;

import com.tc.aspectwerkz.expression.regexp.Pattern;
import com.tc.aspectwerkz.expression.regexp.TypePattern;
import com.tc.aspectwerkz.expression.SubtypePatternType;

/**
 * TODO document class
 *
 * @author Jonas Bon&#233;r
 */
public class Properties {

  // -- public properties --
  public static final String ASPECT_MODULES;
  public static final boolean USE_GLOBAL_CONTEXT;
  public final static boolean VERBOSE_LOGGING;
  public static final boolean PRINT_DEPLOYMENT_INFO;
  public final static boolean EAGERLY_GENERATE_CLOSURE;
  public static final String DUMP_DIR_CLOSURES;
  public static final String DUMP_DIR_FACTORIES;
  public static final boolean DUMP_JIT_CLOSURES;
  public final static boolean DUMP_JIT_FACTORIES;
  public final static TypePattern DUMP_PATTERN;
  public final static boolean DUMP_BEFORE;
  public final static boolean DUMP_AFTER;
  public static final String DUMP_DIR_BEFORE;
  public static final String DUMP_DIR_AFTER;

  // private options
  private final static String AW_PRINT_DEPLOYMENT_INFO = "aspectwerkz.deployment.info";
  private final static String AW_VERBOSE_LOGGING = "aspectwerkz.details";
  private final static String AW_EAGERLY_GENERATE_CLOSURES = "aspectwerkz.gen.closures";
  private final static String AW_DUMP_PATTERN = "aspectwerkz.dump.pattern";
  private final static String AW_DUMP_CLOSURES = "aspectwerkz.dump.closures";
  private final static String AW_DUMP_FACTORIES = "aspectwerkz.dump.factories";
  private final static String AW_ASPECT_MODULES = "aspectwerkz.aspectmodules";

  static {
    // DSO options
    String global = System.getProperty("tc.dso.global");
    if (global != null) {
      USE_GLOBAL_CONTEXT = Boolean.valueOf(global).booleanValue();
    } else {
      USE_GLOBAL_CONTEXT = true;
    }

    // AW options
    ASPECT_MODULES = System.getProperty(AW_ASPECT_MODULES, "").trim();
    String dumpJP = System.getProperty(AW_DUMP_CLOSURES, null);
    DUMP_JIT_CLOSURES = "yes".equalsIgnoreCase(dumpJP) || "true".equalsIgnoreCase(dumpJP);
    String dumpFactories = System.getProperty(AW_DUMP_FACTORIES, null);
    DUMP_JIT_FACTORIES = "yes".equalsIgnoreCase(dumpFactories) || "true".equalsIgnoreCase(dumpFactories);
    String verbose = System.getProperty(AW_PRINT_DEPLOYMENT_INFO, null);
    PRINT_DEPLOYMENT_INFO = "yes".equalsIgnoreCase(verbose) || "true".equalsIgnoreCase(verbose);
    String details = System.getProperty(AW_VERBOSE_LOGGING, null);
    VERBOSE_LOGGING = "yes".equalsIgnoreCase(details) || "true".equalsIgnoreCase(details);
    String genjp = System.getProperty(AW_EAGERLY_GENERATE_CLOSURES, null);
    EAGERLY_GENERATE_CLOSURE = "yes".equalsIgnoreCase(genjp) || "true".equalsIgnoreCase(genjp);
    String dumpPattern = System.getProperty(AW_DUMP_PATTERN, null);
    if (dumpPattern == null) {
      DUMP_BEFORE = false;
      DUMP_AFTER = false;
      DUMP_PATTERN = null;
    } else {
      dumpPattern = dumpPattern.trim();
      DUMP_AFTER = true;
      DUMP_BEFORE = dumpPattern.indexOf(",before") > 0;
      if (DUMP_BEFORE) {
        DUMP_PATTERN = Pattern.compileTypePattern(dumpPattern.substring(0, dumpPattern.indexOf(',')),
                SubtypePatternType.NOT_HIERARCHICAL);
      } else {
        DUMP_PATTERN = Pattern.compileTypePattern(dumpPattern, SubtypePatternType.NOT_HIERARCHICAL);
      }
    }
    DUMP_DIR_CLOSURES = "_dump/closures";
    DUMP_DIR_BEFORE = "_dump/before";
    DUMP_DIR_AFTER = "_dump/after";
    DUMP_DIR_FACTORIES = "_dump/factories";
  }

}
