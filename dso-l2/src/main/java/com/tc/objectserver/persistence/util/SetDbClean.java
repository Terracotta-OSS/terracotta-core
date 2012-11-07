/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.util;

import java.io.File;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class SetDbClean extends BaseUtility {
  protected List        oidlogsStatsList = new ArrayList();
  private final OPTIONS option;

  private enum OPTIONS {
    S, C
  }

  public SetDbClean(File dir, String opt) throws Exception {
    this(dir, new OutputStreamWriter(System.out), opt);
  }

  public SetDbClean(File dir, Writer writer, String opt) throws Exception {
    super(writer, new File[] { dir });
    option = validateOption(opt);
  }

  public void setDbClean() throws Exception {
//  TODO:  implement core-storage cleaner
  }

  public static void main(String[] args) {
    if (args == null || args.length < 2) {
      usage();
      System.exit(1);
    }

    try {
      String opt = args[0];
      File dir = new File(args[1]);
      validateDir(dir);
      SetDbClean cleaner = new SetDbClean(dir, opt);
      cleaner.setDbClean();
    } catch (Exception e) {
      e.printStackTrace();
      usage();
      System.exit(2);
    }
  }

  private OPTIONS validateOption(String opt) {
    if (opt == null || (opt.length() != 2) || !opt.startsWith("-")) { throw new RuntimeException("invalid option \""
                                                                                                 + opt + "\""); }
    OPTIONS opts;
    try {
      opts = OPTIONS.valueOf(opt.substring(1).toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new RuntimeException("invalid option \"" + opt + "\"");
    }
    return opts;
  }

  private static void validateDir(File dir) {
    if (!dir.exists() || !dir.isDirectory()) { throw new RuntimeException("Not a valid directory : " + dir); }
  }

  private static void usage() {
    System.out.println("Usage: SetDbClean [-s status] [-c clean] <environment home directory>");
  }

}