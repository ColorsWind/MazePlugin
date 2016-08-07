package org.dogeop.MazePlugin;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.plugin.java.JavaPlugin;
/**
 * Created by lyt on 16-8-3.
 */
public interface IMaze {


    public void init();
    public void removeWall(MazeNode A, MazeNode B);
    public int checkdir(int x, int y,int z);
    public ArrayList<MazeNode> getAdjunvisitedNode(int x, int y, int z);
    public ArrayList<MazeNode> getAdjunvisitedNode(int x, int y);
    public void generate(int startx, int starty,int startz);
    public void generate(int startx, int starty);
    public void setNode(int x, int y, boolean isWall);
    public void setNode(int x, int y, int z,boolean isWall);
    public MazeNode getNode(int x, int y, int z);
    public MazeNode getNode(int x, int y);
    public MazeNode getNodePlayerStandOn(int X, int Z);
    public boolean checkNodeIsEdge(MazeNode node);
    public boolean checkIfInMaze(Location loc);
    public void CheckPlayerPosition(MazePlugin _plugin);
    public void GenBukkitWorld(JavaPlugin plugin, Map<String, Object> settings, boolean UpdateB);
    public void handleItemsReshuffle(MazePlugin plugin, ItemStack[] items);
    public boolean HandleFinish(Location loc);
    public JSONObject JSONSerialize();
    static interface MazeFactory
    {
        public IMaze GenBlankMaze(int width);
        public IMaze GenMaze(int width);
    }
}
