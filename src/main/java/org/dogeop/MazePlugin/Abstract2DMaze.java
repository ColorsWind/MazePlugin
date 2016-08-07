package org.dogeop.MazePlugin;

import com.boydti.fawe.bukkit.wrapper.AsyncWorld;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.SpawnEgg;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;

/**
 * Created by lyt on 16-8-3.
 */
public abstract class Abstract2DMaze implements IMaze {
    protected Random random;
    protected MazeNode maze[][];
    public int width;
    public int mazeblockwidth;
    int OriginX;
    int OriginZ;
    int OriginY;
    Material Maze_Material_Wall;
    Material Maze_Material_ROOF_A;
    Material Maze_Material_ROOF_B;
    Material Maze_Material_Light;
    String world;
    double chance_takaramono_rare;
    double chance_takaramono;
    double chance_monster;
    ArrayList<Material> BonusItems;
    ArrayList<Material> BonusItems_rare;
    ArrayList<Enchantment> Enchantments;
    ArrayList<EntityType> Monsters;
    int try_count_takaramono;
    int gen_count_takaramono;
    int lastxmax;

    int mazeHeight_Node = 8;
    ArrayList<MazeNode> unvisited = new ArrayList<MazeNode>();
    ArrayList<BlockMeta> chestmeta;
    NumberFormat format = NumberFormat.getInstance();
    final int RepeatingAntiHangId = -1;
    final int chance_monster_zombiepigman = 1;
    final int chance_monster_skeleton = 2;
    final int chance_monster_slime = 3;
    final int chance_monster_blaze = 4;
    final int chance_monster_killerbunny = 5;
    final int chance_monster_powercreeper = 6;
    final int chance_monster_wither_skeleton = 7;
    final int chance_monster_witch = 8;
    final int chance_monster_ghast = 9;

    public void init() {
        random = new Random();
        for (int i = 0; i < 2 * width + 1; i++) {
            for (int j = 0; j < 2 * width + 1; j++) {
                if (i % 2 != 0 && j % 2 != 0) {
                    setNode(i, j, false);
                    unvisited.add(maze[i][j]);
                } else {
                    setNode(i, j, true);
                }

            }
        }
    }

    @Override
    public synchronized void GenBukkitWorld(JavaPlugin plugin, Map<String, Object> settings, boolean Update) {
        Maze_Material_Wall = (Material) settings.get("Maze_Material_Wall");
        Maze_Material_ROOF_A = (Material) settings.get("Maze_Material_ROOF_A");
        Maze_Material_ROOF_B = (Material) settings.get("Maze_Material_ROOF_B");
        Maze_Material_Light = (Material) settings.get("Maze_Material_Light");
        world = (String) settings.get("world");
        chance_takaramono_rare = (double) settings.get("Chance_Takaramono_rare");
        chance_takaramono = (double) settings.get("Chance_Chest");
        chance_monster = (double) settings.get("Chance_Monster");

        BonusItems = (ArrayList<Material>) settings.get("BonusItems");
        BonusItems_rare = (ArrayList<Material>) settings.get("BonusItems_rare");
        Enchantments = (ArrayList<Enchantment>) settings.get("Enchantments");
        Monsters = (ArrayList<EntityType>) settings.get("Monsters");
        try_count_takaramono = (int) settings.get("try_count_takaramono");
        gen_count_takaramono = (int) settings.get("gen_count_takaramono");
        lastxmax = (int) settings.get("lastxmax") * 2 + 1;
        OriginX = (int) settings.get("OriginX");
        OriginY = (int) settings.get("OriginY");
        OriginZ = (int) settings.get("OriginZ");
        mazeblockwidth = maze.length;
        if(!Update)
        {
            return;
        }
        MazePlugin _plugin = (MazePlugin) plugin;
        chestmeta = new ArrayList<BlockMeta>();
        String MazeType = "";
        World bukkitw = _plugin.getServer().getWorld(world);
        AsyncWorld w = AsyncWorld.wrap(bukkitw);
        long begintime = System.currentTimeMillis();
        _plugin.busy = true;


        _plugin.getServer().getScheduler().callSyncMethod(_plugin, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                for (Player p : _plugin.getServer().getOnlinePlayers()) {
                    p.sendMessage("开始异步计算方块");
                }
                return null;
            }
        });
        //Begin of clear extra blocks

        if (lastxmax > maze.length) {
            int vertical_limit = 6 * (lastxmax - maze.length + 1);
            int vertical_begin = 6 * maze.length;
            int horizontal_limit = 6 * (lastxmax + 1);
            int ylimit = 3 * mazeHeight_Node + OriginY + 1;
            for (int i = 3; i < vertical_limit; i++) {
                for (int j = 0; j < horizontal_limit; j++) {
                    for (int k = OriginY; k < ylimit; k++) {
                        w.getBlockAt(OriginX + vertical_begin + i, k, OriginZ + j).setType(Material.AIR);
                        w.getBlockAt(OriginZ + j, k, OriginX + vertical_begin + i).setType(Material.AIR);
                    }
                }
            }
        }
        //end of clear
        clearentities(_plugin);
        for (String UUID : _plugin.OffLineMazePlayers.keySet()) {
            _plugin.OffLineMazePlayers.put(UUID, true);
        }

        if (_plugin.RepeatingTaskId == -1) {
            _plugin.RepeatingTaskId = _plugin.getServer().getScheduler().scheduleSyncRepeatingTask(_plugin, new Runnable() {
                @Override
                public void run() {
                    CheckPlayerPosition(_plugin);
                }
            }, 0, 10);
        }
        if (_plugin.RepeatingAntiHangId == -1) {
            _plugin.RepeatingAntiHangId = _plugin.getServer().getScheduler().scheduleSyncRepeatingTask(_plugin, _plugin.Antihang, 0, 400);
        }
        if (_plugin.RepeatingSerializeTaskId == -1) {
            _plugin.RepeatingSerializeTaskId = _plugin.getServer().getScheduler().scheduleAsyncRepeatingTask(_plugin, _plugin.mSerializePlayerInfo, 0, 300);
        }
        //change lastxmax to xmax
        FileConfiguration conf = _plugin.getConfig();
        conf.set("lastWidthX", width);
        try {
            conf.save(_plugin.getDataFolder() + File.separator + "config.yml");
        } catch (IOException e) {
            e.printStackTrace();
        }
        //reset trigger

        ///Begin of block async update
        //set blocks
        GenBlocks(_plugin);

        //set chest at the end

        genFinalBonus(_plugin);
        for (MazeNode[] nodes : maze) {
            GenMobs_Concurrent(nodes, _plugin);
        }
        System.out.println("return");
        //clear items
        for (Entity e : w.getEntities()) {
            if (checkIfInMaze(e.getLocation())) {
                if (e instanceof Item) {
                    //("clearing " + ((Item)e).getName());
                    Item item = (Item) e;
                    item.remove();
                }
            }
        }
        //end of clear
        w.commit();
        long elapsed = (System.currentTimeMillis() - begintime) / 1000;
        _plugin.getServer().getScheduler().callSyncMethod(_plugin, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                for (Player p : _plugin.getServer().getOnlinePlayers()) {
                    p.sendMessage("清理中----完成！");
                    p.sendMessage("耗时" + format.format(elapsed) + "秒");
                }
                return null;
            }
        });
        //JSON Serialize Maze and store in file
        String json = JSONSerialize().toString();
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(_plugin.getDataFolder().getPath() + File.separator + _plugin.MazeSerialize_File))));
            bw.write(json);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if(bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        //end of serialize

        _plugin.getServer().getScheduler().callSyncMethod(_plugin, new Callable<Void>() {

            @Override
            public Void call() throws Exception {

                for (BlockMeta meta : chestmeta) {

                    bukkitw.getBlockAt(meta.x, meta.y, meta.z).setType(meta.type);
                    Chest chest = (Chest) bukkitw.getBlockAt(meta.x, meta.y, meta.z).getState();
                    Inventory inv = chest.getInventory();
                    for (int j = 0; j < 27; j++) {
                        if (meta.Inventories.get(j) != null) {
                            inv.setItem(j, ItemStack.deserialize(meta.Inventories.get(j)));
                        }
                    }
                }
                _plugin.busy = false;
                return null;
            }
        });
    }
    public void handleItemsReshuffle(MazePlugin _plugin, ItemStack[] items)
    {
        //Synchronous item reshuffle
        World w = _plugin.getServer().getWorld(world);
        BlockMeta meta;
        Block b;
        ArrayList<Integer> emptyslotidx = new ArrayList<Integer>();
        for(ItemStack item : items)
        {
            if(item != null)
            {
                do {
                    emptyslotidx.clear();
                    emptyslotidx.trimToSize();
                    meta = chestmeta.get(random.nextInt(chestmeta.size()));
                    b = w.getBlockAt(meta.x,meta.y,meta.z);
                    BlockState state = b.getState();
                    if(!(state instanceof Chest))
                    {
                        continue;
                    }
                    Chest c = (Chest) state;
                    Inventory i = c.getBlockInventory();
                    ItemStack[] contents = i.getContents();
                    for(int j = 0; j < 27; j++) {
                        if (contents[j] == null) {
                            emptyslotidx.add(j);
                        } else if (contents[j].getType() == Material.AIR)
                        {
                            emptyslotidx.add(j);
                        }
                    }
                    if(emptyslotidx.size() > 0)
                    {
                        i.setItem(emptyslotidx.get(0),item);
                    }
                }while (b.getType() != Material.CHEST || emptyslotidx.size() <= 0);
            }
        }
    }
    public void genFinalBonus(MazePlugin _plugin)
    {
        //
        AsyncWorld w = AsyncWorld.wrap(_plugin.getServer().getWorld(world));
        for (int i = 1; i < 4; i++) {
            for (int j = 1; j < 4; j++) {
                for (int k = 1; k < 4; k++) {
                    w.getBlockAt(OriginX + (maze.length - 1) * 3 - i, OriginY + j, OriginZ + (maze.length - 1) * 3 - k).setType(Material.AIR);
                }
            }
        }
        w.commit();
        double lastchance = chance_takaramono_rare;
        chance_takaramono_rare *= 4;
        for(int i = 1;i < 4;i++)
        {
            for(int j = 1; j < 4; j++)
            {
                for(int k = 1; k < 4;k++)
                {
                    w.getBlockAt(OriginX + (mazeblockwidth - 1) * 3 - i, OriginY  + j, OriginZ + (mazeblockwidth - 1) * 3 - k).setType(Material.AIR);
                }
            }
        }
        chestmeta.add(setChest_Concurrent(OriginX + (mazeblockwidth - 1) * 3 - 1, OriginY + 1, OriginZ + (mazeblockwidth - 1) * 3 - 1));
        chestmeta.add(setChest_Concurrent(OriginX + (mazeblockwidth - 1) * 3 - 2, OriginY + 1, OriginZ + (mazeblockwidth - 1) * 3 - 2));
        chestmeta.add(setChest_Concurrent(OriginX + (mazeblockwidth - 1) * 3 - 3, OriginY + 1, OriginZ + (mazeblockwidth - 1) * 3 - 3));
        chestmeta.add(setChest_Concurrent(OriginX + (mazeblockwidth - 1) * 3 - 1, OriginY + 1, OriginZ + (mazeblockwidth - 1) * 3 - 3));
        chestmeta.add(setChest_Concurrent(OriginX + (mazeblockwidth - 1) * 3 - 3, OriginY + 1, OriginZ + (mazeblockwidth - 1) * 3 - 1));
        chestmeta.add(setChest_Concurrent(OriginX + (mazeblockwidth - 1) * 3 - 1, OriginY + 3, OriginZ + (mazeblockwidth - 1) * 3 - 1));
        chestmeta.add(setChest_Concurrent(OriginX + (mazeblockwidth - 1) * 3 - 2, OriginY + 3, OriginZ + (mazeblockwidth - 1) * 3 - 2));
        chestmeta.add(setChest_Concurrent(OriginX + (mazeblockwidth - 1) * 3 - 3, OriginY + 3, OriginZ + (mazeblockwidth - 1) * 3 - 3));
        chestmeta.add(setChest_Concurrent(OriginX + (mazeblockwidth - 1) * 3 - 3, OriginY + 3, OriginZ + (mazeblockwidth - 1) * 3 - 1));
        chestmeta.add(setChest_Concurrent(OriginX + (mazeblockwidth - 1) * 3 - 1, OriginY + 3, OriginZ + (mazeblockwidth - 1) * 3 - 3));
        chance_takaramono_rare = lastchance;
        w.commit();

    }
    public MazeNode getNodePlayerStandOn(int X, int Z)
    {
        try {
            return maze[(X - OriginX) / 3][(Z - OriginZ) / 3];
        }catch (ArrayIndexOutOfBoundsException e)
        {
            return null;
        }
    }
    public boolean checkNodeIsEdge(MazeNode node) {
        int nodeX = node.X;
        int nodeY = node.Y;
        if (nodeX == 0 || nodeY == 0 || nodeX == 2 * width || nodeY == 2 * width)
        {
            return true;
        }
        else
        {
            return false;
        }
    }
    public boolean checkIfInMaze(Location loc)
    {
        if(maze == null)
        {
            return false;
        }
        int xboundend = OriginX + 3 * (maze.length);
        int zboundend = OriginZ + 3 * (maze.length);
        //System.out.println(xboundend + " " + zboundend);
        // System.out.println(xboundend + " " + zboundend + " " + loc.getBlockX() + " " + loc.getBlockZ());
        if(loc.getX() > OriginX && loc.getX() < xboundend && loc.getZ() > OriginZ && loc.getZ() < zboundend)
        {
            return true;
        }
        else {
            return false;
        }
    }
    public void GenMobs_Concurrent(MazeNode[] nodes,MazePlugin _plugin)
    {
        World bukkitw = _plugin.getServer().getWorld(world);
        AsyncWorld w = AsyncWorld.wrap(bukkitw);
        random = new Random();
        int idx = 0;
        int idz = 0;
        for (MazeNode node : nodes) {
            node.visited = false;
            idx = OriginX + node.X * 3 + 1;
            idz = OriginZ + node.Y * 3 + 1;
            float roll = random.nextFloat();
            // System.out.println(idx + " " + idz);
            if(roll > chance_takaramono && roll < chance_monster) {
                int spawnY = OriginY + 1;
                if(node.isWall)
                {
                    spawnY = OriginY + 6;
                }
                Location loc = new Location(w,idx,spawnY,idz);
                int decide = 1 +random.nextInt(9);
                int count =random.nextInt(2);
                for (int i = 0; i < count; i++)
                {
                    LivingEntity e;
                    switch (decide)
                    {

                        case chance_monster_zombiepigman:
                        {
                            Zombie z = (Zombie) w.spawnEntity(loc, EntityType.ZOMBIE);
                            e = z;
                            ItemStack sword = new ItemStack(Material.GOLD_SWORD);
                            if(random.nextFloat() < 0.3)
                            {
                                sword.setType(Material.DIAMOND_SWORD);
                                sword.addEnchantment(Enchantment.DAMAGE_ALL,4);
                                sword.addEnchantment(Enchantment.FIRE_ASPECT,2);
                                sword.addEnchantment(Enchantment.DURABILITY,3);
                            }
                            z.getEquipment().setItemInHand(sword);

                            if(random.nextFloat() < 0.5)
                            {

                                z.setBaby(true);
                            }
                            else
                            {
                                z.setBaby(false);
                            }
                            if(random.nextFloat() < 0.02) {
                                ItemStack helmet = new ItemStack(Material.DIAMOND_HELMET);
                                ItemStack chestplate = new ItemStack(Material.DIAMOND_CHESTPLATE);
                                ItemStack legging = new ItemStack(Material.DIAMOND_LEGGINGS);
                                ItemStack boots = new ItemStack(Material.DIAMOND_BOOTS);
                                helmet.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL,4);
                                helmet.addEnchantment(Enchantment.DURABILITY,3);
                                chestplate.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL,4);
                                chestplate.addEnchantment(Enchantment.DURABILITY,3);
                                legging.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL,4);
                                legging.addEnchantment(Enchantment.DURABILITY,3);
                                boots.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL,4);
                                boots.addEnchantment(Enchantment.DURABILITY,3);
                                z.getEquipment().setChestplate(chestplate);
                                z.getEquipment().setBoots(boots);
                                z.getEquipment().setHelmet(helmet);
                                z.getEquipment().setLeggings(legging);
                            }

                        }break;
                        case chance_monster_blaze:
                        {
                            e = (LivingEntity) w.spawnEntity(loc,EntityType.BLAZE);
                        }break;
                        case chance_monster_wither_skeleton:
                        {
                            Skeleton s = (Skeleton) w.spawnEntity(loc, EntityType.SKELETON);
                            e = s;
                            s.setSkeletonType(Skeleton.SkeletonType.WITHER);
                            s.getEquipment().setItemInHand(new ItemStack(Material.IRON_SWORD));
                        }break;
                        case chance_monster_killerbunny:
                        {
                            Rabbit r = (Rabbit) w.spawnEntity(loc,EntityType.RABBIT);
                            r.setRabbitType(Rabbit.Type.THE_KILLER_BUNNY);
                            r.setMaxHealth(20.0);
                            r.setHealth(20.0);
                            e = r;
                            if(random.nextFloat() < 0.5)
                            {
                                r.setBaby();
                            }
                        }break;
                        case chance_monster_powercreeper:
                        {
                            e = (LivingEntity) w.spawnEntity(loc,EntityType.CREEPER);
                            ((Creeper)e).setPowered(true);

                        }break;
                        case chance_monster_slime:
                        {
                            e = (LivingEntity) w.spawnEntity(loc,EntityType.SLIME);
                        }break;
                        case chance_monster_skeleton:
                        {
                            e = (LivingEntity) w.spawnEntity(loc,EntityType.SKELETON);
                        }break;
                        case chance_monster_witch:
                        {
                            e = (LivingEntity) w.spawnEntity(loc,EntityType.WITCH);
                        }break;
                        case chance_monster_ghast:
                        {
                            loc.setY(OriginY + 12);
                            e = (LivingEntity) w.spawnEntity(loc,EntityType.GHAST);
                        }break;
                        default:e = null;
                    }
                    e.setCustomName("Maze Monster");
                    e.setRemoveWhenFarAway(false);
                }

            }
            else if (!node.isWall) {
                //    System.out.println("Generation Block : "  + node.toString());

                if(roll < chance_takaramono)
                {
                    //System.out.printf("Chest X %d Y %d Z %d", idx, 193 ,idz);
                    chestmeta.add(setChest_Concurrent(idx, OriginY + 1, idz));

                }
            }

        }
    }
    public BlockMeta setChest_Concurrent(int x,int y,int z)
    {
        ArrayList<Map<String,Object>> Inventory = new ArrayList<Map<String,Object>>();
        Random random = new Random();
        int maxsize = 27;
        for(int i = 0; i < maxsize;i++)
        {
            Inventory.add(i,null);
        }
        for(int j = 0; j < try_count_takaramono; j++) {
            for (int i = 0; i < gen_count_takaramono; i++) {
                int invnumber = random.nextInt(maxsize);

                ItemStack item;
                if(Inventory.get(i) != null)
                {
                    item = ItemStack.deserialize(Inventory.get(i));
                }
                else
                {
                    item = null;
                }
                Material m;
                if (item == null) {
                    if (random.nextFloat() < chance_takaramono_rare)
                    {
                        m = BonusItems_rare.get(random.nextInt(BonusItems_rare.size()));
                    }
                    else
                    {
                        m = BonusItems.get(random.nextInt(BonusItems.size()));
                    }
                    if(m.toString().matches("(SWORD|CHESTPLATE|HELMET|BOOTS|LEGGINGS|PICKAXE|BOW|BOOK)"))
                    {
                        item = new ItemStack(m);
                        for (int k = 0; k < random.nextInt(4); k++) {
                            try {
                                item.addEnchantment(Enchantments.get(random.nextInt(Enchantments.size())), random.nextInt(5));
                            } catch (IllegalArgumentException e)
                            {

                            }
                        }
                    }
                    else if(m == Material.MONSTER_EGG)
                    {

                        item = new SpawnEgg(Monsters.get(random.nextInt(Monsters.size()))).toItemStack();
                    }
                    else
                    {
                        item = new ItemStack(m);
                    }
                    int amount = 1 + random.nextInt(3);
                    if(amount > item.getMaxStackSize())
                    {
                        amount = 1;
                    }
                    item.setAmount(amount);
                }
                Inventory.add(invnumber,item.serialize());
            }
        }
        return new BlockMeta(Material.CHEST,x,y,z,Inventory);
    }
    public void clearentities(MazePlugin _plugin)
    {
        _plugin.getServer().getScheduler().callSyncMethod(_plugin, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                World w = _plugin.getServer().getWorld(world);
                for(Player p : _plugin.getServer().getOnlinePlayers())
                {
                    p.sendMessage("迷宫计算中");
                }
                for(Entity e : w.getEntities()) {
                    if (checkIfInMaze(e.getLocation())) {
                        if (e instanceof Monster || e instanceof Rabbit || e instanceof Ghast) {
                            //System.out.println("Clearing " + e.toString() + " " + e.getLocation().getBlockX() + " " + e.getLocation().getBlockY() + " " + e.getLocation().getBlockZ());
                            e.remove();
                        } else if (e instanceof Player) {
                            Player p = (Player) e;
                            p.sendMessage("本轮迷宫探险已经结束");
                            Location loc = p.getLocation();
                            if (loc.getX() >= OriginX + (maze.length - 1) * 3 && loc.getX() <= OriginX + maze.length * 6 && loc.getZ() >= OriginZ + (maze.length - 1) * 3 && loc.getZ() < OriginZ + maze.length * 6 && loc.getY() > OriginY) {
                                //System.out.println("Finish");
                            } else {
                                Inventory inv = p.getInventory();
                                ItemStack[] stacks = inv.getContents();
                                System.out.println(stacks.length);
                                for (int i = 0; i < stacks.length; i++) {
                                    if (stacks[i] == null) {
                                        //         System.out.println("MULL Stack");
                                    } else if (random.nextInt(10) < 7) {
                                        //        System.out.println("SET TO NULL");
                                        inv.setItem(i, null);
                                    }
                                }
                            }
                            Location spawn = null;
                            if (p.getBedSpawnLocation() != null) {
                                spawn = p.getBedSpawnLocation();
                            } else {
                                spawn = w.getSpawnLocation();
                            }
                            ArrayList<Map<String, Object>> list = _plugin.MazePlayerItemStack.get(p.getUniqueId().toString());
                            if (list != null) {
                                for (Map<String, Object> item : list) {
                                    try {
                                        w.dropItem(spawn, ItemStack.deserialize(item));
                                    } catch (IllegalArgumentException ex) {
                                        //  ex.printStackTrace();
                                    }
                                }
                            }

                            _plugin.MazePlayerItemStack.remove(p.getUniqueId().toString());
                            p.teleport(spawn);
                        }
                    }
                }
                //end of clear
                return null;
            }
        });


    }
    public void CheckPlayerPosition(MazePlugin _plugin)
    {

        for (Player p : _plugin.getServer().getOnlinePlayers())
        {
            Location loc = p.getLocation();
            //System.out.println(checkIfInMaze(loc));
            if(!checkIfInMaze(loc))
            {
                continue;
            }
            MazeNode node = getNodePlayerStandOn(loc.getBlockX(),loc.getBlockZ());
            //System.out.println(maze.length);
            //System.out.println(node.isWall);
            //  System.out.println("Node : " + node.X + " " + node.Y + " " + node.isWall);
            if(node.isWall && loc.getBlockY() >= OriginY + 5)
            {
                loc.setY(loc.getY() - 1);
                if(_plugin.getServer().getWorld(world).getBlockAt(loc).getType() != Material.AIR)
                {
                    //Player on the wall
                    if (!p.isOp())
                    {
                        //check whether Node at x-1 y-1 is not wall
                        int X = node.X;
                        int Y = node.Y;
                        //  System.out.printf("X:%d, Y:%d,isWall " + node.isWall,X,Y)
                        // ;
                        while(maze[X][Y].isWall) {
                            if(X == 1 && Y > 1) {
                                Y--;
                            }
                            else if(Y == 1 && X > 1)
                            {
                                X--;
                            }
                            else
                            {
                                X--;
                                Y--;
                            }

                        }
                        loc.setX(OriginX + X * 3);
                        loc.setZ(OriginZ + Y * 3);
                        loc.setY(OriginY + 2);
                        p.sendMessage("禁止爬墙");
                        p.teleport(loc);
                    }
                }
            }
        }
    }
    public void GenBlocks(MazePlugin _plugin)
    {
        AsyncWorld w = AsyncWorld.wrap(_plugin.getServer().getWorld(world));
        int posx = 0;
        int posy = 0;
        int posz = 0;
        int light = OriginY + 4;
        for (int idx = 0; idx < mazeblockwidth; idx++) {
            for (int idy = 0; idy < mazeblockwidth; idy++) {
                MazeNode node = maze[idx][idy];
                if (checkNodeIsEdge(node) && node.isWall) {
                    //Generate Basic Walls
                    for (int i = 0; i < mazeHeight_Node; i++) {
                        for (int j = 0; j < 3; j++) { //3 X Outer Layer
                            for (int k = 0; k < 3; k++) {   //3 X
                                for (int l = 0; l < 3; l++) { //3 X
                                    posx = OriginX + node.X * 3 + k;
                                    posy = OriginY + i * 3 + j;
                                    posz = OriginZ + node.Y * 3 + l;
                                    w.getBlockAt(posx,posy,posz).setType(Maze_Material_Wall);
                                }
                            }
                        }
                    }
                } else {
                    //Generate Lamp ,Outer Walls , Roof
                    for (int j = 0; j < 3; j++) {
                        for (int k = 0; k < 3; k++) {
                            posx = OriginX + node.X * 3 + j;
                            posz = OriginZ + node.Y * 3 + k;
                            w.getBlockAt(posx,OriginY,posz).setType(Maze_Material_Wall);
                            //assert we generate from 0,64,0 in practice
                            for (int i = 0; i < 3; i++) {
                                if (node.isWall) {
                                    w.getBlockAt(posx, OriginY + 1 + i, posz).setType(Maze_Material_Wall);
                                } else {
                                    w.getBlockAt(posx, OriginY + 1 + i, posz).setType(Material.AIR);
                                }
                            }

                            if (node.isWall) {
                                w.getBlockAt(posx, light, posz).setType(Maze_Material_Light);
                                w.getBlockAt(posx, OriginY + mazeHeight_Node * 3, posz).setType(Maze_Material_ROOF_B);
                            } else {
                                w.getBlockAt(posx, light, posz).setType(Material.AIR);
                                w.getBlockAt(posx, OriginY + mazeHeight_Node * 3, posz).setType(Maze_Material_ROOF_A);
                            }
                        }
                    }
                    //Clear
                    for (int j = 0; j < 3; j++) {
                        for (int k = 0; k < 3; k++) {
                            for (int i = 5; i < mazeHeight_Node * 3 - 2; i++) {
                                posx = OriginX + node.X * 3 + j;
                                posz = OriginZ + node.Y * 3 + k;
                                posy = OriginY + i;
                                w.getBlockAt(posx, posy, posz).setType(Material.AIR);
                            }
                        }
                    }
                    //end of clear
                }

            }
            //System.out.printf("Generated line #" + idz);
        }
        _plugin.getServer().getScheduler().callSyncMethod(_plugin, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                for (Player p : _plugin.getServer().getOnlinePlayers()) {
                    p.sendMessage("方块全部计算完成，正在提交.....");
                }
                return null;
            }
        });
        w.commit();
        ///End of block async update
    }

    public JSONObject JSONSerialize()
    {
        JSONObject o = new JSONObject();
        o.put("width",width);
        JSONArray jarr = new JSONArray();
        for(int i = 0; i < width * 2 + 1; i++)
        {
            JSONArray _jarr = new JSONArray();
            for(int j = 0; j < width * 2 + 1; j++)
            {
                JSONObject _o = new JSONObject();
                _o.put("X",maze[i][j].X);
                _o.put("Y",maze[i][j].Y);
                _o.put("isWall",maze[i][j].isWall);
                _jarr.put(_o);
            }
            jarr.put(_jarr);
        }
        o.put("dimension",2);
        o.put("Nodes",jarr);
        JSONArray chests = new JSONArray();
        for(BlockMeta meta: chestmeta)
        {
            JSONObject chest = new JSONObject();
            chest.put("X",meta.x);
            chest.put("Y",meta.y);
            chest.put("Z",meta.z);
            chests.put(chest);
        }
        o.put("ChestMeta",chests);
        return o;
    }
    public boolean HandleFinish(Location loc)
    {
        return loc.getX() >= OriginX +  (maze.length - 1) * 3  && loc.getX() <=OriginX + (maze.length) * 3 && loc.getZ() >= OriginZ + (maze.length - 1) * 3 && loc.getZ() < (maze.length) * 3 && loc.getY() > OriginY;
    }
    public ArrayList<MazeNode> getAdjunvisitedNode(int x, int y) {
        ArrayList<MazeNode> adjnodes = new ArrayList<MazeNode>();
        if (maze[x][y].isWall) {
            throw new IllegalArgumentException("Cannot set in the wall");
        }
        if (x + 2 > 0 && x + 2 < 2 * width + 1 && !maze[x + 2][y].visited) {
            adjnodes.add(maze[x + 2][y]);
        }
        if (y + 2 > 0 && y + 2 < 2 * width + 1 && !maze[x][y + 2].visited) {
            adjnodes.add(maze[x][y + 2]);
        }
        if (x - 2 > 0 && x - 2 < 2 * width + 1 && !maze[x - 2][y].visited) {
            adjnodes.add(maze[x - 2][y]);
        }
        if (y - 2 > 0 && y - 2 < 2 * width + 1 && !maze[x][y - 2].visited) {
            adjnodes.add(maze[x][y - 2]);
        }
        return adjnodes;
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
    public void removeWall(MazeNode A, MazeNode B) {
        int wallX = (A.X + B.X) / 2;
        int wallY = (A.Y + B.Y) / 2;
        maze[wallX][wallY].isWall = false;
    }
    public void setNode(int i, int j, boolean isWall) {
        maze[i][j] = new MazeNode(isWall, i, j);
    }

    @Override
    public int checkdir(int x, int y, int z) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ArrayList<MazeNode> getAdjunvisitedNode(int x, int y, int z) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void generate(int startx, int starty, int startz) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MazeNode getNode(int x, int y, int z) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MazeNode getNode(int x, int y) {
        return maze[x][y];
    }
    @Override
    public void setNode(int x, int y, int z, boolean isWall) {
        throw new UnsupportedOperationException();
    }
    public static IMaze JSONDeSerialize(File f)
    {
        BufferedReader reader = null;
        IMaze m = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
            String s = "";
            String read = null;
            do {
                read = reader.readLine();
                if (read != null) {
                    s += read;

                }
            } while (read != null);
            JSONObject jo = new JSONObject(s);
            int width = jo.getInt("width");
            try {
                int dimension = jo.getInt("dimension");
                if(dimension != 2)
                {
                    return null;
                }
                String name = jo.getString("type");
                if(name.contains("BackTrack_Recursive"))
                {
                    m = new DFS_Recursive_Backtrack_Maze.Factory().GenBlankMaze(width);
                }
                else if(name.contains("Aldous_Broder"))
                {
                    m = new Aldous_Broder_Maze(width);
                }
                else if(name.contains("Random_Kruskal"))
                {
                    m = new Random_Kruskal_Maze.Factory().GenBlankMaze(width);
                }
                else
                {
                    m = new DFS_Recursive_Backtrack_Maze.Factory().GenBlankMaze(width);
                }


            }
            catch (JSONException e)
            {
                m = new DFS_Recursive_Backtrack_Maze.Factory().GenBlankMaze(width);
            }
            ArrayList<BlockMeta> metas = new ArrayList<BlockMeta>();
            try{
                JSONArray arr = jo.getJSONArray("ChestMeta");
                for(int i = 0; i < arr.length(); i++)
                {
                    JSONObject o = (JSONObject) arr.get(i);
                    metas.add(new BlockMeta(Material.CHEST,o.getInt("X"),o.getInt("Y"),o.getInt("Z"),null));
                }
            }
            catch (JSONException e)
            {

            }
            ((Abstract2DMaze)m).chestmeta = metas;
            for (int i = 0; i < width * 2 + 1; i++)
            {
                JSONArray ja = jo.getJSONArray("Nodes").getJSONArray(i);
                for(int j = 0; j < width * 2 + 1; j++)
                {
                    JSONObject o = (JSONObject) ja.get(i);
                    m.setNode(o.getInt("X"),o.getInt("Y"),o.getBoolean("isWall"));
                }
            }
            return m;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if(reader != null)
            {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return m;
    }
}