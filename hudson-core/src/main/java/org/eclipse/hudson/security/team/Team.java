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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A simple model to hold team members and name of jobs belong to the team
 * @since 3.1.0
 * @author Winston Prakash
 */
public class Team {

    private List<String> members = new CopyOnWriteArrayList<String>();
    private List<String> ownedJobNames = new CopyOnWriteArrayList<String>();
    private String teamName;

    public Team(String name) {
        teamName = name;
    }
    
    public String getName(){
        return teamName;
    }

    public void addMember(String userName) {
        members.add(userName);
    }

    public void removeMember(String userName) {
        members.remove(userName);
    }

    public boolean isMember(String userName) {
        return members.contains(userName);
    }

    public void addJobName(String jobname) {
        ownedJobNames.add(jobname);
    }

    public void removeJobName(String jobname) {
        ownedJobNames.remove(jobname);
    }

    public boolean isJobOwner(String jobName) {
        return ownedJobNames.contains(jobName);
    }

    void renameJobName(String oldJobName, String newJobName) {
        ownedJobNames.remove(oldJobName);
        ownedJobNames.add(newJobName);
    }
}
