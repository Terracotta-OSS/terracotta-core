/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.metadata;

import java.util.Date;

/**
 * This class holds a collection of metadata
 * 
 * @author Nabib
 */
public interface MetaDataDescriptor {

  void add(String name, boolean value);

  void add(String name, byte value);

  void add(String name, char value);

  void add(String name, double value);

  void add(String name, float value);

  void add(String name, int value);

  void add(String name, long value);

  void add(String name, short value);

  void add(String name, Date value);

  void add(String name, Enum value);

  void add(String name, String value);

  void add(String name, byte[] value);

  void add(String name, Object value);

  String getCategory();

  int size();

}
