package org.dogeop.MazePlugin;

import java.util.ArrayList;

/**
 * Created by lyt on 16-8-6.
 */
public class Aldous_Broder_3DMaze extends Abstract3DMaze{
    static class Factory implements IMaze.MazeFactory
    {
        @Override
        public IMaze GenBlankMaze(int width) {
            IMaze maze =  new Aldous_Broder_3DMaze(width);
            maze.init();
            return  maze;
        }

        @Override
        public IMaze GenMaze(int width) {
            IMaze maze =  new Aldous_Broder_3DMaze(width);
            maze.init();
            maze.generate(1,1,1);
            return  maze;
        }
    }
    @Override
    public void generate(int startx, int starty,int startz) {
        ArrayList<MazeNode> unvisited = new ArrayList<MazeNode>();
        for(MazeNode[][] nodess : maze) {
            for (MazeNode[] nodes : nodess) {
                for (MazeNode node : nodes)
                {
                    if (!node.isWall && !node.visited) {
                        unvisited.add(node);
                    }
                }
            }
        }
        MazeNode current = maze[startx][starty][startz];
        MazeNode next;
        current.visited = true;
        unvisited.remove(current);
        unvisited.trimToSize();
        while (unvisited.size() > 0)
        {
            next = getAdjunvisitedNode(current.X,current.Y,current.Z).get(0);
            if(!next.visited)
            {
                removeWall(current,next);
                next.visited = true;
                unvisited.remove(next);
                unvisited.trimToSize();
            }
            current = next;
            //System.out.printf("%d, %d",current.X,current.Y);
            //    System.out.println(unvisited.size());
        }
    }



    public int minpos()
    {
        return (random.nextDouble() > 0.5) ? -2 : 2;
    }
    @Override
    public ArrayList<MazeNode> getAdjunvisitedNode(int x, int y, int z) {

        ArrayList<MazeNode> list = new ArrayList<>();
        int nx;
        int ny;
        int nz;
        double roll = random.nextDouble();
        if(roll < 0.3333333) {
            nx = x + minpos();
            if (nx < 0 || nx > maze.length - 1)
            {
                nx = x;
            }
            ny = y;
            nz = z;
        }
        else if(roll > 0.3333333 && roll <= 0.6666667)
        {
            ny = y+ minpos();
            if (ny < 0 || ny > maze.length - 1)
            {
                ny = y;
            }
            nx = x;
            nz = z;
        }
        else
        {
            nz = z + minpos();
            if (nz < 0 || nz > maze.length - 1)
            {
                nz = z;
            }
            ny = y;
            nx = x;
        }
        list.add(maze[nx][ny][nz]);
        return list;
    }
    public Aldous_Broder_3DMaze(int width) {
        maze = new MazeNode[2 * width + 1][2 * width + 1][2 * width + 1];
        this.width = width;
    }

}
