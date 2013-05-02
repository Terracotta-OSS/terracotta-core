/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.io.TCByteBufferOutput;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.MetaDataReader;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.dna.impl.UTF8ByteDataHolder;
import com.tc.object.metadata.MetaDataDescriptorInternal;
import com.tc.object.metadata.NVPairSerializer;
import com.tc.objectserver.api.EvictableEntry;
import com.terracottatech.search.AbstractNVPair;
import com.terracottatech.search.AbstractNVPair.EnumNVPair;
import com.terracottatech.search.AbstractNVPair.IntNVPair;
import com.terracottatech.search.AbstractNVPair.StringNVPair;
import com.terracottatech.search.AbstractNVPair.ValueIdNVPair;
import com.terracottatech.search.NVPair;
import com.terracottatech.search.SearchCommand;
import com.terracottatech.search.SearchMetaData;
import com.terracottatech.search.ValueID;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

public class RemoveAllMetaDataReader implements MetaDataReader {

  private static final NVPairSerializer NVPAIR_SERIALIZER = new NVPairSerializer();

  private final String                  cacheName;
  private final Map<Object, EvictableEntry>                     candidates;
  private final ObjectID                oid;

  public RemoveAllMetaDataReader(ObjectID oid, String cacheName, Map<Object, EvictableEntry> candidates) {
    this.cacheName = cacheName;
    this.candidates = candidates;
    this.oid = oid;
  }

  @Override
  public Iterator<MetaDataDescriptorInternal> iterator() {
    return new RemoveIterator();
  }

  private class RemoveIterator implements Iterator<MetaDataDescriptorInternal>, MetaDataDescriptorInternal {

    private boolean nextCalled = false;

    @Override
    public boolean hasNext() {
      return !nextCalled;
    }

    @Override
    public MetaDataDescriptorInternal next() {
      if (nextCalled) { throw new NoSuchElementException(); }
      nextCalled = true;
      return this;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void add(String name, boolean value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void add(String name, byte value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void add(String name, char value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void add(String name, double value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void add(String name, float value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void add(String name, int value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void add(String name, long value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void add(String name, short value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void add(String name, Date value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void add(String name, java.sql.Date value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void add(String name, Enum value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void add(String name, String value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void add(String name, byte[] value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void add(String name, Object value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void add(String name, ObjectID value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void addNull(String name) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void set(String name, Object newValue) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getCategory() {
      return "SEARCH";
    }

    @Override
    public void serializeTo(TCByteBufferOutput out, ObjectStringSerializer serializer) {
      serializer.writeString(out, getCategory());
      out.writeLong(oid.toLong());
      out.writeInt(numberOfNvPairs());

      NVPAIR_SERIALIZER.serialize(new StringNVPair(SearchMetaData.CACHENAME.toString(), cacheName), out, serializer);
      NVPAIR_SERIALIZER
          .serialize(new EnumNVPair(SearchMetaData.COMMAND.toString(), SearchCommand.REMOVE_IF_VALUE_EQUAL), out,
                     serializer);
      NVPAIR_SERIALIZER.serialize(new IntNVPair("", (numberOfNvPairs() - 3) / 2), out, serializer);

      for (Entry<Object, EvictableEntry> e : candidates.entrySet()) {

        String key;
        // XXX: assumes key/value types of UTF8ByteDataHolder/ObjectID!
        if (e.getKey() instanceof UTF8ByteDataHolder) {
          key = ((UTF8ByteDataHolder) e.getKey()).asString();
        } else {
          // assume literals
          key = e.getKey().toString();
        }

        ObjectID value = e.getValue().getObjectID();

        NVPAIR_SERIALIZER.serialize(new StringNVPair("", key), out, serializer);
        NVPAIR_SERIALIZER.serialize(new ValueIdNVPair("", new ValueID(value.toLong())), out, serializer);
      }
    }

    @Override
    public Iterator<NVPair> getMetaDatas() {
      return new RemoveMetaDataIterator(numberOfNvPairs());
    }

    @Override
    public int numberOfNvPairs() {
      // 2 (cache name and command)
      // 1 (number of removes)
      // 2 for each remove (key, value)
      return 2 + 1 + (candidates.size() * 2);
    }

    @Override
    public ObjectID getObjectId() {
      return oid;
    }

    @Override
    public void setObjectID(ObjectID id) {
      throw new AssertionError();
    }

    private class RemoveMetaDataIterator implements Iterator<NVPair> {

      private int                   count = 0;
      private final int             numberOfNvPairs;
      private final Iterator<Entry<Object, EvictableEntry>> toRemove;
      private Entry<Object, EvictableEntry>                 next;

      public RemoveMetaDataIterator(int numberOfNvPairs) {
        this.toRemove = candidates.entrySet().iterator();
        this.numberOfNvPairs = numberOfNvPairs;
      }

      @Override
      public boolean hasNext() {
        return count < numberOfNvPairs;
      }

      @Override
      public NVPair next() {
        count++;
        if (count > numberOfNvPairs) { throw new NoSuchElementException(); }

        if (count <= 3) {
          switch (count) {
            case 1:
              return new AbstractNVPair.StringNVPair(SearchMetaData.CACHENAME.toString(), cacheName);
            case 2:
              next = toRemove.next();
              return new AbstractNVPair.EnumNVPair(SearchMetaData.COMMAND.toString(),
                                                   SearchCommand.REMOVE_IF_VALUE_EQUAL);
            case 3:
              return new AbstractNVPair.IntNVPair("", (numberOfNvPairs - 3) / 2);
            default:
              throw new AssertionError(count);
          }
        } else if ((count % 2) == 0) {
          // XXX: assumes Literal key!
          Object key = next.getKey();
          if (key instanceof UTF8ByteDataHolder) {
            key = ((UTF8ByteDataHolder) key).asString();
          } else {
            // assume literals
            key = key.toString();
          }
          return AbstractNVPair.createNVPair("", key);
        } else {
          // XXX: assumes ObjectID value!
          ObjectID valueOid = next.getValue().getObjectID();
          NVPair nv = new AbstractNVPair.ValueIdNVPair("", new ValueID(valueOid.toLong()));
          if (toRemove.hasNext()) {
            next = toRemove.next();
          }
          return nv;
        }
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    }

  }

}
