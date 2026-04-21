package Logic;

import Logic.World.WallManager;
import UI.Game.GamePanel;
import java.awt.*;
import java.util.*;
import java.util.List;

public class PathFinding {
    public static final int GRID_SIZE = GamePanel.BLOCK_SIZE;

    // ── Globální limit A* za frame – max 3 výpočty per 16ms ──────────────────
    private static int  aStarThisFrame = 0;
    private static long lastFrameMs    = 0;
    // Globální cooldown mezi A* dávkami – zabraňuje storm spikům při resetu
    private static long lastAStarMs    = 0;
    private static final long A_STAR_GLOBAL_COOLDOWN = 2; // min 2ms mezi A* výpočty

    private static boolean canRunAStar() {
        long now = System.currentTimeMillis();
        // Nový frame = reset počítadla
        if (now - lastFrameMs > 16) {
            aStarThisFrame = 0;
            lastFrameMs = now;
        }
        // Nepřekroč limit per frame
        if (aStarThisFrame >= 3) return false;
        // Nezačínáme příliš rychle po sobě (rozmaž výpočty přes čas)
        if (now - lastAStarMs < A_STAR_GLOBAL_COOLDOWN && aStarThisFrame > 0) return false;
        aStarThisFrame++;
        lastAStarMs = now;
        return true;
    }

    // Long key = žádné String alokace v A* inner loop
    private static long nodeKey(int x, int y) {
        return ((long)(x + 10000)) << 20 | (y + 10000);
    }

    public static boolean hasClearPath(int startX, int startY, int goalX, int goalY, WallManager wallManager) {
        // Hrubší krok (GRID_SIZE místo GRID_SIZE/2) = 2× méně isWall() volání
        int steps = (int)(Math.hypot(goalX - startX, goalY - startY) / GRID_SIZE);
        if (steps == 0) return true;
        double dx = (goalX - startX) / (double) steps;
        double dy = (goalY - startY) / (double) steps;
        double cx = startX, cy = startY;
        for (int i = 0; i < steps; i++) {
            cx += dx; cy += dy;
            if (wallManager.isWall((int)cx, (int)cy))           return false;
            if (wallManager.isWall((int)cx + 15, (int)cy + 15)) return false;
            if (wallManager.isWall((int)cx - 15, (int)cy - 15)) return false;
        }
        return true;
    }

    public static Point findNextStep(int startX, int startY, int goalX, int goalY, WallManager wallManager) {
        if (!canRunAStar()) return null;
        int gsx = startX / GRID_SIZE, gsy = startY / GRID_SIZE;
        int ggx = goalX  / GRID_SIZE, ggy = goalY  / GRID_SIZE;
        List<Node> path = findPathInternal(gsx, gsy, ggx, ggy, wallManager, 0);
        if (path != null && path.size() > 1) {
            Node n = path.get(1);
            return new Point(n.x * GRID_SIZE + GRID_SIZE / 2, n.y * GRID_SIZE + GRID_SIZE / 2);
        }
        return new Point(goalX, goalY);
    }

    public static Point findNextStepLarge(int startX, int startY, int goalX, int goalY,
                                          WallManager wallManager, int entitySize) {
        if (!canRunAStar()) return null;
        int gsx = startX / GRID_SIZE, gsy = startY / GRID_SIZE;
        int ggx = goalX  / GRID_SIZE, ggy = goalY  / GRID_SIZE;
        List<Node> path = findPathInternal(gsx, gsy, ggx, ggy, wallManager, entitySize);
        if (path != null && path.size() > 1) {
            Node n = path.get(1);
            return new Point(n.x * GRID_SIZE + GRID_SIZE / 2, n.y * GRID_SIZE + GRID_SIZE / 2);
        }
        // Fallback: zkus bez entitySize padding – ale jen pokud máme token
        if (canRunAStar()) {
            List<Node> fb = findPathInternal(gsx, gsy, ggx, ggy, wallManager, 0);
            if (fb != null && fb.size() > 1) {
                Node n = fb.get(1);
                return new Point(n.x * GRID_SIZE + GRID_SIZE / 2, n.y * GRID_SIZE + GRID_SIZE / 2);
            }
        }
        return new Point(goalX, goalY);
    }

    private static final int[][] DIRS = {{0,-1},{0,1},{-1,0},{1,0},{-1,-1},{-1,1},{1,-1},{1,1}};

    private static List<Node> findPathInternal(int startX, int startY, int goalX, int goalY,
                                               WallManager wallManager, int entitySize) {
        PriorityQueue<Node>  open     = new PriorityQueue<>();
        HashSet<Long>        closed   = new HashSet<>();
        HashMap<Long, Node>  allNodes = new HashMap<>();

        Node start = new Node(startX, startY, null, 0, heuristic(startX, startY, goalX, goalY));
        open.add(start);
        allNodes.put(nodeKey(startX, startY), start);

        int iter = 0;
        while (!open.isEmpty() && iter < 150) {   // 150 místo 500
            iter++;
            Node cur = open.poll();
            if (cur.x == goalX && cur.y == goalY) return reconstructPath(cur);
            long ck = nodeKey(cur.x, cur.y);
            if (!closed.add(ck)) continue;

            for (int[] dir : DIRS) {
                int nx = cur.x + dir[0], ny = cur.y + dir[1];
                long nk = nodeKey(nx, ny);
                if (closed.contains(nk)) continue;
                int wx = nx * GRID_SIZE + GRID_SIZE / 2;
                int wy = ny * GRID_SIZE + GRID_SIZE / 2;
                if (wallManager.isWall(wx, wy)) continue;
                if (entitySize > GRID_SIZE / 2) {
                    int pad = entitySize / 2, bx = nx * GRID_SIZE, by = ny * GRID_SIZE;
                    if (wallManager.isWall(bx + pad, by + pad) ||
                            wallManager.isWall(bx + GRID_SIZE - pad, by + pad) ||
                            wallManager.isWall(bx + pad, by + GRID_SIZE - pad) ||
                            wallManager.isWall(bx + GRID_SIZE - pad, by + GRID_SIZE - pad)) continue;
                }
                double moveCost = (Math.abs(dir[0]) + Math.abs(dir[1]) == 2) ? 1.414 : 1.0;
                double newG = cur.g + moveCost;
                Node nb = allNodes.get(nk);
                if (nb == null) {
                    nb = new Node(nx, ny, cur, newG, heuristic(nx, ny, goalX, goalY));
                    allNodes.put(nk, nb);
                    open.add(nb);
                } else if (newG < nb.g) {
                    nb.g = newG; nb.parent = cur; nb.f = nb.g + nb.h;
                    open.remove(nb); open.add(nb);
                }
            }
        }
        return null;
    }

    private static double heuristic(int x1, int y1, int x2, int y2) {
        int dx = Math.abs(x1 - x2), dy = Math.abs(y1 - y2);
        return Math.max(dx, dy) + (Math.sqrt(2.0) - 1.0) * Math.min(dx, dy);
    }

    private static List<Node> reconstructPath(Node goal) {
        List<Node> path = new ArrayList<>();
        for (Node n = goal; n != null; n = n.parent) path.add(0, n);
        return path;
    }

    private static class Node implements Comparable<Node> {
        int x, y; Node parent; double g, h, f;
        Node(int x, int y, Node p, double g, double h) {
            this.x=x; this.y=y; parent=p; this.g=g; this.h=h; f=g+h;
        }
        @Override public int compareTo(Node o) { return Double.compare(f, o.f); }
    }
}