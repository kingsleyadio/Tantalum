/*
 Copyright (c) 2013, Paul Houghton and Futurice Oy
 All rights reserved.

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

import java.util.Vector;
import org.tantalum.util.L;

/**
 * A Task is the base unit of work in the Tantalum toolset. Any piece of code
 * which runs in response to an event and does not explicitly need to be run on
 * the user interface (UI) thread is usually implemented in an extension of
 * Task.exec(). It will automatically be executed on a background worker thread
 * once fork() has been called.
 *
 * @author phou
 */
public abstract class Task {

    /**
     * Start the task as soon as possible. The
     * <code>fork()</code> operation will place this as the next Task to be
     * completed unless subsequent
     * <code>HIGH_PRIORITY</code> fork operations occur before a Worker start
     * execution.
     *
     * This is the normal priority for user interface tasks where the user
     * expects a fast response regardless of previous incomplete requests they
     * may have made.
     */
    public static final int HIGH_PRIORITY = 2;
    /**
     * Start execution after any previously
     * <code>fork()</code>ed work, first in is usually first out, however
     * multiple Workers in parallel means that execution start and completion
     * order is not guaranteed.
     */
    public static final int NORMAL_PRIORITY = 0;
    /**
     * All items with are executed in guaranteed sequence on a single background
     * worker thread. This is normally used for persistence operations like
     * flash write. Note that in most other cases it is preferable for you to
     * sequence items explicitly within a single Task or by the sequence with
     * which one
     * <code>Task</code> chains to or forks other
     * <code>Task</code>s.
     */
    public static final int SERIAL_PRIORITY = -1;
    /**
     * Start execution if there is nothing else for the Workers to do. At least
     * one Worker will always be left idle for immediate activation if only
     * <code>LOW_PRIORITY</code> work is queued for execution. This is intended
     * for background tasks such as pre-fetch and pre-processing of data that
     * doe not affect the current user view.
     */
    public static final int LOW_PRIORITY = -2;
    /**
     * Start execution when
     * <code>PlatformUtils.getInstance().shutdown()</code> is called, and do not
     * exit the program until all such shutdown tasks are completed.
     *
     * Note that if shutdown takes too long and the phone is telling the
     * application to exit, then the phone may give the application a limited
     * time window (typically around 3 seconds) to complete all shutdown
     * activities before killing the application. Shutdown time can be unlimited
     * if the application initiates the exit as by clicking an "Exit" button in
     * the application, but since this can never be guaranteed to be the only
     * shutdown sequence, you must design for quick shutdown.
     */
    public static final int SHUTDOWN_PRIORITY = -3;
    /**
     * Synchronize on the following object if your processing routine will
     * temporarily need a large amount of memory. Only one such activity can be
     * active at a time.
     */
    public static final Object LARGE_MEMORY_MUTEX = new Object();
    // status values
    /**
     * For join(), if a timeout is not specified, no thread can wait more than 2
     * minutes.
     */
    public static final int MAX_TIMEOUT = 120000; // 
    /**
     * status: created and forked for execution by a background Worker thread,
     * but no Worker has yet been free to accept this queued Task
     */
    public static final int EXEC_PENDING = 0;
    /**
     * status: exec() has started but not yet finished
     */
    public static final int EXEC_STARTED = 1;
    /**
     * status: exec() has finished. If this is a UI thread there still may be
     * pending activity for the user interface thread
     */
    public static final int EXEC_FINISHED = 2;
    /**
     * status: both Worker thread exec() and UI thread onPostExecute() have
     * completed
     */
    public static final int UI_RUN_FINISHED = 3;
    /**
     * state: cancel() was called
     */
    public static final int CANCELED = 4;
    /**
     * status: an uncaught exception was thrown during execution
     */
    public static final int EXCEPTION = 5;
    /**
     * status: the Task has been created, but fork() has not yet been called to
     * queue this for execution on a background Worker thread
     */
    public static final int READY = 6;
    private static final String[] STATUS_STRINGS = {"EXEC_PENDING", "EXEC_STARTED", "EXEC_FINISHED", "UI_RUN_FINISHED", "CANCELED", "EXCEPTION", "READY"};
    /**
     * Task will run without interruption or dequeue during shutdown
     */
    public static final int EXECUTE_NORMALLY_ON_SHUTDOWN = 0;
    /**
     * Tasks which have not yet started are removed from the task queue when an
     * application shutdown begins.
     */
    public static final int DEQUEUE_ON_SHUTDOWN = 1;
    /**
     * If a Task is still queued but not yet started, you can request
     * notification of the shutdown in order to complete some cleanup activity.
     * Note that you should usually not be holding resources when queued, so
     * cleanup of that form is probably a design error. You may however want to
     * briefly notify the user.
     *
     * An alternative to this is to create a Task that is run at shutdown time
     * using Worker.queueShutdownTask(Task).
     */
    public static final int DEQUEUE_BUT_LEAVE_RUNNING_IF_ALREADY_STARTED_ON_SHUTDOWN = 2;
    /**
     * Default shutdown behavior.
     *
     * If you explicitly do not want your Task to be cancel(true)ed during
     * shutdown or silently cancel(false)ed when it is queued, use this shutdown
     * behavior. This is useful for example if you need to complete an ongoing
     * Flash memory write operation.
     *
     * An alternative to this is to create a Task that is run at shutdown time
     * using Worker.queueShutdownTask(Task).
     */
    public static final int DEQUEUE_OR_CANCEL_ON_SHUTDOWN = 3;
    private Object value; // Always access within a synchronized block
    /**
     * The current execution state, one of several predefined constants
     */
    protected int status = READY; // Always access within a synchronized block
    /**
     * All tasks are removed from the queue without notice automatically on
     * shutdown unless specifically marked. For example, writing to flash memory
     * may be required during shutdown, but HttpGetter could block or a long
     * time and should be cancel()ed during shutdown to speed application close.
     */
    private int shutdownBehaviour = DEQUEUE_OR_CANCEL_ON_SHUTDOWN;
    /**
     * The next Task to be executed after this Task completes successfully. If
     * the current task is canceled or throws an exception, the chainedTask(s)
     * will have cancel() called to notify that they will not execute.
     */
    private Task chainedTask = null; // Run afterwords, passing output as input parameter
    private final Object MUTEX = new Object();

    /**
     * Create a Task with input value of null
     *
     * Use this constructor if your Task does not accept an input value,
     * otherwise use the Task(Object) constructor.
     *
     */
    public Task() {
        value = null;
    }

    /**
     * Create a Task with the specified input value.
     *
     * The default action is for the output value to be the same as the input
     * value, however many Tasks will return their own value.
     *
     * @param in
     */
    public Task(final Object in) {
        value = in;
    }

    /**
     * Return the currently set shutdown behavior
     *
     * @return
     */
    public final int getShutdownBehaviour() {
        synchronized (MUTEX) {
            return shutdownBehaviour;
        }
    }

    /**
     * Override the default shutdown behavior
     *
     * If for example you want an ongoing task to finish if it has already
     * started when the shutdown signal comes, but be removed from the queue if
     * it has not yet started, set to Task.DEQUEUE_ON_SHUTDOWN
     *
     * Note that items passed to Worker.queueShutdownTask() ignore this value
     * and will all run normally to completion during shutdown unless the
     * platform (not the application) initiated the shutdown and slow shutdown
     * Tasks result in a shutdown times-out. If this occurs during persistent
     * state write operations, behavior is unpredictable, so it is best to write
     * state as-you-go so that you can shut down quickly.
     *
     * @param shutdownBehaviour
     * @return
     */
    public final Task setShutdownBehaviour(final int shutdownBehaviour) {
        synchronized (MUTEX) {
            if (shutdownBehaviour < Task.EXECUTE_NORMALLY_ON_SHUTDOWN || shutdownBehaviour > Task.DEQUEUE_OR_CANCEL_ON_SHUTDOWN) {
                throw new IllegalArgumentException("Invalid shutdownBehaviour value: " + shutdownBehaviour);
            }

            this.shutdownBehaviour = shutdownBehaviour;

            return this;
        }
    }

    /**
     * Check status of the object to ensure it can be queued at this time (it is
     * not already queued and running)
     *
     * @throws IllegalStateException if the task is currently queued or
     * currently running
     */
    public final void notifyTaskForked() throws IllegalStateException {
        synchronized (MUTEX) {
            if (status < EXEC_FINISHED || (this instanceof Runnable && status < UI_RUN_FINISHED)) {
                throw new IllegalStateException("Task can not be re-forked, wait for previous exec to complete: status=" + getStatusString());
            }
            setStatus(EXEC_PENDING);
        }
    }

    /**
     * Get the current input or result value of this Task without forcing
     * execution.
     *
     * If the task has not yet been executed, this will be the input value. If
     * the task has been executed, this will be the return value.
     *
     * @return
     */
    public final Object getValue() {
        synchronized (MUTEX) {
            return value;
        }
    }

    /**
     * Execute synchronously if needed and return the resulting value.
     *
     * This is similar to join() with a very long timeout. Note that a
     * MAX_TIMEOUT of 2 minutes is enforced.
     *
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws CancellationException
     * @throws TimeoutException
     */
    public final Object get() throws InterruptedException, ExecutionException, CancellationException, TimeoutException {
        return join(MAX_TIMEOUT);
    }

    /**
     * Override the current value of the task with a new value
     *
     * Note that this is not used in normal operations. Usually you set the
     * value when creating the task, or pass in the value as an argument from
     * the previous Task in a chain. It can be useful to update a Task not yet
     * forked, or to override the normal result of a Task.
     *
     * Because you face a race condition when setting the value on Task which is
     * currently forked or already running, you will receive an
     * IllegalStateException. Only use this method before forking and after
     * execution has completed.
     *
     * @param value
     */
    public final void set(final Object value) {
        synchronized (MUTEX) {
            if (status < AsyncTask.EXEC_FINISHED) {
                throw new IllegalStateException("Unpredictable run sequence- can not set Task value when status is " + status);
            }

            this.value = value;
        }
    }

    /**
     * Set the return value of this task during or at the end of execution.
     *
     * Note that although you can use this to set or override the initial input
     * value of a task before fork()ing it, it is more clear and preferred to
     * use the Task(Object) constructor to set the input value.
     *
     * @param value
     * @return
     */
    public final Object setValue(final Object value) {
        synchronized (MUTEX) {
            return this.value = value;
        }
    }

    /**
     * Execute this task asynchronously on a Worker thread.
     *
     * This will queue the Task with Worker.NORMAL_PRIORITY
     *
     * @return
     */
    public final Task fork() {
        return fork(Task.NORMAL_PRIORITY);
    }

    /**
     * Execute this task asynchronously on a Worker thread at the specified
     * priority.
     *
     * @param priority
     * @return
     */
    public final Task fork(final int priority) {
        Worker.fork(this, priority);

        return this;
    }

    /**
     * Wait for up to MAX_TIMEOUT milliseconds for the Task to complete.
     *
     * @return
     * @throws InterruptedException
     * @throws CancellationException
     * @throws ExecutionException
     * @throws TimeoutException
     */
    public final Object join() throws InterruptedException, CancellationException, ExecutionException, TimeoutException {
        return join(MAX_TIMEOUT);
    }

    /**
     * Wait for a maximum of timeout milliseconds for the Task to run and return
     * it's evaluation value, otherwise throw a TimeoutExeception.
     *
     * Similar to get(), except the total wait() time if the AsyncTask has not
     * completed is explicitly limited to prevent long delays.
     *
     * Never call join() from the UI thread with a timeout greater than 100ms.
     * This is still bad design and better handled with a chained Task or
     * UITask. You will receive a debug warning, but are not prevented from
     * making longer join() calls from the user interface Thread.
     *
     * @param timeout in milliseconds
     * @return final evaluation result of the Task
     * @throws InterruptedException - task was running when it was explicitly
     * canceled by another thread
     * @throws CancellationException - task was explicitly canceled by another
     * thread
     * @throws ExecutionException - an uncaught exception was thrown
     * @throws TimeoutException - UITask failed to complete within timeout
     * milliseconds
     */
    public final Object join(long timeout) throws InterruptedException, CancellationException, ExecutionException, TimeoutException {
        if (timeout < 0) {
            throw new IllegalArgumentException("Can not join() with timeout < 0: timeout=" + timeout);
        }
        //#mdebug
        if (PlatformUtils.getInstance().isUIThread() && timeout > 100) {
            L.i("WARNING- slow Task.join() on UI Thread", "timeout=" + timeout + " " + this);
        }
        //#enddebug
        boolean doExec = false;
        Object out;

        synchronized (MUTEX) {
            //#debug
            L.i("Start join", "timeout=" + timeout + " " + this);
            switch (status) {
                case EXEC_PENDING:
                    //#debug
                    L.i("Start join of EXEC_PENDING task", "timeout=" + timeout + " " + this.toString());
                    if (Worker.tryUnfork(this)) {
                        doExec = true;
                        break;
                    }
                // Continue to next state
                case READY:
                    if (status == READY) {
                        doExec = true;
                        break;
                        //Worker.fork(this, Task.HIGH_PRIORITY);
                    }
                // Continue to next state
                case EXEC_STARTED:
                    //#debug
                    L.i("Start join wait()", "status=" + getStatusString());
                    do {
                        final long t = System.currentTimeMillis();

                        MUTEX.wait(timeout);
                        if (status == EXEC_FINISHED) {
                            break;
                        }
                        if (status == CANCELED) {
                            throw new CancellationException("join() was to a Task which had already been canceled: " + this);
                        }
                        if (status == EXCEPTION) {
                            throw new ExecutionException("join() was to a Task which had already expereienced an uncaught runtime exception: " + this);
                        }
                        timeout -= System.currentTimeMillis() - t;
                    } while (timeout > 0);
                    //#debug
                    L.i("End join wait()", "status=" + getStatusString());
                    if (status == EXEC_STARTED) {
                        throw new TimeoutException("Task was already started when join() was call and did not complete during " + timeout + " milliseconds");
                    }
                    break;
                case CANCELED:
                    throw new CancellationException("join() was to a Task which was running but then canceled: " + this);
                case EXCEPTION:
                    throw new ExecutionException("join() was to a Task which had an uncaught exception: " + this);
                default:
                    ;
            }
            out = value;
        }
        if (doExec) {
            //#debug
            L.i("Start exec() out-of-sequence exec() after join() and successful unfork()", this.toString());
            out = executeTask(out);
        }

        return out;
    }

    /**
     * Wait up to a Task.MAX_TIMEOUT milliseconds for all tasks in the list to
     * complete execution. If any or all of them have any problem, the first
     * associated Exception to occur will be thrown.
     *
     * @param tasks
     * @throws InterruptedException
     * @throws CancellationException
     * @throws ExecutionException
     * @throws TimeoutException
     */
    public static void joinAll(final Task[] tasks) throws InterruptedException, CancellationException, ExecutionException, TimeoutException {
        doJoinAll(tasks, Task.MAX_TIMEOUT, false);
    }

    /**
     * Wait up to a maximum specified timeout for all tasks in the list to
     * complete execution. If any or all of them have any problem, the first
     * associated Exception to occur will be thrown.
     *
     * @param tasks - list of Tasks to wait for
     * @param timeout - milliseconds, max total time from for all Tasks to
     * complete
     * @throws InterruptedException
     * @throws CancellationException
     * @throws ExecutionException
     * @throws TimeoutException
     */
    public static void joinAll(final Task[] tasks, final long timeout) throws InterruptedException, CancellationException, ExecutionException, TimeoutException {
        doJoinAll(tasks, timeout, false);
    }

    /**
     * Wait up to Task.MAX_TIMEOUT milliseconds for all tasks in the list to
     * complete execution including any follow up tasks on the UI thread.
     *
     * @param tasks
     * @throws InterruptedException
     * @throws CancellationException
     * @throws ExecutionException
     * @throws TimeoutException
     */
    public static void joinAllUI(final Task[] tasks) throws InterruptedException, CancellationException, ExecutionException, TimeoutException {
        doJoinAll(tasks, Task.MAX_TIMEOUT, true);
    }

    /**
     * Wait up to a maximum specified timeout for all tasks in the list to
     * complete execution including any follow up tasks on the UI thread. If any
     * or all of them have any problem, the first associated Exception to occur
     * will be thrown.
     *
     * Note that unlike joinUI(), it is legal to call joinUI() with some, or
     * all, of the Tasks being of type Task, not type UITask. This allows easier
     * mixing of Task and UITask in the list for convenience, where joinUI()
     * calls to a Task that is not a UITask() would be a logical error.
     *
     * @param tasks - list of Tasks to wait for
     * @param timeout - milliseconds, max total time from for all Tasks to
     * complete
     * @throws InterruptedException
     * @throws CancellationException
     * @throws ExecutionException
     * @throws TimeoutException
     */
    public static void joinAllUI(final Task[] tasks, final long timeout) throws InterruptedException, CancellationException, ExecutionException, TimeoutException {
        doJoinAll(tasks, timeout, true);
    }

    private static void doJoinAll(final Task[] tasks, final long timeout, final boolean joinUI) throws InterruptedException, CancellationException, ExecutionException, TimeoutException {
        if (tasks == null) {
            throw new IllegalArgumentException("Can not joinAll(), list of tasks to join is null");
        }
        if (timeout < 0) {
            throw new IllegalArgumentException("Can not joinAll() with timeout < 0: timeout=" + timeout);
        }
        //#mdebug
        if (PlatformUtils.getInstance().isUIThread() && timeout > 100) {
            L.i("WARNING- slow Task.joinAll() on UI Thread", "timeout=" + timeout);
        }
        //#enddebug

        //#debug
        L.i("Start joinAll(" + timeout + ")", "numberOfTasks=" + tasks.length);
        long timeLeft = Long.MAX_VALUE;
        try {
            final long startTime = System.currentTimeMillis();
            for (int i = 0; i < tasks.length; i++) {
                final Task task = tasks[i];
                timeLeft = startTime + timeout - System.currentTimeMillis();

                if (timeLeft <= 0) {
                    throw new TimeoutException("joinAll(" + timeout + ") timout exceeded (" + timeLeft + ")");
                }
                if (joinUI && task instanceof UITask) {
                    task.joinUI(timeout);
                } else {
                    task.join(timeout);
                }
            }
        } finally {
            //#debug
            L.i("End joinAll(" + timeout + ")", "numberOfTasks=" + tasks.length + " timeElapsed=" + (timeout - timeLeft));
        }
    }
    
    /**
     * Wait up to MAX_TIMEOUT milliseconds for the UI thread to complete the
     * Task
     *
     * @return
     * @throws InterruptedException
     * @throws CancellationException
     * @throws ExecutionException
     * @throws TimeoutException
     */
    public final Object joinUI() throws InterruptedException, CancellationException, ExecutionException, TimeoutException {
        return joinUI(MAX_TIMEOUT);
    }

    /**
     * Wait for a maximum of timeout milliseconds for the UITask to complete if
     * needed and then also complete the followup action on the user interface
     * thread.
     *
     * You will receive a debug time warning if you are currently on the UI
     * thread and the timeout value is greater than 100ms.
     *
     * @param timeout in milliseconds
     * @return final evaluation result of the Task
     * @throws InterruptedException - task was running when it was explicitly
     * canceled by another thread
     * @throws CancellationException - task was explicitly canceled by another
     * thread
     * @throws ExecutionException - an uncaught exception was thrown
     * @throws TimeoutException - UITask failed to complete within timeout
     * milliseconds
     */
    public final Object joinUI(long timeout) throws InterruptedException, CancellationException, ExecutionException, TimeoutException {
        if (!(this instanceof UITask)) {
            throw new ClassCastException("Can not joinUI() unless Task is a UITask");
        }

        long t = System.currentTimeMillis();
        join(timeout);

        synchronized (MUTEX) {
            if (status < UI_RUN_FINISHED) {
                //#debug
                L.i("Start joinUI wait", "status=" + getStatusString());
                timeout -= System.currentTimeMillis() - t;
                while (timeout > 0) {
                    final long t2 = System.currentTimeMillis();
                    MUTEX.wait(timeout);
                    if (status == UI_RUN_FINISHED) {
                        break;
                    }
                    timeout -= System.currentTimeMillis() - t2;
                }
                //#debug
                L.i("End joinUI wait", "status=" + getStatusString());
                if (status < UI_RUN_FINISHED) {
                    throw new TimeoutException("JoinUI(" + timeout + ") failed to complete quickly enough");
                }
            }

            return value;
        }
    }

    /**
     * Find out the execution state of the Task
     *
     * @return
     */
    public final int getStatus() {
        synchronized (MUTEX) {
            return status;
        }
    }

    /**
     * Return the current status as a string for easy debug information display
     *
     * @return
     */
    public final String getStatusString() {
        synchronized (MUTEX) {
            return Task.STATUS_STRINGS[status];
        }
    }

    /**
     * Change the status
     *
     * You can only change status CANCELED or EXCEPTION to the READY state to
     * explicitly indicate you are going to re-use this Task. Note that Task
     * re-use is generally not advised, but can be valid if you have a special
     * need such as performance when re-creating the Task is particularly
     * expensive.
     *
     * @param status
     */
    public final void setStatus(final int status) {
        if (status == CANCELED) {
            throw new IllegalArgumentException("Do not setStatus(Task.CANCELED). Call Task.cancel(false, \"Reason for cancel\") instead to keep your code debuggable");
        }

        doSetStatus(status);
    }

    private void doSetStatus(final int status) {
        if (status < EXEC_PENDING || status > READY) {
            throw new IllegalArgumentException("setStatus(" + status + ") not allowed");
        }
        final Task t;
        synchronized (MUTEX) {
            if (this.status == status || ((this.status == CANCELED || this.status == EXCEPTION) && status != READY)) {
                //#debug
                L.i("State change from " + getStatusString() + " to " + Task.STATUS_STRINGS[status] + " is ignored", this.toString());
                return;
            }

            this.status = status;
            MUTEX.notifyAll();
            t = chainedTask;
            if (status == CANCELED || status == EXCEPTION) {
                /*
                 * Unchain as we cancel to simplify avoiding any future reference to
                 * canceled chained tasks. This can also speed garbage collection.
                 */
                chainedTask = null;
            }
        }

        if (status == CANCELED || status == EXCEPTION) {
            PlatformUtils.getInstance().runOnUiThread(new Runnable() {
                public void run() {
                    //#debug
                    L.i("Task onCanceled()", Task.this.toString());
                    onCanceled();
                }
            });
            // Also cancel any chained Tasks expecting the output of this Task
            if (t != null) {
                t.cancel(false, "Previous task in chain was canceled");
            }
        }
    }

    /**
     * Add a Task (or UITAsk, etc) which will run immediately after the present
     * Task and on the same Worker thread.
     *
     * The output result of the present task is fed as the input to exec() on
     * the nextTask, so any processing changes can propagated forward if the
     * nextTask is so designed. This Task behavior may thus be slightly
     * different from the first Task in the chain, which by default receives
     * "null" as the input argument unless setValue() is called before fork()ing
     * the first task in the chain.
     *
     * Note that if the Task is already chained, the new additional nextTask
     * will be added after the last existing link in the chain.
     *
     * Note that you can not unchain. Do not start chaining until you know that
     * you really do want to chain. If you change your mind later before you
     * fork(), make a new Task. If you change your mind later after you fork(),
     * you may want to cancel() the Task already running, but cancel() for
     * simple logic reasons is usually indicative of a design problem, is bad
     * practice and constructing and starting then stopping Task chains will
     * decrease performance.
     *
     * Setting nextTask to null is treated as an application logic error and
     * will throw an IllegalArgumentException
     *
     * @param nextTask
     * @return nextTask
     */
    public final Task chain(final Task nextTask) {
        if (nextTask != null) {
            final Task multiLinkChain;
            synchronized (MUTEX) {
                if (chainedTask == null) {
                    chainedTask = nextTask;
                    multiLinkChain = null;
                } else {
                    // Already chained- we must add to the end of the chain
                    multiLinkChain = chainedTask;
                }
            }
            if (multiLinkChain != null) {
                /*
                 * Call this outside the above synchronized block so that we are not
                 * holding multiple locks for Tasks at the same time
                 */
                multiLinkChain.chain(nextTask);
            }
        }

        return nextTask;
    }

    /**
     * You can call this as the return statement of your overriding method once
     * you have set the result
     *
     * @return
     */
    final Object executeTask(Object in) {
        try {
            synchronized (MUTEX) {
                if (in == null) {
                    // No input provided- used the stored value as input
                    in = value;
                } else {
                    // Input is provided- override the stored value
                    value = in;
                }
                if (status == Task.CANCELED || status == Task.EXCEPTION) {
                    throw new IllegalStateException(this.getStatusString() + " state can not be executed: " + this);
                }
                setStatus(Task.EXEC_STARTED);
            }
            /*
             * Execute the Task without holding any locks
             */
            in = exec(in);

            final boolean doRun;
            final Task t;
            synchronized (MUTEX) {
                value = in;
                doRun = status == EXEC_STARTED;
                if (doRun) {
                    setStatus(EXEC_FINISHED);
                }
                t = chainedTask;
            }
            if (this instanceof UITask && doRun) {
                PlatformUtils.getInstance().runOnUiThread((UITask) this);
            }
            if (t != null) {
                //#debug
                L.i("Begin fork chained task", t.toString() + " INPUT: " + in);
                t.setValue(in);
                t.fork(Task.HIGH_PRIORITY);
            }
        } catch (final Throwable t) {
            //#debug
            L.e("Unhandled task exception", this.toString(), t);
            setStatus(EXCEPTION);
        }

        return in;
    }

    /**
     * Override to implement Worker thread code
     *
     * @param in
     * @return
     */
    protected abstract Object exec(Object in);

    /**
     * Cancel execution if possible. This is called on the Worker thread
     *
     * Do not override this unless you also call super.cancel(boolean).
     *
     * Override onCanceled() is the normal notification location, and is called
     * from the UI thread with Task state updates handled for you.
     *
     * @param mayInterruptIfRunning
     * @param reason
     * @return
     */
    public boolean cancel(final boolean mayInterruptIfRunning, final String reason) {
        synchronized (MUTEX) {
            boolean canceled = false;

            if (reason == null || reason.length() == 0) {
                throw new IllegalArgumentException("For clean debug, you must provide a reason for cancel(), null will not do");
            }

            //#debug
            L.i("Begin cancel() - " + reason, "status=" + this.getStatusString() + " " + this);
            switch (status) {
                case EXEC_STARTED:
                    doSetStatus(CANCELED);
                    canceled = true;
                    if (mayInterruptIfRunning) {
                        Worker.interruptTask(this);
                    }
                    break;

                case EXEC_FINISHED:
                    //#debug
                    L.i("Ignored attempt to interrupt an EXEC_FINISHED Task", this.toString());
                    break;
                case UI_RUN_FINISHED:
                    //#debug
                    L.i("Attempt to cancel Task after EXEC_FINISHED. Suspicious but may be normal due to race-to-cancel condition", this.toString());
                    break;

                default:
                    doSetStatus(CANCELED);
                    canceled = true;
            }
            //#debug
            L.i("End cancel() - " + reason, "status=" + this.getStatusString() + " " + this);

            return canceled;
        }
    }

    /**
     * This is executed on the UI thread
     *
     * Override if needed, the default implementation does nothing except
     * provide debug output.
     *
     * Use getStatus() to distinguish between CANCELED and EXCEPTION states if
     * necessary.
     *
     */
    protected void onCanceled() {
        //#debug
        L.i("Task canceled", this.toString());
    }

    /**
     * Check of the task has been had cancel() called or has thrown an uncaught
     * exception while executing.
     *
     * @return
     */
    public final boolean isCanceled() {
        synchronized (MUTEX) {
            return status == AsyncTask.CANCELED || status == EXCEPTION;
        }
    }

    //#mdebug
    /**
     * When debugging, show what each Worker is doing and the queue length
     *
     * @return One ore more lines of text depending on the number of active
     * Workers
     */
    public static Vector getCurrentState() {
        return Worker.getCurrentState();
    }
    //#enddebug

    /**
     * For debug
     *
     * @return
     */
    public String toString() {
        synchronized (MUTEX) {
            return "TASK: status=" + getStatusString() + " result=" + value + " nextTask=(" + chainedTask + ")";
        }
    }
}
