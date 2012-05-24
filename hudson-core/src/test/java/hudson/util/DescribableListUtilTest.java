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
package hudson.util;

import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.tasks.Mailer;
import hudson.tasks.Publisher;
import java.io.IOException;
import java.util.Map;
import org.eclipse.hudson.model.project.property.ExternalProjectProperty;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.easymock.EasyMock.expect;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;

/**
 * Test for DescribableListUtil class
 * </p>
 * Date: 10/20/2011
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Hudson.class, FreeStyleProject.class, Mailer.DescriptorImpl.class})
public class DescribableListUtilTest {

    private Job job;

    @Test
    public void testConvertToProjectProperties1() {
        prepareJob();
        DescribableList<Publisher, Descriptor<Publisher>> list = null;
        Map<String, ExternalProjectProperty<Publisher>> map = DescribableListUtil.convertToProjectProperties(list, job);
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testConvertToProjectProperties2() {
        prepareJob();
        DescribableList<Publisher, Descriptor<Publisher>> list
            = new DescribableList<Publisher, Descriptor<Publisher>>();
        Map<String, ExternalProjectProperty<Publisher>> map = DescribableListUtil.convertToProjectProperties(list, job);
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testConvertToProjectProperties3() throws IOException {
        Hudson hudson = createMock(Hudson.class);
        Mailer.DescriptorImpl descriptor = createMock(Mailer.DescriptorImpl.class);
        String mailerName = "hudson-tasks-Mailer";
        expect(descriptor.getJsonSafeClassName()).andReturn(mailerName);
        expect(hudson.getDescriptorOrDie(Mailer.class)).andReturn(descriptor);
        mockStatic(Hudson.class);
        expect(Hudson.getInstance()).andReturn(hudson).anyTimes();
        prepareJob();

        DescribableList<Publisher, Descriptor<Publisher>> list
            = new DescribableList<Publisher, Descriptor<Publisher>>();

        list.add(new Mailer());

        Map<String, ExternalProjectProperty<Publisher>> map = DescribableListUtil.convertToProjectProperties(list, job);
        assertNotNull(map);
        assertEquals(map.size(), 1);
        assertNotNull(map.get(mailerName));
        assertEquals(map.get(mailerName).getValue().getClass(), Mailer.class);

    }

    public void prepareJob() {
        job = createMock(FreeStyleProject.class);
        expect(job.hasCascadingProject()).andReturn(false);
        replayAll();
    }
}
