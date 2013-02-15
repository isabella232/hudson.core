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
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;
import hudson.security.SecurityRealm;
import hudson.util.XStream2;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
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

    private List<Team> teams = new CopyOnWriteArrayList<Team>();
    private transient final String teamsConfigFileName = "teams.xml";
    private static final XStream XSTREAM = new XStream2();
    private transient Logger logger = LoggerFactory.getLogger(TeamManager.class);
    private transient File hudsonHomeDir;

    public TeamManager(File homeDir) {
        hudsonHomeDir = homeDir;
        load();
    }

    public List<Team> getTeams() {
        return teams;
    }

    public Team createTeam(String teamName) {
        Team newTeam = new Team(teamName);
        teams.add(newTeam);
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

    private Team findCurrentUserTeam() {
        Team team;
        SecurityRealm securityRelm = Hudson.getInstance().getSecurityManager().getSecurityRealm();
        if (securityRelm instanceof TeamAwareSecurityRealm) {
            TeamAwareSecurityRealm teamAwareSecurityRealm = (TeamAwareSecurityRealm) securityRelm;
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
            if (userTeam.isJobOwner(jobName)){
                return true;
            }else{
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
            team.addJobName(jobName);
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
            team.removeJobName(jobName);
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
            team.renameJobName(oldJobName, newJobName);
            save();
        }
    }

    /**
     * The file where the teams settings are saved.
     */
    private XmlFile getConfigFile() {
        return new XmlFile(XSTREAM, new File(hudsonHomeDir, teamsConfigFileName));
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

    public static class TeamNotFoundException extends Exception {

        public TeamNotFoundException(String message) {
            super(message);
        }
    }
}
