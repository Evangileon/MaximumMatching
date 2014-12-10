/**
 * @author Jun Yu
 */

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;


public class MaximumMatching {
    // 0 is unused
    ArrayList<Vertex> vertices;

    /**
     * Find a free node from 1...n
     * @return index of free node
     */
    private int findFreeNode() {
        for (Vertex v : vertices) {
            if (v.isFreeNode()) {
                return v.index;
            }
        }
        return 0;
    }

    /**
     * Find and mark a maximal matching using BFS
     * @param freeNode start from this node
     * @return number of matched nodes
     */
    private int findMaximalMatchingFromAFreeNode(int freeNode) {
        for (Vertex v : vertices) {
            v.visited = false;
            v.inMatchingSet = false;
        }

        int num = 0;

        LinkedList<Integer> queue = new LinkedList<>();
        Vertex start = vertices.get(freeNode);

        start.visited = true;
        start.inMatchingSet = false;
        queue.add(start.index);

        while (!queue.isEmpty()) {
            int u_index = queue.remove();
            Vertex u = vertices.get(u_index);

            for (int v_index : u.adj) {
                Vertex v = vertices.get(v_index);
                if (!v.visited) {
                    v.visited = true;

                    if (!u.inMatchingSet && !v.inMatchingSet) {
                        u.inMatchingSet = true;
                        u.mate = v_index;
                        v.inMatchingSet = true;
                        v.mate = u_index;
                        num += 2; // a pair of matching
                    }

                    queue.add(v_index);
                }
            }
        }

        return num;
    }

    public void buildAlternatingTree() {
        Queue<Integer> Q = new LinkedList<>();

        for (Vertex v : vertices) {
            if (!v.inMatchingSet) {
                Q.add(v.index);
            }
        }

        while (!Q.isEmpty()) {
            int u_index = Q.remove();
            Vertex u = vertices.get(u_index);

            for (int v_index : u.adj) {
                Vertex v = vertices.get(v_index);
                if (u.mate == v_index) {
                    continue;
                }

                if (v.seen && v.isOuter() && v.augmentingRoot != u.augmentingRoot) {
                    // case 1

                } else if (v.isInner() && v.seen) {
                    // case 2
                    continue;
                } else if (!v.seen) {
                    // case 3
                    v.seen = true;
                    v.isOuter = false;
                    v.augmentingParent = u.index;
                    int x_index = v.mate;
                    Vertex x = vertices.get(x_index);
                    x.isOuter = true;
                    x.augmentingParent = v.index;
                    Q.add(v.index);
                } else if (v.isOuter() && v.augmentingRoot == u.augmentingRoot) {
                    // case 4
                }
            }
        }
    }

    /**
     * Find LCA of u and v in the same augmenting tree
     * @param u_index u
     * @param v_index v
     * @return LCA, 0 if u and v are not in the same augmenting tree
     */
    private int lowestCommonAncestor(int u_index, int v_index) {
        Vertex u = vertices.get(u_index);
        Vertex v = vertices.get(v_index);

        if (u.augmentingRoot != v.augmentingRoot || u.augmentingRoot == 0) {
            return 0;
        }

        Vertex root = vertices.get(u_index);
        return lowestCommonAncestor(root, u, v).index;
    }

    /**
     * Recursively solve the sub-problem, RT = O(n)
     * @param root of sub-tree
     * @param u u
     * @param v v
     * @return the node found in the sub-tree, u, v, LCA or null
     */
    private Vertex lowestCommonAncestor(Vertex root, Vertex u, Vertex v) {
        if (root == u) {
            return u;
        } else if (root == v) {
            return v;
        }

        Vertex firstFound = null;

        for (int k_index : root.augmentingChildren) {
            Vertex k = vertices.get(k_index);

            Vertex found = lowestCommonAncestor(k, u, v);

            if (found != null) {
                if (found != u && found != v) {
                    // a node that is nor u neither v
                    // it's the LCA, return it immediately
                    return found;
                }

                if (firstFound == null) {
                    // find first node, u or v
                    firstFound = found;
                } else {
                    // find second node
                    // so this root should be LCA
                    return root;
                }
            }
        }

        return firstFound;
    }
}
