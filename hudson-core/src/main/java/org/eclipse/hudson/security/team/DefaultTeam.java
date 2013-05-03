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

import java.io.File;
import java.util.List;

/**
 * The default team contains the jobs not specific to any team. 
 * Every one has read access to these jobs except sysadmin
 * @since 3.1.0
 * @author Winston Prakash
 */
public final class DefaultTeam extends Team{
    
    DefaultTeam() {
        super(DEFAULT_TEAM_NAME);
    }
    
    /**
     * Scan and find the jobs in the existing Hudson home and add them to the 
     * default team.
     * @param hudsonHome 
     */
    void loadExistingJobs(File rootFolder){
        List<File> jobRootFolders = getJobsRootFolders(rootFolder);
        for (File file : jobRootFolders){
            addJob(file.getName());
        }
    }
    
    @Override
    protected File getJobsFolder(File rootFolder){
        return new File(rootFolder, "/" + "jobs");
    }
}
