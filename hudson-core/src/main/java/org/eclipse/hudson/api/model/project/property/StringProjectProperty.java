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

package org.eclipse.hudson.api.model.project.property;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.hudson.api.model.IJob;
import org.eclipse.hudson.api.model.project.property.BaseProjectProperty;

/**
 * String property for project.
 * <p/>
 * Date: 9/22/11
 *
 * @author Nikita Levyankov
 */
public class StringProjectProperty extends BaseProjectProperty<String> {

    public StringProjectProperty(IJob job) {
        super(job);
    }

    @Override
    protected String prepareValue(String candidateValue) {
        return StringUtils.trimToNull(candidateValue);
    }

    /**
     * {@inheritDoc}
     */
    protected boolean allowOverrideValue(String cascadingValue, String candidateValue) {
        return !StringUtils.equalsIgnoreCase(cascadingValue, candidateValue);
    }
}