/*******************************************************************************
 *
 * Copyright (c) 2011 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *    Nikita Levyankov
 *
 *******************************************************************************/
package org.eclipse.hudson.model.project.property;

import org.eclipse.hudson.api.model.ICascadingJob;

/**
 * Represents integer property for project.
 * <p/>
 * Date: 9/22/11
 *
 * @author Nikita Levyankov
 */
public class IntegerProjectProperty extends BaseProjectProperty<Integer> {

    public IntegerProjectProperty(ICascadingJob job) {
        super(job);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getDefaultValue() {
        return 0;
    }
}
