/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.reflect;

/**
 * Inspects info.
 *
 * @author <a href="mailto:alex@gnilux.com">Alexandre Vasseur </a>
 */
public class MetaDataInspector {
  /**
   * Checks if a class has a certain field.
   *
   * @param classInfo
   * @param fieldName
   * @return
   */
  public static boolean hasField(final ClassInfo classInfo, final String fieldName) {
    for (int i = 0; i < classInfo.getFields().length; i++) {
      FieldInfo fieldMetaData = classInfo.getFields()[i];
      if (fieldMetaData.getName().equals(fieldName)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks if a class implements a certain interface.
   *
   * @param classInfo
   * @param interfaceName
   * @return
   */
  public static boolean hasInterface(final ClassInfo classInfo, final String interfaceName) {
    for (int i = 0; i < classInfo.getInterfaces().length; i++) {
      ClassInfo interfaceMetaData = classInfo.getInterfaces()[i];
      if (interfaceMetaData.getName().equals(interfaceName)) {
        return true;
      }
    }
    return false;
  }
}
