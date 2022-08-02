package org.amir;

import java.util.*;

import org.graphstream.graph.*;
import org.graphstream.graph.implementations.*;

// Graph Attributes:
//  numberOfNodes, numberOfEdges

// Node Attributes:
//  shape, number


public class Main {
    static Stack<Graph> graphs = new Stack<>();
    static ArrayList<ArrayList<Integer>> deadlockTracker = new ArrayList<>();
    static Stack<Node> states = new Stack<>();
    static Graph curGraph;
    static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        graphMaker();
        while (!graphs.empty()) {
            curGraph = graphs.pop();
            doTheTrick();
            writeData();
            curGraph.display();
        }
    }

    private static void doTheTrick() {
        while (!isFinished() || checkIfANodeIsFilledWrong()) {
            printer();
            if (existInDeadlockTracker()) {
                goingBackward();
            }
            else if (checkIfANodeIsFilledWrong()) {
                addToDeadlockTracker();
                goingBackward();
            }

            Node curNode = getNextCandidateNode();
            goingForward(curNode);
        }
        printer();
        deadlockTracker.clear();
        states.clear();
    }

    private static boolean existInDeadlockTracker() {
        ArrayList<Integer> curState = new ArrayList<>();
        for (int i = 0; i < curGraph.getNodeCount(); i++) {
            if (curGraph.getNode(String.valueOf(i)).getAttribute("number") != null) {
                curState.add(curGraph.getNode(String.valueOf(i)).getAttribute("number"));
            } else {
                // -8 represents null
                curState.add(-8);
            }
        }
        return deadlockTracker.contains(curState);
    }

    private static boolean isFinished() {
        // it checks whether all the nodes have numbers or not
        for (int i = 0; i < curGraph.getNodeCount(); i++) {
            if (!curGraph.getNode(String.valueOf(i)).hasAttribute("number"))
                return false;
        }
        return true;
    }

    private static void printer() {
        for (int i = 0; i < curGraph.getNodeCount(); i++) {
            Object object = curGraph.getNode(String.valueOf(i)).getAttribute("number");
            System.out.print(object + " ");
        }
        System.out.println();
    }

    private static void addToDeadlockTracker() {
        ArrayList<Integer> deadlockState = new ArrayList<>();
        int nodeNumber;
        for (int i = 0; i < curGraph.getNodeCount(); i++) {
            if (curGraph.getNode(String.valueOf(i)).getAttribute("number") != null) {
                nodeNumber = curGraph.getNode(String.valueOf(i)).getAttribute("number");
                deadlockState.add(nodeNumber);
            } else {
                // -8 represents null
                deadlockState.add(-8);
            }
        }
        deadlockTracker.add(deadlockState);
    }

    private static void graphMaker() {
        System.setProperty("org.graphstream.ui.renderer",
                "org.graphstream.ui.j2dviewer.J2DGraphRenderer");

        int numberOfGraphs = scanner.nextInt();
        for (int i = 0; i < numberOfGraphs; i++) {
            String index = String.valueOf(graphs.size() + 1);
            curGraph = new SingleGraph(index);
            curGraph.setAttribute("ui.antialias");
            curGraph.setAttribute("ui.stylesheet", "url('src/main/java/org/amir/ui.stylesheet')");
            graphs.push(curGraph);
            readData();
        }
    }

    private static void readData() {
        int numberOfNodes = scanner.nextInt();
        int numberOfEdges = scanner.nextInt();
        curGraph.setAttribute("numberOfNodes", numberOfNodes);
        curGraph.setAttribute("numberOfEdges", numberOfEdges);

        // Adding Nodes to graph and Specifying Shapes to Nodes
        for (int i = 0; i < numberOfNodes; i++) {
            curGraph.addNode(String.valueOf(i));
            String nodeShape = scanner.next();
            curGraph.getNode(String.valueOf(i)).setAttribute("shape", nodeShape);
            curGraph.getNode(String.valueOf(i)).setAttribute("ui.class", nodeShape);
        }

        // Adding Edges to graph
        for (int i = 0; i < numberOfEdges; i++) {
            String node1 = scanner.next();
            String node2 = scanner.next();
            curGraph.addEdge((node1 + node2), node1, node2);
        }
    }

    private static void writeData() {
        for (int i = 0; i < curGraph.getNodeCount(); i++) {
            Node curNode = curGraph.getNode(String.valueOf(i));
            int number = curNode.getAttribute("number");
            curNode.setAttribute("ui.label", String.valueOf(number));
        }
    }

    private static int getRandomNumber(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);
    }

    private static void goingBackward() {
        while (!states.isEmpty()) {
            Node node = states.pop();
            node.removeAttribute("number");
        }
    }

    private static void goingForward(Node node) {
        Node fullNeighbor = checkIfNeighborISFullExceptMe(node);

        if (getNumberOfAvailableNeighbors(node) == 0) {
            node.setAttribute("number", getANumberAccordingToConstraints(node, exploreNeighbors(node)));
        } else if (fullNeighbor != null) {
            node.setAttribute("number", getANumberWithRespectToNeighbor(node, fullNeighbor));
        } else {
            node.setAttribute("number", getRandomNumber(1, 9));
        }
        states.push(node);
    }

    private static boolean checkIfANodeIsFilledWrong() {
        for (int i = 0; i < curGraph.getNodeCount(); i++) {
            Node curNode = curGraph.getNode(String.valueOf(i));
            if (getNumberOfAvailableNeighbors(curNode) == 0 && curNode.getAttribute("number") != null) {
                if (!curNode.getAttribute("shape").equals("C")) {
                    int correctNumber = getANumberAccordingToConstraints(curNode, exploreNeighbors(curNode));
                    int filledNumber = -400;
                    if (curNode.getAttribute("number") != null) {
                        filledNumber = curNode.getAttribute("number");
                    }
                    if (correctNumber != filledNumber)
                        return true;
                }
            }
        }
        return false;
    }

    private static int getANumberAccordingToConstraints(Node node, Stack<Node> neighbors) {
        String shape = node.getAttribute("shape");
        int output = 0;
        if (Objects.equals(shape, "T") || Objects.equals(shape, "S")) output = 1;
        while (!neighbors.isEmpty()) {
            Node curNode = neighbors.pop();
            if (curNode.getAttribute("number") == null)
                return -3;
            int number;
            try {
                number = Integer.parseInt(curNode.getAttribute("number"));

            } catch (Exception ClassCastException) {
                number = curNode.getAttribute("number");
            }
            switch (shape) {
                case "T", "S" -> output *= number;
                case "P", "H" -> output += number;
            }
        }

        output = switch (shape) {
            case "T", "P" -> getHighestValueDigit(output);
            case "S", "H" -> getLowestValueDigit(output);
            case "C" -> getRandomNumber(1, 9);
            default -> output;
        };
        if (output == 0) {
            goingBackward();
            return getRandomNumber(1, 9);
        }
        return output;
    }

    private static int getANumberWithRespectToNeighbor(Node me, Node neighbor) {
        Stack<Node> neighborsOfNeighbor = exploreNeighbors(neighbor);
        neighborsOfNeighbor.remove(me);
        neighborsOfNeighbor.push(neighbor);
        return getANumberAccordingToConstraints(me, neighborsOfNeighbor);
    }

    private static Node checkIfNeighborISFullExceptMe(Node me) {
        Stack<Node> neighbors = exploreNeighbors(me);
        while (!neighbors.isEmpty()) {
            Node neighbor = neighbors.pop();
            if (getNumberOfAvailableNeighbors(neighbor) == 1 && neighbor.hasAttribute("number"))
                return neighbor;
        }
        return null;
    }

    private static int getHighestValueDigit(int number) {
        while (number / 10 != 0) number = number / 10;
        return number;
    }

    private static int getLowestValueDigit(int number) {
        return number % 10;
    }

    public static Stack<Node> exploreNeighbors(Node source) {
        Iterator<? extends Node> k = source.getNeighborNodeIterator();
        Stack<Node> neighbors = new Stack<>();
        while (k.hasNext()) {
            Node next = k.next();
            neighbors.push(next);
        }
        return neighbors;
    }

    public static int getNumberOfAvailableNeighbors(Node source) {
        int numberOfAvailableNeighbors = 0;
        Iterator<? extends Node> k = source.getNeighborNodeIterator();
        while (k.hasNext()) {
            Node next = k.next();
            if (next.getAttribute("number") == null)
                numberOfAvailableNeighbors++;
        }
        return numberOfAvailableNeighbors;
    }

    public static int compareByAvailableNeighbors(Node n1, Node n2) {
        return Integer.compare(getNumberOfAvailableNeighbors(n1), getNumberOfAvailableNeighbors(n2));
    }

    private static ArrayList<Node> sortedBasedOnNumberOfAvailableAdjacentNodes() {
        ArrayList<Node> nodes = new ArrayList<>();
        for (int i = 0; i < curGraph.getNodeCount(); i++) {
            Node curNode = curGraph.getNode(String.valueOf(i));
            if (curNode.getAttribute("number") == null)
                nodes.add(curNode);
        }
        nodes.sort(Main::compareByAvailableNeighbors);
        return nodes;
    }

    // isReversed is used to act like a minmax algorithm
    // it chooses node with highest and lowest number of adjacent node in each round
    static boolean isReversed = false;

    private static Node getNextCandidateNode() {
        ArrayList<Node> sortedNodes = sortedBasedOnNumberOfAvailableAdjacentNodes();
        isReversed = !isReversed;
        if (isReversed)
            return sortedNodes.get(sortedNodes.size() - 1);
        else
            return sortedNodes.get(0);
    }

}