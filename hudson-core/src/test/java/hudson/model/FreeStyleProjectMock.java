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

/**
 * Mock class for FreeStyleProject
 * <p/>
 * Date: 9/27/11
 *
 * @author Nikita Levyankov
 */
class FreeStyleProjectMock extends FreeStyleProject {

    public FreeStyleProjectMock(String name) {
        super((ItemGroup) null, name);
        setAllowSave(false);
    }

    @Override
    protected void updateTransientActions() {
    }
}