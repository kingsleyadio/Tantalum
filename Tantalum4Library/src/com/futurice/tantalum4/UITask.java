/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.futurice.tantalum4;

import com.futurice.tantalum4.log.L;

/**
 *
 * @author phou
 */
public abstract class UITask extends Task implements Runnable {

    public final void run() {
        try {
            onPostExecute(getResult());
        } catch (Throwable t) {
            L.e("UITask onPostExecute uncaught error", this.toString(), t);
        } finally {
            if (this.status < UI_RUN_FINISHED) {
                setStatus(UI_RUN_FINISHED);
            }
        }
    }

    /**
     * You may optionally override this method if you wish to perform work on a
     * Worker thread before proceeding to the UI thread.
     *
     * @param in
     * @return
     */
    public Object doInBackground(final Object in) {
        return in;
    }

    /**
     * Override this method with the work you want to complete on the UI thread
     * after the Task is complete on the Worker thread.
     *
     * @param result
     */
    protected abstract void onPostExecute(Object result);
}