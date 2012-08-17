/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * ChainableIOException.java
 *
 * Created on 30. Oktober 2004, 03:08
 */
/*
 * Copyright 2005 Schlichtherle IT Services
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

package org.terracotta.agent.repkg.de.schlichtherle.io;

import java.io.*;
import java.util.*;

/**
 * Represents a chain of {@link IOException}s.
 * This class supports chaining exceptions for reasons other than causes
 * (which is a functionality already provided by J2SE 1.4 and later).
 * A <code>ChainableIOException</code> can be used to implement an algorithm
 * which must be able to continue with some work although one or more
 * <code>IOException</code>s have occured.
 * <p>
 * For example, when looping through a list of files, an algorithm might
 * encounter an <code>IOException</code> when processing a file element in the list.
 * However, it may still be required to process the remaining files in the list
 * before actually throwing the corresponding <code>IOException</code>.
 * Hence, whenever this algorithm encounters an <code>IOException</code>,
 * it would catch the <code>IOException</code>, create a
 * <code>ChainableIOException</code> for it and continue processing the
 * remainder of the list.
 * Finally, at the end of the algorithm, if any <code>IOException</code>s
 * have occured, the <code>ChainableIOException</code> chain would get sorted
 * according to priority (see {@link #getPriority()} and
 * {@link #sortPriority()}) and finally thrown.
 * This would allow a client application to filter the exceptions by priority
 * with a simple try-catch statement, ensuring that no other exception of
 * higher priority is in the catched exception chain.
 * <p>
 * <b>Note:</b> This is not related to the cause concept of exceptions in
 * J2SE 1.4 and higher. Exceptions chained by this class are <b>not</b>
 * causes of each other, but have just been merely collected over time
 * and then thrown as one exception (list).
 * 
 * @see File#update
 * @see File#umount
 *
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.0 (generalized from the former
 *        <code>ArchiveControllerException</code>).
 */
public class ChainableIOException extends IOException implements Cloneable {

    /**
     * Compares two ZIP controller exceptions in descending order of their
     * priority.
     * If the priority is equal, the elements are compared in descending
     * order of their appearance.
     */
    // Note: Not private for unit testing purposes only!
    static final Comparator PRIORITY_COMP = new Comparator() {
        public int compare(Object o1, Object o2) {
            final int cmp = ((ChainableIOException) o1).getPriority()
                          - ((ChainableIOException) o2).getPriority();
            return cmp != 0 ? cmp : APPEARANCE_COMP.compare(o1, o2);
        }
    };
    
    /**
     * Compares two ZIP controller exceptions in descending order of their
     * appearance.
     */
    // Note: Not private for unit testing purposes only!
    static final Comparator APPEARANCE_COMP = new Comparator() {
        public int compare(Object o1, Object o2) {
            return ((ChainableIOException) o1).getAppearance()
                 - ((ChainableIOException) o2).getAppearance();
        }
    };

    private static int maxPrintExceptions = 3;
    
    /**
     * The tail chain of this exception chain.
     * Maybe <tt>null</tt> if there are no more exceptions.
     * If this exception chain has not been reordered,
     * the head of the tail is either <tt>null</tt> or an exception which
     * occured before this exception was created.
     * <p>
     * Please note that this is totally unrelated to a possible cause for
     * this exception!
     */
    private ChainableIOException prior;

    private final int appearance;

    int maxAppearance;

    /**
     * Constructs a new exception with the specified prior exception.
     *
     * @param  priorException An exception that happened before and that was
     *         caught - may be <tt>null</tt>.
     * 
     * @see ChainableIOException
     */
    public ChainableIOException(ChainableIOException priorException) {
        this(priorException, null, null);
    }

    /**
     * Constructs a new exception with the specified prior exception
     * and a message.
     *
     * @param  priorException An exception that happened before and that was
     *         caught - may be <tt>null</tt>.
     * @param  message The message for this exception.
     * 
     * @see ChainableIOException
     */
    public ChainableIOException(
            ChainableIOException priorException,
            String message) {
        this(priorException, message, null);
    }
    
    /**
     * Constructs a new exception with the specified prior exception and the
     * cause.
     *
     * @param  priorException An exception that happened before and that was
     *         caught - may be <tt>null</tt>.
     * @param  cause The cause (which is saved for later retrieval by the
     *         {@link #getCause()} method).  (A <tt>null</tt> value is
     *         permitted, and indicates that the cause is nonexistent or
     *         unknown.).
     * 
     * @see ChainableIOException
     */
    public ChainableIOException(
            ChainableIOException priorException,
            IOException cause) {
        this(priorException, null, cause);
    }

    /**
     * Constructs a new exception with the specified prior exception,
     * a message and a cause.
     *
     * @param  priorException An exception that happened before and that was
     *         caught - may be <tt>null</tt>.
     * @param  message The message for this exception.
     * @param  cause The cause (which is saved for later retrieval by the
     *         {@link #getCause()} method).  (A <tt>null</tt> value is
     *         permitted, and indicates that the cause is nonexistent or
     *         unknown.).
     * 
     * @see ChainableIOException
     */
    public ChainableIOException(
            final ChainableIOException priorException,
            final String message,
            final IOException cause) {
        super(message);
        this.prior = priorException;
        if (cause != null)
            initCause(cause);
        if (priorException != null)
            maxAppearance = priorException.maxAppearance + 1;
        else
            maxAppearance = 0;
        appearance = maxAppearance;
    }

    /**
     * Returns a <em>shallow</em> clone of this exception.
     */
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException cannotHappen) {
            throw new AssertionError(cannotHappen);
        }
    }

    /**
     * Returns the priority for this class of exception.
     * This should always return the same value for all instances of a
     * particular class.
     *
     * @return 0 for all instances of this class.
     */
    public int getPriority() {
        return 0;
    }

    /**
     * @return The order of appearance for this ZIP exception.
     */
    public final int getAppearance() {
        return appearance;
    }

    /**
     * @return The exception chain represented by the prior exception,
     *         or <code>null</code> if no prior exception exists.
     */
    public ChainableIOException getPrior() {
        return prior;
    }

    /**
     * Sorts the elements of this exception chain in descending order
     * of their priority.
     * If the priority of two elements is equal, they are sorted in descending
     * order of their appearance.
     *
     * @return The sorted exception chain, consisting of cloned elements where
     *         required to enforce the immutability of this class.
     *         This is a non-destructive sort, i.e. elements already in order
     *         are guaranteed not to get rearranged.
     *         If and only if all elements are in order, this exception chain
     *         is returned and no elements are cloned.
     */
    public ChainableIOException sortPriority() {
        return sort(PRIORITY_COMP);
    }
    
    /**
     * Sorts the elements of this exception chain in descending order
     * of their appearance.
     *
     * @return The sorted exception chain, consisting of cloned elements where
     *         required to enforce the immutability of this class.
     *         This is a non-destructive sort, i.e. elements already in order
     *         are guaranteed not to get rearranged.
     *         If and only if all elements are in order, this exception chain
     *         is returned and no elements are cloned.
     */
    public ChainableIOException sortAppearance() {
        return sort(APPEARANCE_COMP);
    }
    
    private ChainableIOException sort(final Comparator comp) {
        if (prior != null) {
            final ChainableIOException sortedPrior = prior.sort(comp);
            if (sortedPrior == prior && comp.compare(this, prior) >= 0)
                return this;
            else
                return sortedPrior.insert((ChainableIOException) clone(), comp);
        } else {
            return this;
        }
    }

    private ChainableIOException insert(
            final ChainableIOException element,
            final Comparator comp) {
        if (comp.compare(element, this) >= 0) {
            // Prepend to chain.
            element.prior = this;
            element.maxAppearance = Math.max(element.appearance, maxAppearance);
            return element;
        } else {
            // Insert element in the prior exception chain.
            final ChainableIOException clone
                    = (ChainableIOException) clone();
            if (prior != null) {
                clone.prior = prior.insert(element, comp);
                clone.maxAppearance = Math.max(clone.appearance, clone.prior.maxAppearance);
            } else {
                element.prior = null;
                clone.prior = element;
                clone.maxAppearance = element.maxAppearance;
            }
            return clone;
        }
    }

    /**
     * Calls {@link #initCause(IOException) initCause((IOException) cause)}.
     *
     * @throws ClassCastException If <code>cause</code> is not an instance
     *         of {@link IOException}.
     */
    public final Throwable initCause(final Throwable cause) {
        return initCause((IOException) cause);
    }

    public Throwable initCause(final IOException cause) {
        return super.initCause(cause);
    }

    /**
     * Prints up to {@link #getMaxPrintExceptions()} exceptions in this
     * chain to the provided {@link PrintStream}.
     * <p>
     * Exceptions are printed in ascending order of this chain.
     * If this chain has not been sorted, this results in the exceptions being
     * printed in order of their appearance.
     * <p>
     * If more exceptions are in this chain than are allowed to be printed,
     * then the printed message starts with a line indicating the number of
     * exceptions which have been omitted from the beginning of this chain.
     * Thus, this exception is always printed as the last exception in the
     * list.
     */
    public void printStackTrace(PrintStream s) {
        printStackTrace(s, getMaxPrintExceptions());
    }

    /**
     * Prints up to <code>maxExceptions()</code> exceptions in this
     * chain to the provided {@link PrintStream}.
     * <p>
     * Exceptions are printed in ascending order of this chain.
     * If this chain has not been sorted, this results in the exceptions being
     * printed in order of their appearance.
     * <p>
     * If more exceptions are in this chain than are allowed to be printed,
     * then the printed message starts with a line indicating the number of
     * exceptions which have been omitted from the beginning of this chain.
     * Thus, this exception is always printed as the last exception in the
     * list.
     */
    public void printStackTrace(final PrintStream s, int maxExceptions) {
        maxExceptions--;

        if (prior != null) {
            if (maxExceptions > 0) {
                prior.printStackTrace(s, maxExceptions);
                s.println("Followed, but not caused by:");
            } else {
                s.println("(Omitting " + prior.getNumExceptions() + " exception(s) at the start of this list)");
            }
        }

        super.printStackTrace(s);
    }

    private int getNumExceptions() {
        return prior != null ? prior.getNumExceptions() + 1 : 1;
    }

    /**
     * Prints up to {@link #getMaxPrintExceptions()} exceptions in this
     * chain to the provided {@link PrintStream}.
     * <p>
     * Exceptions are printed in ascending order of this chain.
     * If this chain has not been sorted, this results in the exceptions being
     * printed in order of their appearance.
     * <p>
     * If more exceptions are in this chain than are allowed to be printed,
     * then the printed message starts with a line indicating the number of
     * exceptions which have been omitted from the beginning of this chain.
     * Thus, this exception is always printed as the last exception in the
     * list.
     */
    public void printStackTrace(PrintWriter s) {
        printStackTrace(s, getMaxPrintExceptions());
    }

    /**
     * Prints up to <code>maxExceptions()</code> exceptions in this
     * chain to the provided {@link PrintStream}.
     * <p>
     * Exceptions are printed in ascending order of this chain.
     * If this chain has not been sorted, this results in the exceptions being
     * printed in order of their appearance.
     * <p>
     * If more exceptions are in this chain than are allowed to be printed,
     * then the printed message starts with a line indicating the number of
     * exceptions which have been omitted from the beginning of this chain.
     * Thus, this exception is always printed as the last exception in the
     * list.
     */
    public void printStackTrace(final PrintWriter s, int maxExceptions) {
        maxExceptions--;

        if (prior != null) {
            if (maxExceptions > 0) {
                prior.printStackTrace(s, maxExceptions);
                s.println("Followed, but not caused by:");
            } else {
                s.println("(Omitting " + prior.getNumExceptions() + " exception(s) at the start of this list)");
            }
        }

        super.printStackTrace(s);
    }

    /**
     * @see #printStackTrace(PrintStream)
     * @see #printStackTrace(PrintWriter)
     */
    public static int getMaxPrintExceptions() {
        return maxPrintExceptions;
    }
    
    /**
     * @see #printStackTrace(PrintStream)
     * @see #printStackTrace(PrintWriter)
     */
    public static void setMaxPrintExceptions(int maxPrintExcepions) {
        ChainableIOException.maxPrintExceptions = maxPrintExcepions;
    }
}
