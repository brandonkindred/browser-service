package com.looksee.browser;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class ElementNodeTest {

    @Test
    public void testConstructorWithData() {
        ElementNode<String> node = new ElementNode<>("root");
        assertEquals("root", node.getData());
        assertNull(node.getParent());
        assertTrue(node.getChildren().isEmpty());
    }

    @Test
    public void testConstructorWithParent() {
        ElementNode<String> parent = new ElementNode<>("parent");
        ElementNode<String> child = new ElementNode<>("child", parent);
        assertEquals("child", child.getData());
        assertEquals(parent, child.getParent());
    }

    @Test
    public void testAddChildByData() {
        ElementNode<String> parent = new ElementNode<>("parent");
        parent.addChild("child1");
        parent.addChild("child2");

        assertEquals(2, parent.getChildren().size());
        assertEquals("child1", parent.getChildren().get(0).getData());
        assertEquals("child2", parent.getChildren().get(1).getData());
        assertEquals(parent, parent.getChildren().get(0).getParent());
    }

    @Test
    public void testAddChildByNode() {
        ElementNode<String> parent = new ElementNode<>("parent");
        ElementNode<String> child = new ElementNode<>("child");
        parent.addChild(child);

        assertEquals(1, parent.getChildren().size());
        assertEquals(child, parent.getChildren().get(0));
        assertEquals(parent, child.getParent());
    }

    @Test
    public void testIsRoot() {
        ElementNode<String> root = new ElementNode<>("root");
        assertTrue(root.isRoot());

        ElementNode<String> child = new ElementNode<>("child", root);
        assertFalse(child.isRoot());
    }

    @Test
    public void testIsLeaf() {
        ElementNode<String> node = new ElementNode<>("leaf");
        assertTrue(node.isLeaf());

        node.addChild("child");
        assertFalse(node.isLeaf());
    }

    @Test
    public void testRemoveParent() {
        ElementNode<String> parent = new ElementNode<>("parent");
        ElementNode<String> child = new ElementNode<>("child", parent);
        assertFalse(child.isRoot());

        child.removeParent();
        assertTrue(child.isRoot());
        assertNull(child.getParent());
    }

    @Test
    public void testTreeStructure() {
        ElementNode<Integer> root = new ElementNode<>(1);
        root.addChild(2);
        root.addChild(3);
        root.getChildren().get(0).addChild(4);

        assertTrue(root.isRoot());
        assertFalse(root.isLeaf());
        assertFalse(root.getChildren().get(0).isLeaf());
        assertTrue(root.getChildren().get(1).isLeaf());
        assertTrue(root.getChildren().get(0).getChildren().get(0).isLeaf());
    }

    @Test
    public void testSetData() {
        ElementNode<String> node = new ElementNode<>("original");
        node.setData("modified");
        assertEquals("modified", node.getData());
    }
}
