package Logic;

import UI.GamePanel;
import java.awt.*;
import java.util.*;
import java.util.List;

public class PathFinding {
    private static final int GRID_SIZE = GamePanel.BLOCK_SIZE;
    private static final int MAX_PATH_LENGTH = 30;


    public static boolean hasClearPath(int startX, int startY, int goalX, int goalY, WallManager wallManager) {
        int steps = (int) (Math.hypot(goalX - startX, goalY - startY) / (GRID_SIZE / 2));
        if (steps == 0) return true;

        double dx = (goalX - startX) / (double) steps;
        double dy = (goalY - startY) / (double) steps;

        double currentX = startX;
        double currentY = startY;

        for (int i = 0; i < steps; i++) {
            currentX += dx;
            currentY += dy;
            if (wallManager.isWall((int)currentX, (int)currentY)) {
                return false;
            }
            if (wallManager.isWall((int)currentX + 15, (int)currentY + 15)) return false;
            if (wallManager.isWall((int)currentX - 15, (int)currentY - 15)) return false;
        }

        return true;
    }

    public static Point findNextStep(int startX, int startY, int goalX, int goalY, WallManager wallManager) {
        int gridStartX = startX / GRID_SIZE;
        int gridStartY = startY / GRID_SIZE;
        int gridGoalX = goalX / GRID_SIZE;
        int gridGoalY = goalY / GRID_SIZE;

        List<Node> path = findPath(gridStartX, gridStartY, gridGoalX, gridGoalY, wallManager);

        if (path != null && path.size() > 1) {
            Node nextNode = path.get(1);
            return new Point(nextNode.x * GRID_SIZE + GRID_SIZE/2, nextNode.y * GRID_SIZE + GRID_SIZE/2);
        }

        return new Point(goalX, goalY);
    }

    private static List<Node> findPath(int startX, int startY, int goalX, int goalY, WallManager wallManager) {
        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Set<String> closedSet = new HashSet<>();
        Map<String, Node> allNodes = new HashMap<>();

        Node startNode = new Node(startX, startY, null, 0, heuristic(startX, startY, goalX, goalY));
        openSet.add(startNode);
        allNodes.put(startNode.key(), startNode);

        int iterations = 0;
        int maxIterations = 500;

        while (!openSet.isEmpty() && iterations < maxIterations) {
            iterations++;
            Node current = openSet.poll();

            if (current.x == goalX && current.y == goalY) {
                return reconstructPath(current);
            }

            closedSet.add(current.key());

            int[][] directions = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}, {-1, -1}, {-1, 1}, {1, -1}, {1, 1}};

            for (int[] dir : directions) {
                int newX = current.x + dir[0];
                int newY = current.y + dir[1];
                String neighborKey = newX + "," + newY;

                if (closedSet.contains(neighborKey)) continue;

                int worldX = newX * GRID_SIZE + GRID_SIZE / 2;
                int worldY = newY * GRID_SIZE + GRID_SIZE / 2;

                if (wallManager.isWall(worldX, worldY)) continue;

                double moveCost = (Math.abs(dir[0]) + Math.abs(dir[1]) == 2) ? 1.414 : 1.0;
                double newG = current.g + moveCost;

                Node neighbor = allNodes.get(neighborKey);
                if (neighbor == null) {
                    neighbor = new Node(newX, newY, current, newG, heuristic(newX, newY, goalX, goalY));
                    allNodes.put(neighborKey, neighbor);
                    openSet.add(neighbor);
                } else if (newG < neighbor.g) {
                    neighbor.g = newG;
                    neighbor.parent = current;
                    neighbor.f = neighbor.g + neighbor.h;
                    openSet.remove(neighbor);
                    openSet.add(neighbor);
                }
            }
        }
        return null;
    }

    private static double heuristic(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    private static List<Node> reconstructPath(Node goalNode) {
        List<Node> path = new ArrayList<>();
        Node current = goalNode;
        while (current != null) {
            path.add(0, current);
            current = current.parent;
        }
        return path;
    }

    private static class Node implements Comparable<Node> {
        int x, y;
        Node parent;
        double g, h, f;

        public Node(int x, int y, Node parent, double g, double h) {
            this.x = x;
            this.y = y;
            this.parent = parent;
            this.g = g;
            this.h = h;
            this.f = g + h;
        }

        public String key() { return x + "," + y; }

        @Override
        public int compareTo(Node other) { return Double.compare(this.f, other.f); }
    }
}