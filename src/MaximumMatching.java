/**
 * @author Jun Yu
 */


import java.io.*;
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

    public void addEdge(int u, int v, int weight) {
        vertices.get(u).addAdj(v, weight);
        vertices.get(v).addAdj(u, weight);
    }

    public int procedure() {
        for (Vertex v : vertices) {
            v.augmentingRoot = 0;
            v.augmentingChildren.clear();
            v.augmentingParent = 0;
            v.mate = 0;
            v.isOuter = true;
            v.inMatchingSet = false;
            v.seen = false;
            v.visited = false;
        }


        int free = findFreeNode();
        int numMatching = findMaximalMatchingFromFreeNode(free);
        if (numMatching == numVertices) {
            return numVertices;
        }

        buildAlternatingTree();

        return 0;
    }

    /**
     * Find a free node from 1...n
     *
     * @return index of free node
     */
    private int findFreeNode() {
        for (Vertex v : vertices) {
            if (v.index != 0 && v.toBeProcessed && v.isFreeNode()) {
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

                if (!u.inMatchingSet && !v.inMatchingSet) {
                    u.inMatchingSet = true;
                    u.mate = v_index;
                    v.inMatchingSet = true;
                    v.mate = u_index;
                    num += 2; // a pair of matching
                }

                if (!v.visited) {
                    v.visited = true;

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
            if (v.index != 0 && v.toBeProcessed && !v.inMatchingSet) {
                Q.add(v.index);
                v.augmentingRoot = v.index; // self root
                v.seen = true;
            }
        }

        while (!Q.isEmpty()) {
            int u_index = Q.remove();
            Vertex u = vertices.get(u_index);

            for (int v_index : u.adj) {
                Vertex v = vertices.get(v_index);
                if (u.augmentingParent == v_index) {
                    continue; // TODO add
                }

                if (u.mate == v_index) {
                    continue;
                }

                if (v.seen && v.isOuter() && v.augmentingRoot != u.augmentingRoot) {

                    case1FoundAnAugmentingPath(u, v);

                } else if (v.isInner() && v.seen) {
                    // case 2
                    continue;
                } else if (!v.seen) {
                    // case 3
                    v.seen = true;
                    v.isOuter = false;
                    v.augmentingParent = u.index;
                    v.augmentingRoot = u.augmentingRoot;
                    u.augmentingChildren.add(v.index);
                    int x_index = v.mate;
                    Vertex x = vertices.get(x_index);
                    x.seen = true;
                    x.isOuter = true;
                    x.augmentingParent = v.index;
                    x.augmentingRoot = v.augmentingRoot;
                    v.augmentingChildren.add(x.index);
                    Q.add(v.index);
                    Q.add(x.index); // TODO add
                } else if (v.isOuter() && v.augmentingRoot == u.augmentingRoot) {
                    // case 4
                    int LCA_index = lowestCommonAncestor(u.index, v.index);
                    List<Integer> cycle = formCycle(LCA_index, u.index, v.index);
                    int x_index = shrinkCycle(cycle);
                    MaximumMatching subProblem = new MaximumMatching(vertices);
                    subProblem.run = this.run + 1;
                    subProblem.procedure();
                    recoverCycle(cycle, x_index);
                }
            }
        }
    }

    /**
     * Find an augmenting path from u to its root and v to its root
     * @param u in one augmenting tree
     * @param v int another augmenting tree
     * @return the number of extra matching
     */
    private int case1FoundAnAugmentingPath(Vertex u, Vertex v) {
        // case 1
        // set root of all nodes in v's tree to root of u
        int v_old_root_index = v.augmentingRoot;
        Vertex v_old_root = vertices.get(v_old_root_index);
        setAugmentingTreeRoot(vertices.get(v.augmentingRoot), u.augmentingRoot);

        // reverse path from v to its root in augmenting path

        Vertex prev = u;
        Vertex current = v;
        //Vertex next;
        Vertex oldParent;

        while (current != v_old_root) {
            prev.augmentingChildren.add(current.index);
            current.augmentingChildren.remove(Integer.valueOf(prev.index));
            oldParent = vertices.get(current.augmentingParent);
            current.augmentingParent = prev.index;
            prev = current;
            current = oldParent;
        }

        prev.augmentingChildren.add(current.index);
        current.augmentingChildren.remove(Integer.valueOf(prev.index));
        current.augmentingParent = prev.index;

        // form a path from u's root to v's previous root, using backtrace
        ArrayList<Integer> path = new ArrayList<>();
        int u_root_index = u.augmentingRoot;
        Vertex u_root = vertices.get(u_root_index);
        current = v_old_root;

        while (current != u_root) {
            path.add(current.index);
            current = vertices.get(current.augmentingParent);
        }
        path.add(u_root_index);

        Collections.reverse(path);

        augmentPath(path);

        return 0;
    }

    /**
     *
     * @param path must be in subsequent order
     */
    private void augmentPath(List<Integer> path) {
        assert path.size() >= 2;

        int begin;

        Vertex first = vertices.get(path.get(0));
        if (first.isInMatchingSet() && first.mate == path.get(1)) {
            // matched in path
            begin = 0;
        } else if (!first.isInMatchingSet()) {
            // free node
            begin = 0;
        } else {
            // matched to node outside the path
            begin = 1;
        }

        for (int i = begin; i < path.size() - 1; i += 2) {
            Vertex u = vertices.get(path.get(i));
            Vertex v = vertices.get(path.get(i + 1));

            u.inMatchingSet = true;
            u.mate = v.index;
            v.inMatchingSet = true;
            v.mate = u.index;
        }
    }

    /**
     * Form a cycle with u and v to their LCA, and (u,v)
     *
     * @param LCA_index lowest common ancestor of u and v
     * @param u_index   u
     * @param v_index   v
     * @return cycle, follow the order u -> LCA -> v -> u
     */
    private List<Integer> formCycle(int LCA_index, int u_index, int v_index) {
        List<Integer> cycle = new ArrayList<>();

        Vertex u = vertices.get(u_index);
        Vertex v = vertices.get(v_index);

        Vertex p = u;
        // u to LCA
        while (p.index != LCA_index) {
            cycle.add(p.index);
            p = vertices.get(p.augmentingParent);
        }

        // add LCA
        cycle.add(LCA_index);

        List<Integer> reverse = new LinkedList<>();
        // v to LCA
        p = v;
        while (p.index != LCA_index) {
            reverse.add(p.index);
            p = vertices.get(p.augmentingParent);
        }
        ListIterator<Integer> itor = reverse.listIterator(reverse.size());
        while (itor.hasPrevious()) {
            cycle.add(itor.previous());
        }

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

        Vertex root = vertices.get(u.augmentingRoot);
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
        edgesShrunkOutside = new HashMap<>();
        nodesHaveConnectionWithCycle = new HashSet<>();

        // for each vertex in cycle
        for (Integer u_index : cycle) {
            Vertex u = vertices.get(u_index);

            Iterator<Integer> adjItor = u.adj.iterator();
            Iterator<Integer> weightItor = u.weight.iterator();

            // for all adjacent edge of u
            while (adjItor.hasNext()) {
                int v_index = adjItor.next();
                int weight = weightItor.next();

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
                if (edgesShrunkFromUOutside == null) {
                    edgesShrunkFromUOutside = new ArrayList<>();
                    edgesShrunkOutside.put(v_index, edgesShrunkFromUOutside);
                }
                Pair<Integer> pair = new Pair<>(u_index, v_index);
                edgesShrunkFromU.add(pair);
                edgesShrunkFromUOutside.add(pair);

                // remove edge end point at u
                adjItor.remove();
                weightItor.remove();
                // remove edge end point at v
                v.removeAdj(u_index);
            }
        }

        // insert new vertex x
        Vertex x = new Vertex(vertices.size());
        int x_index = vertices.size();
        vertices.add(x);
        x.toBeProcessed = true;

        for (Integer k_index : nodesHaveConnectionWithCycle) {
            x.addAdj(k_index, 0);
            Vertex k = vertices.get(k_index);
            k.addAdj(x.index, 0);
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
     * @return number of matching in this cycle
     */
    private int recoverCycle(List<Integer> cycle, int x_index) {
        if (cycle == null || cycle.size() == 0) {
            return 0;
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

        int numMatching = (cycle.size() - 1) / 2;

        int start_index;
        if (nodeInCycleMatchedToNodeOutsideCycle.size() > 0) {
            // find a node in cycle that matched to outside
            int start_index_cycle = cycle.indexOf(nodeInCycleMatchedToNodeOutsideCycle.get(0));
            start_index = start_index_cycle + 1;
        } else {
            // no node in cycle matched to outside
            start_index = 0;
        }

        int matched = 0;
        for (int i = start_index; i < i + cycle.size(); i += 2) {
            int j = i % cycle.size();

            int inner_index = cycle.get(j);
            Vertex inner = vertices.get(inner_index);
            int outet_index = cycle.get(j + 1);
            Vertex outer = vertices.get(outet_index);

            inner.isOuter = false;
            outer.isOuter = true;
            inner.inMatchingSet = true;
            outer.inMatchingSet = true;
            inner.mate = outet_index;
            outer.mate = inner_index;

            matched++;

            if (matched == numMatching) {
                break;
            }
        }

        x.toBeProcessed = false;

        return numMatching;
    }

    /**
     * Set roots of all nodes in tree rooted at T to index root
     *
     * @param T    root of tree
     * @param root new root index
     */
    private void setAugmentingTreeRoot(Vertex T, int root) {
        T.augmentingRoot = root;

        if (T.augmentingChildren.size() == 0) {
            return;
        }

        for (int v_index : T.augmentingChildren) {
            Vertex v = vertices.get(v_index);
            setAugmentingTreeRoot(v, root);
        }
    }

    public void printMatching() {
        for (int i = 1; i <= numVertices; i++) {
            Vertex v = vertices.get(i);

            if (v.inMatchingSet) {
                System.out.println(v.index + " " + v.mate);
            } else {
                System.out.println(v.index + " -");
            }
        }
    }

    public static void main(String[] args) {
        BufferedReader reader = null;
        if (args.length > 0) {
            try {
                reader = new BufferedReader(new FileReader(args[0]));
            } catch (FileNotFoundException e) {
                System.exit(-1);
            }
        } else {
            reader = new BufferedReader(new InputStreamReader(System.in));
        }

        assert reader != null;

        try {
            String metaLine = reader.readLine();
            String[] metas = metaLine.split("[\\s\\t]+");
            int numNodes = Integer.parseInt(metas[0]);
            int numEdges = Integer.parseInt(metas[1]);

            ArrayList<Vertex> nodeSet = new ArrayList<>(numNodes + 1);
            nodeSet.add(new Vertex(0)); // 0 unused
            for (int i = 1; i <= numNodes; i++) {
                nodeSet.add(new Vertex(i));
            }

            MaximumMatching solution = new MaximumMatching(nodeSet);

            String line;
            while ((line = reader.readLine()) != null && !line.equals("")) {
                String[] params = line.split("[\\s\\t]+");
                int u_index = Integer.parseInt(params[0]);
                int v_index = Integer.parseInt(params[1]);
                int u_v_weight = Integer.parseInt(params[2]);

                solution.addEdge(u_index, v_index, u_v_weight);
            }

            solution.procedure();
            solution.printMatching();


        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
