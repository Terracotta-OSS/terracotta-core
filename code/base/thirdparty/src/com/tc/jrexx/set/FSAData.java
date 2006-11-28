/*
* 01/07/2003 - 15:19:32
*
* Automaton.java -
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

/**
 * FSAData is a simple representation of a finite state automaton as a ValueObject.
 * It provides no functionality, but only the data of a specific automaton.
 * It is used for the interchange of automatons between different applications or tools.
 * In future there will be also an compatible XML representation.
 * @author Ralf Meyer
 */
public class FSAData implements java.io.Serializable {
  static final long serialVersionUID = -8666364495865759270L;
  static final int classVersion = 1;
  int objectVersion = classVersion;


  public static class State implements java.io.Serializable {
    static final long serialVersionUID = -8580316209323007588L;
    static final int classVersion = 1;
    int objectVersion = classVersion;

    public static class Transition implements java.io.Serializable {
      static final long serialVersionUID = -7679256544857989306L;
      static final int classVersion = 1;
      int objectVersion = classVersion;

      public com.tc.jrexx.automaton.IProperties properties;
      public String charSet;
      public int toStateNumber;

      public Transition(
          com.tc.jrexx.automaton.IProperties properties,
        String charSet,
        int toStateNumber
      ) {
        this.properties = properties;
        this.charSet = charSet;
        this.toStateNumber = toStateNumber;
      }

      public Transition(
        String charSet,
        int toStateNumber
      ) {
        this(null,charSet,toStateNumber);
      }

    }
    public int number;
    public boolean isFinal;
    public Transition[] transitions;
    public Boolean transitionsAreDeterministic;

    public State(
      int number,
      boolean isFinal,
      FSAData.State.Transition[] transitions,
      boolean transitionsAreDeterministic
    ) {
      this.number = number;
      this.isFinal = isFinal;
      this.transitions = transitions;
      this.transitionsAreDeterministic = new Boolean(transitionsAreDeterministic);
    }


    public State(
      int number,
      boolean isFinal,
      FSAData.State.Transition[] transitions
    ) {
      this.number = number;
      this.isFinal = isFinal;
      this.transitions = transitions;
      this.transitionsAreDeterministic = null;
    }
  }
  public State[] states;
  public Integer startStateNumber;
  public Boolean isDeterministic;

  public FSAData(
    FSAData.State[] states,
    Integer startStateNumber,
    boolean isDeterministic
  ) {
    this.states = states;
    this.startStateNumber = startStateNumber;
    this.isDeterministic = new Boolean(isDeterministic);
  }

  public FSAData(
    FSAData.State[] states,
    Integer startStateNumber
  ) {
    this.states = states;
    this.startStateNumber = startStateNumber;
    this.isDeterministic = null;
  }

}
