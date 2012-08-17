/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * CanonicalStringSet.java
 *
 * Created on 13. Februar 2007, 17:07
 */
/*
 * Copyright 2007 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terracotta.agent.repkg.de.schlichtherle.util;

import java.util.*;

/**
 * A set of canonicalized strings in natural order.
 * A string is canonicalized by the template method {@link #canonical},
 * which should get overridden by subclasses.
 * <p>
 * String sets can be converted from and to string lists by using
 * {@link #addAll(String)} and {@link #toString()}.
 * A <i>string list</i> is a string which consists of zero or more elements
 * which are separated by the <i>separator character</i> provided to the
 * constructor.
 * Note that in general, a string list is just a sequence of strings elements.
 * In particular, a string list may be empty (but not <code>null</code>) and
 * its elements don't have to be in canonical form, may be duplicated in the
 * list and may be listed in arbitrary order.
 * However, string lists have a canonical form, too:
 * A string list in canonical form (or <i>canonical string list</i> for short)
 * is a string list which contains only canonical strings in natural order
 * and does not contain any duplicates (so it's actually a set).
 * <p>
 * Unless otherwise documented, all {@link Set} methods work on the canonical
 * form of the string elements in this set.
 * <p>
 * Null elements are not permitted in this set.
 * 
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.5
 */
public class CanonicalStringSet extends AbstractSet {

    /** The separator for string lists. */
    private final char separator;

    /** The sorted map which implements the behaviour of this class. */
    private final SortedMap map = new TreeMap();

    /**
     * Constructs a new, empty set of canonical strings.
     *
     * @param separator The separator character to use in string lists.
     */
    public CanonicalStringSet(final char separator) {
        this.separator = separator;
    }

    /**
     * Constructs a new set of canonical strings from the given set of
     * canonical strings.
     * 
     * @param separator The separator character to use in string lists.
     * @param set A set of canonical strings - may be <code>null</code> to
     *        construct an empty set.
     */
    public CanonicalStringSet(final char separator, final CanonicalStringSet set) {
        this.separator = separator;
        if (set != null)
            addAll(set ); // no dangerous constructor - method is final!
    }

    /**
     * Constructs a new set of canonical strings from the given string list.
     * 
     * @deprecated This constructor is dangerous: It may call
     *             {@link canonicalize}, which may result in a call from a
     *             superclass if this constructor is called from a subclass.
     * @param separator The separator character to use in string lists.
     * @param list A string list - may be <code>null</code> to
     *        construct an empty set.
     */
    public CanonicalStringSet(final char separator, final String list) {
        this.separator = separator;
        if (list != null)
            addAll(list);
    }

    /** @deprecated Override and use {@link canonicalize} instead. */
    protected String canonical(final String s) {
        return canonicalize(s);
    }

    /**
     * A template method which returns the canonical form of <code>s</code> or
     * <code>null</code> if the given string does not have a canonical form.
     * The implementation in this class simply returns the parameter.
     *
     * @param s The string to get canonicalized.
     *        Never <code>null</code> and never contains the separator.
     * @return The canonical form of <code>s</code> or <code>null</code> if
     *         <code>s</code> does not have a canonical form.
     */
    protected String canonicalize(final String s) {
        assert s != null;
        assert s.indexOf(separator) < 0 : "separator in string is illegal";
        return s;
    }

    public final boolean isEmpty() {
        return super.isEmpty();
    }

    public final int size() {
        return map.size();
    }

    /**
     * Tests if the canonical form of all strings in the given string list
     * is contained in this set.
     * If a string in the list does not have a canonical form, it's skipped.
     * This implies that if the list is empty or entirely consists of strings
     * which do not have a canonical form, <code>true</code> is returned.
     * In other words, an empty set is considered to be a true subset of this
     * set.
     *
     * @param list A non-null string list.
     * @return <code>true</code> Iff the canonical form of all strings in the
     *         given string list is contained in this set.
     * @throws NullPointerException If the parameter is <code>null</code>.
     * @throws ClassCastException If <code>list</code> is not a <code>String</code>.
     */
    public final boolean contains(Object list) {
        return containsAll((String) list);
    }

    /**
     * Returns a new iterator for all canonical string elements in this set.
     * 
     * @return A new iterator for all canonical string elements.
     */
    public final Iterator iterator() {
        return map.keySet().iterator();
    }

    /**
     * Returns a new iterator for all original string elements in this set.
     * Note that strings which don't have a canonical form cannot get added
     * to this class and hence cannot get returned by the iterator.
     * 
     * @return A new iterator for all original string elements.
     */
    public final Iterator originalIterator() {
        return map.values().iterator();
    }

    public final Object[] toArray() {
        return map.keySet().toArray();
    }

    public final Object[] toArray(Object[] array) {
        return map.keySet().toArray(array);
    }

    //
    // Modification operations.
    //

    /**
     * Adds the canonical form of all strings in the given list to this set.
     * If a string in the list does not have a canonical form or its canonical
     * form is already contained in this set, it's ignored.
     *
     * @param list A non-null string list.
     * @return <code>true</code> Iff this set changed as a result of the call.
     * @throws NullPointerException If <code>list</code> is <code>null</code>.
     * @throws ClassCastException If <code>list</code> is not a <code>String</code>.
     */
    public final boolean add(Object list) {
        return addAll((String) list);
    }

    /**
     * Removes the canonical form of all strings in the given list from this set.
     * If a string in the list does not have a canonical form, it's ignored.
     *
     * @param list A non-null string list.
     * @return <code>true</code> Iff this set changed as a result of the call.
     * @throws NullPointerException If <code>list</code> is <code>null</code>.
     * @throws ClassCastException If <code>list</code> is not a <code>String</code>.
     */
    public final boolean remove(Object list) {
        return removeAll((String) list);
    }

    //
    // Bulk operations.
    //

    /**
     * Tests if all canonical strings in the given set are contained in this
     * set.
     * An empty set is considered to be a true subset of this set.
     * 
     * @param set A non-null set of canonical strings.
     * @return <code>true</code> Iff all strings in the given set are contained
     *         in this set.
     * @throws NullPointerException If the parameter is <code>null</code>.
     */
    public final boolean containsAll(final CanonicalStringSet set) {
        return map.keySet().containsAll(set.map.keySet());
    }

    /**
     * Tests if the canonical form of all strings in the given string list
     * is contained in this set.
     * If a string in the list does not have a canonical form, it's skipped.
     * This implies that if the list is empty or entirely consists of strings
     * which do not have a canonical form, <code>true</code> is returned.
     * In other words, an empty set is considered to be a true subset of this
     * set.
     * 
     * @param list A non-null string list.
     * @return <code>true</code> Iff the canonical form of all strings in the
     *         given string list is contained in this set.
     * @throws NullPointerException If the parameter is <code>null</code>.
     */
    public final boolean containsAll(final String list) {
        final Iterator i = new CanonicalStringIterator(list);
        while (i.hasNext())
            if (!map.containsKey(i.next()))
                return false;
        return true;
    }

    /**
     * Adds all canonical strings in the given set to this set after they have
     * been canonicalized by this set again.
     * 
     * @param set A non-null set of canonical strings.
     * @return <code>true</code> Iff this set changed as a result of the call.
     * @throws NullPointerException If <code>set</code> is <code>null</code>.
     */
    public final boolean addAll(final CanonicalStringSet set) {
        return super.addAll(set);
        // This doesn't canonicalize the strings in the set again!
        /*final int s = map.size();
        map.putAll(set.map);
        return s != map.size();*/
    }

    /**
     * Adds the canonical form of all strings in the given list to this set.
     * If a string in the list does not have a canonical form, it's skipped.
     * 
     * @param list A non-null string list.
     * @return <code>true</code> Iff this set changed as a result of the call.
     * @throws NullPointerException If <code>list</code> is <code>null</code>.
     */
    public final boolean addAll(final String list) {
        boolean changed = false;
        final Iterator i = new StringIterator(list);
        while (i.hasNext()) {
            final String element = (String) i.next();
            final String canonical = canonicalize(element);
            if (canonical != null) {
                final String previous = (String) map.put(canonical, element);
                if (!changed)
                    changed = previous == null || !element.equals(previous);
            }
        }
        return changed;
    }

    /**
     * Retains all canonical strings in the given set in this set.
     * 
     * @param set A non-null set of canonical strings.
     * @return <code>true</code> Iff this set changed as a result of the call.
     * @throws NullPointerException If <code>set</code> is <code>null</code>.
     */
    public final boolean retainAll(CanonicalStringSet set) {
        return map.keySet().retainAll(set.map.keySet());
    }

    /**
     * Retains the canonical form of all strings in the given list in this set.
     * If a string in the list does not have a canonical form, it's skipped.
     * 
     * @param list A non-null string list.
     * @return <code>true</code> Iff this set changed as a result of the call.
     * @throws NullPointerException If <code>set</code> is <code>null</code>.
     */
    public final boolean retainAll(final String list) {
        class CustomSet extends CanonicalStringSet {
            CustomSet() {
                super(separator);
                super.addAll(list);
            }

            protected String canonicalize(String s) {
                return CanonicalStringSet.this.canonicalize(s);
            }
        }
        return map.keySet().retainAll(new CustomSet());
    }

    /**
     * Removes all canonical strings in the given set from this set.
     * 
     * @param set A non-null set of strings.
     * @return <code>true</code> Iff this set changed as a result of the call.
     * @throws NullPointerException If <code>set</code> is <code>null</code>.
     */
    public final boolean removeAll(CanonicalStringSet set) {
        return map.keySet().removeAll(set.map.keySet());
    }

    /**
     * Removes the canonical form of all strings in the given list from this set.
     * If a string in the list does not have a canonical form, it's skipped.
     * 
     * @param list A non-null string list.
     * @return <code>true</code> Iff this set changed as a result of the call.
     * @throws NullPointerException If <code>list</code> is <code>null</code>.
     */
    public final boolean removeAll(final String list) {
        boolean changed = false;
        final Iterator i = new CanonicalStringIterator(list);
        while (i.hasNext())
            changed |= (map.remove(i.next()) != null);
        return changed;
    }

    public final void clear() {
        map.clear();
    }

    //
    // Miscellaneous.
    //

    /**
     * Returns the canonical string list representation of this set.
     * If this string set is empty, an empty string is returned.
     */
    public final String toString() {
        final Iterator i = iterator();
        if (i.hasNext()) {
            final StringBuffer sb = new StringBuffer();
            int c = 0;
            do {
                final String string = (String) i.next();
                if (c++ > 0)
                    sb.append(separator);
                sb.append(string);
            } while (i.hasNext());
            return sb.toString();
        } else {
            return "";
        }
    }

    //
    // Inner classes.
    //

    private class CanonicalStringIterator implements Iterator {
        private final Iterator i;
        private String canonical;

        private CanonicalStringIterator(final String list) {
            i = new StringIterator(list);
            advance();
        }

        public boolean hasNext() {
            return canonical != null;
        }

        public Object next() {
            if (canonical == null)
                throw new NoSuchElementException();
            final String c = canonical;
            advance();
            return c;
        }

        private void advance() {
            while (i.hasNext()) {
                canonical = canonicalize((String) i.next());
                if (canonical != null)
                    return;
            }
            canonical = null; // no such element
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    } // class CanonicalSuffixIterator

    private class StringIterator implements Iterator {
        private final String[] split;
        private int i = 0;

        private StringIterator(final String list) {
            split = list.split("\\" + separator); // NOI18N
        }

        public boolean hasNext() {
            return i < split.length;
        }

        public Object next() {
            try {
                return split[i++];
            } catch (IndexOutOfBoundsException ex) {
                throw new NoSuchElementException();
            }
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    } // class StringIterator
}
