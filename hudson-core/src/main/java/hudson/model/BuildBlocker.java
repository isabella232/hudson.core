/*
 * Copyright (c) 2014 Hudson.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Hudson - initial API and implementation and/or initial documentation
 */

package hudson.model;

import hudson.ExtensionPoint;
import hudson.model.queue.CauseOfBlockage;

/**
 * Extension point that allows plugins to prevent projects from building
 * based on external considerations, e.g., site-specific quota exceeded,
 * or plugin configuration.
 * 
 * @author Bob Foster
 */
public abstract class BuildBlocker implements ExtensionPoint {

    /**
     * If project is to be blocked from building, return CauseOfBlockage.
     * @param project to be checked
     * @return CauseOfBlockage or null if not blocked
     */
    abstract public CauseOfBlockage getCauseOfBlockage(AbstractProject project);
}
