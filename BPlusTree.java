import java.util.AbstractMap;
import java.util.Map.Entry;
import java.util.ArrayList;


/**
 * BPlusTree Class Assumptions: 1. No duplicate keys inserted 2. Order D:
 * D<=number of keys in a node <=2*D 3. All keys are non-negative
 * TODO: Rename to BPlusTree
 */
public class BPlusTree<K extends Comparable<K>, T> {

	public Node<K,T> root;
	public static final int D = 2;

	/**
	 * TODO Search the value for a specific key
	 * 
	 * @param key
	 * @return value
	 */
	public T search(K key) {
		// Look for leaf node that key is pointing to
		LeafNode<K,T> leaf = (LeafNode<K,T>)treeSearch(root, key);
		
		// Look for value in the leaf
		for(int i=0; i<leaf.keys.size(); i++) {
			if(key.compareTo(leaf.keys.get(i)) == 0) {
				return leaf.values.get(i);
			}
		}
		
		return null;
	}
	
	private Node<K,T> treeSearch(Node<K,T> node, K key) {
		if(node.isLeafNode) {
			return node;
		} else {
			// The node is index node
			IndexNode<K,T> index = (IndexNode<K,T>)node;
			// K < K1, return treeSearch(P0, K)
			if(key.compareTo(node.keys.get(0)) < 0) {
				return treeSearch((Node<K,T>)index.children.get(0), key);
			}
			// K >= Km, return treeSearch(Pm, K), m = #entries
			else if(key.compareTo(node.keys.get(node.keys.size()-1)) >= 0) {
				return treeSearch((Node<K,T>)index.children.get(index.children.size()-1), key);
			}
			// Find i such that Ki <= K < K(i+1), return treeSearch(Pi,K)
			else {
				// Linear searching
				int hit = 0;
				for(int i=0; i<index.keys.size(); i++) {
					K iKey = index.keys.get(i);
					if(key.compareTo(iKey) == 0) {
						hit = i;
						break;
					}
				}
				return treeSearch((Node<K,T>)index.children.get(hit), key);
 			}
		}
	} 
	
//
//	public T search(K key) {
//        if (root == null || key == null)
//            return null;
//        LeafNode leafFound = (LeafNode)treeSearch(root, key); // down casting
//
//        T val = null;
//        for(int pos = 0; pos < leafFound.keys.size(); pos++){
//            if (key.compareTo((K) (leafFound.keys.get(pos))) == 0)
//                val = (T)leafFound.values.get(pos);
//        }
//
//        return val;
//	}
	
	
	/**
	 * TODO Insert a key/value pair into the BPlusTree
	 * 
	 * @param key
	 * @param value
	 */
	public void insert(K key, T value) {
		LeafNode<K,T> newLeaf = new LeafNode<K,T>(key, value);
		Entry<K, Node<K,T>> entry = new AbstractMap.SimpleEntry<K, Node<K,T>>(key, newLeaf);
		
		// Insert entry into subtree with root node pointer
		if(root == null) {
			root = entry.getValue();
		}
		
		// newChildEntry null initially, and null on return unless child is split
		Entry<K, Node<K,T>> newChildEntry = getChildEntry(root, entry, null);
		
		if(newChildEntry == null) {
			return;
		} else {
			IndexNode<K,T> newRoot = new IndexNode<K,T>(newChildEntry.getKey(), root, newChildEntry.getValue());
			root = newRoot;
			return;
		}
	}
	
	private Entry<K, Node<K,T>> getChildEntry(Node<K,T> node, Entry<K, Node<K,T>> entry, Entry<K, Node<K,T>> newChildEntry) {
		if(!node.isLeafNode) {
			// Choose subtree, find i such that Ki <= entry's key value < J(i+1)
			IndexNode<K,T> index = (IndexNode<K,T>) node;
			int i = 0;
			while(i < node.keys.size()) {
				if(entry.getKey().compareTo(node.keys.get(i)) < 0) {
					break;
				}
				i++;
			}
			// Recursively, insert entry
			newChildEntry = getChildEntry((Node<K,T>) index.children.get(i), entry, newChildEntry);
			// Usual case, didn't split child
			if(newChildEntry == null) {
				return newChildEntry;
			} 
			// Split child, must insert newChildEntry in node
			else {
				int j = 0;
				while (j < index.keys.size()) {
					if(newChildEntry.getKey().compareTo(node.keys.get(j)) < 0) {
						break;
					}
					j++;
				}
				
				index.insertSorted(newChildEntry, j);
				
				// Usual case, put newChildEntry on it, set newChildEntry to null, return
				if(!index.isOverflowed()) {
					newChildEntry = null;
					return newChildEntry;
				} 
				else{
					newChildEntry = splitIndexNode(index);
					// Root was just split
					if(index == root) {
						// Create new node and make tree's root-node pointer point to newRoot
						IndexNode<K,T> newRoot = new IndexNode<K,T>(newChildEntry.getKey(), root, newChildEntry.getValue());
						root = newRoot;
						newChildEntry = null;
						return newChildEntry;
					}
					return newChildEntry;
				}
			}
		}
		// Node pointer is a leaf node
		else {
			LeafNode<K,T> leaf = (LeafNode<K,T>)node;
			LeafNode<K,T> newLeaf = (LeafNode<K,T>)entry.getValue();
			
			leaf.insertSorted(entry.getKey(), newLeaf.values.get(0));
			
			// Usual case: leaf has space, put entry and set newChildEntry to null and return
			if(!leaf.isOverflowed()) {
				newChildEntry = null;
				return newChildEntry;
			}
			// Once in a while, the leaf is full
			else {
				newChildEntry = splitLeafNode(leaf);
				if(leaf == root) {
					IndexNode<K,T> newRoot = new IndexNode<K,T>(newChildEntry.getKey(), leaf, newChildEntry.getValue());
					root = newRoot;
					newChildEntry = null;
					return newChildEntry;
				}
				return newChildEntry;
			}
		}
	}

	/**
	 * TODO Split a leaf node and return the new right node and the splitting
	 * key as an Entry<slitingKey, RightNode>
	 * 
	 * @param leaf, any other relevant data
	 * @return the key/node pair as an Entry
	 */
	public Entry<K, Node<K,T>> splitLeafNode(LeafNode<K,T> leaf) {
		ArrayList<K> newKeys = new ArrayList<K>();
		ArrayList<T> newValues = new ArrayList<T>();
		
		// The rest D entries move to brand new node
		for(int i=D; i<=2*D; i++) {
			newKeys.add(leaf.keys.get(i));
			newValues.add(leaf.values.get(i));
		}
		
		// First D entries stay
		for(int i=D; i<=2*D; i++) {
			leaf.keys.remove(leaf.keys.size()-1);
			leaf.values.remove(leaf.values.size()-1);
		}
		
		K splitKey = newKeys.get(0);
		LeafNode<K,T> rightNode = new LeafNode<K,T>(newKeys, newValues);
		
		// Set sibling pointers
		LeafNode<K,T> tmp = leaf.nextLeaf;
		leaf.nextLeaf = rightNode;
		leaf.nextLeaf.previousLeaf = rightNode;
		rightNode.previousLeaf = leaf;
		rightNode.nextLeaf = tmp;
        
		Entry<K, Node<K,T>> newChildEntry = new AbstractMap.SimpleEntry<K, Node<K,T>>(splitKey, rightNode);
		
		return newChildEntry;
	}

	/**
	 * TODO split an indexNode and return the new right node and the splitting
	 * key as an Entry<slitingKey, RightNode>
	 * 
	 * @param index, any other relevant data
	 * @return new key/node pair as an Entry
	 */
	public Entry<K, Node<K,T>> splitIndexNode(IndexNode<K,T> index) {
		ArrayList<K> newKeys = new ArrayList<K>();
		ArrayList<Node<K,T>> newChildren = new ArrayList<Node<K,T>>();
		
		// Note difference with splitting leaf page, 2D+1 key values and 2D+2 node pointers
		K splitKey = index.keys.get(D);
		index.keys.remove(D);
		
		// First D key values and D+1 node pointers stay
		// Last D keys and D+1 pointers move to new node
		newChildren.add(index.children.get(D+1));
		index.children.remove(D+1);
		
		while(index.keys.size() > D) {
			newKeys.add(index.keys.get(D));
			index.keys.remove(D);
			newChildren.add(index.children.get(D+1));
			index.children.remove(D+1);
		}

		IndexNode<K,T> rightNode = new IndexNode<K,T>(newKeys, newChildren);
		Entry<K, Node<K,T>> newChildEntry = new AbstractMap.SimpleEntry<K, Node<K,T>>(splitKey, rightNode);

		return newChildEntry;
	}
	
	/**
	 * TODO Delete a key/value pair from this B+Tree
	 * 
	 * @param key
	 */
	public void delete(K key) {

	}

	/**
	 * TODO Handle LeafNode Underflow (merge or redistribution)
	 * 
	 * @param left
	 *            : the smaller node
	 * @param right
	 *            : the bigger node
	 * @param parent
	 *            : their parent index node
	 * @return the splitkey position in parent if merged so that parent can
	 *         delete the splitkey later on. -1 otherwise
	 */
	public int handleLeafNodeUnderflow(LeafNode<K,T> left, LeafNode<K,T> right,
			IndexNode<K,T> parent) {
		return -1;

	}

	/**
	 * TODO Handle IndexNode Underflow (merge or redistribution)
	 * 
	 * @param left
	 *            : the smaller node
	 * @param right
	 *            : the bigger node
	 * @param parent
	 *            : their parent index node
	 * @return the splitkey position in parent if merged so that parent can
	 *         delete the splitkey later on. -1 otherwise
	 */
	public int handleIndexNodeUnderflow(IndexNode<K,T> leftIndex,
			IndexNode<K,T> rightIndex, IndexNode<K,T> parent) {
		return -1;
	}

}
