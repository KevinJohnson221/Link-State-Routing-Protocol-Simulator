/* Kevin Johnson
 * Assignment #4
 * ORIGINAL TOPOFILE.CSV USED FOR TESTING LOOKED LIKE THIS BEFORE RUNNING:
1 2 8
2 3 3
10 20 1
2 5 4
4 1 1
4 5 1
6 7 3
 */
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.Set;

public class LinkStateRoutingProtocol {
    public static void main(String[] args) throws IOException {
        // Creates output.txt if it doesn't exist, empties it if it does exist
        FileWriter fileWriter = new FileWriter("output.txt", false);
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        // Close the writers
        bufferedWriter.close();
        fileWriter.close();

        // topology created to hold the nodes and links
        Topology topology = new Topology();

        // used later to read nodes and links from changesfile.csv
        File changesFiles = new File("changesfile.csv");
        Scanner changesScanner = new Scanner(changesFiles);

        // goes through each line of changesfile.csv later in this while loop
        boolean timeToBreak = false;
        boolean emptyChanges = true;
        while (true) {
            // read nodes and links from topofile.csv, puts them in topology data structure
            File topologyFile = new File("topofile.csv");
            Scanner topologyScanner = new Scanner(topologyFile);
            while (topologyScanner.hasNextLine()) {
                // line holds values on a line of the file
                String line = topologyScanner.nextLine();
                // values separated by a space
                String[] parts = line.split(" ");
                // 1st value in line will be the ID of a node
                int sourceID = Integer.parseInt(parts[0]);
                // 2nd value in line will be the ID of another node
                int destinationID = Integer.parseInt(parts[1]);
                // 3rd value in line will be the cost of a link between the 2 nodes
                int cost = Integer.parseInt(parts[2]);

                // check if node with ID of the node already exists
                Topology.Node sourceNode = topology.getNode(sourceID);
                // if it doesn't already exist, create it and add it to topology
                if (sourceNode == null) {
                    sourceNode = new Topology.Node(sourceID);
                    topology.addNode(sourceNode);
                }

                // check if node with ID of another node already exists
                Topology.Node destinationNode = topology.getNode(destinationID);
                // if it doesn't already exist, create it and add it to topology
                if (destinationNode == null) {
                    destinationNode = new Topology.Node(destinationID);
                    topology.addNode(destinationNode);
                }
                
                // create a link between the 2 nodes with the specified cost and add it to topology
                topology.addLink(new Topology.Link(sourceNode, destinationNode, cost));
            }
            // close the scanner
            topologyScanner.close();

            // sort the nodes by their nodeID
            topology.sortNodes();

            // Prints all the Nodes and Links currently in the topology
            topology.printNodes();
            topology.printLinks();
            System.out.println();

            // Print all paths between every node with nextHop and cost and put it in output.txt
            for (Topology.Node node : topology.getNodes()) {
                topology.linkStatePrintAllPaths(node);
                System.out.println();
            }

            // read nodes and links from messagefile.csv
            File messageFile = new File("messagefile.csv");
            Scanner messageScanner = new Scanner(messageFile);
            while (messageScanner.hasNextLine()) {
                String line = messageScanner.nextLine();
                String[] parts = line.split(" ");
                // 1st value in line will be the ID of the source Node
                int sourceID = Integer.parseInt(parts[0]);
                // 2nd value in line will be the ID of the destination Node
                int destinationID = Integer.parseInt(parts[1]);
                // Everything else is the message, added to the string message
                String message = "";
                for (int i = 2; i < parts.length-1; i++) {
                    message += parts[i] + " ";
                }
                message += parts[parts.length-1];

                // If both Nodes are in the topology, then it runs the linkStatePrintPath method, who's results get added to output.txt
                Topology.Node sourceNode = topology.getNode(sourceID);
                Topology.Node destinationNode = topology.getNode(destinationID);
                if (sourceNode != null && destinationNode != null) {
                    topology.linkStatePrintPath(sourceNode, destinationNode, message);
                }
                
            }
            // close the message scanner
            messageScanner.close();

            // Using the changes scanner created right before the outer while loop in order to read nodes and links from changesfile.csv
            if (changesScanner.hasNextLine()) {
                // if this section is reached then changesfile.csv has at least 1 change in it
                emptyChanges = false;
                String line = changesScanner.nextLine();
                String[] parts = line.split(" ");
                // 1st value in line will be the ID of the source Node
                int sourceID = Integer.parseInt(parts[0]);
                // 2nd value in line will be the ID of the destination Node
                int destinationID = Integer.parseInt(parts[1]);
                // 3rd value in line will be the new cost of the Link between the 2 Nodes
                int cost = Integer.parseInt(parts[2]);
                
                // Initializes the Nodes and Link using the values gotten from changesfile.csv
                Topology.Node sourceNode = topology.getNode(sourceID);
                Topology.Node destinationNode = topology.getNode(destinationID);
                Topology.Link link = topology.getLink(sourceNode, destinationNode);
                // If the Link between the 2 nodes doesn't exist yet...
                if (link == null) {
                    //...and both Nodes are in the topology and the cost isn't -999...
                    if (sourceNode != null && destinationNode !=null && cost != -999) {
                        //...then the new Link is added to the topology
                        topology.addLink(new Topology.Link(sourceNode, destinationNode, cost));
                    //...and at least one of the Nodes isn't in the topology or the cost is -999
                    } else {
                        //...then the Link is invalid ignored
                        continue;
                    }
                // If the Link already exists in the topology...
                } else {
                    //...and the new cost to be associated with it is -999...
                    if (cost == -999) {
                        //...then the Link is removed from the topology
                        topology.removeLink(link);
                    //...and the new cost to be associated with it is positive...
                    } else if (cost > 0) {
                        //...then the Link is updated with its new cost
                        topology.setLinkCost(link, cost);
                    }
                }

                // Reflect the change done in the topofile.csv;
                topology.updateTopologyFile();
            }

            // This while loop will repeat until every change has been handled.
            // timeToBreak ensures that the loop can exit at the correct time
            if (timeToBreak) {
                break;
            }
            // If there is no next line in changesfile.csv...
            if (!changesScanner.hasNextLine()) {
                //...but there was at least one change changesfile.csv...
                if (!emptyChanges) {
                    //...then it jumps back to the start of the while loop and runs 1 last time
                    timeToBreak = true;
                    continue;
                }
                //...and there were no changes in changesfile.csv then the while loop ends right now
                break;
            }
        }
        // closes the changes scanner
        changesScanner.close();

        // Deletes the empty line at the begining of output.txt
        File outputFile = new File("output.txt");
        List<String> lines = Files.readAllLines(outputFile.toPath());
        Files.write(outputFile.toPath(), lines.subList(1, lines.size()));
    }

    // Topology class to hold the Nodes and Links of the topology
    public static class Topology {
        
        // Lists to hold the Nodes and Links 
        private List<Node> nodes;
        private List<Link> links;

        // Constructor for topology, initialzes lists
        public Topology() {
            nodes = new ArrayList<>();
            links = new ArrayList<>();
        }

        // Add a node to the topology if it doesn't already exist
        public void addNode(Node node) {
            for (Node existingNode: nodes) {
                // if the node already exists do nothing
                if (existingNode.nodeID == node.nodeID) {
                    return;
                }
            }
            // if the node doesn't exist yet in the topology add it
            nodes.add(node);
        }

        // Reset the costs and previous nodes of all Nodes in the entire topology
        public void resetNodes() {
            for (Node node : nodes) {
                //cost = infinite
                node.setCost(Integer.MAX_VALUE);
                //previous = null
                node.setPrevious(null);
            }
        }

        // Sort the Nodes by nodeID in ascending order
        public void sortNodes() {
            Collections.sort(nodes, new Comparator<Node>() {
                @Override
                public int compare(Node n1, Node n2) {
                    return n1.nodeID - n2.nodeID;
                }
            });
        }

        // Add a link to the topology if it doesn't already exist 
        public void addLink(Link link) {
            for (Link existingLink : links) {
                // which Node is the starting node and ending node in the Link doesn't matter
                if ((existingLink.start == link.start && existingLink.end == link.end) || (existingLink.start == link.end && existingLink.end == link.start)) {
                    return;
                }
            }
            // link added if it doesn't exist already (Link node1, node2 would be considered the same as Link node2, node1)
            links.add(link);
        }

        // Remove a link from the topology
        public boolean removeLink(Link link) {
            return links.remove(link);
        }

        // Modify the cost of a link
        public void setLinkCost(Link link, int newCost) {
            link.cost = newCost;
        }

        // Returns the Node with the specific nodeID, if it exists
        public Node getNode(int nodeID) {
            for (Node node : nodes) {
                if (node.getNodeID() == nodeID) {
                    return node;
                }
            }
            // returns null if the Node doesn't exist in the topology currently
            return null;
        }

        // Returns the Link that holds 2 specific Nodes, if it exists
        public Link getLink(Node node1, Node node2) {
            for (Link link : links) {
                // which Node is the starting node and ending node in the Link doesn't matter
                if ((link.start == node1 && link.end == node2) || (link.end == node1 && link.start == node2)) {
                    return link;
                }
            }
            // return null if the Link doesn't exist
            return null;
        }

        // Print all Nodes in the terminal
        public void printNodes() {
            System.out.println("Nodes:\n\t" +getNodes());
        }

        // Print all Links in the terminal
        public void printLinks() {
            System.out.println("Links:\n\t" +getLinks());
        }

        // Get the List of all the Nodes in the topology
        public List<Node> getNodes() {
            return nodes;
        }

        // Get the List of all the Links in the topology
        public List<Link> getLinks() {
            return links;
        }

        // Updates the topology file with the Links currently in the topology data structure
        public void updateTopologyFile() throws IOException {
            // Topology file is emptied, writer set up
            FileWriter fileWriter = new FileWriter("topofile.csv", false);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            // Adds each link to the file in the format: <NodeID1 NodeID2 cost>
            for (int i = 0; i < links.size()-1; i++) {
                bufferedWriter.write(links.get(i).start.getNodeID() +" " +links.get(i).end.getNodeID() +" " +links.get(i).cost +"\n");               
            }
            bufferedWriter.write(links.get(links.size()-1).start.getNodeID() +" " +links.get(links.size()-1).end.getNodeID() +" " +links.get(links.size()-1).cost);
            // Close the writers
            bufferedWriter.close();
            fileWriter.close();

        }

        // Link State Algorithm
        public void linkState(Node source) {
            // All nodes are initialized/reset (see method above for specifics)
            resetNodes();

            // Hashset made for visited Nodes
            Set<Node> visited = new HashSet<>();
            // Priority queue made for Nodes, priority based on cost
            PriorityQueue<Node> queue = new PriorityQueue<>(new Comparator<Node>() {
                @Override
                public int compare(Node n1, Node n2) {
                    return Integer.compare(n1.getCost(), n2.getCost());
                }
            });

            // the source Node's cost is 0, added to the queue 
            source.setCost(0);
            queue.add(source);

            // runs until all nodes visited
            while (!queue.isEmpty()) {
                // removes the head of the queue and stores it in current
                Node current = queue.poll();
                // current is added to Hashset of visited nodes
                visited.add(current);
                
                // goes through every link and
                for (Link link : links) {
                    // if the Link's starting node is the current Node and the Link's ending Node isn't the visited HashSet then
                    if (link.getStart().equals(current) && !visited.contains(link.getEnd())) {
                        // The Node neighbor is set to be the ending Node of the Link 
                        Node neighbor = link.getEnd();
                        // The cost is set to be the current Node's cost + the cost of the link
                        int cost = current.getCost() + link.getCost();
                        // If that current Node's cost + the cost of the link is less than the cost of the neighbor then
                        if (cost < neighbor.getCost()) {
                            // the neighbor's cost is set to be the cost of the current Node's cost + the cost of the link
                            neighbor.setCost(cost);
                            // the neighbor's previous node is identified as the current Node
                            neighbor.setPrevious(current);
                            // the neighbor is added to the queue
                            queue.add(neighbor);
                        }
                    // or if the Link's ending node is the current Node and the Link's starting Node isn't the visited HashSet then
                    } else if (link.getEnd().equals(current) && !visited.contains(link.getStart())) {
                        // The Node neighbor is set to be the starting Node of the Link 
                        Node neighbor = link.getStart();
                        // The cost is set to be the current Node's cost + the cost of the link
                        int cost = current.getCost() + link.getCost();
                        // If that current Node's cost + the cost of the link is less than the cost of the neighbor then
                        if (cost < neighbor.getCost()) {
                            // the neighbor's cost is set to be the cost of the current Node's cost + the cost of the link
                            neighbor.setCost(cost);
                            // the neighbor's previous node is identified as the current Node
                            neighbor.setPrevious(current);
                            // the neighbor is added to the queue
                            queue.add(neighbor);
                        }
                    }                
                }
            }
        }

        // Gets the paths from a source Node to every other Node the forwarding table for the source Node
        public void linkStatePrintAllPaths(Node source) throws IOException {
            // Runs the Link State Algorithm on the source node
            linkState(source);
        
            // Output will be appended to output.txt
            FileWriter fileWriter = new FileWriter("output.txt", true);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            // For every Node in the topology...
            for (Node node : nodes) {
                // The path for the Node will be a List of Nodes
                List<Node> path = new ArrayList<>();
                // current is the node currently being dealt with by the for loop
                Node current = node;
                // while current isn't null, current will be added to path and then set to be its previous node
                while (current != null) {
                    path.add(current);
                    current = current.getPrevious();
                }
                // Reverse the path to get the correct most cost effective path from source Node to this particular end Node
                Collections.reverse(path);

                // if the Node was unreachable then print that the cost is -999, don't reflect that Node it the forwarding table for the source Node
                if (node.getCost() == Integer.MAX_VALUE) {
                    System.out.println("Path from Node " +source.getNodeID() +" to Node " +node.getNodeID() +": -999");
                // if the Node was reachable then write the forwarding table to output.txt
                } else {
                    System.out.print("Path from Node " +source.getNodeID() +" to Node " +node.getNodeID() +": ");
                    for (int i = 0; i < path.size(); i++) {
                        System.out.print(path.get(i).getNodeID());
                        if (i < path.size() - 1) {
                            System.out.print(" -> ");
                        }
                    }

                    // String to be written to a new line in output.txt
                    bufferedWriter.newLine();
                    String appendString = "";

                    // String gets the destination NodeID appended to it
                    appendString += path.get(path.size()-1).getNodeID() + " ";
                    // String gets the nextHop Node's ID appended to it
                    if (path.size() == 1) {
                        System.out.print(" (nextHop: " +path.get(0) +")");
                        appendString += path.get(0).getNodeID() + " ";
                    } else if (path.size() > 1) {
                        System.out.print(" (nextHop: " +path.get(1) +")");
                        appendString += path.get(1).getNodeID() + " ";
                    }
                    // String gets the path's total cost appended to it
                    System.out.println(" (cost: " + node.getCost() + ")");
                    appendString += node.getCost();
                    
                    //String is added to output.txt
                    bufferedWriter.write(appendString);
                }
            }
            
            // Space between the tables
            bufferedWriter.newLine();
            // Close the writers
            bufferedWriter.close();
            fileWriter.close();
        }

        // Gets the path from a source Node to a destination Node and sends a message between them
        public void linkStatePrintPath(Node source, Node destination, String message) throws IOException {
            // Runs the Link State Algorithm on the source node
            linkState(source); 

            // The path for the Node will be a List of Nodes
            List<Node> path = new ArrayList<>();
            // The current Node is set to be the destination Node
            Node current = destination;

            // while current isn't null, current will be added to path and then set to be its previous node
            while (current != null) {
                path.add(current);
                current = current.getPrevious();
            }
            // Reverse the path to get the correct most cost effective path from source Node to this particular end Node
            Collections.reverse(path);

            // Output will be appended to output.txt
            FileWriter fileWriter = new FileWriter("output.txt", true);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            // Append the new line to the file
            bufferedWriter.newLine();
            // If the Node was unreachable, then say the cost was -999 and print the message and that result to output.txt
            if (destination.getCost() == Integer.MAX_VALUE) {
                System.out.println("Path from Node " +source.getNodeID() +" to Node " +destination.getNodeID() +": -999");
                bufferedWriter.write("from " +source.getNodeID() +" to " +destination.getNodeID() +" cost infinite hops unreachable message " +message);
            // If the Node was reachable, then print the message and results to output.txt
            } else {
                System.out.print("Path from Node " +source.getNodeID() +" to Node " +destination.getNodeID() +": ");
                String appendString = "from " +source.getNodeID() +" to " +destination.getNodeID() +" cost " +destination.getCost() +" hops ";
                // gets the exact path for the string to be added to output.txt
                for (int i = 0; i < path.size(); i++) {
                    System.out.print(path.get(i).getNodeID());
                    if (i < path.size() - 1) {
                        System.out.print(" -> ");
                        appendString += path.get(i).getNodeID() +" ";
                    }
                }
                appendString += "message " +message;

                if (path.size() == 1) {
                    System.out.print(" (nextHop: " +path.get(0) +")");
                } else if (path.size() > 1) {
                    System.out.print(" (nextHop: " +path.get(1) +")");
                }
                System.out.println(" (cost: " + destination.getCost() + ")");

                //String is added to output.txt
                bufferedWriter.write(appendString);
                System.out.println(appendString);
            }
            // Close the writers
            bufferedWriter.close();
            fileWriter.close();
        }    

        // Node subclass for Topology
        public static class Node {
            // Nodes have a nodeID, cost, and previous node all associated with them
            private int nodeID;
            private int cost;
            private Node previous;

            // Node constructor
            public Node(int nodeID) {
                this.nodeID = nodeID;
                cost = Integer.MAX_VALUE;
            }

            // Returns the Node's ID
            public int getNodeID() {
                return nodeID;
            }

            // Returns the Node's associated cost
            public int getCost() {
                return cost;
            }

            // Set the cost of a Node
            public void setCost(int cost) {
                this.cost = cost;
            }

            // Returns the previous Node of a Node
            public Node getPrevious() {
                return previous;
            }

            // Sets the previous Node of a Node
            public void setPrevious(Node previous) {
                this.previous = previous;
            }

            // toString
            @Override
            public String toString() {
                return "Node " +nodeID;
            }
        }

        // Link subclass for Topology
        public static class Link {
            // All Links have a starting Node, ending Node, and a cost associated with them
            private Node start;
            private Node end;
            private int cost;

            // Constructor for a Link
            public Link(Node start, Node end, int cost) {
                this.start = start;
                this.end = end;
                this.cost = cost;
            }

            // Returns the start Node of a Link
            public Node getStart() {
                return start;
            }

            // Returns the end Node of a Link
            public Node getEnd() {
                return end;
            }

            // Returns the cost of an Link
            public int getCost() {
                return cost;
            }

            // toString
            @Override
            public String toString() {
                return "(" +start +" <-> " +end +" = " +cost +")";
            }
        }

    }
}
