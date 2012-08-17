/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * AbstractArchiveDriver.java
 *
 * Created on 3. April 2006, 19:04
 */
/*
 * Copyright 2006-2007 Schlichtherle IT Services
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

package org.terracotta.agent.repkg.de.schlichtherle.io.archive.spi;


import java.io.*;
import java.nio.charset.*;
import java.util.*;

import javax.swing.*;

import org.terracotta.agent.repkg.de.schlichtherle.io.archive.*;

/**
 * An abstract archive driver implementation to ease the task of developing
 * an archive driver.
 * It provides default implementations for character sets and icon handling.
 * <p>
 * Since TrueZIP 6.4, this class is serializable in order to meet the
 * requirements of the {@link org.terracotta.agent.repkg.de.schlichtherle.io.File} class.
 * 
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.0
 */
public abstract class AbstractArchiveDriver
        implements ArchiveDriver, Serializable {
    private static final long serialVersionUID = 6546816846546846516L;

    private final String charset;

    private final Icon openIcon, closedIcon;

    /**
     * This field should be considered to be <code>final</code>!
     *
     * @see #ensureEncodable
     */
    private transient ThreadLocalEncoder encoder; // never transmit this over the wire!

    /**
     * Constructs a new abstract archive driver.
     *
     * @param charset The name of a character set to use by default for all
     *        entry names and probably other meta data when reading or writing
     *        archive files.
     * @param openIcon The icon to return by {@link #getOpenIcon}.
     *        May be <code>null</code>.
     * @param closedIcon The icon to return by {@link #getClosedIcon}.
     *        May be <code>null</code>.
     * @throws NullPointerException If <code>charset</code> is
     *         <code>null</code>.
     * @throws UnsupportedCharsetException If <code>charset</code> is not
     *         supported by both the JSE 1.1 API and JSE 1.4 API.
     * @throws InconsistentCharsetSupportError If <code>charset</code> is
     *         supported by the JSE 1.1 API, but not the JSE 1.4 API,
     *         or vice versa.
     */
    protected AbstractArchiveDriver(
            final String charset,
            final Icon openIcon,
            final Icon closedIcon) {
        this.charset = charset;
        this.encoder = new ThreadLocalEncoder();
        this.openIcon = openIcon;
        this.closedIcon = closedIcon;

        // Perform fail fast tests for character set charsets using both
        // JSE 1.1 API and the NIO API.
        UnsupportedEncodingException uee = testJSE11Support(charset);
        UnsupportedCharsetException  uce = testJSE14Support(charset);
        if (uee != null || uce != null) {
            if (uee == null)
                throw new InconsistentCharsetSupportError(charset, uce);
            if (uce == null)
                throw new InconsistentCharsetSupportError(charset, uee);
            throw uce; // throw away uee - it has same reason
        }

        assert invariants();
    }

    private static UnsupportedEncodingException testJSE11Support(String charset) {
        try {
            new String(new byte[0], charset);
        } catch (UnsupportedEncodingException ex) {
            return ex;
        }
        return null;
    }

    private static UnsupportedCharsetException testJSE14Support(String charset) {
        try {
            Charset.forName(charset);
        } catch (UnsupportedCharsetException ex) {
            return ex;
        }
        return null;
    }

    /**
     * Postfixes the instance after its default deserialization.
     *
     * @throws InvalidObjectException If the instance invariants are not met.
     */
    private void readObject(final ObjectInputStream in)
    throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        assert encoder == null;
        encoder = new ThreadLocalEncoder();

        try {
            invariants();
        } catch (AssertionError ex) {
            throw (InvalidObjectException) new InvalidObjectException(ex.toString()).initCause(ex);
        }
    }

    /**
     * Checks the invariants of this class and throws an AssertionError if
     * any is violated even if assertion checking is disabled.
     * <p>
     * The constructors call this method like this:
     * <pre>{@code assert invariants(); }</pre>
     * This calls the method if and only if assertions are enabled in order
     * to assert that the instance invariants are properly obeyed.
     * If assertions are disabled, the call to this method is thrown away by
     * the HotSpot compiler, so there is no performance penalty.
     * <p>
     * When deserializing however, this method is called regardless of the
     * assertion status. On error, the {@link AssertionError} is wrapped
     * in an {@link InvalidObjectException} and thrown instead.
     *
     * @throws AssertionError If any invariant is violated even if assertions
     *         are disabled.
     * @return <code>true</code>
     */
    private boolean invariants() {
        if (charset == null)
            throw new AssertionError("character set not initialized");
        try {
            ensureEncodable("");
        } catch (CharConversionException ex) {
            throw new AssertionError(ex);
        }
        return true;
    }

    /**
     * Ensures that the given entry name is representable in this driver's
     * character set charset.
     * Should be called by sub classes in their implementation of the method
     * {@link ArchiveDriver#createArchiveEntry}.
     * 
     * @param entryName A valid archive entry name - <code>null</code> is not
     *        permissible.
     * @see #getCharset
     * @see <a href="ArchiveEntry.html#entryName">Requirements for Archive Entry Names</a>
     * @throws CharConversionException If the entry name contains characters
     *         which cannot get encoded.
     */
    protected final void ensureEncodable(String entryName)
    throws CharConversionException {
        if (!encoder.canEncode(entryName))
            throw new CharConversionException(entryName +
                    " (illegal characters in entry name)");
    }

    /**
     * Returns the value of the property <code>charset</code> which was 
     * provided to the constructor.
     */
    public final String getCharset() {
        return charset;
    }

    /** @deprecated Use {@link #getCharset} instead. */
    public final String getEncoding() {
        return charset;
    }

    /**
     * Returns the value of the property <code>openIcon</code> which was 
     * provided to the constructor.
     *
     * @param archive Ignored.
     */
    public final Icon getOpenIcon(Archive archive) {
        return openIcon;
    }

    /**
     * Returns the value of the property <code>closedIcon</code> which was 
     * provided to the constructor.
     *
     * @param archive Ignored.
     */
    public final Icon getClosedIcon(Archive archive) {
        return closedIcon;
    }

    private final class ThreadLocalEncoder extends ThreadLocal {
        protected Object initialValue() {
            return Charset.forName(charset).newEncoder();
        }

        boolean canEncode(CharSequence cs) {
            return ((CharsetEncoder) get()).canEncode(cs);
        }
    }

    /**
     * Thrown to indicate that the character set implementation in the Java
     * Runtime Environment (JRE) for the Java Standard Edition (JSE) is broken
     * and needs fixing.
     * <p>
     * This error is thrown if and only if the character set provided to the
     * constructor of the enclosing class is either supported by the JSE 1.1
     * style API ({@link String#String(byte[], String)}), but not the JSE 1.4
     * style API ({@link Charset#forName(String)}), or vice versa.
     * This implies that this error is <em>not</em> thrown if the character
     * set is consistently supported or not supported by both APIs!
     * <p>
     * Most of the time, this error happens when accessing regular ZIP files.
     * The respective archive drivers require &quot;IBM437&quot; as the
     * character set.
     * Unfortunately, this character set is optional and Sun's JSE
     * implementations usually only install it if the JSE has been fully
     * installed. Its provider is then located in
     * <i>$JAVA_HOME/lib/charsets.jar</i>, where <i>$JAVA_HOME</i> is the
     * path name of the installed JRE.
     * <p>
     * To ensure that &quot;IBM437&quot; is always available regardless of
     * the JRE installation, TrueZIP provides its own, thoroughly tested
     * provider for this charset.
     * This provider is configured in
     * <i>truezip.jar/META-INF/services/java.nio.charset.spi.CharsetProvider</i>.
     * So you should actually never see this happening (cruel world - sigh...).
     * <p>
     * Because the detected inconsistency would cause subtle bugs in archive
     * drivers and may affect other applications, too, it needs fixing.
     * Your options in order of preference:
     * <ol>
     * <li>Upgrade to a more recent JRE or reinstall it.
     *     When asked during installation, make sure to do a "full install".
     * <li>Fix the JRE by copying <i>$JAVA_HOME/lib/charsets.jar</i> from some
     *     other distribution.
     * </ol>
     * This should ensure that $JAVA_HOME/lib/charsets.jar is present in the
     * JRE, which contains the provider for the &quot;IBM437&quot; character
     * set.
     * Although this should not be necessary due to TrueZIP's own provider,
     * this seems to fix the issue.
     * <p>
     * This error class has protected visibility solely for the purpose of
     * documenting it in the Javadoc.
     * In order to prevent you from catching or throwing it, the class
     * is final and its constructor is private.
     */
    protected static final class InconsistentCharsetSupportError extends Error {
        private InconsistentCharsetSupportError(String charset, Exception cause) {
            super(message(charset, cause), cause);
        }

        private static String message(  final String charset,
                                        final Exception cause) {
            assert cause instanceof UnsupportedEncodingException
                || cause instanceof UnsupportedCharsetException;
            final String[] api = cause instanceof UnsupportedEncodingException
                    ? new String[] { "J2SE 1.4", "JSE 1.1" }
                    : new String[] { "JSE 1.1", "J2SE 1.4" };
            return "The character set '" + charset
                    + "' is supported by the " + api[0]
                    + " API, but not the " + api[1] + " API."
                    + "\nThis requires fixing the Java Runtime Environment!"
                    + "\nPlease read the Javadoc of this error class for more information.";
        }
    }
}
