/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.object.ObjectID;

import java.io.IOException;
import java.util.Iterator;

/**
 *
 * @author mscott
 */
class MaskingObjectIDSet extends BitSetObjectIDSet {
    
    private long start;
    private long end;
    private BitSetObjectIDSet left = new BitSetObjectIDSet();
    
    public MaskingObjectIDSet() {
        start = 0;
        end = 0;
    }
    
    MaskingObjectIDSet(ObjectIDSetBase copy) {
        MaskingObjectIDSet m = (MaskingObjectIDSet)copy;
        for (final Iterator<BitSet> i = m.ranges.iterator(); i.hasNext();) {
             this.ranges.add(new BitSet(i.next()));
        }
        this.size = m.size;
        this.start = m.start;
        this.end = m.end;
        this.left = new BitSetObjectIDSet(m.left);
    }
    
    private void split() {
        long half = ((end - start)/3l) + start;
        for(long x=start;x<half;x++) {
            ObjectID add = new ObjectID(x);
            if ( !super.remove(add) ) {
                left.add(add);
            }
            start = x;
        }
        start = half;
    }

    @Override
    public Iterator iterator() {
        return new Iterator<ObjectID>() {
            Iterator<ObjectID> leftIterator = left.iterator();
            long position = start;
            ObjectID current = null;
            ObjectID last = null;
            
            private boolean advance() {
                if ( leftIterator.hasNext() ) {
                    current = leftIterator.next();
                    return true;
                } else {
                    while ( position <  end ) {
                        current = new ObjectID(position++);
                        if ( MaskingObjectIDSet.super.contains(current) ) {
                            return true;
                        }
                    }
                }
                current = null;
                return false;
            }

            @Override
            public boolean hasNext() {
                if ( current != null ) {
                    return true;
                }
                return advance();
            }

            @Override
            public ObjectID next() {
                if ( current == null && !advance() ) {
                    return ObjectID.NULL_ID;
                }
                try {
                    return current;
                } finally {
                    last = current;
                    current = null;
                }
            }

            @Override
            public void remove() {
                MaskingObjectIDSet.this.remove(last);
            }
        };
    }

    @Override
    public boolean contains(ObjectID id) {
        if ( id.getMaskedObjectID() < start ) {
            return left.contains(id);
        }
        if ( id.getMaskedObjectID() >= end ) {
            return false;
        }
        return !super.contains(id);
    }

    @Override
    public boolean remove(ObjectID o) {
        if ( o.getMaskedObjectID() < start ) {
            return left.remove(o);
        }
        if ( o.getMaskedObjectID() >= end ) {
            return false;
        }
        
        try {
            return !super.add(o);
        } finally {
            if ( factor() > .33 ) {
                split();
            }
        }
    }

    @Override
    public Object deserializeFrom(TCByteBufferInput in) throws IOException {
        start = in.readLong();
        end = in.readLong();
        left.deserializeFrom(in);
        super.deserializeFrom(in);
        return this;
    }

    @Override
    public void serializeTo(TCByteBufferOutput out) {
        out.writeLong(start);
        out.writeLong(end);
        left.serializeTo(out);
        super.serializeTo(out);
    }

    @Override
    public boolean add(ObjectID id) {
        if ( start == end ) {
            start = id.getMaskedObjectID();
            end =  id.getMaskedObjectID() + 1;
            return false;
        }
        if ( id.getMaskedObjectID() < start ) {
            return left.add(id);
        }
        if ( id.getMaskedObjectID() >= end ) {
            for (long x=end;x<id.getMaskedObjectID();x++) {
                super.add(new ObjectID(x));
            }
            end = id.getMaskedObjectID() + 1;
            return false;
        } else {
            return !super.remove(id);
        }
    }

    @Override
    public ObjectID first() {
        if ( !left.isEmpty() ) {
            return left.first();
        }
        for (long x=start;x<end;x++) {
            ObjectID probe = new ObjectID(x);
            if ( !super.contains(probe) ) {
                return probe;
            }
        }
        return ObjectID.NULL_ID;
    }

    @Override
    public ObjectID last() {
        for (long x=end-1;x>=start;x--) {
            ObjectID probe = new ObjectID(x);
            if ( !super.contains(probe) ) {
                return probe;
            }
        }
        return left.last();
    }

    @Override
    public int size() {
    return ((((int) (end - start)) - super.size()) + left.size());
    }
    
    private float factor() {
        return 1f * super.size() / (end-start);
    }

    @Override
    public boolean isEmpty() {
        return super.size() == (end-start) && left.isEmpty();
    }

    @Override
    public void clear() {
        left.clear();
        super.clear();
        start = 0;
        end = 0;
    }

    @Override
    public String toString() {
        return "MaskingObjectIDSet{" + "mask=" + super.size() + ", start=" + start + ", end=" + end + ", left=" + left.size() + '}';
    }
}
