/*
 * Copyright (c) 2013 Oracle Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Winston Prakash
 */
package org.eclipse.hudson.security.team;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.listeners.ItemListener;
import hudson.security.AuthorizationStrategy;
import java.io.IOException;
import org.eclipse.hudson.security.HudsonSecurityEntitiesHolder;
import org.eclipse.hudson.security.HudsonSecurityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple helper to listen to job creation, deletion and renaming and
 * accordingly add, delete or rename it in the current users team.
 *
 * @since 3.1.0
 * @author Winston Prakash
 */
@Extension
public class TeamJobListener extends ItemListener {

    private transient Logger logger = LoggerFactory.getLogger(TeamJobListener.class);

    @Override
    public void onCreated(Item item) {
        if (item instanceof Job<?, ?>) {
            Job job = (Job) item;
            try {
                TeamManager teamManager = getTeamManager();
                if (teamManager != null) {
                    teamManager.addJobToCurrentUserTeam(job.getName());
                }
            } catch (IOException ex) {
                logger.error("Failed to rename job in current user team", ex);
            }
        }
    }

    @Override
    public void onRenamed(Item item, String oldJobName, String newJobName) {
        if (item instanceof Job<?, ?>) {
            try {
                TeamManager teamManager = getTeamManager();
                if (teamManager != null) {
                    teamManager.renameJobInCurrentUserTeam(oldJobName, newJobName);
                }
            } catch (IOException ex) {
                logger.error("Failed to rename job in current user team", ex);
            }
        }
    }

    @Override
    public void onDeleted(Item item) {
        if (item instanceof Job<?, ?>) {
            Job job = (Job) item;
            try {
                TeamManager teamManager = getTeamManager();
                if (teamManager != null) {
                    teamManager.removeJobFromCurrentUserTeam(job.getName());
                }
            } catch (IOException ex) {
                logger.error("Failed to rename job in current user team", ex);
            }
        }
    }

    private TeamManager getTeamManager() {
        HudsonSecurityManager hudsonSecurityManager = HudsonSecurityEntitiesHolder.getHudsonSecurityManager();
        AuthorizationStrategy authorizationStrategy = hudsonSecurityManager.getAuthorizationStrategy();
        if (authorizationStrategy instanceof TeamBasedAuthorizationStrategy) {
            TeamBasedAuthorizationStrategy teamBasedAuthorizationStrategy = (TeamBasedAuthorizationStrategy) authorizationStrategy;
            return teamBasedAuthorizationStrategy.getTeamManager();
        }
        return null;
    }
}
