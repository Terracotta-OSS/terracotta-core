package com.tc.objectserver.managedobject.bytecode;

import com.tc.object.LiteralValues;
import com.tc.test.TCTestCase;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class PhysicalStateClassLoaderTest extends TCTestCase {

  public void testIfMappingForAllLiteralValuesExists() throws Exception {
    Field[] fields = LiteralValues.class.getDeclaredFields();

    for (int i = 0; i < fields.length; i++) {
      Field field = fields[i];

      int fieldModifier = field.getModifiers();
      if (Modifier.isPublic(fieldModifier) && Modifier.isStatic(fieldModifier) && Modifier.isFinal(fieldModifier)) {
        Object type = field.get(null);
        if(type instanceof Integer) {
          Integer literalType  =(Integer) type;
          PhysicalStateClassLoader.verifyTypePresent(literalType.intValue());
        }
      }
    }
  }
}
