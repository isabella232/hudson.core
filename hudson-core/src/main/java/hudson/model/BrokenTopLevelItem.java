/*******************************************************************************
 *
 * Copyright (c) 2004-2014 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Roy Varghese
 */

package hudson.model;

import java.util.SortedMap;

/**
 * A placeholder for TopLevelItems that could not be loaded for whatever
 * reason.
 * 
 * @author rovarghe@gmail.com
 */
public class BrokenTopLevelItem extends Job implements TopLevelItem {
    
    final private RunMap runMap;
    final private Exception exception;
    final private LazyTopLevelItem.Key key;
    
    BrokenTopLevelItem(LazyTopLevelItem.Key key,
                       Exception exception) {
        super(key.parent, key.name);
        
        this.key = key;
        this.exception = exception;
        this.runMap = new RunMap(this);
    }
    
    @Override
    public String getDescription() {
        return "An error occurred while loading this item:" + exception.getMessage();
    }

    @Override
    public boolean isBuildable() {
        return false;
    }

    @Override
    public BuildHistory getBuildHistoryData() {
        return runMap;
    }

    @Override
    protected void removeRun(Run run) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public TopLevelItemDescriptor getDescriptor() {
        return new TopLevelItemDescriptor() {

            @Override
            public String getDisplayName() {
                return key.name + " (Broken/Disabled)";
            }

            @Override
            public TopLevelItem newInstance(ItemGroup parent, String name) {
                throw new UnsupportedOperationException("Not supported."); 
            }
            
        };
    }

    @Override
    protected SortedMap _getRuns() {
        return runMap;
    }

    
}
