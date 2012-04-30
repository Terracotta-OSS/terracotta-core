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
import com.terracottatech.search.AbstractNVPair;
import com.terracottatech.search.AbstractNVPair.IntNVPair;
import com.terracottatech.search.AbstractNVPair.StringNVPair;
import com.terracottatech.search.AbstractNVPair.ValueIdNVPair;
import com.terracottatech.search.NVPair;
import com.terracottatech.search.ValueID;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

public class ServerMapEvictionMetaDataReader implements MetaDataReader {

  private static final NVPairSerializer NVPAIR_SERIALIZER = new NVPairSerializer();

  private final String                  cacheName;
  private final Map                     candidates;
  private final ObjectID                oid;

  public ServerMapEvictionMetaDataReader(ObjectID oid, String cacheName, Map candidates) {
    this.cacheName = cacheName;
    this.candidates = candidates;
    this.oid = oid;
  }

  public Iterator<MetaDataDescriptorInternal> iterator() {
    return new RemoveIterator();
  }

  private class RemoveIterator implements Iterator<MetaDataDescriptorInternal>, MetaDataDescriptorInternal {

    private boolean nextCalled = false;

    public boolean hasNext() {
      return !nextCalled;
    }

    public MetaDataDescriptorInternal next() {
      if (nextCalled) { throw new NoSuchElementException(); }
      nextCalled = true;
      return this;
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }

    public void add(String name, boolean value) {
      throw new UnsupportedOperationException();
    }

    public void add(String name, byte value) {
      throw new UnsupportedOperationException();
    }

    public void add(String name, char value) {
      throw new UnsupportedOperationException();
    }

    public void add(String name, double value) {
      throw new UnsupportedOperationException();
    }

    public void add(String name, float value) {
      throw new UnsupportedOperationException();
    }

    public void add(String name, int value) {
      throw new UnsupportedOperationException();
    }

    public void add(String name, long value) {
      throw new UnsupportedOperationException();
    }

    public void add(String name, short value) {
      throw new UnsupportedOperationException();
    }

    public void add(String name, Date value) {
      throw new UnsupportedOperationException();
    }

    public void add(String name, java.sql.Date value) {
      throw new UnsupportedOperationException();
    }

    public void add(String name, Enum value) {
      throw new UnsupportedOperationException();
    }

    public void add(String name, String value) {
      throw new UnsupportedOperationException();
    }

    public void add(String name, byte[] value) {
      throw new UnsupportedOperationException();
    }

    public void add(String name, Object value) {
      throw new UnsupportedOperationException();
    }

    public void add(String name, ObjectID value) {
      throw new UnsupportedOperationException();
    }

    public void addNull(String name) {
      throw new UnsupportedOperationException();
    }

    public void set(String name, Object newValue) {
      throw new UnsupportedOperationException();
    }

    public String getCategory() {
      return "SEARCH";
    }

    public void serializeTo(TCByteBufferOutput out, ObjectStringSerializer serializer) {
      serializer.writeString(out, getCategory());
      out.writeLong(oid.toLong());
      out.writeInt(numberOfNvPairs());

      NVPAIR_SERIALIZER.serialize(new StringNVPair("CACHENAME@", cacheName), out, serializer);
      NVPAIR_SERIALIZER.serialize(new StringNVPair("COMMAND@", "REMOVE_IF_VALUE_EQUAL"), out, serializer);
      NVPAIR_SERIALIZER.serialize(new IntNVPair("", (numberOfNvPairs() - 3) / 2), out, serializer);

      for (Object o : candidates.entrySet()) {
        Entry e = (Entry) o;

        // XXX: assumes key/value types of UTF8ByteDataHolder/ObjectID!
        String key = ((UTF8ByteDataHolder) e.getKey()).asString();
        ObjectID value = (ObjectID) e.getValue();

        NVPAIR_SERIALIZER.serialize(new StringNVPair("", key), out, serializer);
        NVPAIR_SERIALIZER.serialize(new ValueIdNVPair("", new ValueID(value.toLong())), out, serializer);
      }
    }

    public Iterator<NVPair> getMetaDatas() {
      return new RemoveMetaDataIterator(numberOfNvPairs());
    }

    public int numberOfNvPairs() {
      // 2 (cache name and command)
      // 1 (number of removes)
      // 2 for each remove (key, value)
      return 2 + 1 + (candidates.size() * 2);
    }

    public ObjectID getObjectId() {
      return oid;
    }

    public void setObjectID(ObjectID id) {
      throw new AssertionError();
    }

    private class RemoveMetaDataIterator implements Iterator<NVPair> {

      private int                   count = 0;
      private final int             numberOfNvPairs;
      private final Iterator<Entry> toRemove;
      private Entry                 next;

      public RemoveMetaDataIterator(int numberOfNvPairs) {
        this.toRemove = candidates.entrySet().iterator();
        this.numberOfNvPairs = numberOfNvPairs;
      }

      public boolean hasNext() {
        return count < numberOfNvPairs;
      }

      public NVPair next() {
        count++;
        if (count > numberOfNvPairs) { throw new NoSuchElementException(); }

        if (count <= 3) {
          switch (count) {
            case 1:
              return new AbstractNVPair.StringNVPair("CACHENAME@", cacheName);
            case 2:
              next = toRemove.next();
              return new AbstractNVPair.StringNVPair("COMMAND@", "REMOVE_IF_VALUE_EQUAL");
            case 3:
              return new AbstractNVPair.IntNVPair("", (numberOfNvPairs - 3) / 2);
            default:
              throw new AssertionError(count);
          }
        } else if ((count % 2) == 0) {
          // XXX: assumes String key!
          return AbstractNVPair.createNVPair("", ((UTF8ByteDataHolder) next.getKey()).asString());
        } else {
          // XXX: assumes ObjectID value!
          ObjectID valueOid = (ObjectID) next.getValue();
          NVPair nv = new AbstractNVPair.ValueIdNVPair("", new ValueID(valueOid.toLong()));
          if (toRemove.hasNext()) {
            next = toRemove.next();
          }
          return nv;
        }
      }

      public void remove() {
        throw new UnsupportedOperationException();
      }
    }

  }

}
