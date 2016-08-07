package org.dogeop.MazePlugin;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.Stack;

/**
 * Created by lyt on 16-8-7.
 */
public class DFS_Recursive_Backtrack_3DMaze extends Abstract3DMaze {
    static class Factory implements MazeFactory
    {
        @Override
        public IMaze GenBlankMaze(int width) {
            IMaze maze =  new DFS_Recursive_Backtrack_3DMaze(width);
            maze.init();
            return maze;
        }

        @Override
        public IMaze GenMaze(int width) {
            IMaze maze =  new DFS_Recursive_Backtrack_3DMaze(width);
            maze.init();
            maze.generate(1,1,1);
            return  maze;
        }
    }
    private DFS_Recursive_Backtrack_3DMaze(int width) {
        maze = new MazeNode[2 * width + 1][2 * width + 1][2 * width + 1];
        this.width = width;
    }
    public int checkdir(int x, int y,int z) {
        return  0;
    }

    public ArrayList<MazeNode> getAdjunvisitedNode(int x, int y, int z) throws IllegalArgumentException {
        ArrayList<MazeNode> adjnodes = new ArrayList<MazeNode>();
        if (maze[x][y][z].isWall) {
            throw new IllegalArgumentException("Cannot set in the wall");
        }
        if (x + 1 > 0 && x + 1 < 2 * width + 1 && !maze[x + 1][y][z].visited && !maze[x + 1][y][z].isWall) {
            adjnodes.add(maze[x + 2][y][z]);
        }
        if (y + 1 > 0 && y + 1 < 2 * width + 1 && !maze[x][y + 1][z].visited && !maze[x][y + 1][z].isWall) {
            adjnodes.add(maze[x][y + 2][z]);
        }
        if (x - 1 > 0 && x - 1 < 2 * width + 1 && !maze[x - 1][y][z].visited && !maze[x - 1][y][z].isWall) {
            adjnodes.add(maze[x - 2][y][z]);
        }
        if (y - 1 > 0 && y - 1 < 2 * width + 1 && !maze[x][y - 1][z].visited && !maze[x][y - 1][z].isWall) {

            adjnodes.add(maze[x][y - 1][z]);
        }
        if (z + 1 > 0 && z + 1 < 2 * width + 1 && !maze[x][y][z + 1].visited && !maze[x][y][z + 1].isWall) {
            adjnodes.add(maze[x][y - 1][z]);
        }
        if (z - 1 > 0 && z - 1 < 2 * width + 1 && !maze[x][y][z - 1].visited && !maze[x][y][z - 1].isWall) {
            adjnodes.add(maze[x][y - 1][z]);
        }
        return adjnodes;
    }

    @Override
    public void generate(int startx, int starty, int startz) {
        if (maze[startx][starty][startz].isWall) {
            throw new IllegalArgumentException("Cannot set startpoint in a wall");
        }
        Stack<MazeNode> NodeStack = new Stack<MazeNode>();
        MazeNode current = maze[startx][starty][startz];
        current.visited = true;
        unvisited.remove(current);
        while (!unvisited.isEmpty()) {
            unvisited.trimToSize();
            ArrayList<MazeNode> adjs = getAdjunvisitedNode(current.X, current.Y,current.Z);
            if (adjs.size() > 0) {//System.out.println("Visiting new");
                int idx = 0;
                if (adjs.size() > 1) {
                    idx = random.nextInt(adjs.size());
                }
                MazeNode adj = adjs.get(idx);
                NodeStack.push(current);
                removeWall(adj, current);
                adj.visited = true;
                unvisited.remove(adj);
                current = adj;
            } else {
                try {
                    //System.out.println("Found no way , popping");
                    MazeNode pop = NodeStack.pop();
                    current = pop;
                } catch (EmptyStackException e) {
                    // System.out.println("Choosing a new start");
                    int idx = random.nextInt(unvisited.size());
                    current = unvisited.get(idx);
                    current.visited = true;
                    unvisited.remove(current);
                }
            }
            //printAsString();
        }

    }
}
