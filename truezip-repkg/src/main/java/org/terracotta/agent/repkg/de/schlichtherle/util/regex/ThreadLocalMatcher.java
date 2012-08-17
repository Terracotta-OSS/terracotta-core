/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * ThreadLocalMatcher.java
 *
 * Created on 24. Dezember 2005, 00:01
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

package org.terracotta.agent.repkg.de.schlichtherle.util.regex;

import java.util.regex.*;

/**
 * A thread local {@link Matcher}.
 * This class is intended to be used in multithreaded environments for high
 * performance pattern matching.
 *
 * @see #reset(CharSequence)
 * @version @version@
 * @since TrueZIP 6.5 (refactored from inner class in
 *        de.schlichtherle.io.DefaultArchiveDetector)
 */
public class ThreadLocalMatcher extends ThreadLocal {
    private final Pattern pattern;

    /**
     * Creates a new thread local matcher by compiling the given regex.
     *
     * @param regex The expression to be compiled.
     * @throws PatternSyntaxException If the expression's syntax is invalid.
     */
    public ThreadLocalMatcher(final String regex)
    throws PatternSyntaxException {
        this.pattern = Pattern.compile(regex);
    }

    /**
     * Creates a new thread local matcher by using the given pattern.
     *
     * @param pattern The pattern to be used.
     * @throws NullPointerException If the parameter is <code>null</code>.
     */
    public ThreadLocalMatcher(final Pattern pattern) {
        if (pattern == null)
            throw new NullPointerException();
        this.pattern = pattern;
    }

    protected Object initialValue() {
        return pattern.matcher(""); // NOI18N
    }

    /**
     * Resets the thread local matcher with the given character sequence and
     * returns it.
     */
    public Matcher reset(CharSequence input) {
        return ((Matcher) get()).reset(input);
    }

    /*public boolean matches() {
        return ((Matcher) tl.get()).matches();
    }

    public String group(int i) {
        return ((Matcher) tl.get()).group(i);
    }*/
}