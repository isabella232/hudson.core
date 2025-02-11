/*******************************************************************************
 *
 * Copyright (c) 2004-2012 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   Kohsuke Kawaguchi, Winston Prakash, Brian Westrich, Jean-Baptiste Quenot
 *
 *
 *******************************************************************************/ 

package hudson;

import hudson.model.AbstractProject;
import hudson.model.Hudson;
import org.eclipse.hudson.security.HudsonSecurityManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Collection;
import java.util.logging.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Runs a job on all projects in the order of dependencies
 */
public class DependencyRunner implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(DependencyRunner.class.getName());
    ProjectRunnable runnable;
    List<AbstractProject> polledProjects = new ArrayList<AbstractProject>();

    public DependencyRunner(ProjectRunnable runnable) {
        this.runnable = runnable;
    }

    public void run() {
        Authentication saveAuth = SecurityContextHolder.getContext().getAuthentication();
        HudsonSecurityManager.grantFullControl();

        try {
            Set<AbstractProject> topLevelProjects = new HashSet<AbstractProject>();
            // Get all top-level projects
            LOGGER.fine("assembling top level projects");
            for (AbstractProject p : Hudson.getInstance().getAllItems(AbstractProject.class)) {
                if (p.getUpstreamProjects().size() == 0) {
                    LOGGER.fine("adding top level project " + p.getName());
                    topLevelProjects.add(p);
                } else {
                    LOGGER.fine("skipping project since not a top level project: " + p.getName());
                }
            }
            populate(topLevelProjects);
            for (AbstractProject p : polledProjects) {
                LOGGER.fine("running project in correct dependency order: " + p.getName());
                runnable.run(p);
            }
        } finally {
            SecurityContextHolder.getContext().setAuthentication(saveAuth);
        }
    }

    private void populate(Collection<? extends AbstractProject> projectList) {
        for (AbstractProject<?, ?> p : projectList) {
            if (polledProjects.contains(p)) {
                // Project will be readded at the queue, so that we always use
                // the longest path
                LOGGER.fine("removing project " + p.getName() + " for re-add");
                polledProjects.remove(p);
            }

            LOGGER.fine("adding project " + p.getName());
            polledProjects.add(p);

            // Add all downstream dependencies
            populate(p.getDownstreamProjects());
        }
    }

    public interface ProjectRunnable {

        void run(AbstractProject p);
    }
}
