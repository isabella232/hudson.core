/*
 * Copyright (c) 2015 Hudson.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Hudson - initial API and implementation and/or initial documentation
 */

package hudson.util;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Logger;

/**
 * TrackedExecutor is an executor that can be created my clients knowing only
 * <code>TrackedExecutor.class.getName()</code>, e.g., by:
 * <pre>
 * Class.forName(executorClassName).newInstance()
 * </pre>
 * But all instances of TrackedExecutor and for each instance, all threads
 * created by TrackedExecutor can be shut down when, e.g., Hudson is
 * undeployed by its container.
 * <p>Shutdown is aggressive. If a thread does not respond to <code>interrupt</code>
 * within 1 ms., the deprecated <code>thread.stop()</code> is called. This
 * can mean that sometimes thread will be stopped before it has a chance to
 * respond to interrupt.
 * @author Bob Foster
 */
public class TrackedExecutor implements Executor {

    private static final Logger LOGGER = Logger.getLogger(TrackedExecutor.class.getName());

    /** Default implementation - subclasses may override */
    protected ThreadFactory factory = Executors.defaultThreadFactory();

    /** Executor list. Must not be weak as executors may be used and forgotten. */
    private static final List<TrackedExecutor> executors = new ArrayList<TrackedExecutor>();
    /** Thread list. Must be weak or could hold threads in memory. */
    private final List<WeakReference<Thread>> threads = new ArrayList<WeakReference<Thread>>();
    /** True if executor has been shut down */
    private boolean shutdown;

    /**
     * Must have a null constructor if it is to be created by
     * <code>hudson.util.TrackedExecutor.class.newInstance()</code>,
     * as, e.g., guice does.
     */
    public TrackedExecutor() {
        synchronized (executors) {
            executors.add(this);
        }
    }

    @Override
    public void execute(Runnable command) {
        String runnableClass = command.getClass().getName();
        Thread thread = null;
        synchronized (threads) {
            if (shutdown) {
                LOGGER.severe("Attempt to run "+runnableClass+" after shutdown ignored");
                throw new IllegalStateException();
            }
            thread = factory.newThread(command);
            thread.setName(runnableClass);
            threads.add(new WeakReference<Thread>(thread));
        }
        thread.start();
    }

    /**
     * Shutdown all threads started by any TrackedExecutor whose contextClassLoader
     * is the same as (==) the argument. TODO not certain
     * this precaution is necessary, as a TrackedExecutor should
     * always be created by a different classloader in each instance of
     * Hudson in a container; but it costs very little to test.
     *
     * @param contextClassLoader if not null, only threads with a matching
     * contextClassLoader are stopped; otherwise, all threads are stopped
     */
    public static void shutdownAll(ClassLoader contextClassLoader) {
        synchronized (executors) {
            for (Iterator<TrackedExecutor> it = executors.iterator(); it.hasNext();) {
                TrackedExecutor executor = it.next();
                executor.shutdown(contextClassLoader);
                if (executor.threads.isEmpty()) {
                    it.remove();
                }
            }
        }
    }

    /**
     * Shutdown all threads started by this executor whose contextClassLoader
     * is the same as (==) the argument.
     * @param contextClassLoader only threads whose contextClassLoader is ==
     * are stopped
     */
    public void shutdown(ClassLoader contextClassLoader) {
        synchronized (threads) {
            shutdown = true;
            for (Iterator<WeakReference<Thread>> it = threads.iterator(); it.hasNext(); ) {
                Thread thread = it.next().get();
                if (thread != null
                    && (contextClassLoader == null
                        || thread.getContextClassLoader() == contextClassLoader)) {
                    stopThread(thread);
                    it.remove();
                }
            }
        }
    }

    /**
     * Subclass may override if there is a faster or better way to stop threads
     * for this executor.
     * @param thread to stop
     */
    protected void stopThread(Thread thread) {
        thread.interrupt();
        try { thread.join(1); } catch (InterruptedException ignore) { }
        if (thread.isAlive()) {
            thread.stop();
        }
    }

    /**
     * Subclass for threads that are not interruptable.
     */
    public static class NonInterruptableExecutor extends TrackedExecutor {
        @Override
        public void stopThread(Thread thread) {
            thread.stop();
        }
    }
}
