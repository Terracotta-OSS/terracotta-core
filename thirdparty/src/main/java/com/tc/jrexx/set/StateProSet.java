/*
* 01/07/2003 - 15:19:32
*
* StateProSet.java -
* Copyright (C) 2003 Buero fuer Softwarearchitektur GbR
* ralf.meyer@karneim.com
* http://jrexx.sf.net
*
* This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public License
* as published by the Free Software Foundation; either version 2
* of the License, or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
*/
package com.tc.jrexx.set;

public class StateProSet {

  public class Iterator {
    final int offset;
    Wrapper_State current = null;

    protected Iterator() {
      this(0);
    }
    protected Iterator(int offset) {
      this.offset = offset;
    }

    public IStatePro next() {
      if (this.current==null) {
        this.current = StateProSet.this.elements;
        try {
          for (int i=offset; i>0; --i) this.current = this.current.next;
        } catch(NullPointerException e) {
          if (this.current!=null) throw e; // else null is returned
        }
        // this.offset = 0;
      } else this.current = this.current.next;
      return (this.current==null) ? null : this.current.state;
    }
  }

  protected static final class Wrapper_State {
    public final IStatePro state;
    public Wrapper_State next=null;

    protected Wrapper_State(IStatePro state) {
      this.state = state;
    }
  }

  protected Wrapper_State elements = null;
  protected Wrapper_State lastElement = null;

  transient int size = 0;

  public StateProSet(){}

  public StateProSet(IStatePro state) {
    this.add(state);
  }

  public boolean add(IStatePro state) {
    //if (state.getParent()!=this) throw new IllegalArgumentException("");
    if (this.contains(state)) return false;

    if (this.lastElement==null) {
      this.elements = new Wrapper_State(state);
      this.lastElement = this.elements;
    } else {
      this.lastElement.next = new Wrapper_State(state);
      this.lastElement = this.lastElement.next;
    }
    ++this.size;
    return true;
  }

  int addAll(StateProSet stateSet) {
    int result = 0;
    for (Wrapper_State wrapper=stateSet.elements; wrapper!=null; wrapper=wrapper.next) {
      if (this.add(wrapper.state)) ++result;
    }
    return result;
  }

  public boolean remove(IStatePro state) {
    if (this.contains(state)==false) return false;

    Wrapper_State prev = null;
    for (Wrapper_State wrapper=this.elements; wrapper!=null; wrapper=wrapper.next) {
      if (wrapper.state==state) {
        if (prev==null) this.elements = wrapper.next;
        else prev.next = wrapper.next;

        if (wrapper==this.lastElement) this.lastElement = prev;
        --this.size;
        return true;
      }
      prev = wrapper;
    }
    return false;
  }

  int removeAll(StateProSet stateSet) {
    int answer = 0;
    Wrapper_State prev = null;
    for (Wrapper_State wrapper=this.elements; wrapper!=null; wrapper=wrapper.next) {
      if (stateSet.contains(wrapper.state)) {
        if (prev==null) this.elements = wrapper.next;
        else prev.next = wrapper.next;

        if (wrapper==this.lastElement) this.lastElement = prev;
        --this.size;
        if (++answer==stateSet.size()) return answer;
      }
      prev = wrapper;
    }
    return answer;
  }


  public boolean contains(IStatePro state) {
    for (Wrapper_State wrapper=this.elements; wrapper!=null; wrapper=wrapper.next) {
      if (wrapper.state==state) return true;
    }
    return false;
  }

  public void clear() {
    this.elements = null;
    this.lastElement = null;
    this.size = 0;
  }

  public int size() {
    return this.size;
  }

  public boolean isEmpty() {
    return this.size==0;
  }

  public StateProSet.Iterator iterator() {
    return new Iterator();
  }

  public StateProSet.Iterator iterator(int offset) {
    return new Iterator(offset);
  }

  public boolean equals(Object obj) {
    if (this==obj) return true;
    if (obj==null) return false;
    if (obj.getClass()!=this.getClass()) throw new ClassCastException("");

    final StateProSet set = (StateProSet)obj;
    if (this.size!=set.size) return false;
    for (Wrapper_State wrapper=set.elements; wrapper!=null; wrapper=wrapper.next) {
      if (this.contains(wrapper.state)==false) return false;
    }
    return true;
  }

  public int hashCode() {
    long hash = 0;
    for (Wrapper_State wrapper=this.elements; wrapper!=null; wrapper=wrapper.next) {
      hash = ( (hash<<32) + wrapper.state.hashCode() ) % 4294967291L;
    }
    return (int)hash;
  }

/*
    public IStatePro[] toArray() {
    }
    public IStatePro[] toArray(IStatePro[] destination) {

    }
*/
}