package org.dogeop.MazePlugin;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

class Main {

    public static void main(String[] args) {
	// write your code here
        int width = 100;
        Maze maze = new Maze(width);
        maze.init();
        maze.generate(1,1);
        maze.printAsString();
    }
}
