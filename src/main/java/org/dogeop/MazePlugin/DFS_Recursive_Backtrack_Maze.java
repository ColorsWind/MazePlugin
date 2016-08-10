package org.dogeop.MazePlugin;

import org.apache.commons.lang.NotImplementedException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.Random;
import java.util.Stack;

class MazeNode {
    boolean visited = false;
    boolean isWall;
    int X;
    int Y;
    int Z;
    public MazeNode(boolean isWall, int X, int Y){
        this.isWall = isWall;
        this.X = X;
        this.Y = Y;
    }
    public MazeNode(boolean isWall, int X, int Y,int Z){
        this.isWall = isWall;
        this.X = X;
        this.Y = Y;
        this.Z = Z;
    }

    public MazeNode(int x, int y, int z, boolean isWall) {

    }
}
public class DFS_Recursive_Backtrack_Maze extends Abstract2DMaze {
    static class Factory implements MazeFactory
    {
        @Override
        public IMaze GenBlankMaze(int width) {
            IMaze maze =  new DFS_Recursive_Backtrack_Maze(width);
            maze.init();
            return maze;
        }

        @Override
        public IMaze GenMaze(int width) {
            IMaze maze =  new DFS_Recursive_Backtrack_Maze(width);
            maze.init();
            maze.generate(1,1);
            return  maze;
        }
    }
    private DFS_Recursive_Backtrack_Maze(int width) {
        maze = new MazeNode[2 * width + 1][2 * width + 1];
        this.width = width;
    }
    public int checkdir(int x, int y) {
        if (maze[x][y].isWall) {
            throw new IllegalArgumentException("cannot do this on a wall");
        } else {
            if (x > 0 && x < maze.length - 1 && y > 0 && y < maze.length - 1) {
                if (!maze[x - 1][y].isWall && !maze[x + 1][y].isWall) {
                    //x dir
                    return 0;
                } else if (!maze[x][y - 1].isWall && !maze[x][y + 1].isWall) {
                    //y dir
                    return 1;
                } else {
                    return -1;
                }
            } else if (x == 0) {
                if (!maze[x + 1][y].isWall) {
                    //x dir
                    return 0;
                } else {
                    return -1;
                }

            } else if (y == 0) {
                if (!maze[x][y + 1].isWall) {
                    //x dir
                    return 1;
                } else {
                    return -1;
                }
            } else if (x == maze.length - 1) {
                if (!maze[x - 1][y].isWall) {
                    //x dir
                    return 0;
                } else {
                    return -1;
                }
            } else if (y == maze.length - 1) {
                if (!maze[x][y - 1].isWall) {
                    //x dir
                    return 1;
                } else {
                    return -1;
                }
            }
            return 0;
        }
    }

    public ArrayList<MazeNode> getAdjunvisitedNode_generateterrain(int x, int y) throws IllegalArgumentException {
        ArrayList<MazeNode> adjnodes = new ArrayList<MazeNode>();
        if (maze[x][y].isWall) {
            throw new IllegalArgumentException("Cannot set in the wall");
        }
        if (x + 1 > 0 && x + 1 < 2 * width + 1 && !maze[x + 1][y].visited && !maze[x + 1][y].isWall) {
            adjnodes.add(maze[x + 2][y]);
        }
        if (y + 1 > 0 && y + 1 < 2 * width + 1 && !maze[x][y + 1].visited && !maze[x][y + 1].isWall) {
            adjnodes.add(maze[x][y + 2]);
        }
        if (x - 1 > 0 && x - 1 < 2 * width + 1 && !maze[x - 1][y].visited && !maze[x - 1][y].isWall) {
            adjnodes.add(maze[x - 2][y]);
        }
        if (y - 1 > 0 && y - 1 < 2 * width + 1 && !maze[x][y - 1].visited && !maze[x][y - 1].isWall) {
            adjnodes.add(maze[x][y - 1]);
        }
        return adjnodes;
    }

    @Override
    public void generate(int startx, int starty) {
        if (maze[startx][starty].isWall) {
            throw new IllegalArgumentException("Cannot set startpoint in a wall");
        }
        Stack<MazeNode> NodeStack = new Stack<MazeNode>();
        MazeNode current = maze[startx][starty];
        current.visited = true;
        unvisited.remove(current);
        while (!unvisited.isEmpty()) {
            unvisited.trimToSize();
            ArrayList<MazeNode> adjs = getAdjunvisitedNode(current.X, current.Y);
            if (adjs.size() > 0) {
                //System.out.println("Visiting new");
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
