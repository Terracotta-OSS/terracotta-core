/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * ArchiveEntryStreamClosedException.java
 *
 * Created on 8. Maerz 2006, 21:07
 */

package org.terracotta.agent.repkg.de.schlichtherle.io;

import java.io.*;

/**
 * Thrown if an input or output stream for an archive entry has been forced to
 * close when the archive file was (explicitly or implicitly) unmounted.
 *
 * @see <a href="package-summary.html#streams">Using Archive Entry Streams</a>
 * @see File#umount
 * @see File#update
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.0
 */
public class ArchiveEntryStreamClosedException extends IOException {
    
    // TODO: Make this package private!
    public ArchiveEntryStreamClosedException() {
        super("entry stream has been forced to close() when the archive file was unmounted");
    }
}
