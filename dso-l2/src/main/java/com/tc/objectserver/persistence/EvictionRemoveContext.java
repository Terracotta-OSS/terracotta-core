package com.tc.objectserver.persistence;

import com.tc.object.ObjectID;
import com.tc.objectserver.api.EvictableEntry;

import java.util.Map;

/**
* @author tim
*/
public interface EvictionRemoveContext {
  Map<Object, EvictableEntry> getSamples();

  ObjectID getObjectID();

  String getCacheName();
}
