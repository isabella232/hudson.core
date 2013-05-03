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

import com.thoughtworks.xstream.XStream;
import hudson.BulkChange;
import hudson.XmlFile;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;
import hudson.security.ACL;
import hudson.security.SecurityRealm;
import hudson.util.XStream2;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import org.eclipse.hudson.security.HudsonSecurityEntitiesHolder;
import org.eclipse.hudson.security.HudsonSecurityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manger that manages the teams and their persistence
 *
 * @since 3.1.0
 * @author Winston Prakash
 */
public final class TeamManager implements Saveable {

    private List<String> sysAdmins = new CopyOnWriteArrayList<String>();
    private List<Team> teams = new CopyOnWriteArrayList<Team>();
    private static final XStream XSTREAM = new XStream2();
    private transient Logger logger = LoggerFactory.getLogger(TeamManager.class);
    private transient File hudsonHomeDir;
    private transient File teamsFolder;
    private transient final String teamsConfigFileName = "teams.xml";
    private transient DefaultTeam defaultTeam;
    private static final String TEAMS_FOLDER_NAME = "teams";

    public TeamManager(File homeDir) {
        hudsonHomeDir = homeDir;
        teamsFolder = new File(hudsonHomeDir, TEAMS_FOLDER_NAME);
        if (!teamsFolder.exists()) {
            teamsFolder.mkdirs();
        }
        load();
        ensureDefaultTeam();
    }

    public void addSysAdmin(String adminName) {
        sysAdmins.add(adminName);
    }

    public boolean isSysAdmin(String userName) {
        boolean isSysAdmin;
        HudsonSecurityManager hudsonSecurityManager = HudsonSecurityEntitiesHolder.getHudsonSecurityManager();
        SecurityRealm securityRealm = null;
        if (hudsonSecurityManager != null) {
            securityRealm = hudsonSecurityManager.getSecurityRealm();
        }
        if ((securityRealm != null) && securityRealm instanceof TeamAwareSecurityRealm) {
            TeamAwareSecurityRealm teamAwareSecurityRealm = (TeamAwareSecurityRealm) securityRealm;
            isSysAdmin = teamAwareSecurityRealm.isCurrentUserSysAdmin();
        } else {
            isSysAdmin = sysAdmins.contains(userName);
        }
        return isSysAdmin;
    }

    public List<Team> getTeams() {
        return teams;
    }

    public Team createTeam(String teamName) throws IOException {
        Team newTeam = new Team(teamName);
        teams.add(newTeam);
        save();
        return newTeam;
    }

    public void addUser(String teamName, String userName) throws TeamNotFoundException, IOException {
        Team team = findTeam(teamName);
        team.addMember(userName);
        save();
    }

    public Team findTeam(String teamName) throws TeamNotFoundException {
        for (Team team : teams) {
            if (teamName.equals(team.getName())) {
                return team;
            }
        }
        throw new TeamNotFoundException("Team " + teamName + " does not exist");
    }

    public void removeTeam(String teamName) throws IOException, TeamNotFoundException {
        Team team = findTeam(teamName);
        teams.remove(team);
        save();
    }

    public Team findCurrentUserTeam() {
        Team team;
        HudsonSecurityManager hudsonSecurityManager = HudsonSecurityEntitiesHolder.getHudsonSecurityManager();
        SecurityRealm securityRealm = null;
        if (hudsonSecurityManager != null) {
            securityRealm = hudsonSecurityManager.getSecurityRealm();
        }
        if ((securityRealm != null) && securityRealm instanceof TeamAwareSecurityRealm) {
            TeamAwareSecurityRealm teamAwareSecurityRealm = (TeamAwareSecurityRealm) securityRealm;
            team = teamAwareSecurityRealm.GetCurrentUserTeam();
        } else {
            String currentUser = HudsonSecurityManager.getAuthentication().getName();
            team = findUserTeam(currentUser);
        }
        return team;
    }

    public boolean isCurrentUserHasAccess(String jobName) {
        Team userTeam = findCurrentUserTeam();
        if (userTeam != null) {
            if (userTeam.isJobOwner(jobName)) {
                return true;
            } else {
                return isAnonymousJob(jobName);
            }
        } else {
            return isAnonymousJob(jobName);
        }
    }

    private boolean isAnonymousJob(String jobName) {
        for (Team team : teams) {
            if (team.isJobOwner(jobName)) {
                // job belongs to another team so has no access
                return false;
            }
        }
        // Not belong to any team, so has access
        return true;
    }

    public boolean isUserHasAccess(String userName, String jobName) {
        Team userTeam = findUserTeam(userName);
        if (userTeam != null) {
            return userTeam.isJobOwner(jobName);
        } else {
            for (Team team : teams) {
                if (team.isJobOwner(jobName)) {
                    // Job belongs to a team so has no access
                    return false;
                }
            }
            // Job does not belong to any team, so has access
            return true;
        }
    }

    public Team findUserTeam(String userName) {

        for (Team team : teams) {
            if (team.isMember(userName)) {
                return team;
            }
        }
        HudsonSecurityManager hudsonSecurityManager = HudsonSecurityEntitiesHolder.getHudsonSecurityManager();
        SecurityRealm securityRealm = null;
        if (hudsonSecurityManager != null) {
            securityRealm = hudsonSecurityManager.getSecurityRealm();
        }
        if ((securityRealm != null) && securityRealm instanceof TeamAwareSecurityRealm) {
            TeamAwareSecurityRealm teamAwareSecurityRealm = (TeamAwareSecurityRealm) securityRealm;
            return teamAwareSecurityRealm.GetCurrentUserTeam();
        }
        return null;
    }

    public Team findJobOwnerTeam(String jobName) {
        for (Team team : teams) {
            if (team.isJobOwner(jobName)) {
                return team;
            }
        }
        return null;
    }

    public void addJobToCurrentUserTeam(String jobName) throws IOException {
        addJob(findCurrentUserTeam(), jobName);
    }

    public void addJobToUserTeam(String userName, String jobName) throws IOException {
        addJob(findUserTeam(userName), jobName);

    }

    public void addJob(Team team, String jobName) throws IOException {
        if (team != null) {
            team.addJob(jobName);
            save();
        }
    }

    public void removeJobFromCurrentUserTeam(String jobName) throws IOException {
        removeJob(findCurrentUserTeam(), jobName);
    }

    public void removeJobFromUserTeam(String userName, String jobName) throws IOException {
        removeJob(findUserTeam(userName), jobName);
    }

    public void removeJob(Team team, String jobName) throws IOException {
        if (team != null) {
            team.removeJob(jobName);
            save();
        }
    }

    public void renameJobInCurrentUserTeam(String oldJobName, String newJobName) throws IOException {
        renameJob(findCurrentUserTeam(), oldJobName, newJobName);
    }

    public void renameJobInUserTeam(String userName, String oldJobName, String newJobName) throws IOException {
        renameJob(findUserTeam(userName), oldJobName, newJobName);
    }

    public void renameJob(Team team, String oldJobName, String newJobName) throws IOException {
        if (team != null) {
            team.renameJob(oldJobName, newJobName);
            save();
        }
    }

    /**
     * The file where the teams settings are saved.
     */
    private XmlFile getConfigFile() {
        return new XmlFile(XSTREAM, new File(teamsFolder, teamsConfigFileName));
    }
    // This is purely fo unit test. Since Hudson is not fully loaded during
    // test BulkChange saving mode is not available
    private transient boolean useBulkSaveFlag = true;

    public void setUseBulkSaveFlag(boolean flag) {
        useBulkSaveFlag = flag;
    }

    /**
     * Save the settings to the configuration file.
     */
    @Override
    public synchronized void save() throws IOException {
        if (useBulkSaveFlag && BulkChange.contains(this)) {
            return;
        }
        getConfigFile().write(this);
        if (useBulkSaveFlag) {
            SaveableListener.fireOnChange(this, getConfigFile());
        }
    }

    /**
     * Load the settings from the configuration file
     */
    public void load() {
        XmlFile config = getConfigFile();
        try {
            if (config.exists()) {
                config.unmarshal(this);
            }
        } catch (IOException e) {
            logger.error("Failed to load " + config, e);
        }
    }

    /**
     * Get an ACL that provides system wide authorization
     *
     * @return TeamBasedACL with SYSTEM scope
     */
    ACL getRoolACL() {
        return new TeamBasedACL(this, TeamBasedACL.SCOPE.GLOBAL);
    }

    /**
     * Get an ACL that provides job specific authorization
     *
     * @return TeamBasedACL with JOB scope
     */
    ACL getACL(Job<?, ?> job) {
        return new TeamBasedACL(this, TeamBasedACL.SCOPE.JOB, job);
    }

    /**
     * Get an ACL that provides team specific authorization
     *
     * @return TeamBasedACL with JOB scope
     */
    ACL getACL(Team team) {
        return new TeamBasedACL(this, TeamBasedACL.SCOPE.JOB, team);
    }

    private void ensureDefaultTeam() {
        defaultTeam = new DefaultTeam();
        try {
            Team team = findTeam(DefaultTeam.DEFAULT_TEAM_NAME);
            teams.remove(team);
        } catch (TeamNotFoundException ex) {
            // It's ok, we are going to remove it any way
        }
        defaultTeam.loadExistingJobs(hudsonHomeDir);
        teams.add(defaultTeam);
    }

    Team getDefaultTeam() throws TeamNotFoundException {
        return findTeam(DefaultTeam.DEFAULT_TEAM_NAME);
    }

    public String getTeamQualifiedJobId(String jobId) {
        if (defaultTeam.isJobOwner(jobId)){
            return jobId;
        }
                
        Team team = findCurrentUserTeam();
        if ((team != null) && !Team.DEFAULT_TEAM_NAME.equals(team.getName())) {
            jobId = team.getName() + "_" + jobId;
        }
        return jobId;
    }

    public String getJobsFolderName(String jobId) {
        Team team = findJobOwnerTeam(jobId);
        // May be just created job
        if (team == null) {
            team = findCurrentUserTeam();
        }
        if ((team != null) && !Team.DEFAULT_TEAM_NAME.equals(team.getName())) {
            return TEAMS_FOLDER_NAME + "/" + team.getName() + "/" + Team.JOBS_FOLDER_NAME;
        }
        return "jobs";
    }

    public File[] getJobsRootFolders() {
        List<File> jobsRootFolders = new ArrayList<File>();
        for (Team team : teams) {
            if (Team.DEFAULT_TEAM_NAME.equals(team.getName())) {
                jobsRootFolders.addAll(team.getJobsRootFolders(hudsonHomeDir));
            } else {
                jobsRootFolders.addAll(team.getJobsRootFolders(teamsFolder));
            }
        }
        return jobsRootFolders.toArray(new File[jobsRootFolders.size()]);
    }

    public static class TeamNotFoundException extends Exception {

        public TeamNotFoundException(String message) {
            super(message);
        }
    }
}
