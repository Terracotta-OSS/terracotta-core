/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.exception.ImplementMe;
import com.tc.io.TCByteBufferOutput;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.bytecode.Manageable;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FileSharingTestApp extends AbstractTransparentApp {
  private final static String MOCK_FILE_NAME = "/home/teck/.bashrc";

  private final CyclicBarrier barrier;
  private File                fileRoot;

  public FileSharingTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    try {
      int index = barrier.barrier();

      basicTest(index);
      fileDehydrateTest(index);

      mutateFileTest(index);

    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void mutateFileTest(int index) throws Exception {
    File root = fileRoot; // ensure the root is faulted, so that the mutation will be broadcast

    barrier.barrier();

    if (index == 0) {
      synchronized (root) {
        Field field = File.class.getDeclaredField("path");
        field.setAccessible(true);
        field.set(root, "timmy");
      }
    }

    barrier.barrier();

    synchronized (root) {
      Assert.assertEquals("timmy", root.getPath());
    }
  }

  private void basicTest(int index) throws Exception {
    if (index == 0) {
      fileRoot = new File(MOCK_FILE_NAME);
    }

    barrier.barrier();

    if (index != 0) {
      Assert.assertEquals(new File(MOCK_FILE_NAME).getPath(), fileRoot.getPath());
    }

    barrier.barrier();
  }

  /**
   * This test makes sure that the file separator is contained in the dna via dehydration.
   */
  private void fileDehydrateTest(int index) throws Exception {
    if (index == 0) {
      Manageable managed = (Manageable) fileRoot;
      TCObject tcObject = managed.__tc_managed();
      MockDNAWriter dnaWriter = new MockDNAWriter();

      tcObject.dehydrate(dnaWriter);

      List dna = dnaWriter.getDNA();

      Assert.assertEquals(2, dna.size());

      boolean separatorFound = false;
      for (Iterator i = dna.iterator(); i.hasNext();) {
        PhysicalAction action = (PhysicalAction) i.next();
        Assert.assertTrue(action.isTruePhysical());
        if ("java.io.File._tcFileSeparator".equals(action.getFieldName())) {
          separatorFound = true;
        }
      }
      Assert.assertTrue(separatorFound);
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    TransparencyClassSpec spec = config.getOrCreateSpec(CyclicBarrier.class.getName());
    config.addWriteAutolock("* " + CyclicBarrier.class.getName() + "*.*(..)");

    String testClass = FileSharingTestApp.class.getName();
    spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(testClass + "$*");

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("fileRoot", "fileRoot");
    spec.addRoot("barrier", "barrier");
  }

  private static class MockDNAWriter implements DNAWriter {

    public List dna = new ArrayList();

    public MockDNAWriter() {
      //
    }

    public void addLogicalAction(int method, Object[] parameters) {
      //
    }

    public void addPhysicalAction(String fieldName, Object value) {
      // dna.add(new PhysicalAction(fieldName, value));
      addPhysicalAction(fieldName, value, true);
    }

    public void addArrayElementAction(int index, Object value) {
      //
    }

    public void addEntireArray(Object value) {
      //
    }

    public void addLiteralValue(Object value) {
      //
    }

    public void setParentObjectID(ObjectID id) {
      //
    }

    public void setArrayLength(int length) {
      //
    }

    public void addPhysicalAction(String fieldName, Object value, boolean canBeReference) {
      dna.add(new PhysicalAction(fieldName, value, canBeReference));
    }

    public List getDNA() {
      return dna;
    }

    public void addClassLoaderAction(String classLoaderFieldName, ClassLoader value) {
      //

    }

    public void addSubArrayAction(int start, Object array, int length) {
      //
    }

    public int getActionCount() {
      throw new ImplementMe();
    }

    public void copyTo(TCByteBufferOutput dest) {
      //
    }

    public DNAWriter createAppender() {
      throw new ImplementMe();
    }

    public void finalizeHeader() {
      throw new ImplementMe();

    }

    public boolean isContiguous() {
      throw new ImplementMe();
    }

    public void markSectionEnd() {
      throw new ImplementMe();
    }

  }

}
