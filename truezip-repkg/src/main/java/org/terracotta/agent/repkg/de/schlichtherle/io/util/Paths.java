/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * Copyright 2006 Schlichtherle IT Services
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

import java.io.File;

/**
 * Utility methods for file path names.
 *
 * @deprecated This class will vanish in the next release.
 * @author Christian Schlichtherle
 * @version @version@
 */
public class Paths {

    /**
     * Equivalent to {@link #normalize(String, char)
     * normalize(path, File.separatorChar)}.
     */
    public static final String normalize(
            final String path) {
        return normalize(path, File.separatorChar);
    }

    /**
     * Removes all redundant separators, dot directories
     * (<code>&quot;.&quot;</code>) and dot-dot directories
     * (<code>&quot;..&quot;</code>) from the path and returns the result.
     * An empty path results in <code>&quot;.&quot;</code>.
     * On Windows, a path may be prefixed by a drive letter followed by a
     * colon.
     * On all platforms, a path may be prefixed by two leading separators
     * to indicate a UNC, although this is currently supported on Windows
     * only.
     * A single trailing separator character is always retained if present.
     * 
     * @param path The path to normalize.
     * @param separatorChar The path separator.
     * @return <code>path</code> if it was already in normalized form.
     *         Otherwise, a new String with the normalized form of the given
     *         path.
     * @throws NullPointerException If path is <code>null</code>.
     */
    public static String normalize(
            final String path,
            final char separatorChar) {
        final int prefixLen = prefixLength(path, separatorChar);
        final int pathLen = path.length();
        final StringBuffer buffer = new StringBuffer(pathLen);
        normalize(path.substring(prefixLen, pathLen), separatorChar, 0, pathLen - prefixLen, buffer);
        buffer.insert(0, path.substring(0, prefixLen));
        if (buffer.length() == prefixLen
                && (prefixLen <= 0 || buffer.charAt(prefixLen - 1) != separatorChar))
            buffer.append('.');
        if (pathLen > 0 && path.charAt(pathLen - 1) == separatorChar)
            if (buffer.charAt(buffer.length() - 1) != separatorChar)
                buffer.append(separatorChar); // retain trailing separator
        final String result = buffer.toString();
        return path.equals(result) ? path : result;
    }

    /**
     * Removes all redundant separators, dot directories
     * (<code>&quot;.&quot;</code>) and dot-dot directories
     * (<code>&quot;..&quot;</code>) from the path and collects the result
     * in a string buffer.
     * This is a recursive call: The top level call should provide
     * <code>0</code> as the <code>skip</code> parameter, the length
     * of the path as the <code>end</code> parameter and an empty string
     * buffer as the <code>result</code> parameter.
     * 
     * @param path The path to normalize. A leading separator is ignored.
     *        <code>null</code> is not allowed.
     * @param separatorChar The path separator.
     * @param skip The number of elements in the path to skip because they are
     *        followed by a dot-dot directory.
     *        Must not be negative.
     * @param end Only the string to the left of this index in
     *        <code>path</code> is considered.
     *        If not positive, nothing happens.
     * @param result The string buffer with the collected results.
     *        <code>null</code> is not allowed.
     * @return The number of elements in the path actually skipped.
     *         This is always lesser than or equal to the value of the
     *         parameter <code>skip</code>.
     */
    private static int normalize(
            final String path,
            final char separatorChar,
            final int skip,
            final int end,
            final StringBuffer result) {
        assert skip >= 0;
        if (end <= 0)
            return 0;

        final int next = path.lastIndexOf(separatorChar, end - 1);
        final String base = path.substring(next + 1, end);
        final int skipped;
        if (base.length() == 0 || ".".equals(base)) {
            return normalize(path, separatorChar, skip, next, result);
        } else if ("..".equals(base)) {
            final int toSkip = skip + 1;
            skipped = normalize(path, separatorChar, toSkip, next, result);
            assert skipped <= toSkip;
            if (skipped == toSkip)
                return skip;
        } else if (skip > 0) {
            return normalize(path, separatorChar, skip - 1, next, result) + 1;
        } else {
            assert skip == 0;
            skipped = normalize(path, separatorChar, skip, next, result);
            assert skipped == 0;
        }

        final int resultLen = result.length();
        if (resultLen > 0 && result.charAt(resultLen - 1) != separatorChar)
            result.append(separatorChar);
        result.append(base);
        return skipped;
    }

    /**
     * Cuts off any separator characters at the end of the path, unless the
     * path contains of only separator characters, in which case a single
     * separator character is retained to denote the root directory.
     *
     * @return <code>path</code> if it's a path without trailing separators
     *         or contains the separator only.
     *         Otherwise, the substring until the first of at least one
     *         separating characters is returned.
     * @throws NullPointerException If path is <code>null</code>.
     */
    public final static String cutTrailingSeparators(
            final String path,
            final char separatorChar) {
        int i = path.length();
        if (i <= 0 || path.charAt(--i) != separatorChar)
            return path;
        while (i > 0 && path.charAt(--i) == separatorChar)
            ;
        return path.substring(0, ++i);
    }

    /**
     * Cuts off a trailing separator character of the pathname, unless the
     * pathname contains of only the separator character (i.e. denotes the
     * root directory).
     *
     * @deprecated This method chops off a single trailing separator only.
     *             Use {@link #cutTrailingSeparators} to chop off multiple
     *             trailing separators.
     * @return <code>path</code> if it's a path without a trailing separator
     *         or contains the separator only.
     *         Otherwise, the substring up to the last character is returned.
     * @throws NullPointerException If path is <code>null</code>.
     */
    public final static String cutTrailingSeparator(
            final String path,
            final char separatorChar) {
        final int pathEnd = path.length() - 1;
        if (pathEnd > 0 && path.charAt(pathEnd) == separatorChar)
            return path.substring(0, pathEnd);
        else
            return path;
    }

    /**
     * Equivalent to {@link #split(String, char)
     * split(path, File.separatorChar)}.
     */
    public static final String[] split(
            final String path) {
        return split(path, File.separatorChar);
    }

    /**
     * Splits a path into its parent path and its base name,
     * recognizing platform specific file system roots.
     * 
     * 
     * @param path The name of the path which's parent path and base name
     *        are to be returned.
     * @param separatorChar The path separator to use for this operation.
     * @return An array of at least two strings:
     *         <ol>
     *         <li>Index 0 holds the parent path or <code>null</code> if the
     *             path does not specify a parent. This name compares equal
     *             with {@link java.io.File#getParent()}, except that
     *             redundant separators left of the parent path's base name
     *             are kept (base.e. empty path elements between two separators
     *             left of the parent path's base name are not removed).</li>
     *         <li>Index 1 holds the base name. This name compares
     *             equal with {@link java.io.File#getName()}.</li>
     *         </ol>
     * @throws NullPointerException If path is <code>null</code>.
     */
    public static final String[] split(
            final String path,
            final char separatorChar) {
        return split(path, separatorChar, new String[2]);
    }

    /**
     * Same as {@link #split(String, char)}, but uses the given array
     * <code>split</code> to store the result.
     *
     * @param split An array of at least two String elements to hold the
     *        result.
     * @return <code>split</code>
     */
    public static String[] split(
            final String path,
            final char separatorChar,
            final String[] split) {
        final int prefix = prefixLength(path, separatorChar);

        // Skip any trailing separators and look for the previous separator.
        int base = -1;
        int end = path.length() - 1;
        if (prefix <= end) {
            end = lastIndexNot(path, separatorChar, end);
            base = path.lastIndexOf(separatorChar, end);
        }
        end++; // convert end index to interval boundary

        // Finally split according to our findings.
        if (base >= prefix) { // found separator after the prefix?
            int j = lastIndexNot(path, separatorChar, base) + 1;
            split[0] = path.substring(0, j > prefix ? j : prefix);
            split[1] = path.substring(base + 1, end);
        } else { // no separator
            if (0 < prefix && prefix < end) // prefix exists and we have more?
                split[0] = path.substring(0, prefix); // prefix is parent
            else
                split[0] = null;                      // no parent
            split[1] = path.substring(prefix, end);
        }

        return split;
    }

    /**
     * Returns the length of the file system prefix in <code>path</code>.
     * File system prefixes are:
     * <ol>
     * <li>Two leading separators.
     *     On Windows, this is the notation for a UNC.
     * <li>A letter followed by a colon and optionally a separator.
     *     On Windows, this is the notation for a drive.
     * </ol>
     * This method works the same on all platforms, so even if the separator
     * is <code>'/'</code>, two leading separators would be considered to
     * be a UNC and hence the return value would be <code>2</code>.
     *
     * @param path The file system path.
     * @param separatorChar The separator character.
     * @return The number of characters in the prefix.
     * @throws NullPointerException If <code>path</code> is <code>null</code>.
     */
    private static int prefixLength(final String path, final char separatorChar) {
        final int pathLen = path.length();
        int len = 0; // default prefix length
        if (pathLen > 0 && path.charAt(0) == separatorChar) {
            len++; // leading separator or first character of a UNC.
        } else if (pathLen > 1 && path.charAt(1) == ':') {
            final char drive = path.charAt(0);
            if ('A' <= drive && drive <= 'Z'
                    || 'a' <= drive && drive <= 'z') { // US-ASCII letters only
                // Path is prefixed with drive, e.g. "C:\\Programs".
                len = 2;
            }
        }
        if (pathLen > len && path.charAt(len) == separatorChar)
            len++; // leading separator is considered part of prefix
        return len;
    }

    private static final int lastIndexNot(String path, char separatorChar, int last) {
        while (path.charAt(last) == separatorChar && --last >= 0)
            ;
        return last;
    }
    
    /** You cannot instantiate this class. */
    protected Paths() {
    }
}
