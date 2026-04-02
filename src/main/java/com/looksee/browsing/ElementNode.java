package com.looksee.browsing;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * A node in a tree of {@link ElementNode}s
 */
@Getter
@Setter
public class ElementNode<T> {
	private List<ElementNode<T>> children = new ArrayList<ElementNode<T>>();
	private ElementNode<T> parent = null;
	private T data = null;

	/**
	 * Constructs an {@link ElementNode}
	 *
	 * @param data the data
	 */
	public ElementNode(T data) {
        this.data = data;
    }

	/**
	 * Constructs an {@link ElementNode}
	 *
	 * @param data the data
	 * @param parent the parent
	 */
    public ElementNode(T data, ElementNode<T> parent) {
        this.data = data;
        this.parent = parent;
    }

	/**
	 * Adds a child to the {@link ElementNode}
	 *
	 * @param data the data
	 *
	 * precondition: data != null
	 */
    public void addChild(T data) {
        assert data != null;
        ElementNode<T> child = new ElementNode<T>(data);
        child.setParent(this);
        this.children.add(child);
    }

	/**
	 * Adds a child to the {@link ElementNode}
	 *
	 * @param child the child
	 *
	 * precondition: child != null
	 */
    public void addChild(ElementNode<T> child) {
        assert child != null;
        child.setParent(this);
        this.children.add(child);
    }

	/**
	 * Checks if the {@link ElementNode} is a root
	 *
	 * @return true if the {@link ElementNode} is a root, otherwise false
	 */
    public boolean isRoot() {
        return (this.parent == null);
    }

	/**
	 * Checks if the {@link ElementNode} is a leaf
	 *
	 * @return true if the {@link ElementNode} is a leaf, otherwise false
	 */
    public boolean isLeaf() {
        return this.children.size() == 0;
    }

	/**
	 * Removes the parent of the {@link ElementNode}
	 */
    public void removeParent() {
        this.parent = null;
    }
}
