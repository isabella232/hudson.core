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

package hudson.model;

import org.apache.commons.lang3.StringUtils;

public class StringProperty extends BaseProperty<String> {

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