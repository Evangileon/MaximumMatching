/**
 * @author Jun Yu
 */

import java.util.Iterator;
import java.util.LinkedList;

public class Vertex {
    final int index;

    // for alternating tree
    int augmentingRoot;
    int augmentingParent;
    LinkedList<Integer> augmentingChildren = new LinkedList<>();
    boolean seen;


    LinkedList<Integer> adj = new LinkedList<Integer>();
    // otherwise inner
    boolean isOuter;

    // for traversal
    boolean visited;

    boolean inMatchingSet;
    int mate;

    // flag

    // run is an indicator that mark this vertex is in which recursion
    // If run equals to the flag in MaximumMatching instance, this vertex will be processed
    int run;
    boolean toBeProcessed;

    public Vertex(int index) {
        this.index = index;
        isOuter = false;
        inMatchingSet = false;
        visited = false;
        toBeProcessed = true;
    }

    public boolean isInMatchingSet() {
        return inMatchingSet;
    }

    public boolean isFreeNode() {
        return !isInMatchingSet();
    }

    public void addAdj(int v) {
        this.adj.add(v);
    }

    public void removeAdj(int adjIndex) {
        Iterator<Integer> adjItor = this.adj.iterator();

        while (adjItor.hasNext()) {
            int index = adjItor.next();

            if (index == adjIndex) {
                adjItor.remove();
                return;
            }
        }
    }

    public boolean isOuter() {
        return isOuter;
    }

    public boolean isInner() {
        return !isOuter;
    }
}
