/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * ContainsFileException.java
 *
 * Created on 4. November 2006, 12:26
 */

package org.terracotta.agent.repkg.de.schlichtherle.io;

import java.io.FileNotFoundException;

/**
 * Thrown if two paths are referring to the same file or contain each other.
 * This exception is typically thrown from {@link File#cp}.
 *
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.4
 */
public class ContainsFileException extends FileNotFoundException {

    private final java.io.File ancestor, descendant;

    /**
     * Creates a new instance of <code>ContainsFileException</code>.
     */
    public ContainsFileException(
            final java.io.File ancestor,
            final java.io.File descendant) {
        super("Paths refer to the same file or contain each other!");
        this.ancestor = ancestor;
        this.descendant = descendant;
    }

    public java.io.File getAncestor() {
        return ancestor;
    }

    public java.io.File getDescendant() {
        return descendant;
    }
}
