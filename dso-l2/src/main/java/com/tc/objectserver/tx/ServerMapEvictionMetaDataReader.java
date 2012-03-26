/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.io.TCByteBufferOutput;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.MetaDataReader;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.dna.impl.UTF8ByteDataHolder;
import com.tc.object.metadata.AbstractNVPair;
import com.tc.object.metadata.MetaDataDescriptorInternal;
import com.tc.object.metadata.NVPair;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

public class ServerMapEvictionMetaDataReader implements MetaDataReader {

  private final String   cacheName;
  private final Map      candidates;
  private final ObjectID oid;

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
      new AbstractNVPair.StringNVPair("CACHENAME@", cacheName).serializeTo(out, serializer);
      new AbstractNVPair.StringNVPair("COMMAND@", "REMOVE_IF_VALUE_EQUAL").serializeTo(out, serializer);
      new AbstractNVPair.IntNVPair("", (numberOfNvPairs() - 3) / 2).serializeTo(out, serializer);

      for (Object o : candidates.entrySet()) {
        Entry e = (Entry) o;

        // XXX: assumes key/value types of (Literal or UTF8ByteDataHolder)/ObjectID!
        Object key = e.getKey();
        if (key instanceof UTF8ByteDataHolder) {
          key = ((UTF8ByteDataHolder) key).asString();
        }
        AbstractNVPair.createNVPair("", key).serializeTo(out, serializer);
        new AbstractNVPair.ObjectIdNVPair("", (ObjectID) e.getValue()).serializeTo(out, serializer);
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
          // XXX: assumes Literal key!
          Object key = next.getKey();
          if (key instanceof UTF8ByteDataHolder) {
            key = ((UTF8ByteDataHolder) key).asString();
          }
          return AbstractNVPair.createNVPair("", key);
        } else {
          // XXX: assumes ObjectID value!
          NVPair nv = new AbstractNVPair.ObjectIdNVPair("", (ObjectID) next.getValue());
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
