package org.dogeop.MazePlugin;

import com.sun.org.apache.regexp.internal.RE;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.SystemUtils;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by lyt on 16-8-3.
 */

public class Aldous_Broder_Maze extends Abstract2DMaze {
    static class Factory implements MazeFactory
    {
        @Override
        public IMaze GenBlankMaze(int width) {
            IMaze maze =  new Aldous_Broder_Maze(width);
            maze.init();
            return  maze;
        }

        @Override
        public IMaze GenMaze(int width) {
            IMaze maze =  new Aldous_Broder_Maze(width);
            maze.init();
            maze.generate(1,1);
            return  maze;
        }
    }
    @Override
    public void generate(int startx, int starty) {
        ArrayList<MazeNode> unvisited = new ArrayList<MazeNode>();
        for(MazeNode[] nodes : maze) {
            for (MazeNode node : nodes) {
                if (!node.isWall && !node.visited) {
                    unvisited.add(node);
                }
            }
        }
        MazeNode current = maze[1][1];
        MazeNode next;
        current.visited = true;
        unvisited.remove(current);
        unvisited.trimToSize();
        while (unvisited.size() > 0)
        {
            next = getAdjunvisitedNode(current.X,current.Y).get(0);
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
    public ArrayList<MazeNode> getAdjunvisitedNode(int x, int y) {

        ArrayList<MazeNode> list = new ArrayList<>();
        int nx;
        int ny;
        if(random.nextFloat() < 0.5) {
            nx = x + minpos();
            if (nx < 0 || nx > maze.length - 1)
            {
                nx = x;
            }
            ny = y;
        }
        else
        {
            ny = y+ minpos();
            if (ny < 0 || ny > maze.length - 1)
            {
                ny = y;
            }
            nx = x;
        }
        list.add(maze[nx][ny]);
        return list;
    }
    public Aldous_Broder_Maze(int width) {
        maze = new MazeNode[2 * width + 1][2 * width + 1];
        this.width = width;
    }
}
