/*
 Copyright (c) 2013 Nokia Corporation. All rights reserved.
 Nokia and Nokia Connecting People are registered trademarks of Nokia Corporation.
 Oracle and Java are trademarks or registered trademarks of Oracle and/or its
 affiliates. Other product and company names mentioned herein may be trademarks
 or trade names of their respective owners.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 - Redistributions of source code must retain the above copyright notice, this
 list of conditions and the following disclaimer.
 - Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation
 and/or other materials provided with the distribution.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.
 */
package org.tantalum;

import java.io.IOException;
import java.util.Vector;
import org.tantalum.storage.FlashCache;
import org.tantalum.storage.FlashDatabaseException;
import org.tantalum.storage.ImageCacheView;
import org.tantalum.security.CryptoUtils;
import org.tantalum.util.L;

/**
 * For each platform we support (JME, Android, ..) there is an implementation of
 * PlatformAdapter which hooks to the appropriate platform code.
 *
 * Note that by design this is a very minimal set of functions that provide
 * object persistence, logging, creating an image, UI thread access and network
 * access. Networking differences require one additional adapter, HttpConn.
 *
 * The easiest way to add support for an additional platforms is copy one of the
 * existing adapters and, well, adapt.
 *
 * @author phou
 */
public interface PlatformAdapter {

    /**
     * Initialize logging
     *
     * @param logMode
     */
    void init(int logMode);

    /**
     * Execute the run() method of the action on the platform's user interface
     * thread. The object will be queued by the platform and run soon, after
     * previously queued incoming events and tasks.
     *
     * Beware that calling this too frequently may make your application user
     * interface laggy (response after a long time). If your interface responds
     * slowly, check first that you are not doing slow processes on the UI
     * thread, then try to reduce the number of calls to this method.
     *
     * Unfortunately there is no visibility to how large the user interface
     * event queue has grown so this warning can not be automated.
     *
     * @param action
     */
    void runOnUiThread(final Runnable action);

    /**
     * The application calls this when all Workers have completed shutdown to
     * inform the phone that the program has closed itself.
     *
     * Do not call this directly, call Worker.shutdown() to initiate a close
     *
     */
    void shutdownComplete();

    /**
     * Get a platform-specific logging class. Add Tantalum logs to the
     * platform's own logs allows you to use filters, redirect from remote
     * device, and any any similar features as for example on Android. On JME we
     * provide USB debugging in a terminal on your PC.
     *
     * @return
     */
    L getLog();
    
    /**
     * Get a platform-specific implementation of the cryptography class.
     * 
     * Note that this is made platform dependent since cryptography implementation 
     * on some platforms were implemented independent of the default java.security 
     * package.
     * 
     * @return 
     */
    CryptoUtils getCryptoUtils();

    /**
     * Provide a return the converts from common Internet compressed byte[]
     * formats such as PNG and JPG into the platform's native Image object.
     *
     * Note that if you are using a UI framework such as LWUIT that replaces the
     * native image format with it's own wrapper, you should implement that in
     * your app. There is an example of this in the Tantalum LWUIT BBC Reader
     * app.
     *
     * @return
     */
    ImageCacheView getImageCacheView();

    /**
     * Vibrate the phone
     *
     * @param duration in milliseconds
     * @param timekeeperLambda code which is run just before vibration begins
     */
    void vibrateAsync(int duration, Runnable timekeeperLambda);

    /**
     * Return the platform-specific Image type as a resource decompressed from
     * the application JAR
     *
     * @param jarPathAndFilename
     * @return
     * @throws IOException
     */
    Object readImageFromJAR(String jarPathAndFilename) throws IOException;

    /**
     * Get a persistence support class. Each cache is identified by a single
     * character that is both the name of that cache and the priority. These are
     * guaranteed by runtime exceptions at cache creation time to be globally
     * unique within each application.
     *
     * @param priority
     * @param cacheType
     * @param startupTask
     * @return the existing or new cache
     * @throws FlashDatabaseException
     */
    FlashCache getFlashCache(char priority, int cacheType, FlashCache.StartupTask startupTask) throws FlashDatabaseException;

    /**
     * Delete the files or database associated with this cache. This may be
     * called during startup if an error is detected.
     *
     * @param priority
     * @param cacheType
     */
    void deleteFlashCache(char priority, int cacheType);

    /**
     * Create an HTTP PUT connection appropriate for this phone platform
     *
     * @param url
     * @param requestPropertyKeys
     * @param requestPropertyValues
     * @param bytes
     * @param requestMethod
     * @return
     * @throws IOException
     */
    PlatformUtils.HttpConn getHttpConn(String url, Vector requestPropertyKeys, Vector requestPropertyValues, byte[] bytes, String requestMethod) throws IOException;
}
