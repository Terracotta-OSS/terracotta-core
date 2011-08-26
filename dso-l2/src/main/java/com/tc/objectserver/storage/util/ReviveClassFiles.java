/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.storage.util;

import org.apache.commons.io.IOUtils;

import com.tc.exception.TCRuntimeException;
import com.tc.io.serializer.TCObjectInputStream;
import com.tc.objectserver.managedobject.bytecode.PhysicalStateClassLoader;
import com.tc.objectserver.persistence.api.ClassPersistor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class ReviveClassFiles extends BaseUtility {

  private final ClassPersistor           persistor;
  private final PhysicalStateClassLoader loader;
  private final File                     destDir;

  public ReviveClassFiles(File sourceDir, File destDir) throws Exception {
    this(sourceDir, destDir, new OutputStreamWriter(System.out));
  }

  public ReviveClassFiles(File sourceDir, File destDir, Writer writer) throws Exception {
    super(writer, new File[] { sourceDir });
    this.destDir = destDir;
    persistor = getPersistor(1).getClassPersistor();
    loader = new PhysicalStateClassLoader();
  }

  public void reviveClassesFiles() {
    Map map = persistor.retrieveAllClasses();
    for (Iterator i = map.entrySet().iterator(); i.hasNext();) {
      Map.Entry e = (Entry) i.next();
      Integer clazzId = (Integer) e.getKey();
      byte clazzBytes[] = (byte[]) e.getValue();
      int cid = clazzId.intValue();
      loadFromBytes(cid, clazzBytes);
    }

  }

  private void loadFromBytes(int classId, byte[] clazzBytes) {
    try {
      ByteArrayInputStream bai = new ByteArrayInputStream(clazzBytes);
      TCObjectInputStream tci = new TCObjectInputStream(bai);

      /* String classIdentifier = */tci.readString();

      String genClassName = tci.readString();

      File file = new File(destDir.getPath() + File.separator + genClassName + ".class");
      if (!file.createNewFile()) {
        log("Unable to create " + file.getAbsolutePath());
      }
      ByteArrayInputStream bais = new ByteArrayInputStream(clazzBytes, clazzBytes.length - bai.available(),
                                                           bai.available());
      FileOutputStream fos = new FileOutputStream(file);
      IOUtils.copy(bais, fos);
      IOUtils.closeQuietly(fos);
      IOUtils.closeQuietly(bais);
      verify(genClassName, classId);

    } catch (Exception ex) {
      throw new TCRuntimeException(ex);
    }
  }

  private void verify(String genClassName, int classId) {
    byte[] loadedClassBytes = loadClassData(genClassName);

    Class clazz = loader.defineClassFromBytes(genClassName, classId, loadedClassBytes, 0, loadedClassBytes.length);

    if (clazz != null && genClassName.equals(clazz.getName())) {
      log("successfully loaded class [ " + genClassName + " ]  from generated class file");
    } else {
      log("could not load class [ " + genClassName + " ] from generated class file");
    }
  }

  private byte[] loadClassData(String name) {
    File classFile = new File(destDir.getPath() + File.separator + name + ".class");
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(classFile);

      byte[] b = IOUtils.toByteArray(fis);
      return b;
    } catch (IOException e) {
      return null;
    } finally {
      if (fis != null) {
        IOUtils.closeQuietly(fis);
      }
    }
  }

  public static void main(String[] args) {
    if (args == null || args.length < 2) {
      usage();
      System.exit(1);
    }

    try {
      File sourceDir = new File(args[0]);
      validateDir(sourceDir);
      File destDir = new File(args[1]);
      validateDir(destDir);

      ReviveClassFiles reviver = new ReviveClassFiles(sourceDir, destDir);
      reviver.reviveClassesFiles();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(2);
    }
  }

  private static void validateDir(File dir) {
    if (!dir.exists() || !dir.isDirectory()) { throw new RuntimeException("Not a valid directory : " + dir); }
  }

  private static void usage() {
    System.out.println("Usage: ReviveClassFiles <environment home directory> <class files destination directory>");
  }

}
