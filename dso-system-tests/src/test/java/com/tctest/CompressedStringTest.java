/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.bytecode.JavaLangStringTC;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Test that calling intern() on a String which is still compressed in L1 will
 * decompress String before interning it
 */
public class CompressedStringTest extends TransparentTestBase {

  private static final int NODE_COUNT    = 2;
  private static final String SERIALIZED_UNINSTRUMENTED = "serialized_string.data";
  
  protected Class getApplicationClass() {
    return CompresedStringTestApp.class;
  }

  protected void setUp() throws Exception {
    super.setUp();
    getTransparentAppConfig().setClientCount(NODE_COUNT);
    initializeTestRunner();
  }

  public static class CompresedStringTestApp extends AbstractErrorCatchingTransparentApp {
    
    private final List root;
    private final CyclicBarrier barrier;
    private static final String EXPECTED = getTestString();
    private int nodeIndex;

    public CompresedStringTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
      root = new ArrayList();
      barrier = new CyclicBarrier(getParticipantCount());
    }
    
    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      final String testClass = CompresedStringTestApp.class.getName();
      final TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
      spec.addRoot("root", "root");
      spec.addRoot("barrier", "barrier");
      String methodExpression = "* " + testClass + ".pop(..)";
      config.addWriteAutolock(methodExpression);
      methodExpression = "* " + testClass + ".put(..)";
      config.addWriteAutolock(methodExpression);
      new CyclicBarrierSpec().visit(visitor, config);
    }
    
    protected void runTest() throws Throwable {
      setup();
      runTests();
    }

    private void runTests() throws Throwable {
      if (!isSetupNode()){
        testStringDecompressedBeforeIntern();
        testSerializeCompressedString();
        testDeserializeUninstrumented();
      }
    }
    
    private void setup() throws Throwable{
      // phase 1 - one node loads large Strings
      nodeIndex = barrier.barrier();
      if (isSetupNode()) {
        put(getTestString(), root);
        put(getTestString(), root);
      }
      
      // phase 2 tests - different node retrieves compressed Strings, runs tests
      barrier.barrier();
    }

    private boolean isSetupNode(){
      return (nodeIndex == 0);
    }
    
    private void testStringDecompressedBeforeIntern() throws Throwable {
      final String actual = pop(root).intern();
      assertTCString(actual, false);
      assertEquals(EXPECTED, actual);
    }

    private void testSerializeCompressedString() throws Throwable {
      final String actual = pop(root);
      assertTCString(actual, true);
      byte[] serialized = serializeToByteArray(actual);
      assertTCString(actual, false);
      assertEquals(EXPECTED, actual);
      byte[] serializedUninstrumented = getSerializedBytes();
      assertTrue(Arrays.equals(serialized, serializedUninstrumented));
    }
    
    private void testDeserializeUninstrumented() throws Exception{
      String uninstrumentedSerialized = (String)deserializeFromFile();
      assertEquals(EXPECTED, uninstrumentedSerialized);
    }
    
    private void assertTCString(Object string, boolean isCompressed){
      assertTrue(string instanceof JavaLangStringTC);
      JavaLangStringTC tcString = (JavaLangStringTC)string;
      assertEquals(isCompressed, tcString.__tc_isCompressed());
    }
    
    private void put(String value, List list){
      synchronized(list){
        list.add(value);
      }
    }
    
    private String pop(List list) {
      synchronized (list) {
        return (String) list.remove(0);
      }
    }    
  }
  
  /* needs to be big enough that it is compressed */
  private static String getTestString(){
    final StringBuffer sb = new StringBuffer("f");
    for (int i=0; i<512; i++){
      sb.append("o");
    }
    sb.append("!");
    return sb.toString();
  }  
  
  /*
   * only needs to be run to regenerate test file, which will manually need to be
   * moved into the tests.system.resources folder
   */
  public static void main(String args[]){
    // serialize uninstrumented test String, store in file for comparison with
    // instrumented serialized String
    String string = getTestString();
    try {
      serializeToFile(string);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  private static void serializeToFile(Object obj) throws IOException {
    serialize(obj, new FileOutputStream(SERIALIZED_UNINSTRUMENTED));
  }
  
  private static byte[] serializeToByteArray(Object obj) throws IOException{
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    serialize(obj, baos);
    return baos.toByteArray();
  }
  
//  private static Object deserializeFromByteArray(byte[] data) throws IOException, ClassNotFoundException{
//    return deserialize(new ByteArrayInputStream(data));
//  }
  
  private static Object deserializeFromFile() throws IOException, ClassNotFoundException {
    return deserialize(CompressedStringTest.class.getClassLoader().getResourceAsStream(SERIALIZED_UNINSTRUMENTED));
  }
  
  private static byte[] getSerializedBytes() throws IOException{
    InputStream fis = CompressedStringTest.class.getClassLoader().getResourceAsStream(SERIALIZED_UNINSTRUMENTED);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    while (fis.available() > 0){
      baos.write(fis.read());
    }
    
    return baos.toByteArray();
  }
  
  private static Object deserialize(InputStream is) throws IOException, ClassNotFoundException{
    ObjectInputStream ois = new ObjectInputStream(is);
    Object rv = ois.readObject();
    ois.close();
    return rv;
  }
  
  private static void serialize(Object obj, OutputStream os) throws IOException{
    ObjectOutputStream oos = new ObjectOutputStream(os);
    oos.writeObject(obj);
    oos.close();
  }
  
}
