package org.dogeop.MazePlugin;

import com.google.gson.Gson;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Main {

    public static void main(String[] args) {
	// write your code here
        int width = 5;
        Abstract3DMaze maze = (Abstract3DMaze) new Aldous_Broder_3DMaze.Factory().GenMaze(width);
        Gson g = new Gson();
        HashMap<String,Object> m = new HashMap<String,Object>();
        HashMap<String,Object> m2 = new HashMap<String,Object>();
        m.put("S",1);
        m.put("B","C");
        m2.put("fuck","you");
        m.put("map",m2);
        String s = g.toJson(m);
        System.out.println(s);
        HashMap<String,Object> re = g.fromJson("{\"cd01f4b9-5e4d-34b6-ae31-67be06a1405a\":[{\"type\":\"AIR\",\"damage\":-1,\"amount\":0}],\"5f54b388-46d7-3b71-bc5d-e33b59da70de\":[{\"type\":\"MONSTER_EGGS\"},{\"type\":\"VINE\"},{\"type\":\"WATER_LILY\"},{\"type\":\"RED_ROSE\",\"damage\":5},{\"type\":\"MONSTER_EGGS\",\"damage\":5},{\"type\":\"THIN_GLASS\"},{\"type\":\"MONSTER_EGGS\",\"damage\":2},{\"type\":\"WORKBENCH\"},{\"type\":\"CACTUS\"},{\"type\":\"AIR\",\"damage\":-1,\"amount\":0}]}",HashMap.class);
        System.out.println(re.toString());
        Pattern p  = Pattern.compile("/(tp|home)");
        System.out.println(p.matcher("/tp").matches());
        maze.printAsString();
    }
}
