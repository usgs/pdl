/*
 * FileListenerInterface
 *
 * $Id$
 * $HeadURL$
 */
package gov.usgs.util;

import java.io.File;

/**
 * An object that listens for files.
 * 
 * Typically used with a DirectoryPoller for handling files.
 */
public interface FileListenerInterface {

    /**
     * Called with any files to be processed.
     * 
     * @param file
     *            file to be processed.
     */
    public void onFile(final File file);

}
