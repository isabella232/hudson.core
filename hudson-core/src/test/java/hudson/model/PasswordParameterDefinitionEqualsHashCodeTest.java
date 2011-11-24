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
 *
 *******************************************************************************/
package hudson.model;

import hudson.util.Secret;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.easymock.EasyMock.expect;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;


/**
 * Equals and hashCode test for {@link PasswordParameterDefinition} object
 * <p/>
 * Date: 11/2/11
 *
 * @author Nikita Levyankov
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Secret.class, Hudson.class})
public class PasswordParameterDefinitionEqualsHashCodeTest {

    @Before
    public void setUp() throws Exception {
        Hudson hudson = createMock(Hudson.class);
        mockStatic(Hudson.class);
        expect(Hudson.getInstance()).andReturn(hudson).anyTimes();
        mockStatic(Secret.class);
        Secret secret = Whitebox.invokeConstructor(Secret.class, new Class<?>[]{String.class}, new String[]{"value"});
        expect(Secret.fromString(EasyMock.<String>anyObject())).andReturn(secret).anyTimes();
        expect(Secret.toString(EasyMock.<Secret>anyObject())).andReturn(secret.toString()).anyTimes();
        replayAll();
    }

    @Test
    public void testEquals() throws Exception {
        PasswordParameterDefinition o1 = new PasswordParameterDefinition("name1", "value", "description");
        assertEquals(o1, o1);
        assertFalse(o1.equals(null));
        assertFalse(o1.equals(new StringParameterDefinition("test1", "value", "description")));
        assertFalse(new PasswordParameterDefinition("name1", "value", null).equals(
            new PasswordParameterDefinition(null, "value", null)));

        assertEquals(o1, new PasswordParameterDefinition("name1", "value", "description"));
        assertEquals(o1, new PasswordParameterDefinition("name1", "value", "description1"));
        assertEquals(new PasswordParameterDefinition(null, "value", "d1"),
            new PasswordParameterDefinition(null, "value", "d1"));
        assertEquals(new PasswordParameterDefinition(null, "value", null),
            new PasswordParameterDefinition(null, "value", null));
        verifyAll();
    }

    @Test
    public void testHashCode() {
        PasswordParameterDefinition o1 = new PasswordParameterDefinition("name1", "value", "description");
        assertTrue(o1.hashCode() == o1.hashCode());
        assertFalse(o1.hashCode() == new StringParameterDefinition("test1", "value", "description").hashCode());
        assertFalse(new PasswordParameterDefinition("name1", "value", null).hashCode() ==
            new PasswordParameterDefinition(null, "value", null).hashCode());
        assertTrue(o1.hashCode() == new PasswordParameterDefinition("name1", "value", "description").hashCode());
        assertTrue(o1.hashCode() == new PasswordParameterDefinition("name1", "value", "description1").hashCode());
        assertTrue(new PasswordParameterDefinition(null, "value", "d1").hashCode() ==
            new PasswordParameterDefinition(null, "value", "d1").hashCode());
        assertTrue(new PasswordParameterDefinition(null, "value", null).hashCode() ==
            new PasswordParameterDefinition(null, "value", null).hashCode());
        verifyAll();
    }
}
