/*
* 01/07/2003 - 15:19:32
*
* CharSet.java -
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

import java.util.NoSuchElementException;

public class CharSet implements ISet_char {

  interface IAbstract extends java.io.Serializable {
    abstract int size();
    abstract boolean isEmpty();
    abstract void complement();
    abstract boolean contains(char ch);
    abstract boolean add(char ch);
    abstract boolean remove(char ch);
    abstract void addAll(IAbstract set);
    abstract void removeAll(IAbstract set);
    abstract void retainAll(IAbstract set);
    abstract ISet_char.Iterator iterator();
    abstract void addAll(String chars,int offset,int length);
    abstract void addAll(char[] chars,int offset,int length);
    abstract boolean equals(Object obj);
  }

/*
  final class Empty extends Abstract {
    int size() {return 0;}
    boolean isEmpty() {return true;}
    void complement() {CharSet.this.set = new CharSet.Full();}
    boolean contains(char ch) {return false;}
    boolean add(char ch) {
      CharSet.this.set = new CharSet.Char(ch);
      CharSet.this.modifiedFlag = true;
      return true;
    }
    int addAll(char[] chars,int offset,int length) {
      CharSet.this.set = new CharSet.Char(chars[offset]);
      CharSet.this.modifiedFlag = true;

      return 1+CharSet.this.set.addAll(chars,++offset,--length);
    }
    int addAll(String chars,int offset,int length) {
      CharSet.this.set = new CharSet.Char(chars.charAt(offset));
      CharSet.this.modifiedFlag = true;

      return 1+CharSet.this.set.addAll(chars,++offset,--length);
    }

    int addAll(Abstract set) {
      if (set instanceof Char) CharSet.this.set = new Char( (Char)set );
      else
      if (set instanceof LongMap) CharSet.this.set = new LongMap( (LongMap)set );
      else
      if (set instanceof CharComplement) CharSet.this.set = new CharComplement( (CharComplement)set );
      else
      if (set instanceof LongMapComplement) CharSet.this.set = new LongMapComplement( (LongMapComplement)set );
      else
      if (set instanceof Full) CharSet.this.set = new Full();

      return set.size();
    }

    boolean remove(char ch) {return false;}
    int removeAll(Abstract set) {return 0;}

    boolean retain(char ch) {return false;}
    int retainAll(Abstract set) {return 0;}

    ISet_char.Iterator iterator() {
      return new ISet_char.Iterator() {
        public boolean hasNext() {
          if (CharSet.this.modifiedFlag) throw new ConcurrentModificationException();
          return false;
        }
        public char next() {
          throw new NoSuchElementException();
        }
      };
    }
  }

  final class Full extends Abstract {
    int size() {return 65535;}
    boolean isEmpty() {return false;}
    void complement() {CharSet.this.set = new CharSet.Empty();}
    boolean contains(char ch) {return true;}
    boolean add(char ch) {return false;}
    int addAll(String chars,int offset,int length) {return 0;}
    int addAll(char[] chars,int offset,int length) {return 0;}
    int addAll(Abstract set) {return 0;}
    boolean remove(char ch) {
      CharSet.this.set = new CharSet.CharComplement(ch);
      CharSet.this.modifiedFlag = true;
      return true;
    }
    int removeAll(Abstract set) {
      if (set instanceof Char) CharSet.this.set = new CharComplement( (Char)set );
      else
      if (set instanceof LongMap) CharSet.this.set = new LongMapComplement( (LongMap)set );
      else
      if (set instanceof CharComplement) CharSet.this.set = new Char( (CharComplement)set );
      else
      if (set instanceof LongMapComplement) CharSet.this.set = new LongMap( (LongMapComplement)set );
      else
      if (set instanceof Full) CharSet.this.set = new Empty();

      return set.size();
    }
    boolean retain(char ch) {
      CharSet.this.set = new CharSet.Char(ch);
      return true;
    }
    int retainAll(Abstract set) {
      if (set instanceof Char) CharSet.this.set = new Char( (Char)set );
      else
      if (set instanceof LongMap) CharSet.this.set = new LongMap( (LongMap)set );
      else
      if (set instanceof CharComplement) CharSet.this.set = new CharComplement( (CharComplement)set );
      else
      if (set instanceof LongMapComplement) CharSet.this.set = new LongMapComplement( (LongMapComplement)set );
      else
      if (set instanceof Empty) CharSet.this.set = new Empty();

      return set.size();
    }
    ISet_char.Iterator iterator() {
      return new ISet_char.Iterator() {
        int index = 0;
        public boolean hasNext() {
          if (CharSet.this.modifiedFlag) throw new ConcurrentModificationException();
          return this.index!=65536;
        }
        public char next() {
          if (this.index==65536) throw new NoSuchElementException();
          if (CharSet.this.modifiedFlag) throw new ConcurrentModificationException();
          final char result = (char)this.index;
          ++this.index;
          return result;
        }
      };
    }
  };

  final class Char extends Abstract {
    final char ch;
    Char(char ch) {this.ch = ch;}
    int size() {return 1;}
    boolean isEmpty() {return false;}
    boolean contains(char ch) {return this.ch==ch;}
    void complement() {
      CharSet.this.set = new CharSet.CharComplement(this.ch);
    }
    boolean add(char ch) {
      if (ch==this.ch) return false;
      CharSet.this.set = new CharSet.LongMap(this.ch,ch);
      return true;
    }
    int addAll(String chars,int offset,int length) {
      loop: {
        for (; length>0; ++offset,--length)
          if (chars.charAt(offset)!=this.ch) break loop;
        return 0;
      }

      CharSet.this.set = new CharSet.LongMap(this.ch,chars.charAt(offset));
      return 1+CharSet.this.set.addAll(chars,++offset,--length);
    }

    int addAll(char[] chars,int offset,int length) {
      loop: {
        for (; length>0; ++offset,--length)
          if (chars[offset]!=this.ch) break loop;
        return 0;
      }

      CharSet.this.set = new CharSet.LongMap(this.ch,chars[offset]);
      return 1+CharSet.this.set.addAll(chars,++offset,--length);
    }

    int addAll(Abstract set) {
      if (set instanceof Char) {
        if (this.ch!=((Char)set).ch) CharSet.this.set = new LongMap( this,(Char)set );
        else {
          return 0;
        }
      } else
      if (set instanceof LongMap) {
        if (set.contains(this.ch)==false) CharSet.this.set = new LongMap( this,(LongMap)set );
        else {
          CharSet.this.set = new LongMap( (LongMap)set );
          return set.size()-1;
        }
      } else
      if (set instanceof CharComplement) {
        if ( ((CharComplement)set).ch==this.ch ) CharSet.this.set = new Full();
        else {
          CharSet.this.set = new LongMapComplement( (CharComplement)set );
          return 65534;
        }
      } else
      if (set instanceof LongMapComplement) {
        if (set.contains(this.ch)==false) CharSet.this.set = new LongMapComplement( this,(LongMapComplement)set );
        else {
          CharSet.this.set = new LongMapComplement( (LongMapComplement)set );
          return set.size()-1;
        }
      }

      return set.size();
    }

    boolean remove(char ch) {
      if (this.ch==ch) return false;
      CharSet.this.set = new CharSet.Empty();
      CharSet.this.modifiedFlag = true;
      return true;
    }

    int removeAll(Abstract set) {
      if (set.contains(this.ch)==false) return 0;
      CharSet.this.set = new Empty();
      return 1;
    }

    boolean retain(char ch) {
      if (this.ch==ch) return true;
      CharSet.this.set = new Empty();
      return false;
    }

    int retainAll(Abstract set) {
      if (set.contains(this.ch)) return 1;
      CharSet.this.set = new Empty();
      return false;
    }


    ISet_char.Iterator iterator() {
      return new ISet_char.Iterator() {
        boolean hasNext = true;
        public boolean hasNext() {
          if (CharSet.this.modifiedFlag) throw new ConcurrentModificationException();
          return this.hasNext;
        }
        public char next() {
          if (this.hasNext==false) throw new NoSuchElementException();
          if (CharSet.this.modifiedFlag) throw new ConcurrentModificationException();
          this.hasNext = false;
          return CharSet.Char.this.ch;
        }
      };
    }
  }

  final class CharComplement extends Abstract {
    final char ch;
    CharComplement(char ch) {this.ch = ch;}
    int size() {return 65534;}
    boolean isEmpty() {return false;}
    void complement() {CharSet.this.set = new CharSet.Char(this.ch);}
    boolean contains(char ch) {return this.ch!=ch;}
    boolean add(char ch) {
      if (ch!=this.ch) return false;
      CharSet.this.set = new CharSet.Full();
      return true;
    }
    int addAll(String chars,int offset,int length) {
      for (; length>0; ++offset,--length) {
        if (chars.charAt(offset)==this.ch) {
          CharSet.this.set = new CharSet.Full();
          return 1;
        }
      }
      return 0;
    }
    int addAll(char[] chars,int offset,int length) {
      for (; length>0; ++offset,--length) {
        if (chars[offset]==this.ch) {
          CharSet.this.set = new CharSet.Full();
          return 1;
        }
      }
      return 0;
    }
    int addAll(Abstract set) {
      if (set.contains(this.ch)==false) return 0;
      else {
        CharSet.this.set = new Full();
        return 1;
      }
    }

    boolean remove(char ch) {
      if (ch==this.ch) return false;
      CharSet.this.set = new LongMapComplement(this.ch,ch);
      CharSet.this.modifiedFlag = true;
      return true;
    }

    int removeAll(Abstract set) {
      if (set instanceof Char) {
        if ( set.contains(this.ch) ) return 0;
        else {
          CharSet.this.set = new LongMapComplement(this.ch,((Char)set).ch);
          return 1; // set.size();
        }
      } else
      if (set instanceof LongMap) {
        if ( set.contains(this.ch) ) {
          CharSet.this.set = new LongMapComplement((LongMap)map);
          return set.size()-1;
        } else {
          LongMap tmp = new LongMapComplement(this.ch,(LongMap)map);
          return set.size();
        }
      } else
      if (set instanceof CharComplement) {
        if ( set.contains(this.ch) ) {
          CharSet.this.set = new Char(this.ch);
          return 65534; // set.size()-1
        } else {
          CharSet.this.set = new Empty();
          return 65535;
        }
      } else
      if (set instanceof LongMapComplement) {
        if (set.contains(this.ch)) {
          CharSet.this.set = new LongMap(((LongMapComplement)set).);
          return set.size()-1;
        }
      }

    }

    boolean retain(char ch) {
      if (this.ch==ch) {
        CharSet.this.set = new Empty();
        return false;
      } else {
        CharSet.this.set = new Char(ch);
        return true;
      }
    }

    int retain(Abstract set) {
      if (set.contains(this.ch)) {
        CharSet.this.set = new Empty();
        return 0;
      } else {
        CharSet.this.set = Char((Char)set);
      }

      return set.size();
    }

    ISet_char.Iterator iterator() {
      return new ISet_char.Iterator() {
        int index = 0;
        public boolean hasNext() {
          if (this.index==(int)CharSet.CharComplement.this.ch) ++this.index;
          return this.index!=65536;
        }
        public char next() {
          if (this.index==(int)CharSet.CharComplement.this.ch) ++this.index;
          if (this.index==65536) throw new NoSuchElementException();
          if (CharSet.this.modifiedFlag) throw new ConcurrentModificationException();
          final char result = (char)this.index;
          ++this.index;
          return result;
        }
      };
    }
  }
*/

//  static int wrapperCount = 0;

  static final class Wrapper implements java.io.Serializable {
    final int offset;
    long value;

    Wrapper(int offset,long value) {
      this.offset = offset;
      this.value = value;

//      ++wrapperCount;
//      if (wrapperCount>1060000) System.out.println("+"+wrapperCount);
    }

//    protected void finalize() throws Throwable {
//      --wrapperCount;
//      if (wrapperCount>99900 && wrapperCount<100000) System.out.println("-"+wrapperCount);
//    }

    int size() {
      int answer = 0;
      for (long tmp=this.value; tmp!=0; tmp>>>=4) {
        switch((int)(tmp & 15L)) {
          case  0 : answer+= 0; break;
          case  1 : answer+= 1; break;
          case  2 : answer+= 1; break;
          case  3 : answer+= 2; break;
          case  4 : answer+= 1; break;
          case  5 : answer+= 2; break;
          case  6 : answer+= 2; break;
          case  7 : answer+= 3; break;
          case  8 : answer+= 1; break;
          case  9 : answer+= 2; break;
          case 10 : answer+= 2; break;
          case 11 : answer+= 3; break;
          case 12 : answer+= 2; break;
          case 13 : answer+= 3; break;
          case 14 : answer+= 3; break;
          case 15 : answer+= 4; break;
          default : throw new RuntimeException("error: should never happen");
        }
      }
      return answer;
    }
  }

  final static int[] PRIMENUMBERS = new int[]{
    3,5,7,11,13,17,19,23,29,31,37,41,43,47,53,59,61,67,71,73,79,83,89,97,
    101,103,107,109,113,127,131,137,139,149,151,157,163,167,173,179,181,191,
    193,197,199,211,223,227,229,233,239,241,251,257,263,269,271,277,281,283,
    293,307,311,313,317,331,337,347,349,353,359,367,373,379,383,389,397,401,
    409,419,421,431,433,439,443,449,457,461,463,467,479,487,491,499,503,509,
    521,523,541,547,557,563,569,571,577,587,593,599,601,607,613,617,619,631,
    641,643,647,653,659,661,673,677,683,691,701,709,719,727,733,739,743,751,
    757,761,769,773,787,797,809,811,821,823,827,829,839,853,857,859,863,877,
    881,883,887,907,911,919,929,937,941,947,953,967,971,977,983,991,997,1009,
    1013,1019,1021,1024
  };

  final static long[] VALUES = new long[64];
  static{
    VALUES[0] = 1L;
    for (int t=0,i=1; i<VALUES.length; ++i,++t) VALUES[i] = VALUES[t]<<1;
  }

  final class LongMap implements IAbstract {

    Wrapper[] sets = null;
    int size = 0;

    protected LongMap(LongMap set) {
      this.sets = new Wrapper[set.sets.length];
      for (int i=0; i<set.sets.length; ++i)
        if (set.sets[i]!=null)
          this.sets[i] = new Wrapper(set.sets[i].offset,set.sets[i].value);

      this.size = set.size;
    }

    LongMap() {
      this.sets = new Wrapper[CharSet.PRIMENUMBERS[0]];
    }

    LongMap(char ch1,char ch2) {
      this.sets = new Wrapper[CharSet.PRIMENUMBERS[0]];
      this.add(ch1);
      this.add(ch2);
    }

    public boolean isEmpty() {return this.size()==0;}
    public int size() {
      if (this.size>=0) return this.size;
      int answer = 0;
      for (int i=0; i<this.sets.length; ++i) {
        if (this.sets[i]!=null) answer+= this.sets[i].size();
      }
      this.size = answer;
      return answer;
    }

    public boolean contains(char ch) {
      final int offset = ch/64;
      final int index = offset % this.sets.length;
      if (this.sets[index]==null || this.sets[index].offset != offset) return false;
      return (this.sets[index].value & (VALUES[ch%64]))!=0;
    }

    public void complement() {
      this.size = -1;
      final Wrapper[] tmp = this.sets;
      this.sets = new Wrapper[CharSet.PRIMENUMBERS[0]];
      for (int offset=0; offset<CharSet.this.max; ++offset) {
        int index = offset % tmp.length;
        long value = -1L;
        if (tmp[index]!=null && tmp[index].offset==offset) value^= tmp[index].value;
        if (value!=0) this.addAll(offset,value);
      }
    }

    public boolean add(char ch) {
      if (ch>CharSet.this.maxChar)
        throw new IllegalArgumentException(
          "ch > maxChar = "
          +CharSet.this.maxChar
          +"("+(int)CharSet.this.maxChar+")"
        );

      final int offset = ch/64;
      loop: do {
        int index = offset % this.sets.length;
        if (this.sets[index]==null) {
          this.sets[index] = new Wrapper(offset,1L<<(ch%64));
          if (this.size>=0) ++this.size;
          return true;
        }
        if (this.sets[index].offset==offset) {
          long oldValue = this.sets[index].value;
          this.sets[index].value|= VALUES[ch%64];
          long newValue = this.sets[index].value;

          if (oldValue==newValue) return false;

          if (this.size>=0) ++this.size;
          return true;
        }

        this.expand();
      } while(true);
    }

    public void addAll(char[] chars,int offset,int length) {
      for (; length>0; ++offset,--length) this.add(chars[offset]);
    }

    public void addAll(String chars,int offset,int length) {
      for (; length>0; ++offset,--length) this.add(chars.charAt(offset));
    }


    public void addAll(IAbstract set) {
      this.addAll((LongMap)set);
    }

    void addAll(LongMap set) {
      if (this.sets.length>=set.sets.length) {
        for (int i=0; i<set.sets.length; ++i) {
          if (set.sets[i]!=null)
            this.addAll(set.sets[i].offset,set.sets[i].value);
        }
      } else {
        final Wrapper[] tmp = this.sets;
        this.sets = new Wrapper[set.sets.length];
        for (int i=0; i<set.sets.length; ++i)
          this.sets[i] = (set.sets[i]==null) ? null : new Wrapper(set.sets[i].offset,set.sets[i].value);
        this.size = set.size;

        for (int i=0; i<tmp.length; ++i) {
          if (tmp[i]!=null) this.addAll(tmp[i]);
        }
      }
    }

    private void addAll(int offset,long value) {
      this.size = -1;
      loop: do {
        int index = offset % this.sets.length;
        if (this.sets[index]==null) {
          this.sets[index] = new Wrapper(offset,value);
          return;
        }
        if (this.sets[index].offset==offset) {
          this.sets[index].value|= value;
          return;
        }

        this.expand();
      } while(true);
    }

    private void addAll(Wrapper w) {
      this.size = -1;
      loop: do {
        int index = w.offset % this.sets.length;
        if (this.sets[index]==null) {
          this.sets[index] = w;
          return;
        }
        if (this.sets[index].offset==w.offset) {
          this.sets[index].value|= w.value;
          return;
        }

        this.expand();
      } while(true);
    }


    public boolean remove(char ch) {
      final int offset = ch/64;
      final int index = offset % this.sets.length;
      if (this.sets[index]==null) return false;
      if (this.sets[index].offset!=offset) return false;

      long oldValue = this.sets[index].value;
      this.sets[index].value&= (-1L)^(VALUES[ch%64]);
      long newValue = this.sets[index].value;

      if (oldValue==newValue) return false;

      if (this.size>0) --this.size;
      if (newValue==0) this.sets[index] = null;
      return true;
    }

    public void removeAll(IAbstract set) {
      this.removeAll((LongMap)set);
    }

    void removeAll(LongMap set) {
      for (int i=0; i<set.sets.length; ++i) {
        if (set.sets[i]!=null)
          this.removeAll(set.sets[i].offset,set.sets[i].value);
      }
    }

    private void removeAll(int offset,long value) {
      final int index = offset % this.sets.length;
      if (this.sets[index]==null) return;
      if (this.sets[index].offset!=offset) return;

      this.size = -1;
      this.sets[index].value&= (-1L)^value;
      if (this.sets[index].value==0) this.sets[index] = null;
    }

    public void retainAll(IAbstract set) {
      this.retainAll((LongMap)set);
    }

    void retainAll(LongMap set) {
      this.size = -1;
      for (int i=0; i<this.sets.length; ++i) {
        if (this.sets[i]!=null) {
          Wrapper w1 = this.sets[i];
          Wrapper w2 = set.sets[w1.offset % set.sets.length];
          if (w2==null) this.sets[i] = null;
          else {
            if (w1.offset!=w2.offset) this.sets[i] = null;
            else {
              w1.value&= w2.value;
              if (this.sets[i].value==0) this.sets[i] = null;
            }
          }
        }
      }
    }

    private void expand() {
      final Wrapper[] values = this.sets;
      reInit: do {
        this.sets = new Wrapper[this.nextPrimeNumber()];
        for (int i=0; i<values.length; ++i) {
          if (values[i]!=null) {
            int index = values[i].offset % this.sets.length;
            if (this.sets[index]!=null) continue reInit;
            this.sets[index] = values[i];
          }
        }
        return;
      } while(true);
    }

    private int nextPrimeNumber() {
      final int currentPrimeNumber = this.sets.length;
      int i = 0;
      while (CharSet.PRIMENUMBERS[i]!=currentPrimeNumber) ++i;
      return CharSet.PRIMENUMBERS[++i];
    }

    public ISet_char.Iterator iterator() {
      return new LongMapIterator(CharSet.this, this);
    }

    public boolean equals(Object obj) {
      if (this==obj) return true;
      if (obj==null) return false;
      if (this.getClass()!=obj.getClass()) return false;

      LongMap set = (LongMap)obj;
      if (this.size!=set.size) return false;

      for (int i=0; i<this.sets.length; ++i) {
        if (this.sets[i]!=null) {
if (this.sets[i].value==0) throw new Error("this.sets[i].value==0");
          Wrapper w1 = this.sets[i];
          Wrapper w2 = set.sets[w1.offset % set.sets.length];
          if (w2==null) return false;
          else {
            if (w1.offset!=w2.offset) return false;
            else {
              if (w1.value!=w2.value) return false;
            }
          }
        }
      }

      return true;
    }
  }


  private static final class LongMapIterator implements ISet_char.Iterator {
    int currentOffset = 0;

    long currentValue = 1L;

    char currentChar = '\u0000';

    final int setsLength;  // = CharSet.LongMap.this.sets.length;

    private final LongMap longMap;

    private final CharSet charSet;

    public LongMapIterator(CharSet charSet, LongMap longMap) {
      this.charSet = charSet;
      this.longMap = longMap;
      this.setsLength = longMap.sets.length;
    }

    public boolean hasNext() {
    
              do {
                if (longMap.contains(this.currentChar)) return true;
              } while(++this.currentChar!='\u0000');
              return false;
    
    /*
    //          if (CharSet.this.modifiedFlag) throw new ConcurrentModificationException();
    
              for (; this.currentOffset<=1024; ++this.currentOffset) {
                Wrapper w = CharSet.LongMap.this.sets[this.currentOffset%setsLength];
                if (w==null) this.currentChar+= 64;
                else {
                  if (w.offset==this.currentOffset) {
                    for (; this.currentValue!=0; this.currentValue<<=1, ++this.currentChar) {
                      if ((w.value & this.currentValue)!=0) {
                        return true;
                      }
                    }
                    this.currentValue = 1L;
                  }
                }
              }
              return false;
    */
            }

    public char next() {
              do {
                if (longMap.contains(this.currentChar)) return this.currentChar++;
              } while(++this.currentChar!='\u0000');
    
              throw new NoSuchElementException(charSet.toString());
    
    /*
    //          if (CharSet.this.modifiedFlag) throw new ConcurrentModificationException();
    
              for (; this.currentOffset<=1024; ++this.currentOffset) {
                Wrapper w = CharSet.LongMap.this.sets[this.currentOffset%setsLength];
                if (w==null) this.currentChar+= 64;
                else {
                  if (w.offset==this.currentOffset) {
                    for (; this.currentValue!=0; this.currentValue<<=1, ++this.currentChar) {
                      if ((w.value & this.currentValue)!=0) {
                        final char result = this.currentChar;
                        this.currentValue<<=1;
                        if (this.currentValue!=0) ++this.currentChar;
                        return result;
                      }
                    }
                    this.currentValue = 1L;
                  }
                }
              }
              throw new NoSuchElementException(this.currentChar+" \""+CharSet.this.toString()+"\" "+CharSet.this.size());
    */
            }
  }
  

//  CharSet.Abstract set = new Empty();
//   transient boolean modifiedFlag = false;

  static final int max = 4;
  static final char maxChar = (char)(64*max-1);

  protected CharSet.IAbstract set;

  protected CharSet(IAbstract set) {
    this.set = set;
  }

  public CharSet() {
    this.set = new LongMap();
  }

  public CharSet(char ch) {
    this();
    this.set.add(ch);
  }

  public CharSet(String s) {
    this();
    this.set.addAll(s,0,s.length());
  }

  public void complement() {
    this.set.complement();
  }

  public boolean contains(char ch) {
    return this.set.contains(ch);
  }

  public boolean isEmpty() {
    return this.set.isEmpty();
  }

  public int size() {
    return this.set.size();
  }

  public ISet_char.Iterator iterator() {
    return this.set.iterator();
  }

  public void clear() {
//    this.set = new CharSet.Empty();
    this.set = new LongMap();
  }

  public boolean add(char ch) {

    return this.set.add(ch);
  }

  public boolean remove(char ch) {
    return this.set.remove(ch);
  }

  public void addAll(String chars) {
    this.addAll(chars,0,chars.length());
  }
  public void addAll(String chars,int offset) {
    this.addAll(chars,offset,chars.length()-offset);
  }

  public void addAll(String chars,int offset,int length) {
    if (length==0) return;
    this.set.addAll(chars,offset,length);
  }

  public void addAll(char[] chars) {
    this.addAll(chars,0,chars.length);
  }
  public void addAll(char[] chars,int offset) {
    this.addAll(chars,offset,chars.length-offset);
  }

  public void addAll(char[] chars,int offset,int length) {
    if (length==0) return;
    this.set.addAll(chars,offset,length);
  }

  /**
   * adds all chars from set to this ISet_char without adding doublicates.
   * returns the number of chars added to this ISet_char.
   */
  public void addAll(ISet_char set) {
    if (set instanceof CharSet) this.set.addAll(((CharSet)set).set);
    else {
      final ISet_char.Iterator it = set.iterator();
      for (int i=set.size(); i>0; --i) this.set.add(it.next());
    }
  }

  /**
   * Removes from this set all of its elements that are contained in the specified set (optional operation).
   * returns the number of chars that were removed.
   */
  public void removeAll(ISet_char set) {
    if (set instanceof CharSet) this.set.removeAll(((CharSet)set).set);
    else {
      final ISet_char.Iterator it = set.iterator();
      for (int i=set.size(); i>0; --i) this.set.remove(it.next());
    }
  }

  public void retainAll(ISet_char set) {
    if (set instanceof CharSet) this.set.retainAll(((CharSet)set).set);
    else {
      final CharSet charSet = new CharSet();
      final ISet_char.Iterator it = set.iterator();
      for (int i=set.size(); i>0; --i) charSet.add(it.next());
      this.set.retainAll(charSet.set);
    }
  }

  public boolean equals(Object obj) {
    if (this==obj) return true;
    if (obj==null) return false;
    if (this.getClass()!=obj.getClass()) return false;
    return this.set.equals(((CharSet)obj).set);
  }

  public int hashCode() {
    return this.set.size();
  }

  protected IAbstract cloneAbstract(IAbstract set) {
    if (set instanceof LongMap) return new LongMap((LongMap)set);
    throw new Error("");
  }

  public Object clone() {
    try {
      CharSet clone = (CharSet)super.clone();
      clone.set = clone.cloneAbstract(set);
      return clone;
    } catch(CloneNotSupportedException e) {
      throw new Error("CloneNotSupportedException:\n"+e);
    }
  }

  public String toString() {
    StringBuffer answer = new StringBuffer();

    int from = -1;
    char ch = '\u0000';
    do {
      if (this.contains(ch)) {
        if (from==-1) from = ch;
      } else {
        if (from!=-1) {
          char to = ch; --to;
          if (from==to) {
            if (to=='[' || to==']' || to=='\\' || to=='-') answer.append('\\');
            answer.append(to);
          } else {
            char char_from = (char)from;
            if (char_from=='[' || char_from==']' || char_from=='\\' || char_from=='-') answer.append('\\');
            answer.append((char)from);
            if (to!=++from) answer.append("-");
            if (to=='[' || to==']' || to=='\\' || to=='-') answer.append('\\');
            answer.append(to);
          }

          from = -1;
        }
      }
    } while (++ch!='\u0000');

    if (from!=-1) {
      char char_from = (char)from;
      if (char_from=='[' || char_from==']' || char_from=='\\' || char_from=='-') answer.append('\\');
      answer.append((char)from);
      if (from!='\ufffe') answer.append('-');
      answer.append('\uffff');
    }

    for (int i=answer.length()-1; i>=0; --i) {
      if (answer.charAt(i)>'\u00ff') answer.setCharAt(i,'.');
    }

    return answer.toString();
  }

}