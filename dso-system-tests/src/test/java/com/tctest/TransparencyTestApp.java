/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.bytecode.TransparentAccess;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 * @author steve
 */
public class TransparencyTestApp extends AbstractTransparentApp {

  private Map           myRoot;
  private Object        out;
  private static Object err;

  public TransparencyTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    System.err.println("APPID is " + appId);
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    config.getOrCreateSpec("com.sun.jmx.mbeanserver.RepositorySupport");

    String testAppName = TransparencyTestApp.class.getName();
    config.addIncludePattern(testAppName);
    config.addIncludePattern(testAppName + "$*");

    TransparencyClassSpec appSpec = config.getOrCreateSpec("com.tctest.TransparencyTestApp");
    appSpec.addRoot("myRoot", "rootBabyRoot");
    appSpec.addRoot("vector", "vector");
    appSpec.addRoot("out", "out");

    TransparencyClassSpec spec = config.getOrCreateSpec("com.tctest.TransparencyTestApp$TestObj");
    spec.addTransient("transientObject");
    spec.addTransient("transientPrimitive");
    config.addWriteAutolock("* com.tctest.TransparencyTestApp.*(..)");
    config.addWriteAutolock("* com.tctest.AbstractTransparencyApp.*(..)");

    config.getOrCreateSpec(FunnyCstr.class.getName());
    config.getOrCreateSpec(FunnyBase.class.getName());
  }

  public void run() {
    // Test case for LKC-774 (accessing static fields of the same name as roots)
    out = new Object();
    System.out.println("out is: " + out);
    System.err.println("err is: " + err);

    testStaticNonRoot();

    myRoot = new HashMap();
    synchronized (myRoot) {
      test1();
      Vector v = new Vector();
      v.add("Hello Steve");
      testFunnyInner();
      testFunnyCstr();
    }

    testUnresolveValue();
  }

  private void testUnresolveValue() {
    TestObj to = new TestObj(new TestObj(null));
    synchronized (myRoot) {
      myRoot.clear();
      myRoot.put("key", to);
    }

    String fieldName;
    try {
      fieldName = to.getClass().getName() + "." + to.getClass().getDeclaredField("obj").getName();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    TestObj ref = to.getObj();
    Assert.assertNotNull(ref);
    Map values = new HashMap();
    ((TransparentAccess) to).__tc_getallfields(values);
    Assert.assertTrue(values.containsKey(fieldName));
    Assert.assertEquals(ref, values.get(fieldName));

    ((Manageable) to).__tc_managed().unresolveReference(fieldName);
    ((TransparentAccess) to).__tc_getallfields(values);
    Assert.assertTrue(values.containsKey(fieldName));
    Assert.assertEquals(null, values.get(fieldName));
  }

  private void testStaticNonRoot() {
    try {
      Class c = TestObj.class;
      Field f = c.getDeclaredField("staticFinalNonRoot");

      Assert.assertTrue(ManagerUtil.isPhysicallyInstrumented(c));

      Assert.assertFalse(ManagerUtil.isRoot(f));

      int access = f.getModifiers();
      Assert.assertTrue(Modifier.isFinal(access));
      Assert.assertTrue(Modifier.isStatic(access));
      Assert.assertTrue(Modifier.isPublic(access));

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  FunnyCstr funnyCstr;

  private void testFunnyCstr() {
    funnyCstr = new FunnyCstr(new Object[] { this }, new Object(), true);
  }

  interface Foo {
    public Object foo();
  }

  Foo inner;

  private void testFunnyInner() {
    class Inner implements Foo {

      class Nested implements Foo {
        public Object foo() {
          return this;
        }
      }

      public Object foo() {
        return new Nested();
      }
    }

    inner = new Inner();
    inner = (Foo) inner.foo();
  }

  public void test1() {
    System.out.println("Entering test1");
    if (!myRoot.containsKey("value")) {
      myRoot.put("value", "1");
      Map fullMap = new HashMap();
      fullMap.put("1", new TestObj(new TestObj(null)));
      fullMap.put(new TestObj(new TestObj(null)), new TestObj(new TestObj(null)));
      fullMap.put("3", "4");
      fullMap.put("5", Collections.EMPTY_LIST);
      myRoot.put("testMapWithstuff", fullMap);
      System.out.println("Entering test2 put");
    }

    if (myRoot.get("value").equals("1")) {
      System.out.println("1");
      System.out.println("Creating test objects and putting them in the map");
      TestObj testObjectEmpty = new TestObj(new TestObj(null));
      TestObj testObjectFull = initialize(new TestObj(new TestObj(null)));
      myRoot.put("testObjectEmpty", testObjectEmpty);
      System.out.println("BEGIN ADD TO ROOT:");
      myRoot.put("testObjectFull", testObjectFull);
      System.out.println("END ADD TO ROOT:");
      myRoot.put("InitializeEmpty", null);
      myRoot.put("value", "2");
      return;
    }

    if (myRoot.get("value").equals("2")) {
      System.out.println("2");
      System.out.println("Checking to make sure the stuff we put in is there and that nothing extra is there");
      Assert.eval((TestObj) myRoot.get("testObject") == null);
      Assert.eval((TestObj) myRoot.get("testObjectEmpty") != null);
      Assert.eval((TestObj) myRoot.get("testObjectFull") != null);

      System.out.println("Checking to make sure full is full");
      TestObj full = (TestObj) myRoot.get("testObjectFull");

      verifyFull(full);

      System.out.println("putting stuff in the empty one");
      TestObj testObjectEmpty = (TestObj) myRoot.get("testObjectEmpty");
      initialize(testObjectEmpty);
      myRoot.put("VerifyEmptyFull", null);
      myRoot.remove("InitializeEmpty");
      myRoot.put("value", "3");
      return;
    }

    if (myRoot.get("value").equals("3")) {
      System.out.println("3");
      TestObj testObjectEmpty = (TestObj) myRoot.get("testObjectEmpty");
      verifyFull(testObjectEmpty);
      initialize2(testObjectEmpty);
      myRoot.remove("VerifyEmptyFull");
      myRoot.put("VerifyInitialize2", null);
      System.out.println("Verified Empty Now Full");
      myRoot.put("value", "4");
      return;
    }
    if (myRoot.get("value").equals("4")) {
      System.out.println("4");
      TestObj testObjectEmpty = (TestObj) myRoot.get("testObjectEmpty");
      verifyFull2(testObjectEmpty);
      System.out.println("Verified Empty Now Full");
      myRoot.put("value", "5");
    }
    if (myRoot.get("value").equals("5")) {
      System.out.println("5");
      TestObj testObjectEmpty = (TestObj) myRoot.get("testObjectEmpty");

      // reference equality should work on field values of type java.lang.Boolean (even though they are literals)
      Assert.assertTrue(testObjectEmpty.booleanFALSE == Boolean.FALSE);
      Assert.assertTrue(testObjectEmpty.booleanTRUE == Boolean.TRUE);

      myRoot.put("value", "6");
    }

  }

  private TestObj initialize(TestObj obj) {

    class NamedSubclass implements Runnable {
      public void run() {
        System.out.println("Hello world");
      }
    }

    obj.namedSubclass = new NamedSubclass();
    obj.setSyncMap(Collections.synchronizedMap(new HashMap()));
    obj.setSyncSet(Collections.synchronizedSet(new HashSet()));
    obj.setSyncList(Collections.synchronizedList(new ArrayList()));
    obj.setSyncCollection(Collections.synchronizedCollection(new ArrayList()));

    System.setProperty("steve", "prop1");
    obj.setClassObject(Object.class);
    obj.setSqlDate(new java.sql.Date(10));
    obj.setTransientObject(new HashMap());
    obj.setTransientPrimitive(101L);
    obj.setObjectArray(new Object[5]);
    obj.setObjectArray(1, new File("hello"));
    obj.setObjectArray(2, Character.valueOf('b'));

    Date[] dates = new Date[2];
    obj.setFile(new File("yellow"));
    obj.setDates(dates);
    obj.setDate(0, new Date());

    obj.setCharacters(new Character[2]);
    obj.setCharacter(0, Character.valueOf('c'));

    obj.setDate(new Date());
    obj.setLiteralObject(Long.valueOf(0));
    obj.setBooleanObject(Boolean.valueOf(true));
    obj.setBooleanValue(true);
    obj.setByteObject(Byte.valueOf((byte) 1));
    obj.setByteValue((byte) 2);
    obj.setCharValue('a');
    obj.setCharObject(Character.valueOf('b'));
    obj.setDoubleObject(Double.valueOf(1.1));
    obj.setDoubleValue(1.2);
    obj.floatObject = new Float(2.1);
    obj.setFloatValue((float) 2.2);
    // obj.setIntegerObject(Integer.valueOf(3));
    obj.integerObject = Integer.valueOf(3);
    obj.setIntegerValue(4);
    obj.longObject = Long.valueOf(5);
    obj.setLongValue(6);
    obj.setShortObject(Short.valueOf((short) 7));
    obj.setShortValue((short) 8);
    obj.setStringValue("Steve");
    obj.setObjects(new TestObj[1]);
    TestObj to = new TestObj(null);
    to.setStringValue("cool");
    obj.setObject(0, to);
    obj.setLongs(new long[4]);
    obj.setLong(2, 4);
    obj.setInts(new int[6]);
    obj.setInt(3, 6);
    obj.ints2 = new int[6];
    System.arraycopy(obj.ints, 0, obj.ints2, 0, 6);
    obj.setDoubles(new double[8]);
    obj.setDouble(4, 8);
    obj.setShorts(new short[10]);
    obj.setShort(5, (short) 10);
    obj.setBooleans(new boolean[12]);
    obj.setBoolean(6, true);
    obj.setBytes(new byte[14]);
    obj.setByte(7, (byte) 14);
    obj.setFloats(new float[16]);
    obj.setFloat(8, 16);
    obj.setChars(new char[18]);
    obj.setChar(9, 'A');
    obj.setTwoDobjects(new TestObj[6][4]);
    to = new TestObj(null);
    to.setStringValue("baby");
    obj.setTwoDobject(4, 2, to);
    obj.bigIntegerObject = new BigInteger("100");
    obj.bigDecimalObject = new BigDecimal(100.0);
    return obj;
  }

  private TestObj initialize2(TestObj obj) {
    System.setProperty("steve", "prop2");

    obj.getSyncMap().put("Hello", "Steve");
    obj.getSyncList().add("Hello");
    obj.getSyncSet().add("Hello");
    obj.getSyncCollection().add("Hello");

    obj.setClassObject(Integer.class);
    obj.setSqlDate(new java.sql.Date(11));
    obj.setTransientObject(new Date());
    obj.setTransientPrimitive(1011L);

    obj.setObjectArray(2, new File("hello"));
    obj.setObjectArray(1, Character.valueOf('b'));

    obj.setFile(new File("yellow"));
    obj.setDate(1, new Date());
    obj.setCharacter(1, Character.valueOf('d'));
    obj.setDate(new Date());
    obj.setLiteralObject("Steve");
    obj.setBooleanObject(Boolean.valueOf(false));
    obj.setBooleanValue(false);
    obj.setByteObject(Byte.valueOf((byte) 2));
    obj.setByteValue((byte) 3);
    obj.setCharValue('e');
    obj.setCharObject(Character.valueOf('f'));
    // obj.setDoubleObject(Double.valueOf((double) 5.1));
    obj.doubleObject = Double.valueOf(5.1);
    obj.setDoubleValue(6.2);
    obj.setFloatObject(new Float(7.1));
    obj.setFloatValue((float) 8.2);
    obj.setIntegerObject(Integer.valueOf(9));
    obj.setIntegerValue(10);
    obj.setLongObject(Long.valueOf(11));
    obj.setLongValue(6);
    obj.setShortObject(Short.valueOf((short) 12));
    obj.setShortValue((short) 13);
    obj.setStringValue("Steve2");
    TestObj to = obj.getObject(0);
    to.setStringValue("cool2");
    obj.setObject(0, to);
    obj.setLong(2, 3);
    obj.setInt(3, 5);
    obj.setDouble(4, 7);
    obj.setShort(5, (short) 9);
    obj.setBoolean(6, false);
    obj.setByte(7, (byte) 13);
    obj.setFloat(8, 15);
    obj.setChar(9, 'B');
    to = obj.getTwoDobject(4, 2);
    to.setStringValue("baby2");
    final Object outer = new Object();
    final float oneInt = 100;
    final char oneChar = 'c';
    new Thread(new Runnable() {
      public void run() {
        System.out.println(outer + " " + oneInt + " one:" + oneChar);
      }
    }).start();
    obj.setTwoDobject(5, 1, new TestObj(null));
    obj.setBigIntegerObject(new BigInteger("200"));
    obj.setBigDecimalObject(new BigDecimal(200.0));
    return obj;
  }

  private void verifyFull2(TestObj obj) {

    Assert.eval(obj.getSyncMap().containsKey("Hello"));
    Assert.eval(obj.getSyncMap().get("Hello").equals("Steve"));

    Assert.eval(obj.getSyncSet().contains("Hello"));
    Assert.eval(obj.getSyncList().contains("Hello"));
    Assert.eval(obj.getSyncCollection().contains("Hello"));

    Assert.eval(obj.getSqlDate().getTime() == 11);
    Assert.eval(Integer.class.equals(obj.getClassObject()));
    Assert.eval(obj.getTransientObject() == null);
    Assert.eval(obj.getTransientPrimitive() == 0);
    Assert.eval(obj.getObjectArray(2).equals(new File("hello")));
    Assert.eval(obj.getObjectArray(1).equals(Character.valueOf('b')));

    Assert.eval(obj.getFile().equals(new File("yellow")));
    Assert.eval(System.getProperty("steve").equals("prop2"));
    Assert.eval(obj.getCharacter(0).equals(Character.valueOf('c')));
    Assert.eval(obj.getCharacter(1).equals(Character.valueOf('d')));

    Assert.eval(obj.getDate() != null);
    Assert.eval(obj.getDate(0) != null);
    Assert.eval(obj.getDate(1) != null);
    Assert.eval(obj.getLiteralObject().equals("Steve"));
    Assert.eval(obj.getBooleanObject().equals(Boolean.valueOf(false)));
    Assert.eval(obj.getBooleanValue() == false);
    Assert.eval(obj.getByteObject().equals(Byte.valueOf((byte) 2)));
    Assert.eval(obj.getByteValue() == (byte) 3);
    Assert.eval(obj.getCharValue() == 'e');
    Assert.eval(obj.getCharObject().equals(Character.valueOf('f')));
    Assert.eval(obj.getDoubleObject().equals(Double.valueOf(5.1)));
    Assert.eval(obj.getDoubleValue() == 6.2);
    Assert.eval(obj.getFloatObject().equals(new Float(7.1)));
    Assert.eval(obj.getFloatValue() == (float) 8.2);
    Assert.eval(obj.getIntegerObject().equals(Integer.valueOf(9)));
    Assert.eval(obj.getIntegerValue() == 10);
    Assert.eval(obj.getLongObject().equals(Long.valueOf(11)));
    Assert.eval(obj.getLongValue() == 6);
    Assert.eval(obj.getShortObject().equals(Short.valueOf((short) 12)));
    Assert.eval(obj.getShortValue() == (short) 13);
    Assert.eval(obj.getStringValue().equals("Steve2"));
    Assert.eval(obj.getObjects().length == 1);
    Assert.eval((obj.getObject(0)).getStringValue().equals("cool2"));
    Assert.eval(obj.getBigIntegerObject().equals(new BigInteger("200")));
    Assert.eval(obj.getBigDecimalObject().equals(new BigDecimal(200.0)));

    Assert.eval(obj.getInts().length == 6);
    System.out.println("Got value:" + obj.getInt(3) + " array:" + obj.getInts());
    Assert.eval(obj.getInt(3) == 5);

    Assert.eval(obj.getLongs().length == 4);
    Assert.eval(obj.getLong(2) == 3);

    Assert.eval(obj.getDoubles().length == 8);
    Assert.eval(obj.getDouble(4) == 7);
    Assert.eval(obj.getShorts().length == 10);
    Assert.eval(obj.getShort(5) == (short) 9);
    Assert.eval(obj.getBooleans().length == 12);
    Assert.eval(!obj.getBoolean(6));
    Assert.eval(obj.getBytes().length == 14);
    Assert.eval(obj.getByte(7) == (byte) 13);
    Assert.eval(obj.getFloats().length == 16);
    Assert.eval(obj.getFloat(8) == 15);
    Assert.eval(obj.getChars().length == 18);
    Assert.eval(obj.getChar(9) == 'B');
    Assert.eval(obj.getTwoDobjects().length == 6);
    System.out.println("About to check 2d");
    Assert.eval(obj.getTwoDobjects()[0].length == 4);
    Assert.eval(obj.getTwoDobject(4, 2).getStringValue().equals("baby2"));
    System.out.println("Checked 2d");
  }

  private void verifyFull(TestObj obj) {

    Assert.eval(obj.getSyncMap() != null);
    Assert.eval(obj.getSyncSet() != null);
    Assert.eval(obj.getSyncCollection() != null);
    Assert.eval(obj.getSyncList() != null);

    Assert.eval(obj.getTransientObject() == null);
    Assert.eval(Object.class.equals(obj.getClassObject()));
    Assert.eval(obj.getSqlDate().getTime() == 10);
    System.out.println("Got:" + obj.getTransientPrimitive());
    Assert.eval(obj.getTransientPrimitive() == 0);
    Assert.eval(obj.getObjectArray(1).equals(new File("hello")));
    Assert.eval(obj.getObjectArray(2).equals(Character.valueOf('b')));
    Assert.eval(obj.getFile().equals(new File("yellow")));
    Assert.eval(System.getProperty("steve").equals("prop1"));
    Assert.eval(obj.getCharacter(0).equals(Character.valueOf('c')));
    Assert.eval(obj.getCharacter(1) == null);
    Assert.eval(obj.getDate() != null);
    Assert.eval(obj.getDate(0) != null);
    Assert.eval(obj.getDate(1) == null);
    Assert.eval(obj.getLiteralObject().equals(Long.valueOf(0)));
    Assert.eval(obj.getBooleanObject().equals(Boolean.valueOf(true)));
    Assert.eval(obj.getBooleanValue() == true);
    Assert.eval(obj.getByteObject().equals(Byte.valueOf((byte) 1)));
    Assert.eval(obj.getByteValue() == (byte) 2);
    Assert.eval(obj.getCharValue() == 'a');
    Assert.eval(obj.getCharObject().equals(Character.valueOf('b')));
    Assert.eval(obj.getDoubleObject().equals(Double.valueOf(1.1)));
    Assert.eval(obj.getDoubleValue() == 1.2);
    Assert.eval(obj.floatObject.equals(new Float(2.1)));
    Assert.eval(obj.getFloatValue() == (float) 2.2);
    Assert.eval(obj.getIntegerObject().equals(Integer.valueOf(3)));
    Assert.eval(obj.integerValue == 4);
    Assert.eval(obj.getLongObject().equals(Long.valueOf(5)));
    Assert.eval(obj.getLongValue() == 6);
    Assert.eval(obj.getShortObject().equals(Short.valueOf((short) 7)));
    Assert.eval(obj.getShortValue() == (short) 8);
    Assert.eval(obj.getStringValue().equals("Steve"));
    Assert.eval(obj.getObjects().length == 1);
    Assert.eval((obj.getObject(0)).getStringValue().equals("cool"));
    Assert.eval(obj.bigIntegerObject.equals(new BigInteger("100")));
    Assert.eval(obj.bigDecimalObject.equals(new BigDecimal(100.0)));

    Assert.eval(obj.getInts().length == 6);
    System.out.println("Got value:" + obj.getInt(3) + " array:" + obj.getInts());
    Assert.eval(obj.getInt(3) == 6);
    Assert.eval(obj.ints2[3] == 6);
    Assert.eval(obj.getLongs().length == 4);
    Assert.eval(obj.getLong(2) == 4);

    Assert.eval(obj.getDoubles().length == 8);
    Assert.eval(obj.getDouble(4) == 8);
    Assert.eval(obj.getShorts().length == 10);
    Assert.eval(obj.getShort(5) == (short) 10);
    Assert.eval(obj.getBooleans().length == 12);
    Assert.eval(obj.getBoolean(6));
    Assert.eval(obj.getBytes().length == 14);
    Assert.eval(obj.getByte(7) == (byte) 14);
    Assert.eval(obj.getFloats().length == 16);
    Assert.eval(obj.getFloat(8) == 16);
    Assert.eval(obj.getChars().length == 18);
    Assert.eval(obj.getChar(9) == 'A');
    Assert.eval(obj.getTwoDobjects().length == 6);
    System.out.println("About to check 2d");
    Assert.eval(obj.getTwoDobjects()[0].length == 4);
    Assert.eval(obj.getTwoDobject(4, 2).getStringValue().equals("baby"));
    System.out.println("Checked 2d");
  }

  private static class FunnyBase {
    FunnyBase(Object object, Object arg2) {
      //
    }
  }

  private static class FunnyCstr extends FunnyBase {
    // The only thing interesting about this class is that makes
    // a static method invocation in it's cstr(). Our class adapter
    // tripped up on this construct at one point
    public FunnyCstr(Object[] arg1, Object arg2, boolean b) {
      super(FunnyCstr.foo(arg1[0], b), arg2);
    }

    private static Object foo(Object object, boolean b) {
      return object;
    }
  }

  public static class TestObj {

    public static final Object staticFinalNonRoot = new Object();

    private final Runnable     anonymousSubclass  = new Runnable() {
                                                    private Object o;

                                                    public void run() {
                                                      System.out.println("syncMap" + syncMap);
                                                      System.out.println("o: " + o);
                                                    }
                                                  };

    public Object              namedSubclass;

    public Runnable getAnonymousSubclass() {
      // This method here to silence warnings
      return this.anonymousSubclass;
    }

    public Object getNamedSubclass() {
      // This method here to silence warnings
      return this.namedSubclass;
    }

    private Map           syncMap;
    private Collection    syncCollection;
    private List          syncList;
    private Set           syncSet;

    private Object        transientObject;
    private long          transientPrimitive;

    private Class         classObject;
    private java.sql.Date sqlDate;
    private final TestObj obj;
    private File          file;
    private Date          dateObject;
    private String        stringValue;
    private Integer       integerObject;
    private int           integerValue;
    private Boolean       booleanObject;
    private boolean       booleanValue;
    private Long          longObject;
    private long          longValue;
    private Short         shortObject;
    private short         shortValue;
    private Double        doubleObject;
    private double        doubleValue;
    protected Float       floatObject;
    private float         floatValue;
    private Character     charObject;
    private char          charValue;
    private Byte          byteObject;
    private byte          byteValue;
    private TestObj[]     objects;
    private TestObj[][]   twoDobjects;
    private long[]        longs;
    private int[]         ints;
    private int[]         ints2;
    private double[]      doubles;
    private short[]       shorts;
    private boolean[]     booleans;
    private byte[]        bytes;
    private float[]       floats;
    private char[]        chars;
    private Date[]        dates;
    private Character[]   charObjects;
    private Object        literalObject;
    private Object[]      objectArray;
    private BigInteger    bigIntegerObject;
    private BigDecimal    bigDecimalObject;

    private final Boolean booleanTRUE  = Boolean.TRUE;
    private final Boolean booleanFALSE = Boolean.FALSE;

    public TestObj(TestObj obj) {
      this.obj = obj;
    }

    public void setSyncMap(Map map) {
      this.syncMap = map;
    }

    public void setSyncSet(Set set) {
      this.syncSet = set;
    }

    public void setSyncList(List list) {
      this.syncList = list;
    }

    public void setSyncCollection(Collection collection) {
      this.syncCollection = collection;
    }

    public Map getSyncMap() {
      return this.syncMap;
    }

    public Set getSyncSet() {
      return this.syncSet;
    }

    public Collection getSyncCollection() {
      return this.syncCollection;
    }

    public List getSyncList() {
      return this.syncList;
    }

    public void setLiteralObject(Object o) {
      literalObject = o;
    }

    public Object getLiteralObject() {
      return literalObject;
    }

    /**
     * @return
     */
    public boolean getBooleanValue() {
      return booleanValue;
    }

    /**
     * @return Returns the booleanObject.
     */
    public Boolean getBooleanObject() {
      return booleanObject;
    }

    public java.sql.Date getSqlDate() {
      return sqlDate;
    }

    public void setSqlDate(java.sql.Date sqlDate) {
      this.sqlDate = sqlDate;
    }

    /**
     * @param booleanObject The booleanObject to set.
     */
    public void setBooleanObject(Boolean booleanObject) {
      this.booleanObject = booleanObject;
    }

    /**
     * @return Returns the booleanValue.
     */
    public boolean isBooleanValue() {
      return booleanValue;
    }

    /**
     * @param booleanValue The booleanValue to set.
     */
    public void setBooleanValue(boolean booleanValue) {
      this.booleanValue = booleanValue;
    }

    /**
     * @return Returns the byteObject.
     */
    public Byte getByteObject() {
      return byteObject;
    }

    /**
     * @param byteObject The byteObject to set.
     */
    public void setByteObject(Byte byteObject) {
      this.byteObject = byteObject;
    }

    /**
     * @return Returns the byteValue.
     */
    public byte getByteValue() {
      return byteValue;
    }

    public void setDate(int index, Date value) {
      dates[index] = value;
    }

    /**
     * @param byteValue The byteValue to set.
     */
    public void setByteValue(byte byteValue) {
      this.byteValue = byteValue;
    }

    /**
     * @return Returns the charObject.
     */
    public Character getCharObject() {
      return charObject;
    }

    /**
     * @param charObject The charObject to set.
     */
    public void setCharObject(Character charObject) {
      this.charObject = charObject;
    }

    /**
     * @return Returns the charValue.
     */
    public char getCharValue() {
      return charValue;
    }

    /**
     * @param charValue The charValue to set.
     */
    public void setCharValue(char charValue) {
      this.charValue = charValue;
    }

    public void setCharacters(Character[] charObjects) {
      this.charObjects = charObjects;
    }

    public void setCharacter(int index, Character value) {
      this.charObjects[index] = value;
    }

    public Character getCharacter(int index) {
      return charObjects[index];
    }

    /**
     * @return Returns the doubleObject.
     */
    public Double getDoubleObject() {
      return doubleObject;
    }

    /**
     * @param doubleObject The doubleObject to set.
     */
    public void setDoubleObject(Double doubleObject) {
      this.doubleObject = doubleObject;
    }

    /**
     * @return Returns the doubleValue.
     */
    public double getDoubleValue() {
      return doubleValue;
    }

    /**
     * @param doubleValue The doubleValue to set.
     */
    public void setDoubleValue(double doubleValue) {
      this.doubleValue = doubleValue;
    }

    /**
     * @return Returns the floatObject.
     */
    public Float getFloatObject() {
      return floatObject;
    }

    /**
     * @param floatObject The floatObject to set.
     */
    public void setFloatObject(Float floatObject) {
      this.floatObject = floatObject;
    }

    /**
     * @return Returns the floatValue.
     */
    public float getFloatValue() {
      return floatValue;
    }

    /**
     * @param floatValue The floatValue to set.
     */
    public void setFloatValue(float floatValue) {
      this.floatValue = floatValue;
    }

    /**
     * @return Returns the integerObject.
     */
    public Integer getIntegerObject() {
      return integerObject;
    }

    /**
     * @param integerObject The integerObject to set.
     */
    public void setIntegerObject(Integer integerObject) {
      this.integerObject = integerObject;
    }

    /**
     * @return Returns the integerValue.
     */
    public int getIntegerValue() {
      return integerValue;
    }

    /**
     * @param integerValue The integerValue to set.
     */
    public void setIntegerValue(int integerValue) {
      this.integerValue = integerValue;
    }

    /**
     * @return Returns the longObject.
     */
    public Long getLongObject() {
      return longObject;
    }

    /**
     * @param longObject The longObject to set.
     */
    public void setLongObject(Long longObject) {
      this.longObject = longObject;
    }

    /**
     * @return Returns the longValue.
     */
    public long getLongValue() {
      return longValue;
    }

    /**
     * @param longValue The longValue to set.
     */
    public void setLongValue(long longValue) {
      this.longValue = longValue;
    }

    /**
     * @return Returns the obj.
     */
    public TestObj getObj() {
      return obj;
    }

    /**
     * @return Returns the shortObject.
     */
    public Short getShortObject() {
      return shortObject;
    }

    /**
     * @param shortObject The shortObject to set.
     */
    public void setShortObject(Short shortObject) {
      this.shortObject = shortObject;
    }

    /**
     * @return Returns the shortValue.
     */
    public short getShortValue() {
      return shortValue;
    }

    /**
     * @param shortValue The shortValue to set.
     */
    public void setShortValue(short shortValue) {
      this.shortValue = shortValue;
    }

    /**
     * @return Returns the stringValue.
     */
    public String getStringValue() {
      return stringValue;
    }

    /**
     * @param stringValue The stringValue to set.
     */
    public void setStringValue(String stringValue) {
      this.stringValue = stringValue;
    }

    public TestObj[] getObjects() {
      return objects;
    }

    public void setObjects(TestObj[] objects) {
      this.objects = objects;
    }

    public void setObject(int index, TestObj value) {
      objects[index] = value;
    }

    public TestObj getObject(int index) {
      return objects[index];
    }

    public long getLong(int index) {
      return longs[index];
    }

    public void setLong(int index, long value) {
      longs[index] = value;
    }

    public int getInt(int index) {
      return ints[index];
    }

    public void setInt(int index, int value) {
      ints[index] = value;
    }

    public void setDates(Date[] dates) {
      this.dates = dates;
    }

    public Date getDate(int index) {
      return dates[index];
    }

    public double getDouble(int index) {
      return doubles[index];
    }

    public void setDouble(int index, double value) {
      doubles[index] = value;
    }

    public short getShort(int index) {
      return shorts[index];
    }

    public void setShort(int index, short value) {
      shorts[index] = value;
    }

    public boolean getBoolean(int index) {
      return booleans[index];
    }

    public void setBoolean(int index, boolean value) {
      booleans[index] = value;
    }

    public byte getByte(int index) {
      return bytes[index];
    }

    public void setByte(int index, byte value) {
      bytes[index] = value;
    }

    public void setDate(Date date) {
      this.dateObject = date;
    }

    public Date getDate() {
      return dateObject;
    }

    public float getFloat(int index) {
      return floats[index];
    }

    public void setFloat(int index, float value) {
      floats[index] = value;
    }

    public char getChar(int index) {
      return chars[index];
    }

    public void setChar(int index, char value) {
      chars[index] = value;
    }

    public boolean[] getBooleans() {
      return booleans;
    }

    public void setBooleans(boolean[] booleans) {
      this.booleans = booleans;
    }

    public byte[] getBytes() {
      return bytes;
    }

    public void setBytes(byte[] bytes) {
      this.bytes = bytes;
    }

    public char[] getChars() {
      return chars;
    }

    public void setChars(char[] chars) {
      this.chars = chars;
    }

    public double[] getDoubles() {
      return doubles;
    }

    public void setDoubles(double[] doubles) {
      this.doubles = doubles;
    }

    public float[] getFloats() {
      return floats;
    }

    public void setFloats(float[] floats) {
      this.floats = floats;
    }

    public int[] getInts() {
      return ints;
    }

    public void setInts(int[] ints) {
      this.ints = ints;
    }

    public long[] getLongs() {
      return longs;
    }

    public void setLongs(long[] longs) {
      this.longs = longs;
    }

    public short[] getShorts() {
      return shorts;
    }

    public void setShorts(short[] shorts) {
      this.shorts = shorts;
    }

    public TestObj[][] getTwoDobjects() {
      return twoDobjects;
    }

    public void setTwoDobjects(TestObj[][] twoDobjects) {
      this.twoDobjects = twoDobjects;
    }

    public void setTwoDobject(int i1, int i2, TestObj value) {
      this.twoDobjects[i1][i2] = value;
    }

    public TestObj getTwoDobject(int i1, int i2) {
      return twoDobjects[i1][i2];
    }

    public File getFile() {
      return this.file;
    }

    public void setFile(File file) {
      this.file = file;
    }

    public Object getObjectArray(int index) {
      return objectArray[index];
    }

    public Object[] getObjectArray() {
      return this.objectArray;
    }

    public void setObjectArray(int index, Object value) {
      objectArray[index] = value;
    }

    public void setObjectArray(Object[] objectArray) {
      this.objectArray = objectArray;
    }

    public Object getTransientObject() {
      return this.transientObject;
    }

    public void setTransientObject(Object transientObject) {
      this.transientObject = transientObject;
    }

    public long getTransientPrimitive() {
      return this.transientPrimitive;
    }

    public void setTransientPrimitive(long transientPrimitive) {
      this.transientPrimitive = transientPrimitive;
    }

    public Class getClassObject() {
      return classObject;
    }

    public void setClassObject(Class classObject) {
      this.classObject = classObject;
    }

    public BigDecimal getBigDecimalObject() {
      return bigDecimalObject;
    }

    public void setBigDecimalObject(BigDecimal bigDecimalObject) {
      this.bigDecimalObject = bigDecimalObject;
    }

    public BigInteger getBigIntegerObject() {
      return bigIntegerObject;
    }

    public void setBigIntegerObject(BigInteger bigIntegerObject) {
      this.bigIntegerObject = bigIntegerObject;
    }

  }

  public synchronized void testAbstractLock() throws Exception {
    System.out.println("never called");
  }
}