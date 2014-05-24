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

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * A simple model to hold job visibility information
 *
 * @since 3.1.0
 * @author Winston Prakash
 */
public class TeamJob extends TeamItem{

    private boolean allow_config_view;

    public TeamJob() {
    }

    public TeamJob(String id) {
        super(id);
    }
    
    public boolean isAllowConfigView() {
        return allow_config_view;
    }

    public void setAllowConfigView(boolean allow_config_view) {
        this.allow_config_view = allow_config_view;
    }

    public static class ConverterImpl extends TeamItem.ConverterImpl {

        @Override
        public boolean canConvert(Class type) {
            return type == TeamJob.class;
        }

        @Override
        public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
            super.marshal(source, writer, context);
            TeamJob teamJob = (TeamJob) source;
            if (teamJob.isAllowConfigView()){
                writer.startNode("allowConfigView");
                writer.setValue("true");
                writer.endNode();
            }
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext uc) {
            TeamJob teamJob = new TeamJob();
            TeamItem teamItem = (TeamItem) uc.convertAnother(teamJob, TeamItem.class);
            teamJob.setId(teamItem.getId());
            teamJob.visibilityToTeams = teamItem.visibilityToTeams;
            while (reader.hasMoreChildren()) {
                reader.moveDown();
                if ("allowConfigView".equals(reader.getNodeName())) {
                    if ("true".equals(reader.getValue())) {
                        teamJob.allow_config_view = true;
                    }
                }
                reader.moveUp();
            }
            return teamJob;
        }
    }
}
