package org.dogeop.MazePlugin;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.Random;
import java.util.Stack;

class MazeNode {
    boolean visited = false;
    boolean isWall;
    int X;
    int Y;
    public MazeNode(boolean isWall, int X, int Y){
        this.isWall = isWall;
        this.X = X;
        this.Y = Y;
    }
}
public class Maze
{
    Random random;
    MazeNode maze[][];
    ArrayList<MazeNode> unvisited = new ArrayList<MazeNode>();
    int width;
    public Maze(int width)
    {
        maze = new MazeNode[2 * width + 1][2 * width + 1];
        this.width = width;
    }
    public void init()
    {
        random = new Random();
        for(int i = 0; i < 2 * width + 1; i++)
        {
            for(int j = 0; j < 2 * width + 1; j++) {
                if (i % 2 != 0 && j % 2 != 0) {
                    setNode(i,j,false);
                    unvisited.add(maze[i][j]);
                } else {
                    setNode(i,j,true);
                }

            }
        }
    }
    public void removeWall(MazeNode A, MazeNode B)
    {
        int wallX = (A.X + B.X) / 2;
        int wallY = (A.Y + B.Y) / 2;
        maze[wallX][wallY].isWall = false;
    }
    public int checkdir(int x, int y)
    {
        if(maze[x][y].isWall)
        {
            throw new IllegalArgumentException("cannot do this on a wall");
        }
        else {
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
                }
                else {
                    return -1;
                }

            } else if (y == 0) {
                if (!maze[x][y + 1].isWall) {
                    //x dir
                    return 1;
                }else {
                    return -1;
                }
            } else if (x == maze.length - 1) {
                if (!maze[x - 1][y].isWall) {
                    //x dir
                    return 0;
                }else {
                    return -1;
                }
            } else if (y == maze.length - 1)
            {
                if (!maze[x][y - 1].isWall) {
                    //x dir
                    return 1;
                }else {
                    return -1;
                }
            }
            return 0;
        }
    }
    public ArrayList<MazeNode> getAdjunvisitedNode(int x, int y) {
        ArrayList<MazeNode> adjnodes = new ArrayList<MazeNode>();
        if (maze[x][y].isWall) {
            throw new IllegalArgumentException("Cannot set in the wall");
        }
        if (x + 2 > 0 && x + 2 < 2 * width + 1 && !maze[x + 2][y].visited)
        {
            adjnodes.add(maze[x + 2][y]);
        }
        if(y + 2 > 0 && y + 2 < 2 * width + 1 && !maze[x][y + 2].visited)
        {
            adjnodes.add(maze[x][y + 2]);
        }
        if(x - 2 > 0 && x - 2 < 2 * width + 1 && !maze[x - 2][y].visited)
        {
            adjnodes.add(maze[x - 2][y]);
        }
        if(y - 2 > 0 && y - 2 < 2 * width + 1 && !maze[x][y - 2].visited)
        {
            adjnodes.add(maze[x][y - 2]);
        }
        return adjnodes;
    }
    public ArrayList<MazeNode> getAdjunvisitedNode_generateterrain(int x, int y) throws IllegalArgumentException
    {
        ArrayList<MazeNode> adjnodes = new ArrayList<MazeNode>();
        if (maze[x][y].isWall) {
            throw new IllegalArgumentException("Cannot set in the wall");
        }
        if (x + 1 > 0 && x + 1 < 2 * width + 1 && !maze[x + 1][y].visited && !maze[x + 1][y].isWall)
        {
            adjnodes.add(maze[x + 2][y]);
        }
        if(y + 1 > 0 && y + 1 < 2 * width + 1 && !maze[x][y + 1].visited && !maze[x][y + 1].isWall)
        {
            adjnodes.add(maze[x][y + 2]);
        }
        if(x - 1 > 0 && x - 1 < 2 * width + 1 && !maze[x - 1][y].visited && !maze[x - 1][y].isWall)
        {
            adjnodes.add(maze[x - 2][y]);
        }
        if(y - 1 > 0 && y - 1 < 2 * width + 1 && !maze[x][y - 1].visited && !maze[x][y - 1].isWall)
        {
            adjnodes.add(maze[x][y - 1]);
        }
        return adjnodes;
    }
    public void generate(int startx, int starty)
    {
        if(maze[startx][starty].isWall)
        {
            throw new IllegalArgumentException("Cannot set startpoint in a wall");
        }
        Stack<MazeNode> NodeStack = new Stack<MazeNode>();
        MazeNode current = maze[startx][starty];
        current.visited = true;
        unvisited.remove(current);
        while(!unvisited.isEmpty()) {
            unvisited.trimToSize();
            ArrayList<MazeNode> adjs = getAdjunvisitedNode(current.X, current.Y);
            if (adjs.size() > 0)
            {
                //System.out.println("Visiting new");
                int idx = 0;
                if(adjs.size() > 1)
                {
                    idx = random.nextInt(adjs.size());
                }
                MazeNode adj = adjs.get(idx);
                NodeStack.push(current);
                removeWall(adj,current);
                adj.visited = true;
                unvisited.remove(adj);
                current = adj;
            }
            else {
                try
                {
                    //System.out.println("Found no way , popping");
                    MazeNode pop = NodeStack.pop();
                    current = pop;
                }
                catch(EmptyStackException e)
                {
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
    public void setNode(int i, int j, boolean isWall)
    {
        maze[i][j] = new MazeNode(isWall,i,j);
    }
    public void printAsString()
    {
        System.out.println("\n");
        String s = "";
        for(int i = 0; i < 2 * width + 1; i++) {
            for (int j = 0; j < 2 * width + 1; j++) {
                if (!maze[i][j].isWall) {
                    s += "# ";
                } else {
                    s += "O ";
                }
            }
            System.out.println(s);
            s = "";
        }
    }
}
