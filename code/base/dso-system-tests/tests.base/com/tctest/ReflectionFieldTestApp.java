/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.bytecode.ClassAdapterBase;
import com.tc.object.bytecode.Manageable;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.Root;
import com.tc.object.util.ReadOnlyException;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReflectionFieldTestApp extends GenericTransparentApp {
  // This field is used by reflection.
  private final DataRoot                  reflectionRoot        = null;

  // used by reflection
  @SuppressWarnings("unused")
  private Integer                         literalRoot;

  // used by reflection
  @SuppressWarnings("unused")
  private int                             primitiveRoot;

  private final DataRoot                  nonShared             = new DataRoot();
  private final NonInstrumentedTestObject nonInstrumentedObject = new NonInstrumentedTestObject();

  private final NonInstrumented           nonInstrumented       = new NonInstrumented();

  public ReflectionFieldTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider, DataRoot.class);
  }

  @Override
  protected Object getTestObject(String testName) {
    DataRoot root = (DataRoot) sharedMap.get("root");
    return root;
  }

  @Override
  protected void setupTestObject(String testName) {
    sharedMap.put("root", new DataRoot(Long.MIN_VALUE));
  }

  void testBasicModifyRoot(DataRoot root, boolean validate) {
    if (validate) {
      Assert.assertEquals(12, root.getLongValue());
    } else {
      synchronized (root) {
        root.setLongValue(12);
      }
    }
  }

  void testModifyPhysicalInstrumentedObjectWithStaticManager(DataRoot root, boolean validate) throws Exception {
    if (validate) {
      Assert.assertEquals(12, root.getLongValue());
    } else {
      synchronized (root) {
        Field longValueField = root.getClass().getDeclaredField("longValue");
        longValueField.setAccessible(true);
        longValueField.setLong(root, 12);
      }
    }
  }

  void testModifyPhysicalInstrumentedObjectWithNonStaticManager(DataRoot root, boolean validate) throws Exception {
    if (validate) {
      Assert.assertEquals(200, root.getColor().getRGB());
    } else {
      synchronized (root) {
        Color color = root.getColor();
        Field colorField = color.getClass().getDeclaredField("value");
        colorField.setAccessible(true);
        Assert.assertEquals(100, colorField.getInt(color));
        colorField.setInt(color, 200);
      }
    }
  }

  void testModifyObjectReference(DataRoot root, boolean validate) throws Exception {
    if (validate) {
      Assert.assertEquals(200, root.getColor().getRGB());
    } else {
      synchronized (root) {
        Field colorField = root.getClass().getDeclaredField("color");
        colorField.setAccessible(true);
        colorField.set(root, new Color(200, true));
      }
    }
  }

  void testModifyBoolean(DataRoot root, boolean validate) throws Exception {
    if (validate) {
      Assert.assertTrue(root.isBooleanValue());
    } else {
      synchronized (root) {
        Field booleanField = root.getClass().getDeclaredField("booleanValue");
        booleanField.setAccessible(true);
        booleanField.setBoolean(root, true);
      }
    }
  }

  void testModifyByte(DataRoot root, boolean validate) throws Exception {
    if (validate) {
      Assert.assertEquals(Byte.MAX_VALUE, root.getByteValue());
    } else {
      synchronized (root) {
        Field byteField = root.getClass().getDeclaredField("byteValue");
        byteField.setAccessible(true);
        byteField.setByte(root, Byte.MAX_VALUE);
      }
    }
  }

  void testModifyCharacter(DataRoot root, boolean validate) throws Exception {
    if (validate) {
      Assert.assertEquals(Character.MAX_VALUE, root.getCharValue());
    } else {
      synchronized (root) {

        Field charField = root.getClass().getDeclaredField("charValue");
        charField.setAccessible(true);
        charField.setChar(root, Character.MAX_VALUE);

      }
    }
  }

  void testModifyDoubleWithFloatValue(DataRoot root, boolean validate) throws Exception {
    if (validate) {
      Assert.assertEquals(Float.MAX_VALUE, root.getDoubleValue());
    } else {
      synchronized (root) {

        Field doubleField = root.getClass().getDeclaredField("doubleValue");
        doubleField.setAccessible(true);
        doubleField.setFloat(root, Float.MAX_VALUE);
      }
    }
  }

  void testModifyDoubleWithIntValue(DataRoot root, boolean validate) throws Exception {
    if (validate) {
      Assert.assertEquals(4, root.getDoubleValue());
    } else {
      synchronized (root) {

        Field doubleField = root.getClass().getDeclaredField("doubleValue");
        doubleField.setAccessible(true);
        doubleField.setInt(root, 4);
      }
    }
  }

  void testModifyDoubleWithLongValue(DataRoot root, boolean validate) throws Exception {
    if (validate) {
      Assert.assertEquals(4L, root.getDoubleValue());
    } else {
      synchronized (root) {

        Field doubleField = root.getClass().getDeclaredField("doubleValue");
        doubleField.setAccessible(true);
        doubleField.setLong(root, 4L);
      }
    }
  }

  void testModifyDouble(DataRoot root, boolean validate) throws Exception {
    if (validate) {
      Assert.assertEquals(Double.MAX_VALUE, root.getDoubleValue());
    } else {
      synchronized (root) {

        Field doubleField = root.getClass().getDeclaredField("doubleValue");
        doubleField.setAccessible(true);
        doubleField.setDouble(root, Double.MAX_VALUE);
      }
    }
  }

  void testModifyFloat(DataRoot root, boolean validate) throws Exception {
    if (validate) {
      Assert.assertEquals(Float.MAX_VALUE, root.getFloatValue());
    } else {
      synchronized (root) {

        Field floatField = root.getClass().getDeclaredField("floatValue");
        floatField.setAccessible(true);
        floatField.setFloat(root, Float.MAX_VALUE);
      }
    }
  }

  void testModifyShort(DataRoot root, boolean validate) throws Exception {
    if (validate) {
      Assert.assertEquals(Short.MAX_VALUE, root.getShortValue());
    } else {
      synchronized (root) {

        Field shortField = root.getClass().getDeclaredField("shortValue");
        shortField.setAccessible(true);
        shortField.setShort(root, Short.MAX_VALUE);
      }
    }
  }

  void testModifyIntWithShortValue(DataRoot root, boolean validate) throws Exception {
    if (validate) {
      Assert.assertEquals(Short.MAX_VALUE, root.getIntValue());
    } else {
      synchronized (root) {

        Field intField = root.getClass().getDeclaredField("intValue");
        intField.setAccessible(true);
        intField.setInt(root, Short.MAX_VALUE);
      }
    }
  }

  void testModifyIntWithByteValue(DataRoot root, boolean validate) throws Exception {
    if (validate) {
      Assert.assertEquals(Byte.MAX_VALUE, root.getIntValue());
    } else {
      synchronized (root) {

        Field intField = root.getClass().getDeclaredField("intValue");
        intField.setAccessible(true);
        intField.setInt(root, Byte.MAX_VALUE);
      }
    }
  }

  void testModifyIntWithCharValue(DataRoot root, boolean validate) throws Exception {
    if (validate) {
      Assert.assertEquals(Character.MAX_VALUE, root.getIntValue());
    } else {
      synchronized (root) {

        Field intField = root.getClass().getDeclaredField("intValue");
        intField.setAccessible(true);
        intField.setInt(root, Character.MAX_VALUE);
      }
    }
  }

  void testModifyInt(DataRoot root, boolean validate) throws Exception {
    if (validate) {
      Assert.assertEquals(Integer.MAX_VALUE, root.getIntValue());
    } else {
      synchronized (root) {

        Field intField = root.getClass().getDeclaredField("intValue");
        intField.setAccessible(true);
        intField.setInt(root, Integer.MAX_VALUE);
      }
    }
  }

  void testModifyLongWithIntValue(DataRoot root, boolean validate) throws Exception {
    if (validate) {
      Assert.assertEquals(Integer.MAX_VALUE, root.getLongValue());
    } else {
      synchronized (root) {

        Field longField = root.getClass().getDeclaredField("longValue");
        longField.setAccessible(true);
        longField.setInt(root, Integer.MAX_VALUE);
      }
    }
  }

  void testModifyLongWithShortValue(DataRoot root, boolean validate) throws Exception {
    if (validate) {
      Assert.assertEquals(Short.MAX_VALUE, root.getLongValue());
    } else {
      synchronized (root) {

        Field longField = root.getClass().getDeclaredField("longValue");
        longField.setAccessible(true);
        longField.setShort(root, Short.MAX_VALUE);
      }
    }
  }

  void testModifyLongWithByteValue(DataRoot root, boolean validate) throws Exception {
    if (validate) {
      Assert.assertEquals(Byte.MAX_VALUE, root.getLongValue());
    } else {
      synchronized (root) {

        Field longField = root.getClass().getDeclaredField("longValue");
        longField.setAccessible(true);
        longField.setByte(root, Byte.MAX_VALUE);
      }
    }
  }

  void testModifyLongWithCharValue(DataRoot root, boolean validate) throws Exception {
    if (validate) {
      Assert.assertEquals(Character.MAX_VALUE, root.getLongValue());
    } else {
      synchronized (root) {

        Field longField = root.getClass().getDeclaredField("longValue");
        longField.setAccessible(true);
        longField.setChar(root, Character.MAX_VALUE);
      }
    }
  }

  void testModifyLongWithFloatValue(DataRoot root, boolean validate) throws Exception {
    if (validate) {
      Assert.assertEquals(Long.MIN_VALUE, root.getLongValue());
    } else {
      synchronized (root) {
        try {
          Field longField = root.getClass().getDeclaredField("longValue");
          longField.setAccessible(true);
          longField.setFloat(root, Float.MAX_VALUE);
          throw new AssertionError("should have thrown an exception");
        } catch (IllegalArgumentException re) {
          // Expected.
        }
      }
    }
  }

  void testModifyLongWithDoubleValue(DataRoot root, boolean validate) throws Exception {
    if (validate) {
      Assert.assertEquals(Long.MIN_VALUE, root.getLongValue());
    } else {
      synchronized (root) {
        try {
          Field longField = root.getClass().getDeclaredField("longValue");
          longField.setAccessible(true);
          longField.setDouble(root, Double.MAX_VALUE);
          throw new AssertionError("should have thrown an exception");
        } catch (IllegalArgumentException re) {
          // Expected.
        }
      }
    }
  }

  void testModifyLong(DataRoot root, boolean validate) throws Exception {
    if (validate) {
      Assert.assertEquals(Long.MAX_VALUE, root.getLongValue());
    } else {
      synchronized (root) {
        Field longField = root.getClass().getDeclaredField("longValue");
        longField.setAccessible(true);
        longField.setLong(root, Long.MAX_VALUE);
      }
    }
  }

  void testModifyNonSharedTCManagedField(DataRoot root, boolean validate) throws Exception {
    Field tcManaged = nonShared.getClass().getDeclaredField(ClassAdapterBase.MANAGED_FIELD_NAME);
    tcManaged.setAccessible(true);
    if (validate) {
      Object tcManagedObject = tcManaged.get(nonShared);
      Assert.assertNull(tcManagedObject);
      Assert.assertNull(((Manageable) nonShared).__tc_managed());
    } else {
      Assert.assertNotNull(((Manageable) root).__tc_managed());
      Assert.assertNull(((Manageable) nonShared).__tc_managed());
      tcManaged.set(nonShared, ((Manageable) root).__tc_managed());
      Assert.assertNull(((Manageable) nonShared).__tc_managed());
    }
  }

  void testModifySharedTCManagedField(DataRoot root, boolean validate) throws Exception {
    Field tcManaged = root.getClass().getDeclaredField(ClassAdapterBase.MANAGED_FIELD_NAME);
    tcManaged.setAccessible(true);
    if (validate) {
      Object tcManagedObject = tcManaged.get(root);
      Assert.assertNull(tcManagedObject);
      Assert.assertNotNull(((Manageable) root).__tc_managed());
    } else {
      Assert.assertNotNull(((Manageable) root).__tc_managed());
      tcManaged.set(root, null);
      Assert.assertNotNull(((Manageable) root).__tc_managed());
    }
  }

  void testModifyAndGetLiteralRoot(DataRoot root, boolean validate) throws Exception {
    Field literalRootField = getClass().getDeclaredField("literalRoot");
    literalRootField.setAccessible(true);

    Field primitiveRootField = getClass().getDeclaredField("primitiveRoot");
    primitiveRootField.setAccessible(true);

    if (validate) {
      Integer localLiteralRoot = (Integer) literalRootField.get(this);
      Assert.assertNotNull(localLiteralRoot);
      Assert.assertEquals(new Integer(100), localLiteralRoot);

      Integer localPrimitiveRoot = (Integer) primitiveRootField.get(this);
      Assert.assertNotNull(localPrimitiveRoot);
      Assert.assertEquals(new Integer(200), localPrimitiveRoot);
    } else {
      Object value = literalRootField.get(this);
      Assert.assertNull(value);

      // don't need DSO lock to set roots
      literalRootField.set(this, new Integer(100));

      value = primitiveRootField.get(this);
      Assert.assertEquals(new Integer(0), value);
      primitiveRootField.set(this, new Integer(200));
    }
  }

  void testModifyAndGetRoot(DataRoot root, boolean validate) throws Exception {
    Field rootField = getClass().getDeclaredField("reflectionRoot");
    rootField.setAccessible(true);

    if (validate) {
      DataRoot dataRoot = (DataRoot) rootField.get(this);
      Assert.assertNotNull(dataRoot);
      Assert.assertEquals(200, dataRoot.getLongValue());
      Assert.assertEquals(200, reflectionRoot.getLongValue());
    } else {
      Object value = rootField.get(this);
      Assert.assertNull(value);

      // don't need DSO lock to set roots
      rootField.set(this, new DataRoot(200));
    }
  }

  void testModifyNonSharedObject(DataRoot root, boolean validate) throws Exception {
    if (!validate) {
      synchronized (nonShared) {

        Field longValueField = nonShared.getClass().getDeclaredField("longValue");
        longValueField.setAccessible(true);
        longValueField.setLong(nonShared, Long.MAX_VALUE);
      }
      Assert.assertEquals(Long.MAX_VALUE, nonShared.getLongValue());
    }
  }

  void testModifyNonInstrumentedObject(DataRoot root, boolean validate) throws Exception {
    if (!validate) {
      synchronized (nonInstrumentedObject) {
        Field longValueField = nonInstrumentedObject.getClass().getDeclaredField("longValue");
        longValueField.setAccessible(true);
        longValueField.setLong(nonInstrumentedObject, Long.MAX_VALUE);
      }
      Assert.assertEquals(Long.MAX_VALUE, nonInstrumentedObject.getLongValue());
    }
  }

  void testModifyLogicalInstrumentedObject(DataRoot root, boolean validate) throws Exception {
    if (validate) {
      Assert.assertEquals(0, root.getList().size());
    } else {
      synchronized (root) {
        List list = root.getList();
        try {
          Field sizeField = list.getClass().getDeclaredField("size");
          sizeField.setAccessible(true);
          sizeField.setInt(list, 10);
          throw new AssertionError("should have thrown an exception");
        } catch (IllegalAccessException re) {
          // Checking the exact text of the exception isn't great, but unless we want to create a unique
          // expception type for this condition, this is how we differentiate between other potential
          // IllegalAccessExceptions being thrown here
          Assert.assertEquals("Field modification through reflection for non-physical shared object of type "
                              + list.getClass().getName() + " is not supported!", re.getMessage());
        }
      }

    }
  }

  void testModifyNonInstrumentedObjectRoot(DataRoot root, boolean validate) throws Exception {
    Field nonInstrumentedRootField = nonInstrumented.getClass().getDeclaredField("nonInstrumentedRoot");
    nonInstrumentedRootField.setAccessible(true);
    if (validate) {
      Assert.assertEquals(root, nonInstrumented.getNonInstrumentedRoot());

      Object nonInstrumentedRoot = nonInstrumentedRootField.get(nonInstrumented);
      Assert.assertEquals(root, nonInstrumentedRoot);
    } else {
      nonInstrumentedRootField.set(nonInstrumented, root);
    }
  }

  void testGetObjectReference(DataRoot root, boolean validate) throws Exception {
    if (validate) {
      Field colorField = root.getClass().getDeclaredField("color");
      colorField.setAccessible(true);
      Object color = colorField.get(root);
      Assert.assertNotNull(color);
      Assert.assertEquals(new Color(100, true), color);
    }
  }

  void testLogicalClasses(DataRoot root, boolean validate) throws Exception {
    Map map = root.getMap();
    Map subMap = root.getSubMap();

    Field sizeField = HashMap.class.getDeclaredField("size");
    Field iField = subMap.getClass().getDeclaredField("i");
    sizeField.setAccessible(true);
    iField.setAccessible(true);

    if (validate) {
      int size = sizeField.getInt(map);
      Assert.assertEquals(1, size);
      size = ((Integer) sizeField.get(subMap)).intValue();

      int i = iField.getInt(subMap);
      Assert.assertEquals(5, i);

    } else {
      int i = iField.getInt(subMap);
      Assert.assertEquals(3, i);

      synchronized (subMap) {
        iField.setInt(subMap, 5);
      }
    }

  }

  /*
   * A static field of a non-root shared object is not shared. This test case is to make sure that we could use the
   * current reflection instrumentation to set a static field.
   */
  void testModifyNonRootStaticField(DataRoot root, boolean validate) throws Exception {
    Field staticLongField = root.getClass().getDeclaredField("staticLong");
    staticLongField.setAccessible(true);
    if (!validate) {
      staticLongField.set(null, new Long(33L));
      Assert.assertEquals(new Long(33L), DataRoot.getStaticLong());

      Object staticLongFieldValue = staticLongField.get(null);
      Assert.assertEquals(new Long(33L), staticLongFieldValue);

      staticLongField.set(null, new Long(50L));
      Assert.assertEquals(new Long(50L), DataRoot.getStaticLong());

      staticLongFieldValue = staticLongField.get(null);
      Assert.assertEquals(new Long(50L), staticLongFieldValue);
    }
  }

  void testModifyRootStaticField(DataRoot root, boolean validate) throws Exception {
    Field nonInstrumentedStaticRootField = nonInstrumented.getClass().getDeclaredField("nonInstrumentedStaticRoot");
    nonInstrumentedStaticRootField.setAccessible(true);
    if (validate) {
      Assert.assertEquals(root, NonInstrumented.getNonInstrumentedStaticRoot());

      Object nonInstrumentedStaticRootValue = nonInstrumentedStaticRootField.get(null);
      Assert.assertEquals(root, nonInstrumentedStaticRootValue);
    } else {
      nonInstrumentedStaticRootField.set(null, root);
    }
  }

  // ReadOnly test.
  void testReadOnlyModifyLong(DataRoot root, boolean validate) throws Exception {
    if (validate) {
      Assert.assertEquals(Long.MIN_VALUE, root.getLongValue());
    } else {
      synchronized (root) {
        try {
          Field longField = root.getClass().getDeclaredField("longValue");
          longField.setAccessible(true);
          longField.setLong(root, Long.MAX_VALUE);
          throw new AssertionError("I should have thrown a ReadOnlyException.");
        } catch (ReadOnlyException t) {
          // expected
        }
      }
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = ReflectionFieldTestApp.class.getName();
    config.getOrCreateSpec(testClass);

    config.getOrCreateSpec(SubMap.class.getName());

    String writeAllowedMethodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(writeAllowedMethodExpression);
    String readOnlyMethodExpression = "* " + testClass + "*.*ReadOnly*(..)";
    config.addReadAutolock(readOnlyMethodExpression);

    config.addRoot(new Root(testClass, "reflectionRoot", "reflectionRoot"), true);
    config.addRoot(new Root(testClass, "literalRoot", "literalRoot"), true);
    config.addRoot(new Root(testClass, "primitiveRoot", "primitiveRoot"), true);
    config.addIncludePattern(DataRoot.class.getName());

    config.addRoot(new Root(NonInstrumented.class.getName(), "nonInstrumentedRoot", "nonInstrumentedRoot"), false);
    config.addRoot(new Root(NonInstrumented.class.getName(), "nonInstrumentedStaticRoot", "nonInstrumentedStaticRoot"),
                   false);
  }

  @SuppressWarnings("unused")
  private static class NonInstrumented extends NonInstrumentedTestObject {
    private static DataRoot nonInstrumentedStaticRoot;

    private DataRoot        nonInstrumentedRoot;

    public NonInstrumented() {
      super();
    }

    public static DataRoot getNonInstrumentedStaticRoot() {
      return nonInstrumentedStaticRoot;
    }

    public static void setNonInstrumentedStaticRoot(DataRoot nonInstrumentedStaticRoot) {
      NonInstrumented.nonInstrumentedStaticRoot = nonInstrumentedStaticRoot;
    }

    public DataRoot getNonInstrumentedRoot() {
      return nonInstrumentedRoot;
    }

    public void setNonInstrumentedRoot(DataRoot nonInstrumentedRoot) {
      this.nonInstrumentedRoot = nonInstrumentedRoot;
    }
  }

  private static class SubMap extends HashMap {
    private final int i = 3;

    public SubMap() {
      put("key", "value");
      if (i == 5) { // silence compiler warning
        throw new RuntimeException();
      }
    }
  }

  @SuppressWarnings("unused")
  private static class DataRoot {
    private static Long staticLong;

    private ArrayList   list         = new ArrayList();
    private Color       color        = new Color(100, true);
    private long        longValue    = Long.MIN_VALUE;
    private int         intValue     = Integer.MIN_VALUE;
    private short       shortValue   = Short.MIN_VALUE;
    private boolean     booleanValue = false;
    private byte        byteValue    = Byte.MIN_VALUE;
    private char        charValue    = Character.MIN_VALUE;
    private double      doubleValue  = Double.MIN_VALUE;
    private float       floatValue   = Float.MIN_VALUE;

    private final Map   map          = new HashMap();
    {
      map.put("key", "value");
    }

    private final Map   subMap       = new SubMap();

    public DataRoot(long longValue) {
      this.longValue = longValue;
    }

    public DataRoot() {
      //
    }

    public Map getSubMap() {
      return subMap;
    }

    public Map getMap() {
      return map;
    }

    public static Long getStaticLong() {
      return staticLong;
    }

    public static void setStaticLong(Long staticLong) {
      DataRoot.staticLong = staticLong;
    }

    protected ArrayList getList() {
      return list;
    }

    protected void setList(ArrayList list) {
      this.list = list;
    }

    protected Color getColor() {
      return color;
    }

    protected void setColor(Color color) {
      this.color = color;
    }

    protected long getLongValue() {
      return longValue;
    }

    protected void setLongValue(long longValue) {
      this.longValue = longValue;
    }

    protected int getIntValue() {
      return intValue;
    }

    protected void setIntValue(int intValue) {
      this.intValue = intValue;
    }

    protected boolean isBooleanValue() {
      return booleanValue;
    }

    protected void setBooleanValue(boolean booleanValue) {
      this.booleanValue = booleanValue;
    }

    protected byte getByteValue() {
      return byteValue;
    }

    protected void setByteValue(byte byteValue) {
      this.byteValue = byteValue;
    }

    protected char getCharValue() {
      return charValue;
    }

    protected void setCharValue(char charValue) {
      this.charValue = charValue;
    }

    protected double getDoubleValue() {
      return doubleValue;
    }

    protected void setDoubleValue(double doubleValue) {
      this.doubleValue = doubleValue;
    }

    protected float getFloatValue() {
      return floatValue;
    }

    protected void setFloatValue(float floatValue) {
      this.floatValue = floatValue;
    }

    protected short getShortValue() {
      return shortValue;
    }

    protected void setShortValue(short shortValue) {
      this.shortValue = shortValue;
    }
  }
}
