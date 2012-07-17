/*******************************************************************************
 *
 * Copyright (c) 2011 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *    Nikita Levyankov
 *
 *******************************************************************************/

package org.eclipse.hudson.api.model;

import java.io.IOException;

/**
 * FreeStyle project interface.
 * <p/>
 * Date: 9/15/11
 *
 * @author Nikita Levyankov
 */
public interface IFreeStyleProject extends IProject {

    /**
     * Returns user-specified workspace directory, or null if it's up to Hudson
     *
     * @return string representation of directory.
     * @throws IOException if any.
     */
    String getCustomWorkspace() throws IOException;

    /**
     * User-specified workspace directory, or null if it's up to Hudson.
     *
     * <p> Normally a free-style project uses the workspace location assigned by
     * its parent container, but sometimes people have builds that have
     * hard-coded paths (which can be only built in certain locations. see
     * http://www.nabble.com/Customize-Workspace-directory-tt17194310.html for
     * one such discussion.)
     *
     * <p> This is not {@link java.io.File} because it may have to hold a path
     * representation on another OS.
     *
     * <p> If this path is relative, it's resolved against
     * {@link hudson.model.Node#getRootPath()} on the node where this workspace
     * is prepared.
     *
     * @param customWorkspace new custom workspace to set
     * @since 1.320
     * @throws IOException if any.
     */
    void setCustomWorkspace(String customWorkspace) throws IOException;
}
