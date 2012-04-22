package com.agorikov.rsdnhome.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.agorikov.rsdnhome.common.util.Log;


/**
 * Radix tree: struct for recursive space-effective storage and retrieval of string-like keys.
 * K elementary type for string, i.e. Char is elementary type for String and Byte is elementary type for byte[].
 * @author artem
 *
 * @param <V>
 */
public final class RadixTreeNode<V> {
	final static String TAG = "RadixTreeNode"; 
	
	public interface Cursor<V> {
		Cursor<V> next(char c);
		int length();
		V value();
		Object marker();
	}
	
	public boolean isReal() {
		return isReal;
	}
	public V getValue() {
		if (!isReal())
			throw new RuntimeException();
		return value;
	}
	public void setValue(V v) {
		if (!isReal())
			throw new RuntimeException();
		this.value = v;
	}
	
	public Cursor<V> cursor(final char c, final Object marker) {
		return new Cursor<V>() {
			int counter = 0;
			RadixTreeNode<V> node = RadixTreeNode.this;
			int offset = 0;
			@Override
			public Cursor<V> next(final char c) {
				if (offset == node.key.length) {
					for (final RadixTreeNode<V> child : node.children) {
						if (child.key[0] == c) {
							node = child;
							offset = 1;
							++counter;
							return this;
						}
					}
					return null;
				} else if (node.key[offset] != c) {
					return null;
				}
				++counter;
				++offset;
				return this;
			}
			@Override
			public int length() {
				return counter;
			}
			@Override
			public V value() {
				if (offset == node.key.length && node.isReal)
					return node.value;
				else
					return null;
			}
			@Override
			public Object marker() {
				return marker;
			}
		}.next(c);
	}
	
	protected final int numberOfMatchedCharacters(char[] key) {
		int i = 0;
		while (i < this.key.length && i < key.length && key[i] == this.key[i])
			++i;
		return i;
	}
	
	
	public static <V> RadixTreeNode<V> create() {
		return new RadixTreeNode<V>(new char[0]);
	}
	
	public RadixTreeNode<V> insert(final char[] key) {
		return insert(key, this);
	}
	
	public RadixTreeNode<V> insert(final String key) {
		final char[] k = new char[key.length()];
		for (int i = 0; i != k.length; ++i) {
			k[i] = key.charAt(i);
		}
		return insert(k);
	}
	
	private char[] key;
	private boolean isReal;
	private V value;
	private ArrayList<RadixTreeNode<V>> children = new ArrayList<RadixTreeNode<V>>();

	
	private RadixTreeNode(final char[] key) {
		this.key = key;
	}
	
	private static <V> RadixTreeNode<V> insert(final char[] key, final RadixTreeNode<V> node) {
		
		final int matched = node.numberOfMatchedCharacters(key);
        // we are either at the root node
        // or we need to go down the tree
		if (matched == 0 || matched < key.length && matched >= node.key.length) {
			final char[] newKey = Arrays.copyOfRange(key, matched, key.length);
			for (final RadixTreeNode<V> child : node.children) {
				if (child.key.length > 0 && child.key[0] == newKey[0])
					return insert(newKey, child);
			}
			final RadixTreeNode<V> n = new RadixTreeNode<V>(newKey);
			n.isReal = true;
			node.children.add(n);
			return n;
		}
        // there is a exact match just make the current node as data node
		else if (matched == key.length && matched == node.key.length) {
			if (node.isReal()) {
				throw new RuntimeException("Duplicate key");
			}
			node.isReal = true;
			return node;
		}
        // This node need to be split as the key to be inserted
        // is a prefix of the current node key
		else if (matched > 0 && matched < node.key.length) {
			final char[] newKey = Arrays.copyOfRange(node.key, matched, node.key.length);
			final RadixTreeNode<V> n1 = new RadixTreeNode<V>(newKey);
			n1.isReal = node.isReal();
			n1.value = node.value;
			n1.children = node.children;
			
			node.key = Arrays.copyOfRange(key, 0, matched);
			node.isReal = false;
			node.children = new ArrayList<RadixTreeNode<V>>();
			node.children.add(n1);
			
			if (matched < key.length) {
				final RadixTreeNode<V> n2 = new RadixTreeNode<V>(Arrays.copyOfRange(key, matched, key.length));
				n2.isReal = true;
				node.children.add(n2);
				return n2;
			} else {
				node.isReal = true;
				return node;
			}
		}
        // this key need to be added as the child of the current node
		else {
			final RadixTreeNode<V> n = new RadixTreeNode<V>(Arrays.copyOfRange(node.key, matched, node.key.length));
			n.children = node.children;
			n.isReal = node.isReal;
			n.value = node.value;
			
			node.key = key;
			node.isReal = true;
			node.children.add(n);
			return node;
		}
	}
	
	public static abstract class SubstringMatcher<V> {
		private final Set<Cursor<V>> activeCursors = new HashSet<Cursor<V>>();
		private int longestCursor = 0;
		private int beginIndex = 0;
		private V value = null;
		private final RadixTreeNode<V> root;
		private int counter = 0;
		
		public SubstringMatcher(final RadixTreeNode<V> root) {
			this.root = root;
		}
		
		public final void putc(char c) {
			final List<Cursor<V>> terminatedCursors = new LinkedList<Cursor<V>>();
			
			for (final Cursor<V> activeCursor : activeCursors) {
				if (activeCursor.next(c) == null) {
					terminatedCursors.add(activeCursor);
					if (activeCursor.value() != null && activeCursor.length() > longestCursor) {
						longestCursor = activeCursor.length();
						beginIndex = (Integer) activeCursor.marker();
						value = activeCursor.value();
					}
				}
			}
			
			activeCursors.removeAll(terminatedCursors);
			if (activeCursors.isEmpty()) {
				if (longestCursor != 0) {
					final int endIndex = beginIndex + longestCursor;
					markSubstring(beginIndex, endIndex, value);
				}
				longestCursor = 0;
			}
			
			final Cursor<V> cursor = root.cursor(c, counter);
			if (cursor != null)
				activeCursors.add(cursor);
			++counter;
		}

		protected abstract void markSubstring(int start, int end, V value);

		public void finish() {
			for (final Cursor<V> activeCursor : activeCursors) {
				if (activeCursor.value() != null && activeCursor.length() > longestCursor) {
					longestCursor = activeCursor.length();
					beginIndex = (Integer) activeCursor.marker();
					value = activeCursor.value();
				}
			}
			activeCursors.clear();
			if (longestCursor != 0) {
				final int endIndex = beginIndex + longestCursor;
				markSubstring(beginIndex, endIndex, value);
			}
			longestCursor = 0;
		}
	}
	
	
	// =======================================
	
	public static void main(final String[] args) {
		final RadixTreeNode<Integer> root = RadixTreeNode.create();
		root.insert(":)").setValue(1);
		root.insert(":))").setValue(2);
		root.insert(":)))").setValue(3);
		root.insert(":\\").setValue(4);
		root.insert(":user:").setValue(5);
		
		final String testOutput = "Big lazy fox:)):user:) feels freaked :\\ yes:)))";
		final SubstringMatcher<Integer> matcher = new SubstringMatcher<Integer>(root) {
			@Override
			protected void markSubstring(int beginIndex, int endIndex,
					Integer value) {
				Log.d(TAG, String.format("Longest cursor found: {%d, %d} : \"%s\" : value(%d)", beginIndex, endIndex, testOutput.subSequence(beginIndex, endIndex), value));
			}			
		};
		
		for (int i = 0; i < testOutput.length(); ++i) {
			final char c = testOutput.charAt(i);
			matcher.putc(c);
		}
		matcher.finish();
		
//		final Set<Cursor<Integer>> activeCursors = new HashSet<Cursor<Integer>>();
//		int longestCursor = 0;
//		int beginIndex = 0;
//		int value = -1;
//		for (int i = 0; i < testOutput.length(); ++i) {
//			final char c = testOutput.charAt(i);
//			final List<Cursor<Integer>> terminatedCursors = new LinkedList<Cursor<Integer>>();
//			
//			for (final Cursor<Integer> activeCursor : activeCursors) {
//				if (activeCursor.next(c) == null) {
//					terminatedCursors.add(activeCursor);
//					if (activeCursor.value() != null && activeCursor.length() > longestCursor) {
//						longestCursor = activeCursor.length();
//						beginIndex = (Integer) activeCursor.marker();
//						value = activeCursor.value();
//					}
//				}
//			}
//			
//			activeCursors.removeAll(terminatedCursors);
//			if (activeCursors.isEmpty()) {
//				if (longestCursor != 0) {
//					final int endIndex = beginIndex + longestCursor;
//					Log.d(TAG, String.format("Longest cursor found: {%d, %d} : \"%s\" : value(%d)", beginIndex, endIndex, testOutput.subSequence(beginIndex, endIndex), value));
//				}
//				longestCursor = 0;
//			}
//			
//			final Cursor<Integer> cursor = root.cursor(c, i);
//			if (cursor != null)
//				activeCursors.add(cursor);
//			
//			
//		}
		
		
	}

}
