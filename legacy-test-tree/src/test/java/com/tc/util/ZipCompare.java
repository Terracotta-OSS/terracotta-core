/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipCompare {
  private static final String TAB = "    ";

  public static void main(String args[]) throws IOException {
    boolean hasDiffs = false;

    if (args.length != 2) {
      System.err.println("usage: " + ZipCompare.class.getName() + " <zip file 1> <zip file 2>");
      return;
    }

    File zipfile1 = new File(args[0]);
    File zipfile2 = new File(args[1]);

    check(zipfile1);
    check(zipfile2);

    ZipFile zip1 = new ZipFile(zipfile1);
    ZipFile zip2 = new ZipFile(zipfile2);

    Set files1 = getFiles(zip1);
    Set files2 = getFiles(zip2);

    Set zip1Only = difference(files1, files2);
    Set zip2Only = difference(files2, files1);

    if (zip1Only.size() != zip2Only.size()) {
      hasDiffs = true;
      System.out.println("Number of entries differ:");
      System.out.println(TAB + zip1Only.size() + " " + zipfile1);
      System.out.println(TAB + zip2Only.size() + " " + zipfile2);
      System.out.println();
    }

    listOnlyFiles(zipfile1, zip1Only);
    listOnlyFiles(zipfile2, zip2Only);

    Set diffs = new TreeSet();
    for (Iterator iter = intersect(files1, files2).iterator(); iter.hasNext();) {
      String file = (String) iter.next();
      boolean same = diff(zip1, zip2, file);

      if (!same) {
        diffs.add(file);
      }

      hasDiffs |= !same;
    }

    listDiffs(diffs);

    if (!hasDiffs) {
      System.out.println("No differences");
    }
  }

  private static void listDiffs(Set diffs) {
    if (diffs.size() > 0) {
      System.out.println("Files that differ:");
      for (Iterator iter = diffs.iterator(); iter.hasNext();) {
        System.out.println(TAB + iter.next());
      }
      System.out.println();
    }
  }

  private static boolean diff(ZipFile zip1, ZipFile zip2, String file) throws IOException {
    ZipEntry entry1 = zip1.getEntry(file);
    ZipEntry entry2 = zip2.getEntry(file);

    if (entry1 == null) { throw new AssertionError("Null entry for file " + file + " in zip " + zip1.getName()); }
    if (entry2 == null) { throw new AssertionError("Null entry for file " + file + " in zip " + zip2.getName()); }

    if (entry1.getSize() != entry2.getSize()) { return false; }
    if (entry1.getCrc() != entry2.getCrc()) { return false; }

    byte[] data1 = IOUtils.toByteArray(zip1.getInputStream(entry1));
    byte[] data2 = IOUtils.toByteArray(zip2.getInputStream(entry2));

    return Arrays.equals(data1, data2);
  }

  private static void listOnlyFiles(File zipfile, Set files) {
    if (files.size() > 0) {
      System.out.println("Only in " + zipfile + ":");
      for (Iterator iter = files.iterator(); iter.hasNext();) {
        System.out.println(TAB + iter.next());
      }
      System.out.println("\n");
    }
  }

  private static Set intersect(Set set1, Set set2) {
    Set intersection = new TreeSet(set1);
    intersection.retainAll(set2);
    return Collections.unmodifiableSet(intersection);
  }

  private static Set difference(Set set1, Set set2) {
    Set copy = new TreeSet(set1);
    copy.removeAll(set2);
    return Collections.unmodifiableSet(copy);
  }

  private static Set getFiles(ZipFile zip) {
    Set files = new TreeSet();

    Enumeration e = zip.entries();
    while (e.hasMoreElements()) {
      ZipEntry entry = (ZipEntry) e.nextElement();
      files.add(entry.getName());
    }

    return Collections.unmodifiableSet(files);
  }

  private static void check(File file) {
    if (!file.exists()) {
      exit(file + " does not exist");
    }

    if (!file.isFile()) {
      exit(file + " is not a file");
    }

    if (!file.canRead()) {
      exit(file + " is not readable");
    }

    if (!file.getName().toLowerCase().endsWith(".zip")) {
      System.err.println("WARN --> filename does not have a .zip extension: " + file);
    }
  }

  private static void exit(String msg) {
    System.err.println(msg);
    System.exit(1);
  }

}
