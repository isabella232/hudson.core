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

import hudson.model.Hudson;
import hudson.model.Items;
import hudson.security.ACL;
import hudson.security.AccessControlled;
import hudson.security.AuthorizationStrategy;
import hudson.security.Permission;
import hudson.security.SecurityRealm;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.eclipse.hudson.security.HudsonSecurityEntitiesHolder;
import org.eclipse.hudson.security.HudsonSecurityManager;
import org.springframework.security.AccessDeniedException;
import org.springframework.security.Authentication;

/**
 * A simple model to hold team members and name of jobs belong to the team
 *
 * @since 3.1.0
 * @author Winston Prakash
 */
public class Team implements AccessControlled {

    public static final String DEFAULT_TEAM_NAME = "default";
    private List<String> admins = new CopyOnWriteArrayList<String>();
    private List<String> members = new CopyOnWriteArrayList<String>();
    private List<String> jobs = new CopyOnWriteArrayList<String>();
    private String name;
    protected static final String JOBS_FOLDER_NAME = "jobs";
    private String description;
    private transient TeamManager teamManager;

    Team(String name, TeamManager teamManager) {
        this(name, name, teamManager);
    }

    Team(String teamName, String description, TeamManager teamManager) {
        this.name = teamName;
        this.description = description;
        this.teamManager = teamManager;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void addAdmin(String adminName) throws IOException {
        if (!admins.contains(adminName)) {
            admins.add(adminName);
            getTeamManager().save();
        }
    }

    public void removeAdmin(String adminName) throws IOException {
        if (admins.contains(adminName)) {
            admins.remove(adminName);
            getTeamManager().save();
        }
    }

    public List<String> getAdmins() {
        return admins;
    }
    
    public boolean isCurrentUserSysAdmin() {
        String currentUser = HudsonSecurityManager.getAuthentication().getName();
        return isAdmin(currentUser);
    }

    public boolean isAdmin(String userName) {
        // Team Manager ACL always assume userName current user
        boolean isAdmin = false;
        HudsonSecurityManager hudsonSecurityManager = HudsonSecurityEntitiesHolder.getHudsonSecurityManager();
        SecurityRealm securityRealm = null;
        if (hudsonSecurityManager != null) {
            securityRealm = hudsonSecurityManager.getSecurityRealm();
        }
        if ((securityRealm != null) && securityRealm instanceof TeamAwareSecurityRealm) {
            TeamAwareSecurityRealm teamAwareSecurityRealm = (TeamAwareSecurityRealm) securityRealm;
            isAdmin = teamAwareSecurityRealm.isCurrentUserTeamAdmin();
        } else {
            isAdmin = admins.contains(userName);
        }
        return isAdmin;
    }

    public List<String> getMembers() {
        return members;
    }

    public void addMember(String userName) {
        if (!members.contains(userName)) {
            members.add(userName);
        }
    }

    public void removeMember(String userName) throws IOException {
        if (members.contains(userName)) {
            members.remove(userName);
            getTeamManager().save();
        }
    }

    public boolean isMember(String userName) {
        HudsonSecurityManager hudsonSecurityManager = HudsonSecurityEntitiesHolder.getHudsonSecurityManager();
        SecurityRealm securityRealm = null;
        if (hudsonSecurityManager != null) {
            securityRealm = hudsonSecurityManager.getSecurityRealm();
        }
        if ((securityRealm != null) && securityRealm instanceof TeamAwareSecurityRealm) {
            TeamAwareSecurityRealm teamAwareSecurityRealm = (TeamAwareSecurityRealm) securityRealm;
            Team currentUserTeam = teamAwareSecurityRealm.GetCurrentUserTeam();
            if (currentUserTeam == this) {
                return true;
            } else {
                return false;
            }
        } else {
            return members.contains(userName) || admins.contains(userName);
        }
    }

    public void addJob(String jobname) throws IOException {
        if (!jobs.contains(jobname)) {
            jobs.add(jobname);
            getTeamManager().save();
        }
    }

    public void removeJob(String jobname) throws IOException {
        if (jobs.contains(jobname)) {
            jobs.remove(jobname);
            getTeamManager().save();
        }
    }

    public boolean isJobOwner(String jobName) {
        return jobs.contains(jobName);
    }

    void renameJob(String oldJobName, String newJobName) throws IOException {
        if (jobs.contains(oldJobName)) {
            jobs.remove(oldJobName);
            jobs.add(newJobName);
            getTeamManager().save();
        }
    }

    List<File> getJobsRootFolders(File rootFolder) {
        File jobsFolder = getJobsFolder(rootFolder);
        if (jobsFolder.exists()) {
            File[] jobsRootFolders = jobsFolder.listFiles(new FileFilter() {
                @Override
                public boolean accept(File child) {
                    return child.isDirectory() && Items.getConfigFile(child).exists();
                }
            });
            if (jobsRootFolders != null) {
                return Arrays.asList(jobsRootFolders);
            }
        }
        return Collections.EMPTY_LIST;
    }

    protected File getJobsFolder(File rootFolder) {
        return new File(rootFolder, name + "/" + JOBS_FOLDER_NAME);
    }

    @Override
    public ACL getACL() {
        AuthorizationStrategy authorizationStrategy = HudsonSecurityEntitiesHolder.getHudsonSecurityManager().getAuthorizationStrategy();
        if (authorizationStrategy instanceof TeamBasedAuthorizationStrategy) {
            TeamBasedAuthorizationStrategy teamBasedAuthorizationStrategy = (TeamBasedAuthorizationStrategy) authorizationStrategy;
            return teamBasedAuthorizationStrategy.getACL(this);
        }
        // Team will not be used if Team Based Authorization Strategy is not used
        return new ACL() {
            public boolean hasPermission(Authentication a, Permission permission) {
                return false;
            }
        };
    }

    @Override
    public void checkPermission(Permission permission) throws AccessDeniedException {
        getACL().checkPermission(permission);
    }

    @Override
    public boolean hasPermission(Permission permission) {
        return getACL().hasPermission(permission);
    }
    
    // When the Team is unmarshalled it would not have Team Manager set
    private TeamManager getTeamManager() {
        if (teamManager == null) {
            return Hudson.getInstance().getTeamManager();
        } else {
            return teamManager;
        }
    }
}
