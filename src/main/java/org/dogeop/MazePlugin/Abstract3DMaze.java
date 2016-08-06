package org.dogeop.MazePlugin;

import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by lyt on 16-8-6.
 */
public abstract class Abstract3DMaze implements IMaze {
    MazeNode [][][] maze;
    int width;
    @Override
    public void init() {

    }

    @Override
    public void removeWall(MazeNode A, MazeNode B) {

    }

    @Override
    public int checkdir(int x, int y, int z) {
        return 0;
    }

    @Override
    public ArrayList<MazeNode> getAdjunvisitedNode(int x, int y, int z) {
        return null;
    }

    @Override
    public ArrayList<MazeNode> getAdjunvisitedNode(int x, int y) {
        return null;
    }

    @Override
    public void generate(int startx, int starty, int startz) {

    }

    @Override
    public void generate(int startx, int starty) {

    }

    @Override
    public void setNode(int x, int y, boolean isWall) {

    }

    @Override
    public void setNode(int x, int y, int z, boolean isWall) {

    }

    @Override
    public MazeNode getNode(int x, int y, int z) {
        return null;
    }

    @Override
    public MazeNode getNode(int x, int y) {
        return null;
    }

    @Override
    public MazeNode getNodePlayerStandOn(int X, int Z) {
        return null;
    }

    @Override
    public boolean checkNodeIsEdge(MazeNode node) {
        return false;
    }

    @Override
    public boolean checkIfInMaze(Location loc) {
        return false;
    }

    @Override
    public void CheckPlayerPosition(MazePlugin _plugin) {

    }

    @Override
    public void GenBukkitWorld(JavaPlugin plugin, Map<String, Object> settings, boolean UpdateB) {

    }

    @Override
    public JSONObject JSONSerialize() {
        return null;
    }
}
