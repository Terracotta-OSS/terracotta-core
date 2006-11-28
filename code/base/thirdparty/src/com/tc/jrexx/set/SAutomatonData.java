/*
* 01/07/2003 - 15:19:32
*
* Pattern.java -
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
 * @deprecated
 */
public class SAutomatonData extends XML {
  /**
   * @deprecated
   */
  public static class State extends XML {
    /**
     * @deprecated
     */
    public static class Transition extends XML {
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
    }
    public int number;
    public boolean isFinal;
    public Transition[] transitions;
    boolean transitionsAreDeterministic;

    public State(
      int number,
      SAutomatonData.State.Transition[] transitions,
      boolean transitionsAreDeterministic
    ) {
      this(number,false,transitions,transitionsAreDeterministic);
    }

    public State(
      int number,
      boolean isFinal,
      SAutomatonData.State.Transition[] transitions,
      boolean transitionsAreDeterministic
    ) {
      this.number = number;
      this.isFinal = isFinal;
      this.transitions = transitions;
      this.transitionsAreDeterministic = transitionsAreDeterministic;
    }
  }
  public State[] states;
  public Integer startStateNumber;
  public boolean isDeterministic;


  public SAutomatonData(
    SAutomatonData.State[] states,
    Integer startStateNumber,
    boolean isDeterministic
  ) {
    this.states = states;
    this.startStateNumber = startStateNumber;
    this.isDeterministic = isDeterministic;
  }
}
