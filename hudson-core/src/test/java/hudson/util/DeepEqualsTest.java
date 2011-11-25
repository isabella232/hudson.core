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
 *   Anton Kozak
 *
 *
 *******************************************************************************/
package hudson.util;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertFalse;


/**
 * Test for {@link DeepEquals#deepEquals(Object, Object)}
 */
public class DeepEqualsTest {

    private static class Person {
        private String name;
        private Pet pet;

        private Person(String name, Pet pet) {
            this.name = name;
            this.pet = pet;
        }
    }

    private static class Pet {
        private String name;

        private Pet(String name) {
            this.name = name;
        }
    }

    /**
     * Tests simple deepEquals functionality.
     */
    @Test
    public void testDeepEqualsSimple() {

        Pet pet1 = new Pet("petName");
        Pet pet2 = new Pet("petName");
        Pet pet3 = new Pet("petName1");

        Person person1 = new Person("name", pet1);
        Person person2 = new Person("name", pet2);
        Person person3 = new Person("name", pet3);
        Person person4 = new Person("name1", pet1);

        assertTrue(DeepEquals.deepEquals(null, null));
        assertFalse(DeepEquals.deepEquals(pet1, null));
        assertFalse(DeepEquals.deepEquals(null, pet1));

        assertTrue(DeepEquals.deepEquals(pet1, pet1));
        assertTrue(DeepEquals.deepEquals(pet1, pet2));
        assertFalse(DeepEquals.deepEquals(pet1, pet3));

        assertTrue(DeepEquals.deepEquals(person1, person1));
        assertTrue(DeepEquals.deepEquals(person1, person2));
        assertFalse(DeepEquals.deepEquals(person1, person3));
        assertFalse(DeepEquals.deepEquals(person1, person4));
    }

    private static class Node {
        private Object value;
        private Node next;

        private Node(Object value) {
            this(value, null);
        }

        private Node(Object value, Node next) {
            this.value = value;
            this.next = next;
        }

        public void setNext(Node next) {
            this.next = next;
        }
    }

    /**
     * Tests deepEquals for deeply nested objects.
     */
    @Test
    public void testDeepEquals() {
        Node n0 = new Node(1, new Node(2));
        Node n1 = new Node(1, new Node(2, new Node(3)));
        Node n2 = new Node(1, new Node(2, new Node(3)));
        Node n3 = new Node(1, new Node(2, new Node(4)));
        Node n4 = new Node(1, new Node(2, new Node(4)));
        Node n5 = new Node(1, new Node(2, new Node(4, new Node(5))));

        assertTrue(DeepEquals.deepEquals(n1, n1));
        assertTrue(DeepEquals.deepEquals(n1, n2));
        assertTrue(DeepEquals.deepEquals(n3, n4));

        assertFalse(DeepEquals.deepEquals(n0, n1));
        assertFalse(DeepEquals.deepEquals(n0, n3));
        assertFalse(DeepEquals.deepEquals(n2, n4));
        assertFalse(DeepEquals.deepEquals(n4, n5));
    }

    /**
     * Tests whether recursive objects process correctly.
     */
    @Test
    public void testDeepEqualsRecursively() {
        Node n0 = new Node(1);
        Node n1 = new Node(1, n0);
        n0.setNext(n1);
        assertTrue(DeepEquals.deepEquals(n0, n1));
    }

    /**
     * Tests deepEquals for lists.
     */
    @Test
    public void testDeepEqualsLists() {
        List<Node> list1 = Arrays.asList(new Node(1, new Node(3)), new Node(2, new Node(4)));
        List<Node> list2 = Arrays.asList(new Node(1, new Node(3)), new Node(2, new Node(4)));
        List<Node> list3 = Arrays.asList(new Node(1, new Node(3)), new Node(2, new Node(3)));
        assertTrue(DeepEquals.deepEquals(list1, list2));
        assertFalse(DeepEquals.deepEquals(list1, list3));
    }
}
