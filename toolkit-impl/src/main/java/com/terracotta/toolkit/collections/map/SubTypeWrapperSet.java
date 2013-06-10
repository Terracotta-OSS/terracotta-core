package com.terracotta.toolkit.collections.map;

import org.terracotta.toolkit.ToolkitObjectType;

import com.terracotta.toolkit.util.ToolkitObjectStatus;

import java.util.Collection;
import java.util.Set;


public class SubTypeWrapperSet<E> extends SubTypeWrapperCollection<E> implements Set<E> {

  public SubTypeWrapperSet(Collection<E> collection, ToolkitObjectStatus status, String superTypeName,
                       ToolkitObjectType toolkitObjectType) {
    super(collection, status, superTypeName, toolkitObjectType);
  }

}
