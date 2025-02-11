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
 *    Tom Huybrechts
 *
 *
 *******************************************************************************/ 

package hudson.slaves;

import hudson.ExtensionPoint;
import hudson.Launcher;
import hudson.DescriptorExtensionList;
import hudson.model.queue.CauseOfBlockage;
import hudson.scm.SCM;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Describable;
import hudson.model.Environment;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.Queue.Task;

import java.io.IOException;
import java.util.List;

/**
 * Extensible property of {@link Node}.
 *
 * <p> Plugins can contribute this extension point to add additional data or UI
 * actions to {@link Node}. {@link NodeProperty}s show up in the configuration
 * screen of a node, and they are persisted with the {@link Node} object.
 *
 *
 * <h2>Views</h2> <dl> <dt>config.jelly</dt> <dd>Added to the configuration page
 * of the node. <dt>global.jelly</dt> <dd>Added to the system configuration
 * page. <dt>summary.jelly (optional)</dt> <dd>Added to the index page of the
 * {@link hudson.model.Computer} associated with the node </dl>
 *
 * @param <N> {@link NodeProperty} can choose to only work with a certain
 * subtype of {@link Node}, and this 'N' represents that type. Also see
 * {@link NodePropertyDescriptor#isApplicable(Class)}.
 *
 * @since 1.286
 */
public abstract class NodeProperty<N extends Node> implements Describable<NodeProperty<?>>, ExtensionPoint {

    protected transient N node;

    protected void setNode(N node) {
        this.node = node;
    }

    public NodePropertyDescriptor getDescriptor() {
        return (NodePropertyDescriptor) Hudson.getInstance().getDescriptorOrDie(getClass());
    }

    /**
     * Called by the {@link Node} to help determine whether or not it should
     * take the given task. Individual properties can return a non-null value
     * here if there is some reason the given task should not be run on its
     * associated node. By default, this method returns
     * <code>null</code>.
     *
     * @since 1.360
     */
    public CauseOfBlockage canTake(Task task) {
        return null;
    }
    
    public CauseOfBlockage canTake(Queue.BuildableItem item) {
        return canTake(item.task);  // backward compatible behaviour
    }

    /**
     * Runs before the
     * {@link SCM#checkout(AbstractBuild, Launcher, FilePath, BuildListener, File)}
     * runs, and performs a set up. Can contribute additional properties to the
     * environment.
     *
     * @param build The build in progress for which an {@link Environment}
     * object is created. Never null.
     * @param launcher This launcher can be used to launch processes for this
     * build. If the build runs remotely, launcher will also run a job on that
     * remote machine. Never null.
     * @param listener Can be used to send any message.
     * @return non-null if the build can continue, null if there was an error
     * and the build needs to be aborted.
     * @throws IOException terminates the build abnormally. Hudson will handle
     * the exception and reports a nice error message.
     */
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        return new Environment() {
        };
    }

    /**
     * Lists up all the registered {@link NodeDescriptor}s in the system.
     */
    public static DescriptorExtensionList<NodeProperty<?>, NodePropertyDescriptor> all() {
        return (DescriptorExtensionList) Hudson.getInstance().getDescriptorList(NodeProperty.class);
    }

    /**
     * List up all {@link NodePropertyDescriptor}s that are applicable for the
     * given project.
     */
    public static List<NodePropertyDescriptor> for_(Node node) {
        return NodePropertyDescriptor.for_(all(), node);
    }
}
