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

/**
 * The default team contains the jobs not specific to any team. 
 * Every one has read access to these jobs except sysadmin
 * @since 3.1.0
 * @author Winston Prakash
 */
public class DefaultTeam extends Team{
    static final String DEFAULT_TEAM_NAME = "default";
    public DefaultTeam() {
        super(DEFAULT_TEAM_NAME);
    }
    
    /**
     * Scan and find the jobs in the existing Hudson home and add them to the 
     * default team.
     * @param hudsonHome 
     */
    void loadExistingJobs(File hudsonHome){
        // TODO: scan the Hudson home find all the existing jobs that 
        // don't belong to any team
    }
    
}
