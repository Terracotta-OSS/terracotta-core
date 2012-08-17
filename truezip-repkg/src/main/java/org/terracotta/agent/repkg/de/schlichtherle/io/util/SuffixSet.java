/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * SuffixSet.java
 *
 * Created on 23. Januar 2007, 21:19
 */
/*
 * Copyright 2005-2007 Schlichtherle IT Services
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

package org.terracotta.agent.repkg.de.schlichtherle.io.util;


import java.io.*;
import java.util.*;

import org.terracotta.agent.repkg.de.schlichtherle.util.*;

/**
 * An ordered set of canonicalized suffixes.
 * A <i>suffix</i> is the part of a file name string after the last dot.
 * It must not contain the character <code>'|'</code>.
 * A suffix in canonical form (or <i>canonical suffix</i> for short) is a
 * lowercase string which is not empty and does <em>not</em> start with a
 * dot (<code>'.'</code>).
 * <p>
 * For example, the suffix <code>&quot;zip&quot;</code> is in canonical form,
 * while the suffixes
 * <code>&quot;&quot;</code>,
 * <code>&quot;Zip&quot;</code>,
 * <code>&quot;ZIP&quot;</code>,
 * <code>&quot;.zip&quot;</code>,
 * <code>&quot;.Zip&quot;</code>,
 * <code>&quot;.ZIP&quot;</code>, and
 * <code>&quot;zip|Zip|ZIP|.zip|.Zip|.ZIP&quot;</code> aren't.
 * <p>
 * Suffix sets can be converted from and to suffix lists by using
 * {@link #addAll(String)} and {@link #toString()}.
 * A <i>suffix list</i> is a string which consists of zero or more suffixes
 * which are separated by the character <code>'|'</code>.
 * Note that in general, a suffix list is just a sequence of suffixes.
 * In particular, a suffix list may be empty (but not <code>null</code>) and
 * its suffixes don't have to be in canonical form, may be duplicated in the
 * list and may be listed in arbitrary order.
 * However, suffix lists have a canonical form, too:
 * A suffix list in canonical form (or <i>canonical suffix list</i> for short)
 * is a suffix list which contains only canonical suffixes in natural order
 * and does not contain any duplicates (so it's actually a set).
 * <p>
 * Unless otherwise documented, all {@link Set} methods work on the canonical
 * form of the suffixes in this set.
 * <p>
 * Null suffixes are not permitted in this set.
 * 
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.5
 */
public final class SuffixSet extends CanonicalStringSet {

    /** The separator for suffixes in lists. */
    private static final char SEPARATOR = '|';

    /** The optional prefix for suffixes. */
    private static final char PREFIX = '.';

    /** Constructs a new, empty suffix set. */
    public SuffixSet() {
        super(SEPARATOR);
    }

    /**
     * Constructs a new suffix set from the given suffix list.
     * 
     * @param list A suffix list - may be <code>null</code> to
     *        construct an empty set.
     */
    public SuffixSet(final String list) {
        super(SEPARATOR);
        if (list != null)
            super.addAll(list);
    }

    /**
     * Constructs a new suffix set by adding the canonical form of all suffixes
     * for all suffix lists in the given collection.
     *
     * @param c A collection of suffix lists - may be <code>null</code> to
     *        construct an empty set.
     * @throws ClassCastException If the collection does not only contain
     *         <code>String</code>s.
     */
    public SuffixSet(final Collection c) {
        super(SEPARATOR);
        if (c != null)
            super.addAll(c);
    }

    /**
     * Returns the canonical form of <code>suffix</code> or <code>null</code>
     * if the given suffix does not have a canonical form.
     * An example of the latter case is the empty string.
     */
    protected String canonicalize(String suffix) {
        assert suffix != null;
        assert suffix.indexOf(SEPARATOR) < 0 : "separator in suffix is illegal";
        if (suffix.length() > 0 && suffix.charAt(0) == PREFIX)
            suffix = suffix.substring(1);
        return suffix.length() > 0
                ? suffix.toLowerCase(Locale.ENGLISH)
                : null;
    }

    /**
     * Returns a case insensitive regular expression to match (file) paths
     * against the suffixes in this set.
     * If the regular expression matches, the matching suffix is captured as
     * the first matching group.
     * If this suffix set is empty, an unmatchable expression is returned.
     */
    public String toRegex() {
        final Iterator i = iterator();
        if (i.hasNext()) {
            final StringBuffer sb = new StringBuffer(".*\\.(?i)("); // NOI18N
            int c = 0;
            do {
                final String suffix = (String) i.next();
                if (c++ > 0)
                    sb.append('|'); // not SEPARATOR !!!
                sb.append("\\Q"); // NOI18N
                sb.append(suffix);
                sb.append("\\E"); // NOI18N
            } while (i.hasNext());
            assert c > 0;
            sb.append(")[\\" + File.separatorChar + "/]*");
            return sb.toString();
        } else {
            return "\\00"; // NOT "\00"! Effectively never matches anything. // NOI18N
        }
    }
}
