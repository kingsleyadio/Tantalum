/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.tantalum.j2me;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Vector;
import javax.microedition.io.CommConnection;
import javax.microedition.io.Connector;
import org.tantalum.util.L;

/**
 *
 * @author phou
 */
public class J2MELog extends L {

    private static OutputStream os = null;
//#mdebug
    private static final byte[] LFCR = "\n\r".getBytes();
    private static final Vector byteArrayQueue = new Vector();
    private static J2MELog.UsbWriter usbWriter = null;
    private static CommConnection comm = null;
//#enddebug    

    protected void printMessage(final String message, final boolean errorMessage) {
//#mdebug        
        if (os != null) {
            byteArrayQueue.addElement(message.getBytes());
            synchronized (L.class) {
                L.class.notifyAll();
            }
        } else {
            synchronized (L.class) {
                System.out.println(message);
            }
        }
//#enddebug        
    }

//#mdebug    
    protected void close() {

        if (usbWriter != null) {
            synchronized (L.class) {
                usbWriter.shutdownStarted = true;
                L.class.notifyAll();
            }

            // Give the queue time to flush final messages
            synchronized (usbWriter) {
                try {
                    if (!usbWriter.shutdownComplete) {
                        usbWriter.wait(1000);
                    }
                } catch (InterruptedException ex) {
                }
            }
        }
    }
//#enddebug

    protected void routeDebugOutputToUsbSerialPort() {
//#mdebug
        try {
            final String commPort = System.getProperty("microedition.commports");
            if (commPort != null) {
                comm = (CommConnection) Connector.open("comm:" + commPort);
                os = comm.openOutputStream();
                usbWriter = new J2MELog.UsbWriter();
                new Thread(usbWriter).start();
            }
        } catch (IOException ex) {
            System.out.println("Usb debug output error: " + ex);
        }
//#enddebug
    }

//#mdebug
    private static final class UsbWriter implements Runnable {

        boolean shutdownStarted = false;
        boolean shutdownComplete = false;

        public void run() {
            try {
                while (!shutdownStarted || !byteArrayQueue.isEmpty()) {
                    synchronized (L.class) {
                        if (byteArrayQueue.isEmpty()) {
                            L.class.wait(1000);
                        }
                    }
                    while (!byteArrayQueue.isEmpty()) {
                        os.write((byte[]) byteArrayQueue.firstElement());
                        byteArrayQueue.removeElementAt(0);
                        os.write(LFCR);
                    }
                    os.flush();
                }
            } catch (Exception e) {
            } finally {
                try {
                    os.close();
                } catch (IOException ex) {
                }
                os = null;
                try {
                    comm.close();
                } catch (IOException ex) {
                }
                synchronized (this) {
                    shutdownComplete = true;
                    this.notifyAll(); // All done- let shutdown() proceed
                }
            }
        }
    }
//#enddebug    
}