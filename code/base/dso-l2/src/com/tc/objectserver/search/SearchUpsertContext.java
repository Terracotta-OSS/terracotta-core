/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.async.api.MultiThreadedEventContext;
import com.tc.net.NodeID;
import com.tc.object.metadata.AbstractNVPair;
import com.tc.object.metadata.ValueType;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.metadata.AbstractMetaDataContext;

import java.util.List;
import java.util.Map;

/**
 * Context holding search index creation information.
 *  
 * @author Nabib El-Rahman
 */
public class SearchUpsertContext extends AbstractMetaDataContext implements MultiThreadedEventContext {
  
   private final String name;
  private final Map<String, ValueType> schema;
  private final List<AbstractNVPair> attributes;
  private final String cacheKey;
  
  public SearchUpsertContext(NodeID id, TransactionID transactionID, String name, 
                             String cacheKey, Map<String,ValueType> schema, List<AbstractNVPair> attributes ) {
    super(id, transactionID);
    this.name = name;
    this.cacheKey = cacheKey;
    this.schema = schema;
    this.attributes = attributes;
  }

  /**
   * Name of index.
   * 
   * @return String name
   */
  public String getName() {
    return name;
  }

  /**
   * Key for cache entry.
   */
  public String getCacheKey() {
    return cacheKey;
  }
  
  
  /**
   * Index schema. Maps Attribute -> ValueType
   * 
   * @return Map schema.
   */
  public Map<String, ValueType> getSchema() {
    return schema;
  }
  
  
  /**
   * Return List of attributes-value associated with the key.
   * 
   */
  public List<AbstractNVPair> getAttributes() {
    return attributes;
  }
  
  /**
   * {@inheritDoc}
   */
  public Object getKey() {
    return getSourceID();
  }
  
 
}