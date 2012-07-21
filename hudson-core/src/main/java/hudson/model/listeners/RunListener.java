/*******************************************************************************
 *
 * Copyright (c) 2004-2009 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 
 *    Kohsuke Kawaguchi, Tom Huybrechts
 *
 *
 *******************************************************************************/ 

package hudson.model.listeners;

import hudson.ExtensionPoint;
import hudson.ExtensionListView;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.Hudson;
import hudson.util.CopyOnWriteList;
import org.jvnet.tiger_types.Types;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Receives notifications about builds.
 *
 * <p> Listener is always Hudson-wide, so once registered it gets notifications
 * for every build that happens in this Hudson.
 *
 * <p> This is an abstract class so that methods added in the future won't break
 * existing listeners.
 *
 * @author Kohsuke Kawaguchi
 * @since 1.145
 */
public abstract class RunListener<R extends Run> implements ExtensionPoint {
    //TODO: review and check whether we can do it private

    public final Class<R> targetType;

    protected RunListener(Class<R> targetType) {
        this.targetType = targetType;
    }

    protected RunListener() {
        Type type = Types.getBaseClass(getClass(), RunListener.class);
        if (type instanceof ParameterizedType) {
            targetType = Types.erasure(Types.getTypeArgument(type, 0));
        } else {
            throw new IllegalStateException(getClass() + " uses the raw type for extending RunListener");
        }
    }

    public Class<R> getTargetType() {
        return targetType;
    }

    /**
     * Called after a build is completed.
     *
     * @param r The completed build.
     * @param listener The listener for this build. This can be used to produce
     * log messages, for example, which becomes a part of the "console output"
     * of this build. But when this method runs, the build is considered
     * completed, so its status cannot be changed anymore.
     */
    public void onCompleted(R r, TaskListener listener) {
    }

    /**
     * Called after a build is moved to the {@link Run.State#COMPLETED} state.
     *
     * <p> At this point, all the records related to a build is written down to
     * the disk. As such, {@link TaskListener} is no longer available. This
     * happens later than {@link #onCompleted(Run, TaskListener)}.
     */
    public void onFinalized(R r) {
    }

    /**
     * Called when a build is started (i.e. it was in the queue, and will now
     * start running on an executor)
     *
     * @param r The started build.
     * @param listener The listener for this build. This can be used to produce
     * log messages, for example, which becomes a part of the "console output"
     * of this build.
     */
    public void onStarted(R r, TaskListener listener) {
    }

    /**
     * Called right before a build is going to be deleted.
     *
     * @param r The build.
     */
    public void onDeleted(R r) {
    }

    /**
     * Registers this object as an active listener so that it can start getting
     * callbacks invoked.
     *
     * @deprecated as of 1.281 Put {@link Extension} on your class to get it
     * auto-registered.
     */
    public void register() {
        all().add(this);
    }

    /**
     * Reverse operation of {@link #register()}.
     */
    public void unregister() {
        all().remove(this);
    }
    /**
     * List of registered listeners.
     *
     * @deprecated as of 1.281 Use {@link #all()} for read access, and use
     * {@link Extension} for registration.
     */
    public static final CopyOnWriteList<RunListener> LISTENERS = ExtensionListView.createCopyOnWriteList(RunListener.class);

    /**
     * Fires the {@link #onCompleted} event.
     */
    public static void fireCompleted(Run r, TaskListener listener) {
        for (RunListener l : all()) {
            if (l.targetType.isInstance(r)) {
                // See issue https://bugs.eclipse.org/bugs/show_bug.cgi?id=384786
                // Gaurd against any failures from listeners 
                try {
                    l.onCompleted(r, listener);
                } catch (Exception exc) {
                    logger.warn("Exception from Runlistener.onCompleted", exc);
                }
            }
        }
    }

    /**
     * Fires the {@link #onStarted} event.
     */
    public static void fireStarted(Run r, TaskListener listener) {
        for (RunListener l : all()) {
            if (l.targetType.isInstance(r)) {
                // See issue https://bugs.eclipse.org/bugs/show_bug.cgi?id=384786
                // Gaurd against any failures from listeners 
                try {
                    l.onStarted(r, listener);
                } catch (Exception exc) {
                    logger.warn("Exception from Runlistener.fireStarted", exc);
                }
            }
        }
    }

    /**
     * Fires the {@link #onFinalized(Run)} event.
     */
    public static void fireFinalized(Run r) {
        for (RunListener l : all()) {
            if (l.targetType.isInstance(r)) {
                // See issue https://bugs.eclipse.org/bugs/show_bug.cgi?id=384786
                // Gaurd against any failures from listeners 
                try {
                    l.onFinalized(r);
                } catch (Exception exc) {
                    logger.warn("Exception from Runlistener.fireFinalized", exc);
                }
            }
        }
    }

    /**
     * Fires the {@link #onFinalized(Run)} event.
     */
    public static void fireDeleted(Run r) {
        for (RunListener l : all()) {
            if (l.targetType.isInstance(r)) {
                // See issue https://bugs.eclipse.org/bugs/show_bug.cgi?id=384786
                // Gaurd against any failures from listeners 
                try {
                    l.onDeleted(r);
                } catch (Exception exc) {
                    logger.warn("Exception from Runlistener.fireDeleted", exc);
                }
            }
        }
    }

    /**
     * Returns all the registered {@link RunListener} descriptors.
     */
    public static ExtensionList<RunListener> all() {
        return Hudson.getInstance().getExtensionList(RunListener.class);
    }
    
    static  Logger logger = LoggerFactory.getLogger(RunListener.class);
}
