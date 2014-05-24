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
 * Team based slave isolation information.
 *
 * @since 3.2.0
 * @author Winston Prakash
 */
public class TeamNode extends TeamItem{

    public TeamNode() {
    }

    public TeamNode(String id) {
        super(id);
    }
    
    public static class ConverterImpl extends TeamItem.ConverterImpl {

        @Override
        public boolean canConvert(Class type) {
            return type == TeamJob.class;
        }

        @Override
        public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
            super.marshal(source, writer, context);
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext uc) {
            TeamNode teamNode = new TeamNode();
            TeamItem teamItem = (TeamItem) uc.convertAnother(teamNode, TeamItem.class);
            teamNode.setId(teamItem.getId());
            teamNode.visibilityToTeams = teamItem.visibilityToTeams;
            return teamNode;
        }
    }
}
