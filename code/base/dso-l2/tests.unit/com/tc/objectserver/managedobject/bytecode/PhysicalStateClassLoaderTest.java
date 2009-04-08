package com.tc.objectserver.managedobject.bytecode;

import com.tc.object.LiteralValues;
import com.tc.test.TCTestCase;

import junit.framework.Assert;

public class PhysicalStateClassLoaderTest extends TCTestCase {

  public void testIfMappingForAllLiteralValuesExists() throws Exception {
    for (LiteralValues type : LiteralValues.values()) {

      Assert.assertNotNull(type.getInputMethodName());
      Assert.assertNotNull(type.getInputMethodDescriptor());

      Assert.assertNotNull(type.getOutputMethodName());
      Assert.assertNotNull(type.getOutputMethodDescriptor());
    }
  }
}
