package org.dogeop.MazePlugin;

import com.boydti.fawe.bukkit.wrapper.AsyncWorld;
import com.boydti.fawe.util.TaskManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.block.BlockMultiPlaceEvent;
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
 * Created by lyt on 16-8-6.
 */
public abstract class Abstract3DMaze implements IMaze {
    MazeNode [][][] maze;
    int width;
    Random random;
    public ArrayList<MazeNode> unvisited = new ArrayList<MazeNode>();
    public Material Maze_Material_Wall;
    public Material Maze_Material_Light;
    private String world;
    private double chance_takaramono_rare;
    private double chance_takaramono;
    private double chance_monster;
    public ArrayList<BlockMeta> chestmeta;
    private ArrayList<Material> BonusItems;
    private ArrayList<Material> BonusItems_rare;
    private ArrayList<Enchantment> Enchantments;
    private ArrayList<EntityType> Monsters;
    protected int[] vertices = new int[]{0,0,0,1,1,1};
    private int try_count_takaramono;
    private int gen_count_takaramono;
    private int lastxmax;
    private int OriginX;
    private int OriginY;
    private int OriginZ;
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
    public void wrappedcommit(MazePlugin _plugin, AsyncWorld w)
    {
        try {
            w.commit();
        }
        catch (IllegalStateException e)
        {
            _plugin.getServer().getScheduler().callSyncMethod(_plugin,new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    for(Player p : _plugin.getServer().getOnlinePlayers())
                    {
                        p.sendMessage("生成出现了内部错误，Message: " + e.getMessage() + " 管理员请重新运行/GenMaze");
                    }
                    return null;
                }
            });
        }
    }
    public synchronized void GenBukkitWorld(JavaPlugin plugin, Map<String, Object> settings, boolean Update) {
        Maze_Material_Wall = (Material) settings.get("Maze_Material_Wall");
        Maze_Material_Light = (Material) settings.get("Maze_Material_Light");
        world = (String) settings.get("world");
        chance_takaramono_rare = (double) settings.get("Chance_Takaramono_rare");
        chance_takaramono = (double) settings.get("Chance_Chest");
        chance_monster = (double) settings.get("Chance_Monster");
        BonusItems = (ArrayList<Material>) settings.get("BonusItems");
        BonusItems.add(Material.SAND);
        BonusItems.add(Material.COBBLESTONE);
        BonusItems.add(Material.WOOD);
        BonusItems_rare = (ArrayList<Material>) settings.get("BonusItems_rare");
        Enchantments = (ArrayList<Enchantment>) settings.get("Enchantments");
        Monsters = (ArrayList<EntityType>) settings.get("Monsters");
        try_count_takaramono = (int) settings.get("try_count_takaramono");
        gen_count_takaramono = (int) settings.get("gen_count_takaramono");
        lastxmax = (int) settings.get("lastxmax") * 2 + 1;
        OriginX = (int) settings.get("OriginX");
        OriginY = (int) settings.get("OriginY");
        OriginZ = (int) settings.get("OriginZ");
        if(!Update)
        {
            return;
        }
        MazePlugin _plugin = (MazePlugin) plugin;
        String MazeType = "";
        World bukkitw = _plugin.getServer().getWorld(world);
        AsyncWorld w = AsyncWorld.wrap(bukkitw);
        long begintime = System.currentTimeMillis();
        _plugin.busy = true;
        chestmeta = new ArrayList<BlockMeta>();
        ///Begin of block async update

        for (Player p : _plugin.getServer().getOnlinePlayers()) {
            p.sendMessage("开始异步计算方块");
        }
        IMaze tmpMaze = Abstract2DMaze.JSONDeSerialize(new File(_plugin.getDataFolder() + File.separator + "Maze.json"));
        int[] verts = null;
        if(tmpMaze == null)
        {
            //On Fail:
            tmpMaze = Abstract3DMaze.JSONDeSerialize(new File(_plugin.getDataFolder() + File.separator + "Maze.json"));
            if(tmpMaze != null)
            {
                verts = ((Abstract3DMaze)tmpMaze).vertices;
            }
        }
        else
        {
            verts = ((Abstract2DMaze)tmpMaze).vertices;
        }
        //what? not 3d or 2d?
        //skip clear process
        //assign vertices
        vertices[0] = OriginX;
        vertices[1] = OriginY;
        vertices[2] = OriginZ;
        vertices[3] = maze.length * 3;
        vertices[4] = maze.length * 3;
        vertices[5] = maze.length * 3;
        clearPlayer(_plugin, tmpMaze);
        clearentities(_plugin);
        for (String UUID : _plugin.OffLineMazePlayers.keySet()) {
            _plugin.OffLineMazePlayers.put(UUID, true);
        }

        for (String UUID : _plugin.OffLineMazePlayers.keySet()) {
            _plugin.OffLineMazePlayers.put(UUID, true);
        }

        if (_plugin.RepeatingTaskId != -1) {
            _plugin.getServer().getScheduler().cancelTask(_plugin.RepeatingTaskId);
            _plugin.RepeatingTaskId = -1;
        }
        if (_plugin.RepeatingAntiHangId == -1) {
            _plugin.RepeatingAntiHangId = _plugin.getServer().getScheduler().scheduleSyncRepeatingTask(_plugin, _plugin.Antihang, 0, 400);
        }
        if (_plugin.RepeatingSerializeTaskId == -1) {
            _plugin.RepeatingSerializeTaskId = _plugin.getServer().getScheduler().scheduleAsyncRepeatingTask(_plugin, _plugin.mSerializePlayerInfo, 0, 300);
        }



        if(tmpMaze != null) {
            int xorig = verts[0];
            int yorig = verts[1];
            int zorig = verts[2];
            int xlength = verts[3];
            int ylength = verts[4];
            int zlength = verts[5];
            if(xorig != vertices[0] || yorig != vertices[1] || zorig != vertices[2] || xlength != vertices[3] || ylength != vertices[4] || zlength != vertices[5])
            {
                for (int i = 0; i < xlength; i++) {
                    for (int j = 0; j < ylength; j++) {
                        for (int k = 0; k < zlength; k++) {
                            w.getBlockAt(xorig + i, yorig + j, zorig + k).setType(Material.AIR);
                        }
                    }
                }
            }
            wrappedcommit(_plugin,w);
            //end of clear
        }

        TaskManager.IMP.async(new Runnable() {
            @Override
            public void run() {
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
                for (MazeNode[][] nodess : maze) {
                    for(MazeNode [] nodes : nodess)
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
                wrappedcommit(_plugin,w);
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
                    meta = chestmeta.get(random.nextInt(chestmeta.size()));
                    emptyslotidx.clear();
                    emptyslotidx.trimToSize();
                    b = w.getBlockAt(meta.x,meta.y,meta.z);
                    BlockState state = b.getState();
                    if(!(state instanceof Chest))
                    {
                        continue;
                    }
                    Chest c = (Chest)state;
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
                    };
                }while (b.getType() != Material.CHEST || emptyslotidx.size() <= 0);
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
        for (int idx = 0; idx < maze.length; idx++) {
            for (int idy = 0; idy < maze.length; idy++) {
                for (int idz = 0; idz < maze.length; idz++)
                {
                    MazeNode node = maze[idx][idy][idz];
                    //Generate Basic Walls
                    for (int i = 0; i < 3; i++) { //3 X Outer Layer
                        for (int j = 0; j < 3; j++) {   //3 X
                            for (int k = 0; k < 3; k++) { //3 X
                                posx = OriginX + node.X * 3 + i;
                                posy = OriginY + node.Y * 3 + j;
                                posz = OriginZ + node.Z * 3 + k;
                                if(node.isWall) {
                                    boolean cond1 = (j == 0 || j == 2) && i == 1 && k == 1;
                                    boolean cond2 = (k == 0 || k == 2) && i == 1 && j == 1;
                                    boolean cond3 = (i == 0 || i == 2) && j == 1 && k == 1;
                                    if (cond1 || cond2 || cond3) {
                                        w.getBlockAt(posx, posy, posz).setType(Material.JACK_O_LANTERN);
                                    } else {
                                        w.getBlockAt(posx, posy, posz).setType(Maze_Material_Wall);
                                    }
                                }
                                else {
                                    w.getBlockAt(posx, posy, posz).setType(Material.AIR);
                                }
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
        wrappedcommit(_plugin,w);
        ///End of block async update
    }
    public void genFinalBonus(MazePlugin _plugin)
    {
        //
        AsyncWorld w = AsyncWorld.wrap(_plugin.getServer().getWorld(world));
        for (int i = 1; i < 4; i++) {
            for (int j = 1; j < 4; j++) {
                for (int k = 1; k < 4; k++) {
                    w.getBlockAt(OriginX + (maze.length - 1) * 3 - i, OriginY + (maze.length - 1) * 3 + j, OriginZ + (maze.length - 1) * 3 - k).setType(Material.AIR);
                }
            }
        }
        wrappedcommit(_plugin,w);
        double lastchance = chance_takaramono_rare;
        chance_takaramono_rare *= 4;
        for(int i = 1;i < 4;i++)
        {
            for(int j = 1; j < 4; j++)
            {
                for(int k = 1; k < 4;k++)
                {
                    w.getBlockAt(OriginX + (maze.length - 1) * 3 - i, OriginY + (maze.length - 1) * 3 + j, OriginZ + (maze.length - 1) * 3 - k).setType(Material.AIR);
                }
            }
        }
        chestmeta.add(setChest_Concurrent(OriginX + (maze.length - 1) * 3 - 1, OriginY + (maze.length - 1) * 3  - 1, OriginZ + (maze.length - 1) * 3 - 1));
        chestmeta.add(setChest_Concurrent(OriginX + (maze.length - 1) * 3 - 2, OriginY + (maze.length - 1) * 3  - 1, OriginZ + (maze.length - 1) * 3 - 2));
        chestmeta.add(setChest_Concurrent(OriginX + (maze.length - 1) * 3 - 3, OriginY + (maze.length - 1) * 3  - 1, OriginZ + (maze.length - 1) * 3 - 3));
        chestmeta.add(setChest_Concurrent(OriginX + (maze.length - 1) * 3 - 1, OriginY + (maze.length - 1) * 3  - 1, OriginZ + (maze.length - 1) * 3 - 3));
        chestmeta.add(setChest_Concurrent(OriginX + (maze.length - 1) * 3 - 3, OriginY + (maze.length - 1) * 3  - 1, OriginZ + (maze.length - 1) * 3 - 1));
        chestmeta.add(setChest_Concurrent(OriginX + (maze.length - 1) * 3 - 1, OriginY + (maze.length - 1) * 3  - 3, OriginZ + (maze.length - 1) * 3 - 1));
        chestmeta.add(setChest_Concurrent(OriginX + (maze.length - 1) * 3 - 2, OriginY + (maze.length - 1) * 3  - 3, OriginZ + (maze.length - 1) * 3 - 2));
        chestmeta.add(setChest_Concurrent(OriginX + (maze.length - 1) * 3 - 3, OriginY + (maze.length - 1) * 3  - 3, OriginZ + (maze.length - 1) * 3 - 3));
        chestmeta.add(setChest_Concurrent(OriginX + (maze.length - 1) * 3 - 3, OriginY + (maze.length - 1) * 3  - 3, OriginZ + (maze.length - 1) * 3 - 1));
        chestmeta.add(setChest_Concurrent(OriginX + (maze.length - 1) * 3 - 1, OriginY + (maze.length - 1) * 3  - 3, OriginZ + (maze.length - 1) * 3 - 3));
        chance_takaramono_rare = lastchance;
        wrappedcommit(_plugin,w);

    }
    public void GenMobs_Concurrent(MazeNode[] nodes,MazePlugin _plugin)
    {
        World bukkitw = _plugin.getServer().getWorld(world);
        AsyncWorld w = AsyncWorld.wrap(bukkitw);
        random = new Random();
        int idx = 0;
        int idz = 0;
        int idy = 0;
        for (MazeNode node : nodes) {
            node.visited = false;
            idx = OriginX + node.X * 3 + 1;
            idy = OriginY + node.Y * 3 + 1;
            idz = OriginZ + node.Z * 3 + 1;
            float roll = random.nextFloat();
            // System.out.println(idx + " " + idz)
            if(!node.isWall) {
                if (roll > chance_takaramono && roll < chance_monster) {
                    System.out.println(idx + " " + idy + " " + idz + " ");
                    int spawnY = idy;
                    Location loc = new Location(w, idx, spawnY, idz);
                    int decide = 1 + random.nextInt(8);
                    int count = random.nextInt(2);
                    for (int i = 0; i < count; i++) {
                        LivingEntity e;
                        switch (decide) {

                            case chance_monster_zombiepigman: {
                                Zombie z = (Zombie) w.spawnEntity(loc, EntityType.ZOMBIE);
                                e = z;
                                ItemStack sword = new ItemStack(Material.GOLD_SWORD);
                                if (random.nextFloat() < 0.3) {
                                    sword.setType(Material.DIAMOND_SWORD);
                                    sword.addEnchantment(Enchantment.DAMAGE_ALL, 4);
                                    sword.addEnchantment(Enchantment.FIRE_ASPECT, 2);
                                    sword.addEnchantment(Enchantment.DURABILITY, 3);
                                }
                                z.getEquipment().setItemInHand(sword);

                                if (random.nextFloat() < 0.5) {

                                    z.setBaby(true);
                                } else {
                                    z.setBaby(false);
                                }
                                if (random.nextFloat() < 0.02) {
                                    ItemStack helmet = new ItemStack(Material.DIAMOND_HELMET);
                                    ItemStack chestplate = new ItemStack(Material.DIAMOND_CHESTPLATE);
                                    ItemStack legging = new ItemStack(Material.DIAMOND_LEGGINGS);
                                    ItemStack boots = new ItemStack(Material.DIAMOND_BOOTS);
                                    helmet.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
                                    helmet.addEnchantment(Enchantment.DURABILITY, 3);
                                    chestplate.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
                                    chestplate.addEnchantment(Enchantment.DURABILITY, 3);
                                    legging.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
                                    legging.addEnchantment(Enchantment.DURABILITY, 3);
                                    boots.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
                                    boots.addEnchantment(Enchantment.DURABILITY, 3);
                                    z.getEquipment().setChestplate(chestplate);
                                    z.getEquipment().setBoots(boots);
                                    z.getEquipment().setHelmet(helmet);
                                    z.getEquipment().setLeggings(legging);
                                }

                            }
                            break;
                            case chance_monster_blaze: {
                                e = (LivingEntity) w.spawnEntity(loc, EntityType.BLAZE);
                            }
                            break;
                            case chance_monster_wither_skeleton: {
                                Skeleton s = (Skeleton) w.spawnEntity(loc, EntityType.SKELETON);
                                e = s;
                                s.setSkeletonType(Skeleton.SkeletonType.WITHER);
                                s.getEquipment().setItemInHand(new ItemStack(Material.IRON_SWORD));
                            }
                            break;
                            case chance_monster_killerbunny: {
                                Rabbit r = (Rabbit) w.spawnEntity(loc, EntityType.RABBIT);
                                r.setRabbitType(Rabbit.Type.THE_KILLER_BUNNY);
                                r.setMaxHealth(20.0);
                                r.setHealth(20.0);
                                e = r;
                                if (random.nextFloat() < 0.5) {
                                    r.setBaby();
                                }
                            }
                            break;
                            case chance_monster_powercreeper: {
                                e = (LivingEntity) w.spawnEntity(loc, EntityType.CREEPER);
                                ((Creeper) e).setPowered(true);

                            }
                            break;
                            case chance_monster_slime: {
                                e = (LivingEntity) w.spawnEntity(loc, EntityType.SLIME);
                            }
                            break;
                            case chance_monster_skeleton: {
                                e = (LivingEntity) w.spawnEntity(loc, EntityType.SKELETON);
                            }
                            break;
                            case chance_monster_witch: {
                                e = (LivingEntity) w.spawnEntity(loc, EntityType.WITCH);
                            }
                            break;
                            default:
                                e = null;
                        }
                        e.setCustomName("Maze Monster");
                        e.setRemoveWhenFarAway(false);
                    }

                } else if (roll < chance_takaramono) {
                    //System.out.printf("Chest X %d Y %d Z %d", idx, 193 ,idz);
                    chestmeta.add(setChest_Concurrent(idx, idy, idz));

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
                    else if(m == Material.SAND || m == Material.COBBLESTONE)
                    {
                        item = new ItemStack(m,64);
                        Inventory.add(invnumber,item.serialize());
                        continue;
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
    public void clearPlayer(MazePlugin _plugin, IMaze mazebefore)
    {
        int origx;
        int origy;
        int origz;
        int xlength;
        int ylength;
        int zlength;
        if(mazebefore instanceof Abstract2DMaze) {
            origx = ((Abstract2DMaze)mazebefore).vertices[0];
            origy = ((Abstract2DMaze)mazebefore).vertices[1];
            origz = ((Abstract2DMaze)mazebefore).vertices[2];
            xlength = ((Abstract2DMaze)mazebefore).vertices[3];
            ylength = ((Abstract2DMaze)mazebefore).vertices[4];
            zlength = ((Abstract2DMaze)mazebefore).vertices[5];
        }
        else
        {
            origx = ((Abstract3DMaze)mazebefore).vertices[0];
            origy = ((Abstract3DMaze)mazebefore).vertices[1];
            origz = ((Abstract3DMaze)mazebefore).vertices[2];
            xlength = ((Abstract3DMaze)mazebefore).vertices[3];
            ylength = ((Abstract3DMaze)mazebefore).vertices[4];
            zlength = ((Abstract3DMaze)mazebefore).vertices[5];
        }

        World w = _plugin.getServer().getWorld(world);
        for(LivingEntity e : w.getLivingEntities()) {
            Location loc = e.getLocation();
            int x = loc.getBlockX();
            int y = loc.getBlockY();
            int z = loc.getBlockZ();

            if (e instanceof Player) {
                Player p = (Player) e;
                p.sendMessage("本轮迷宫探险已经结束");
                if(x >= origx && x <= origx + xlength && z > origz && z < origz + zlength && loc.getY() > origy) {
                    if (x >= origx + xlength - 3 && x <= origx + xlength && z > origz + zlength - 3 && z < origz + zlength && loc.getY() > origy) {
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
    }
    public void clearentities(MazePlugin _plugin)
    {

        _plugin.getServer().getScheduler().callSyncMethod(_plugin, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                for(Player p : _plugin.getServer().getOnlinePlayers())
                {
                    p.sendMessage("迷宫计算中");
                }
                World bw = _plugin.getServer().getWorld(world);
                Location _void = new Location(bw,0,-64,0);
                for (LivingEntity e : bw.getLivingEntities()) {
                    if (e != null) {
                        if (e.getCustomName() != null) {
                            if (e.getCustomName().contains("Maze Monster")) {
                                //     System.out.println("Clearing " + e.toString() + " " + e.getLocation().getBlockX() + " " + e.getLocation().getBlockY() + " " + e.getLocation().getBlockZ());
                                if(e instanceof Slime)
                                {
                                    e.teleport(_void);
                                }
                                else {
                                    e.damage(100000);
                                }
                            }
                        }
                    }
                }
                return null;
            }
        });
    }
    @Override
    public void init() {
        random = new Random();
        for (int i = 0; i < maze.length ; i++) {
            for (int j = 0; j < maze.length ; j++) {
                for(int k = 0; k < maze.length ; k++)
                    if (i % 2 != 0 && j % 2 != 0 && k % 2 != 0) {
                    setNode(i, j, k,false);
                    unvisited.add(maze[i][j][k]);
                } else {
                    setNode(i, j, k, true);
                }

            }
        }
    }
    public void printAsString() {
        System.out.println("\n");
        String s = "";
        for (int k = 0; k < 2 * width + 1; k++)
        {
            for(int i = 0; i < 2 * width + 1; i++) {
                for (int j = 0; j < 2 * width + 1; j++) {
                    if (!maze[k][i][j].isWall) {
                        s += "# ";
                    } else {
                        s += "O ";
                    }
                }
                System.out.println(s);
                s = "";
            }
            System.out.println("\n");
        }
    }
    @Override
    public void removeWall(MazeNode A, MazeNode B) {
        int wallX = (A.X + B.X) / 2;
        int wallY = (A.Y + B.Y) / 2;
        int wallZ = (A.Z + B.Z) / 2;
        maze[wallX][wallY][wallZ].isWall = false;
    }

    @Override
    public int checkdir(int x, int y, int z) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ArrayList<MazeNode> getAdjunvisitedNode(int x, int y, int z) {
        return null;
    }

    @Override
    public ArrayList<MazeNode> getAdjunvisitedNode(int x, int y) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void generate(int startx, int starty) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setNode(int x, int y, boolean isWall) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setNode(int x, int y, int z, boolean isWall) {
        maze[x][y][z] = new MazeNode(isWall,x,y,z);
    }

    @Override
    public MazeNode getNode(int x, int y, int z) {
        return maze[x][y][z];
    }

    @Override
    public MazeNode getNode(int x, int y) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MazeNode getNodePlayerStandOn(int X, int Z) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean checkNodeIsEdge(MazeNode node) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean checkIfInMaze(Location loc) {
        int bound = maze.length * 3;
        int xbound = OriginX + bound;
        int ybound = OriginY + bound;
        int zbound = OriginZ + bound;
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        if(x > OriginX && x < xbound  && y > OriginY && y < ybound && z > OriginZ && z < OriginZ + bound)
        {
            return true;
        }
        return false;
    }

    @Override
    public void CheckPlayerPosition(MazePlugin _plugin) {
        return;
    }
    public boolean HandleFinish(Location loc)
    {
        System.out.println(loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ());
        return loc.getX() >= OriginX +  (maze.length - 2) * 3  && loc.getX() <=OriginX + (maze.length - 1) * 3 && loc.getZ() >= OriginZ + (maze.length - 2) * 3 && loc.getZ() <  OriginZ + (maze.length - 1) * 3 && loc.getY() >= OriginY +  (maze.length - 2) * 3  && loc.getY() <=OriginY + (maze.length - 1) * 3 ;
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
                JSONArray __jarr = new JSONArray();
                for(int k = 0; k < width * 2 + 1; k++) {
                    JSONObject _o = new JSONObject();
                    _o.put("X", maze[i][j][k].X);
                    _o.put("Y", maze[i][j][k].Y);
                    _o.put("Z", maze[i][j][k].Z);
                    _o.put("isWall", maze[i][j][k].isWall);
                    __jarr.put(_o);
                }
                _jarr.put(__jarr);
            }
            jarr.put(_jarr);
        }
        o.put("dimension",3);
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
        JSONArray _vertices = new JSONArray();
        for(int i = 0; i < vertices.length;i++)
        {
            _vertices.put(vertices[i]);
        }
        o.put("vertices", _vertices);
        o.put("ChestMeta",chests);
        return o;
    }
    public static IMaze JSONDeSerialize(File f) {
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
                String name = jo.getString("type");
                int dimension = jo.getInt("dimension");
                if(dimension != 3)
                {
                    return null;
                }

                if(name.contains("Aldous_Broder"))
                {
                    m = new Aldous_Broder_3DMaze.Factory().GenBlankMaze(width);
                }
                else if(name.contains("DFS_ReursiveBT_3D"))
                {
                    m = new DFS_Recursive_Backtrack_3DMaze.Factory().GenBlankMaze(width);
                }
                else
                {
                    m = new Aldous_Broder_3DMaze.Factory().GenBlankMaze(width);
                }
            }
            catch (JSONException e)
            {
                m = new Aldous_Broder_3DMaze.Factory().GenBlankMaze(width);
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
            try {
                int[] vertices = ((Abstract3DMaze)m).vertices;
                JSONArray verts = jo.getJSONArray("vertices");
                for(int i = 0; i < verts.length();i++)
                {
                    vertices[i] = verts.getInt(i);
                }
            }catch (JSONException e)
            {

            }
            ((Abstract3DMaze)m).chestmeta = metas;
            for (int i = 0; i < width * 2 + 1; i++)
            {
                JSONArray ja = jo.getJSONArray("Nodes").getJSONArray(i);
                for(int j = 0; j < width * 2 + 1; j++)
                {
                    JSONArray _ja = ja.getJSONArray(j);
                    for(int k = 0; k < width * 2 + 1; k++)
                    {
                        JSONObject o = (JSONObject) _ja.get(k);
                        m.setNode(o.getInt("X"),o.getInt("Y"),o.getInt("Z"),o.getBoolean("isWall"));
                    }
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
