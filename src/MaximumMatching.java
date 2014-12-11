/**
 * @author Jun Yu
 */


import java.util.*;


public class MaximumMatching {
    // 0 is unused
    ArrayList<Vertex> vertices;

    int numVertices;

    HashMap<Integer, List<Pair<Integer>>> edgesShrunk;
    HashMap<Integer, List<Pair<Integer>>> edgesShrunkOutside;
    HashSet<Integer> nodesHaveConnectionWithCycle;

    // flag
    int run = 0;

    public MaximumMatching(ArrayList<Vertex> vertices) {
        this.vertices = vertices;
        numVertices = vertices.size() - 1;
    }

    public void addEdge(int u, int v) {
        vertices.get(u).addAdj(v);
        vertices.get(v).addAdj(u);
    }

    public int procedure() {
        int free = findFreeNode();
        int numMatching = findMaximalMatchingFromFreeNode(free);
        if (numMatching == numVertices) {
            return numVertices;
        }

        return 0;
    }

    /**
     * Find a free node from 1...n
     *
     * @return index of free node
     */
    private int findFreeNode() {
        for (Vertex v : vertices) {
            if (v.toBeProcessed && v.isFreeNode()) {
                return v.index;
            }
        }
        return 0;
    }

    /**
     * Find and mark a maximal matching using BFS
     *
     * @param freeNode start from this node
     * @return number of matched nodes
     */
    private int findMaximalMatchingFromFreeNode(int freeNode) {
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

    /**
     * Build tree only using vertices whose run flag equals to solution run flag
     */
    private void buildAlternatingTree() {
        Queue<Integer> Q = new LinkedList<>();

        for (Vertex v : vertices) {
            if (v.toBeProcessed && !v.inMatchingSet) {
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
                    int LCA_index = lowestCommonAncestor(u.index, v.index);
                    List<Integer> cycle = formCycle(LCA_index, u.index, v.index);
                    int x_index = shrinkCycle(cycle);
                    MaximumMatching subProblem = new MaximumMatching(vertices); // TODO issue about vertices set
                    subProblem.run = this.run + 1;
                    subProblem.procedure();
                    recoverCycle(cycle, x_index);
                }
            }
        }
    }

    /**
     * Form a cycle with u and v to their LCA, and (u,v)
     *
     * @param LCA_index lowest common ancestor of u and v
     * @param u_index   u
     * @param v_index   v
     * @return cycle
     */
    private List<Integer> formCycle(int LCA_index, int u_index, int v_index) {
        List<Integer> cycle = new LinkedList<>();

        Vertex u = vertices.get(u_index);
        Vertex v = vertices.get(v_index);

        Vertex p = u;
        // u to LCA
        while (p.index != LCA_index) {
            cycle.add(p.index);
            p = vertices.get(p.augmentingParent);
        }

        // v to LCA
        p = v;
        while (p.index != LCA_index) {
            cycle.add(p.index);
        }

        // add LCA
        cycle.add(LCA_index);

        return cycle;
    }

    /**
     * Find LCA of u and v in the same augmenting tree
     *
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
     *
     * @param root of sub-tree
     * @param u    u
     * @param v    v
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

    class Pair<T> {
        T from, to;

        public Pair(T from, T to) {
            this.from = from;
            this.to = to;
        }
    }

    /**
     * Shrink cycle C into a single node
     *
     * @param cycle cycle to be shrunk
     * @return index of new node
     */
    private int shrinkCycle(List<Integer> cycle) {
        edgesShrunk = new HashMap<>();
        nodesHaveConnectionWithCycle = new HashSet<>();

        // for each vertex in cycle
        for (Integer u_index : cycle) {
            Vertex u = vertices.get(u_index);

            Iterator<Integer> adjItor = u.adj.iterator();

            // for all adjacent edge of u
            while (adjItor.hasNext()) {
                int v_index = adjItor.next();
                Vertex v = vertices.get(v_index);

                // skip vertex that in cycle
                if (cycle.contains(v_index)) {
                    continue;
                }

                // record nodes that have connection to cycle
                nodesHaveConnectionWithCycle.add(v_index);

                // first record the almost shrunk edge
                List<Pair<Integer>> edgesShrunkFromU = edgesShrunk.get(u_index);
                List<Pair<Integer>> edgesShrunkFromUOutside = edgesShrunk.get(v_index);
                if (edgesShrunkFromU == null) {
                    edgesShrunkFromU = new ArrayList<>();
                    edgesShrunk.put(u_index, edgesShrunkFromU);
                }
                if (edgesShrunkOutside == null) {
                    edgesShrunkFromUOutside = new ArrayList<>();
                    edgesShrunkOutside.put(v_index, edgesShrunkFromUOutside);
                }
                Pair<Integer> pair = new Pair<>(u_index, v_index);
                edgesShrunkFromU.add(pair);
                edgesShrunkFromUOutside.add(pair);

                // remove edge end point at u
                adjItor.remove();
                // remove edge end point at v
                v.removeAdj(u_index);
            }
        }

        // insert new vertex x
        Vertex x = new Vertex(vertices.size());
        int x_index = vertices.size();
        vertices.add(x);

        for (Integer k_index : nodesHaveConnectionWithCycle) {
            x.addAdj(k_index);
            Vertex k = vertices.get(k_index);
            k.addAdj(x.index);
        }

        // the direction of path is the reverse of cycle list

        // prevent node int cycle from be processed in next recursion
        for (Integer y_index : cycle) {
            Vertex y = vertices.get(y_index);
            y.toBeProcessed = false;
        }

        return x_index;
    }

    /**
     * Include the cycle into MST
     *
     * @param cycle   the zero cycle
     * @param x_index to which cycle shrunk
     */
    private void recoverCycle(List<Integer> cycle, int x_index) {
        if (cycle == null || cycle.size() == 0) {
            return;
        }

        // include the cycle into graph
        System.out.println(cycle.size() + " " + x_index);
        for (Integer y_index : cycle) {
            Vertex y = vertices.get(y_index);
            y.toBeProcessed = true;
        }

        List<Integer> nodeInCycleMatchedToNodeOutsideCycle = new ArrayList<>();

        Vertex x = vertices.get(x_index);
        if (x.isInMatchingSet()) {
            int x_mate = x.mate;
            List<Pair<Integer>> edgesShrunkFromUOutside = edgesShrunkOutside.get(x_mate);
            assert edgesShrunkFromUOutside != null;
            // for edge connect node matched to x with node in cycle
            for (Pair<Integer> pair : edgesShrunkFromUOutside) {
                nodeInCycleMatchedToNodeOutsideCycle.add(pair.from);
            }
        }

//        // for each node in cycle
//        for (int u_index : cycle) {
//            Vertex u = vertices.get(u_index);
//            List<Pair<Integer>> association = edgesShrunk.get(u_index);
//            if (association == null) {
//                continue;
//            }
//
//            // for each node outside cycle that has edge to cycle
//            for (Pair<Integer> edge : association) {
//                int v_index = edge.to;
//                Vertex v = vertices.get(v_index);
//                if (v.isInMatchingSet() && v.mate == u.index) {
//                    // node in cycle matched with node outside the cycle
//                    nodeInCycleMatchedToNodeOutsideCycle.add(u.index);
//                    break;
//                }
//            }
//        }

        int numMatching = (cycle.size() - 1) / 2;

    }
}
