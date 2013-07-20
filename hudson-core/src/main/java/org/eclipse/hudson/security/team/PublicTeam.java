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

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * The public team contains the jobs not specific to any team. 
 * Every one has read access to these jobs except sysadmin
 * @since 3.1.0
 * @author Winston Prakash
 */
public final class PublicTeam extends Team{
    
    //Used for unmarshalling
    PublicTeam(){
        
    }
    PublicTeam(TeamManager teamManager) {
        super(PUBLIC_TEAM_NAME, teamManager);
    }
    
    /**
     * Scan and find the jobs in the existing Hudson home and add them to the 
     * public team.
     * @param hudsonHome 
     */
    void loadExistingJobs(File rootFolder) throws IOException{
        List<File> jobRootFolders = getJobsRootFolders(rootFolder, true/*initializingTeam*/);
        for (File file : jobRootFolders){
            TeamJob job = new TeamJob(file.getName());
            job.addVisibility(PUBLIC_TEAM_NAME); 
            addJob(job);
        }
    }
    
    @Override
    protected File getJobsFolder(File rootFolder){
        return new File(rootFolder, "/" + "jobs");
    }
    
    public static class ConverterImpl implements Converter {

        @Override
        public boolean canConvert(Class type) {
            return type == PublicTeam.class;
        }

        @Override
        public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext mc) {
            Team team = (Team) source;
            writer.startNode("name");
            writer.setValue(team.getName());
            writer.endNode();
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext uc) {
            return new PublicTeam();
        }
    }
}
