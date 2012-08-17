/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * UncacheableIOException.java
 *
 * Created on 19. November 2006, 19:48
 */

package org.terracotta.agent.repkg.de.schlichtherle.io.archive.spi;


import java.io.IOException;

import org.terracotta.agent.repkg.de.schlichtherle.io.File;

/**
 * May be thrown by archive drivers to indicate that an {@link IOException}
 * occured as a transient event when accessing an archive file and another
 * try to access the same archive file may finally succeed.
 * On the other hand, if the archive controller catches an IOException from
 * an an archive driver when trying to access an archive file which is
 * <em>not</em> a <code>TransientIOException</code>, then the archive
 * controller may consider the archive file to be a false positive and cache
 * the exception until {@link File#umount} or {@link File#update} is called.
 * <p>
 * This feature is primarily used by the RAES encrypted ZIP file driver
 * family when prompting for passwords has been cancelled by the user.
 * In this case, the driver will wrap the <code>IOException</code> in a
 * <code>TransientIOException</code> and throw this instead to signal that
 * another attempt to prompt the user should be allowed.
 * <p>
 * This class is marked final since the archive controller will throw
 * away this exception anyway and deal with the transient cause only.
 *
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.4
 */
public final class TransientIOException extends IOException {

    /**
     * @param cause The transient cause of this exception.
     * @throws NullPointerException If <code>cause</code> is <code>null</code>.
     */
    public TransientIOException(IOException cause) {
        if (cause == null)
            throw new NullPointerException();
        super.initCause(cause);
    }

    /**
     * Returns the transient cause of this exception as an
     * <code>IOException</code> - <code>null</code> is never returned.
     */
    public IOException getTransientCause() {
        return (IOException) getCause();
    }
}
