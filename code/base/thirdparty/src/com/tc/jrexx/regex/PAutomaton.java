/*
* 01/07/2003 - 15:19:32
*
* PAutomaton.java -
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
package com.tc.jrexx.regex;

import com.tc.jrexx.set.*;

import java.util.*;
import java.io.*;

public class PAutomaton extends SAutomaton {
  private static final HashMap AUTOMATON_MAP = new HashMap();

  protected PAutomaton(Automaton_Pattern automaton) {
    super(automaton);
  }

  protected AutomatonSet_String getAutomaton() {
    return this.automaton;
  }

  public PAutomaton() {
    super(new Automaton_Pattern());
  }

  public PAutomaton(String regEx) {
    super(new Automaton_Pattern(regEx));
  }

  public PAutomaton(FSAData data) {
    super(new Automaton_Pattern());
    this.init(data);
  }

  public PAutomaton(InputStream automatonDataStream) throws java.io.IOException,ClassNotFoundException {
    super(new Automaton_Pattern());
//    this.init((FSAData)automatonDataStream.readObject());
    this.init(toFSAData(new ObjectInputStream(automatonDataStream).readObject()));
    try {
      ObjectInputStream oin = new ObjectInputStream( automatonDataStream) {
        /**
         * The readStreamHeader method is provided to allow subclasses to
         * read and verify their own stream headers. It reads and
         * verifies the magic number and version number.
         *
         * @throws IOException if there are I/O errors while reading from the
         * underlying <code>InputStream</code>
         * @throws StreamCorruptedException if control information in the
         * stream is inconsistent
        */
        protected void readStreamHeader()
            throws IOException, StreamCorruptedException
        {
            short incoming_magic = 0;
            short incoming_version = 0;
//            try {
                incoming_magic = readShort();
                incoming_version = readShort();
//            } catch (EOFException e) {
//                throw new StreamCorruptedException("Caught EOFException " +
//                                                   "while reading the stream header");
//            }
            if (incoming_magic != STREAM_MAGIC)
                throw new StreamCorruptedException("InputStream does not contain a serialized object");

            if (incoming_version != STREAM_VERSION)
                throw new StreamCorruptedException("Version Mismatch, Expected " +
                                                   STREAM_VERSION + " and got " +
                                                   incoming_version);
        }
      };
      ((Automaton_Pattern)this.automaton).regEx = (String)oin.readObject();
    } catch(EOFException e) {

    }
  }

  /**
   * writes this.toData() to the automatonDataStream and appends this.getRegEx() to the automatonDataStream.
   */
  public void toData(OutputStream automatonDataStream) throws IOException {
    super.toData(automatonDataStream);
    ObjectOutputStream oOut = new ObjectOutputStream(automatonDataStream) {
      /**
       * The writeStreamHeader method is provided so subclasses can
       * append or prepend their own header to the stream.
       * It writes the magic number and version to the stream.
       *
       * @throws IOException if I/O errors occur while writing to the underlying
       * stream
       */
      protected void writeStreamHeader() throws IOException {
          writeShort(STREAM_MAGIC);
          writeShort(STREAM_VERSION);
      }
    };

    oOut.writeObject(this.getRegEx());
  }

  public void addAll(String regEx) {
    ((Automaton_Pattern)this.automaton).addAll(regEx);
  }

  public void retainAll(String regEx) {
    ((Automaton_Pattern)this.automaton).retainAll(regEx);
  }

  public void removeAll(String regEx) {
    ((Automaton_Pattern)this.automaton).removeAll(regEx);
  }

  public String getRegEx() {
    return ((Automaton_Pattern)this.automaton ).regEx;
  }
}