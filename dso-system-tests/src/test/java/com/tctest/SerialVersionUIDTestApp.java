/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import org.apache.commons.io.IOUtils;

import com.tc.lcp.LinkedJavaProcess;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.process.StreamCollector;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Util;
import com.tctest.builtin.HashMap;
import com.tctest.runner.AbstractTransparentApp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;

public class SerialVersionUIDTestApp extends AbstractTransparentApp {

  public static final String TEMP_FILE_KEY = "tempFile";

  private final Map          map           = new HashMap();
  private final String       fileName;

  public SerialVersionUIDTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    this.fileName = cfg.getAttribute(TEMP_FILE_KEY);
  }

  public void run() {
    final boolean first;
    synchronized (map) {
      first = map.isEmpty();
      if (first) {
        map.put("with", new WithUID());
        map.put("without", new WithoutUID());
      }
    }

    WithUID with = (WithUID) map.get("with");
    WithoutUID without = (WithoutUID) map.get("without");

    checkUID(with, WithUID.serialVersionUID);
    checkUID(without, WithoutUID.EXPECTED_UID);

    doTheDance(with);
    doTheDance(without);

    verifyAddedField(without);

    if (first) {
      // only do this in one node
      verifyExternal();
    }
  }

  private void verifyAddedField(WithoutUID without) {
    Field field = getSerialUIDField(without);

    if (field == null) { throw new RuntimeException("Could not find the serialVersionUID field: "
                                                    + Arrays.asList(without.getClass().getDeclaredFields())); }

    int access = field.getModifiers();
    if ((!Modifier.isStatic(access)) || (!Modifier.isFinal(access))) {
      // make formatter sane
      throw new RuntimeException("Bad permissions: " + access);
    }

    Class type = field.getType();
    if (!Long.TYPE.equals(type)) { throw new RuntimeException("Bad type: " + type); }

    // a-okay, just return
    return;
  }

  private void verifyExternal() {
    // this verifies that we can share classes (via serialization) with a VM that doesn't use DSO instrumentation

    byte[] dataIn = null;

    try {
      OutputStream out = new FileOutputStream(fileName + ".in", false);
      out.write(serialize(new WithoutUID()));
      out.close();

      LinkedJavaProcess process = new LinkedJavaProcess(ExternalSerialize.class.getName(), Arrays.asList(fileName));
      process.start();

      process.STDIN().close();
      StreamCollector stdout = new StreamCollector(process.STDOUT());
      stdout.start();
      StreamCollector stderr = new StreamCollector(process.STDERR());
      stderr.start();

      int exitCode = process.waitFor();

      stdout.join();
      stderr.join();

      if (exitCode != 0) { throw new RuntimeException("Process exited with code " + exitCode + ", stdout: "
                                                      + stdout.toString() + ", stderr: " + stderr); }

      InputStream in = new FileInputStream(fileName + ".out");
      dataIn = IOUtils.toByteArray(in);
      if (dataIn.length == 0) { throw new RuntimeException("No data read"); }
      in.close();
      deserialize(dataIn);
    } catch (Exception e) {
      throw new RuntimeException(Util.enumerateArray(dataIn), e);
    }
  }

  private static Field getSerialUIDField(Object obj) {
    Field[] fields = obj.getClass().getDeclaredFields();
    for (Field f : fields) {
      if ("serialVersionUID".equals(f.getName())) { return f; }
    }
    return null;
  }

  private void doTheDance(Object obj) {
    // this doesn't really test all that much, but it makes sure that the given object can be serialized and
    // deserialized
    try {
      byte[] data = serialize(obj);
      deserialize(data);
    } catch (Exception e) {
      notifyError(e);
    }
  }

  private static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
    Object rv = ois.readObject();
    ois.close();
    return rv;
  }

  private static byte[] serialize(Object obj) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    oos.writeObject(obj);
    oos.close();
    return baos.toByteArray();
  }

  private void checkUID(Object obj, long expect) {
    long uid = ObjectStreamClass.lookup(obj.getClass()).getSerialVersionUID();
    if (uid != expect) { throw new RuntimeException("Unexpected UID: " + uid + ", expected " + expect); }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = SerialVersionUIDTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    spec.addRoot("map", "map");

    String methodExpression = "* " + testClass + ".*(..)";
    config.addWriteAutolock(methodExpression);

    config.addIncludePattern(testClass + "$*");

    config.getOrCreateSpec(WithoutUID.class.getName());
  }

  private static class WithUID implements Serializable {
    static final long serialVersionUID = 0xDECAFBAD;
  }

  public static class ExternalSerialize {

    public static void main(String args[]) throws Exception {
      try {
        if (args.length != 1) {
          error("invalid number of args " + args.length);
        }

        String file = args[0];

        FileInputStream in = new FileInputStream(file + ".in");
        byte[] dataIn = IOUtils.toByteArray(in);
        in.close();

        Object o = deserialize(dataIn);

        verifyNoSerialUID(o);

        FileOutputStream out = new FileOutputStream(file + ".out", false);
        out.write(serialize(o));
        out.flush();
        out.close();
        System.exit(0);
      } catch (Throwable t) {
        t.printStackTrace();
        error(t.getMessage());
      }

    }

    private static void verifyNoSerialUID(Object o) {
      Field f = getSerialUIDField(o);
      if (f != null) { throw new RuntimeException("Class has a serialVersionUID field: " + f); }
    }

    private static void error(String msg) {
      System.err.println(msg);
      System.err.flush();
      System.exit(1);
      throw new RuntimeException(msg);
    }
  }

}
