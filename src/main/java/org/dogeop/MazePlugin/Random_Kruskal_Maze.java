package org.dogeop.MazePlugin;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by lyt on 16-8-3.
 */
public class Random_Kruskal_Maze extends Abstract2DMaze {
    int [] dirs = new int[]{1,0,0,1,-1,0,0,-1};
    static class Factory implements MazeFactory
    {
        @Override
        public IMaze GenBlankMaze(int width) {
            IMaze maze =  new Random_Kruskal_Maze(width);
            maze.init();
            return maze;
        }

        @Override
        public IMaze GenMaze(int width) {
            IMaze maze =  new Random_Kruskal_Maze(width);
            maze.init();
            maze.generate(1,1);
            return  maze;
        }
    }
    public Random_Kruskal_Maze(int width) {
        maze = new Mazenode_tree[2 * width + 1][2 * width + 1];
        this.width = width;
    }
    static class Mazenode_tree extends MazeNode
    {
        public Mazenode_tree(boolean isWall, int X, int Y) {
            super(isWall, X, Y);
        }

        void Union(Mazenode_tree Another)
        {
            Mazenode_tree roota = find(this);
            Mazenode_tree rootb = find(Another);
            roota.parent = rootb;
        }
        Mazenode_tree find(Mazenode_tree node)
        {
            if(node.equals(node.parent))
            {
                return node.parent;
            }
            else
            {
                return find(node.parent);
            }
        }
        Mazenode_tree parent = null;
    }
    @Override
    public void setNode(int i, int j, boolean isWall) {
        maze[i][j] = new Mazenode_tree(isWall, i, j);
    }
    @Override
    public void generate(int startx, int starty) {
        ArrayList<Mazenode_tree> walls = new ArrayList<Mazenode_tree>();
        for(MazeNode[] nodes: maze)
        {
            for(MazeNode node : nodes)
            {
                if(!node.isWall)
                {
                    Mazenode_tree ref = (Mazenode_tree)node;
                    ref.parent = ref;
                }
                else {
                    if(((node.X % 2 == 0 && node.Y % 2 == 1) || (node.X % 2 == 1 &&  node.Y % 2 == 0))&& node.X > 0 && node.Y > 0 && node.Y < maze.length - 1 && node.X < maze.length - 1)
                    {
                        walls.add((Mazenode_tree) node);
                    }
                }
            }
        }
        while(walls.size() > 0)
        {
            Mazenode_tree currentwall = walls.get(random.nextInt(walls.size()));
            int dx;
            int dy;
            Mazenode_tree node = null;
            boolean shouldremove = true;
            for (int i = 0; i < 4; i++) {
                dx = dirs[i * 2];
                dy = dirs[i * 2 + 1];
                if (currentwall.X + dx < 0 || currentwall.Y  + dy < 0 || currentwall.X + dx > maze.length || currentwall.Y  + dy > maze.length) {
                    continue;
                } else {
                    if (node == null) {
                        node = (Mazenode_tree) maze[currentwall.X + dx][currentwall.Y + dy];
                        if (node.isWall) {
                            node = null;

                        }
                    }else {
                        if(!maze[currentwall.X + dx][currentwall.Y + dy].isWall) {
                            shouldremove &= (!node.find(node).equals(node.find((Mazenode_tree) maze[currentwall.X + dx][ currentwall.Y + dy])));
                            if (shouldremove) {
                                node.Union((Mazenode_tree) maze[currentwall.X + dx][currentwall.Y + dy]);
                                break;
                            }
                        }
                    }
                }
            }
            if(shouldremove)
            {
                currentwall.isWall = false;
                currentwall.parent = currentwall;
                node.Union(currentwall);
            }
            walls.remove(currentwall);
            walls.trimToSize();
         //   System.out.println(walls.size());
         //   printAsString();
        }

    }
}