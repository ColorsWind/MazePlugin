package org.dogeop.MazePlugin;


import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.ObjectConstructor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.SpawnEgg;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;

import java.io.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.RunnableFuture;
import java.util.logging.Logger;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

public final class MazePlugin extends JavaPlugin
{
    ArrayList<Material> BonusItems = new ArrayList<Material>();
    ArrayList<Material> BonusItems_rare = new ArrayList<Material>();
    ArrayList<Enchantment> Enchantments = new ArrayList<Enchantment>();
    ArrayList<EntityType> Monsters = new ArrayList<EntityType>();
    Map<String,Boolean> OffLineMazePlayers = new HashMap<String,Boolean>();
    Map<String,ArrayList<Map<String,Object>>> MazePlayerItemStack = new HashMap<String,ArrayList<Map<String,Object>>>();
    double chance_takaramono_rare = 0.01f;
    double chance_takaramono = 0.02f;
    double chance_monster = 0.1f;
    float chance_trap = 0.2f;
    int lastxmax = 0;
    int xmax = 0;
    int OriginX = 0;
    int OriginZ = 0;
    int OriginY = 0;
    int mazeHeight_Node = 8;
    int try_count_takaramono = 0;
    int gen_count_takaramono = 0;
    int total_gen_lines = 0;
    int RepeatingTaskId = -1;
    int RepeatingSerializeTaskId = -1;
    int RepeatingAntiHangId = -1;
    final int chance_monster_zombiepigman = 1;
    final int chance_monster_skeleton = 2;
    final int chance_monster_slime = 3;
    final int chance_monster_blaze = 4;
    final int chance_monster_killerbunny = 5;
    final int chance_monster_powercreeper = 6;
    final int chance_monster_wither_skeleton = 7;
    final int chance_monster_witch = 8;
    final int chance_monster_ghast = 9;
    boolean ReConstruct_Maze_On_Finish = true;
    boolean Admin_ReConstruct_trigger = false;
    boolean Enable_Async_Block_Gen = false;
    Material Maze_Material_Wall = Material.BEDROCK;
    Material Maze_Material_ROOF_A = Material.OBSIDIAN;
    Material Maze_Material_ROOF_B = Material.GLASS;
    Material Maze_Material_Light = Material.PUMPKIN;
    ArrayList<BlockMeta> blockmeta = null;
    ArrayList<EntityMeta> entitymeta = null;
    HashMap<String, Object> Anti_hanging = new HashMap<String,Object>();
    String MazeSerialize_File = "Maze.json";
    String world = "world";
    String CommmandPattern = "";
    NumberFormat format = NumberFormat.getInstance();
    Random random = new Random();
    Maze maze = null;
    MazeListener listener = new MazeListener();
    class BlockMeta implements Cloneable {
        Material type;
        int x;
        int y;
        int z; //16 byte
        ArrayList<Map<String, Object>> Inventories = null; //kb level (a few)
        //if we generate 500 * 500 * 24
        //will take 250000 * 24 * 16 = about 100 MB memory on generate

        public BlockMeta(Material type, int x, int y, int z, ArrayList<Map<String, Object>> inventory)
        {
            this.type = type;
            this.x = x;
            this.y = y;
            this.z = z;
            this.Inventories = inventory;
        }
    }
    class EntityMeta implements Cloneable {
        EntityType type;
        int x;
        int y;
        int z;
        Map<String,Object> Details = null;
        Map<String,Object> Inventories = null;
        public  EntityMeta(EntityType type, int x, int y, int z, Map<String,Object> Details, Map<String, Object> inventory)
        {
            this.type = type;
            this.x = x;
            this.y = y;
            this.z = z;
            this.Details = Details;
            this.Inventories = inventory;
        }
    }
    class MazeListener implements Listener {
        @EventHandler
        public void onBlockPlace(BlockPlaceEvent event)
        {
            Location loc = event.getBlock().getLocation();
            if(checkIfInMaze(loc))
            {
                if(event.getBlock().getY() > OriginY + 5)
                {
                    event.getPlayer().sendMessage("在这个位置放置方块将被视为作弊！");
                    event.setCancelled(true);
                    event.getPlayer().setHealth(0);
                    getServer().getWorld(world).createExplosion(loc,5,true);
                }
                else if(event.getBlock().getType() == Material.ENDER_CHEST)
                {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage("不允许在迷宫设置这个方块");
                }
            }
        }
        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event)
        {
            Player p = event.getPlayer();
            String UUID = p.getUniqueId().toString();
            Boolean mazereset = OffLineMazePlayers.get(UUID);
            p.sendMessage("本服务器使用了MazePlugin，执行/StartMazeTour开始一次迷宫寻宝，你的随身物品将被寄存到系统，退出迷宫之后返还。");
            p.sendMessage("/Abort 放弃，身上所有物品将被剥夺，并被传回出生点");
            p.sendMessage("/Finish 当来到迷宫对角（相对入口）时，执行此命令将结束本局，所有人传送到出生位置并重置迷宫，最先完成的人和他的伙伴将得到身上的所有宝物，其他人身上的宝物有70%机会消失掉");
            if(p.isOp())
            {
                p.sendMessage("管理员使用/GenMaze进行迷宫的初次生成，以及手动重置");
                p.sendMessage("/SetRebuildOnFinish <true|false> 设置是否在/Finish执行的时候重建迷宫，如果关闭，则迷宫只能通过/SetSize 或者 /GenMaze重建");
                p.sendRawMessage("/SetSize <size> 设置迷宫的大小");
                p.sendMessage("plugins/MazePlugin/MazePlayerItem.json是玩家进入迷宫前寄存物品的地方，如果要卸载本插件，务必让所有仍然在迷宫的玩家执行/Abort,并备份这个文件");
                p.sendMessage("根据这个文件存储UUID值对应的物品，来确定某个玩家进入迷宫前，持有什么样的物品，以参考回档");
                p.sendMessage("如果这个文件丢失了，那么进入迷宫的玩家储存的物品将永久丢失");
                p.sendMessage("plugins/MazePlugin/enchantments.txt 定义了刷在箱子里面的装备可能的附魔");
                p.sendMessage("plugins/MazePlugin/takaramono.txt 定义了刷在箱子里面一般稀有度的宝物");
                p.sendMessage("plugins/MazePlugin/takaramono_rare.txt 定义了刷在箱子里面非常稀有的宝物");
                p.sendMessage("plugins/MazePlugin/spawner_egg_entities.txt 如果takaramono.txt或者takramono_rare.txt里面允许刷怪蛋，则这个文件定义了刷怪蛋可能的种类");
            }
            if(mazereset != null)
            {
                //handling offline player if maze got refreshed
                //to prevent them from stucking in the wall
                if(mazereset)
                {
                    Location spawn = null;
                    if(p.getBedSpawnLocation() != null)
                    {
                       spawn = p.getBedSpawnLocation();
                    }
                    else
                    {
                        spawn = getServer().getWorld(world).getSpawnLocation();
                    }
                    ItemStack[] items = p.getInventory().getContents();
                    for(int i = 0;i < items.length;i++)
                    {
                        items[i] = null;
                    }
                    ArrayList<Map<String, Object>> list = MazePlayerItemStack.get(p.getUniqueId().toString());
                    if (list != null) {
                        for (Map<String, Object> item : list) {
                            try {
                                getServer().getWorld(world).dropItem(spawn, ItemStack.deserialize(item));
                            }catch (IllegalArgumentException ex)
                            {
                                //  ex.printStackTrace();
                            }
                        }
                    }
                    MazePlayerItemStack.remove(p.getUniqueId().toString());
                    p.teleport(spawn);
                    p.sendMessage("由于上一轮迷宫在你下线后被破解，或者管理员重置了迷宫，因此你被重新传送到出生点,所有迷宫内获得的物品被收回。");
                }
                OffLineMazePlayers.remove(UUID);
            }
            AntiHang_onJoin(p);
        }
        @EventHandler
        public void onPlayerQuit(PlayerQuitEvent event)
        {
            Player p = event.getPlayer();
            if(checkIfInMaze(p.getLocation()))
            {
                OffLineMazePlayers.put(p.getUniqueId().toString(),false);
            }
        }
        @EventHandler
        public void onInterceptCommand(PlayerCommandPreprocessEvent event)
        {
            if(event.getMessage().split(" ")[0].matches(CommmandPattern))
            {
                System.out.println(event.getMessage());
                if(!event.getPlayer().isOp())
                {
                    if(checkIfInMaze(event.getPlayer().getLocation())) {
                        event.setMessage("/");
                        event.getPlayer().sendMessage("你的命令在迷宫禁止使用");
                    }
                }
            }
        }
        @EventHandler
        public void OnGhastProjectileLaunch(ProjectileLaunchEvent event)
        {
            ProjectileSource e = event.getEntity().getShooter();
            if(e instanceof Ghast && checkIfInMaze(((Ghast) e).getLocation()))
            {
                Fireball ball = (Fireball) event.getEntity();
                float roll = 1 + 3 * random.nextFloat();
                ball.setYield(roll);
            }
        }
        @EventHandler
        public void OnDamage(EntityDamageEvent event)
        {
            if(event.getEntity() instanceof Player)
            {
                Player p = (Player) event.getEntity();
                    if(checkIfInMaze(p.getLocation()))
                    {
                        Map<String,Object> status = (Map<String,Object>) Anti_hanging.get(p.getUniqueId().toString());
                        status.put("interrupted",true);
                    }
            }
        }
        @EventHandler
        public void onInventoryOpen(InventoryOpenEvent e)
        {
                if(checkIfInMaze(e.getPlayer().getLocation()))
                {
                    HashMap<String,Object> status = (HashMap<String, Object>) Anti_hanging.get(e.getPlayer().getUniqueId().toString());
                    status.put("interrupted",true);
                }
        }
        public void AntiHang_onJoin (Player p) {
            if(Anti_hanging.get(p.getUniqueId().toString()) != null) {
                return;
            }
            HashMap<String, Object> status = new HashMap<String,Object>();
            status.put("lastX",OriginX);
            status.put("lastZ",OriginZ);
            status.put("JoinTime",System.currentTimeMillis() / 1000);
            status.put("times",0);
            status.put("interrupted",false);
            Anti_hanging.put(p.getUniqueId().toString(),status);
        }
    }

    Runnable Antihang = new Runnable() {
        @Override
        public void run() { //interval = 5s
            for(Player p : getServer().getOnlinePlayers())
            {
                if(checkIfInMaze(p.getLocation()))
                {
                    if(!p.isOp())
                    {
                        HashMap<String,Object> status = (HashMap<String, Object>) Anti_hanging.get(p.getUniqueId().toString());
                        int lastX = (int) status.get("lastX");
                        int lastZ = (int) status.get("lastZ");
                        int X = p.getLocation().getBlockX();
                        int Z = p.getLocation().getBlockZ();
                        int times = (int) status.get("times");
                        boolean interrupt = (boolean)status.get("interrupted");
                        if((X - lastX <= 9 && Z - lastZ <= 9) && times < 5 && !interrupt)
                        {
                            float roll = random.nextFloat();
                            if(roll < 0.2)
                            {
                                roll = random.nextFloat();
                                if(roll < 0.7) {
                                    getServer().getWorld(world).spawnEntity(new Location(getServer().getWorld(world), X + 3, OriginY + 12, Z + 3), EntityType.GHAST);
                                }
                                else
                                {
                                    Creeper c = (Creeper) getServer().getWorld(world).spawnEntity(p.getLocation(),EntityType.CREEPER);
                                    c.setPowered(true);
                                }
                            }
                        }
                        status.put("interrupted",false);
                        status.put("lastX",X);
                        status.put("lastZ",Z);
                    }
                }
            }
        }
    };




    Runnable mSerializePlayerInfo = new Runnable() {
        @Override
        public void run() {
            String jsonOfflineMazePlayerInfo = PlayerInfoSerializer();
            String jsonMazePlayerItem = PlayerItemSerializer();
            BufferedWriter bw1 = null;
            BufferedWriter bw2 = null;
            try {
                bw1 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(getDataFolder().getPath() + File.separator + "OfflineMazePlayer.json"))));
                bw1.write(jsonOfflineMazePlayerInfo);
                bw2 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(getDataFolder().getPath() + File.separator + "MazePlayerItem.json"))));
                bw2.write(jsonMazePlayerItem);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                if(bw1 != null) {
                    try {
                        bw1.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if(bw2 != null) {
                    try {
                        bw2.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };
    Runnable mCheckPlayerPositionRunnable = new Runnable() {
        @Override
        public void run() {
            for (Player p : getServer().getOnlinePlayers())
            {
                Location loc = p.getLocation();
                if(!checkIfInMaze(loc))
                {
                    continue;
                }
                MazeNode node = getNodePlayerStandOn(loc.getBlockX(),loc.getBlockZ());
              //  System.out.println("Node : " + node.X + " " + node.Y + " " + node.isWall);
                if(node.isWall && loc.getBlockY() >= OriginY + 5)
                {
                    loc.setY(loc.getY() - 1);
                    if(getServer().getWorld(world).getBlockAt(loc).getType() != Material.AIR)
                    {
                        //Player on the wall
                        if (!p.isOp())
                        {
                            //check whether Node at x-1 y-1 is not wall
                            int X = node.X;
                            int Y = node.Y;
                          //  System.out.printf("X:%d, Y:%d,isWall " + node.isWall,X,Y);
                            while(maze.maze[X][Y].isWall) {
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
    };
    Runnable mMazeRunnable_Concurrent = new Runnable() {
        public void GenMobs_Concurrent(MazeNode[] nodes)
        {
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
                    int decide = 1 + random.nextInt(9);
                    int count = 1 + random.nextInt(3);
                    for (int i = 0; i < count; i++)
                    {
                        EntityMeta entity;
                        HashMap<String,Object> Detail = new HashMap<String,Object>();
                        HashMap<String,Object> Inventory = null;
                        EntityType type;
                        switch (decide)
                        {

                            case chance_monster_zombiepigman:
                            {
                                Inventory = new HashMap<String,Object>();
                                type = EntityType.ZOMBIE;
                                ItemStack sword = new ItemStack(Material.GOLD_SWORD);
                                if(random.nextFloat() < 0.3)
                                {
                                    sword.setType(Material.DIAMOND_SWORD);
                                    sword.addEnchantment(Enchantment.DAMAGE_ALL,4);
                                    sword.addEnchantment(Enchantment.FIRE_ASPECT,2);
                                    sword.addEnchantment(Enchantment.DURABILITY,3);
                                }
                                Inventory.put("ItemInHand", sword.serialize());

                                if(random.nextFloat() < 0.5)
                                {

                                    Detail.put("isBaby",true);
                                }
                                else
                                {
                                    Detail.put("isBaby",false);
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
                                    Inventory.put("Helmet", helmet.serialize());
                                    Inventory.put("Chestplate", chestplate.serialize());
                                    Inventory.put("Legging",legging.serialize());
                                    Inventory.put("Boots",boots.serialize());
                                }

                            }break;
                            case chance_monster_blaze:
                            {
                                type = EntityType.BLAZE;
                            }break;
                            case chance_monster_wither_skeleton:
                            {
                                type = EntityType.SKELETON;
                                Inventory = new HashMap<String,Object>();
                                Detail.put("SkeletonType",Skeleton.SkeletonType.WITHER);
                                Inventory.put("ItemInHand",new ItemStack(Material.IRON_SWORD).serialize());
                            }break;
                            case chance_monster_killerbunny:
                            {
                                type = EntityType.RABBIT;
                                Detail.put("RabbitType",Rabbit.Type.THE_KILLER_BUNNY);
                                Detail.put("MaxHealth",20.0);
                                Detail.put("Health",20.0);
                                if(random.nextFloat() < 0.5)
                                {
                                    Detail.put("isBaby",true);
                                }
                                else
                                {
                                    Detail.put("isBaby",false);
                                }
                            }break;
                            case chance_monster_powercreeper:
                            {
                                Detail.put("Powered",true);
                                type = EntityType.CREEPER;
                            }break;
                            case chance_monster_slime:
                            {
                                type = EntityType.SLIME;

                            }break;
                            case chance_monster_skeleton:
                            {
                                type = EntityType.SKELETON;
                            }break;
                            case chance_monster_witch:
                            {
                                type = EntityType.WITCH;
                            }break;
                            case chance_monster_ghast:
                            {
                                spawnY = OriginY + 12;
                                type = EntityType.GHAST;
                            }break;
                            default:type=EntityType.BAT;
                        }
                        Detail.put("CustomName","Maze Monster");
                        entitymeta.add(new EntityMeta(type,idx,spawnY,idz,Detail,Inventory));
                    }

                }
                else if (!node.isWall) {
                    //    System.out.println("Generation Block : "  + node.toString());

                    if(roll < chance_takaramono)
                    {
                        //System.out.printf("Chest X %d Y %d Z %d", idx, 193 ,idz);
                        blockmeta.add(setChest_Concurrent(idx, OriginY + 1, idz));

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
                        item.setAmount(1 + random.nextInt(3));
                    }
                    Inventory.add(invnumber,item.serialize());
                }
            }
            return new BlockMeta(Material.CHEST,x,y,z,Inventory);
        }
        @Override
        public void run() {
            long begintime = System.currentTimeMillis();
            for(Player p : getServer().getOnlinePlayers())
            {
                p.sendMessage("迷宫计算中");
            }
            maze = new Maze(xmax);
            maze.init();
            maze.generate(1,1);
            //JSON Serialize Maze and store in file
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String json = maze.JSONSerialize().toString();
                    BufferedWriter bw = null;
                    try {
                        bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(getDataFolder().getPath() + File.separator + MazeSerialize_File))));
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
                }
            }).start();
            //Location locvoid = new Location(getServer().getWorld(world),0,-64,0);
            for(Entity e : getServer().getWorld(world).getEntities()) {
                if (checkIfInMaze(e.getLocation())) {
                    if (e instanceof Monster || e instanceof Rabbit || e instanceof Ghast) {
                        //System.out.println("Clearing " + e.toString() + " " + e.getLocation().getBlockX() + " " + e.getLocation().getBlockY() + " " + e.getLocation().getBlockZ());
                        e.remove();
                    } else if (e instanceof Player) {
                        Player p = (Player) e;
                        p.sendMessage("本轮迷宫探险已经结束");
                        Location loc = p.getLocation();
                        if (loc.getX() >= OriginX + lastxmax * 6 - 3 && loc.getX() <= OriginX + lastxmax * 6 && loc.getZ() >= OriginZ + lastxmax * 6 - 3 && loc.getZ() < OriginZ + lastxmax * 6 && loc.getY() > OriginY) {
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
                            spawn = getServer().getWorld(world).getSpawnLocation();
                        }
                        ArrayList<Map<String, Object>> list = MazePlayerItemStack.get(p.getUniqueId().toString());
                        if (list != null) {
                            for (Map<String, Object> item : list) {
                                try {
                                    getServer().getWorld(world).dropItem(spawn, ItemStack.deserialize(item));
                                } catch (IllegalArgumentException ex) {
                                    //  ex.printStackTrace();
                                }
                            }
                        }

                        MazePlayerItemStack.remove(p.getUniqueId().toString());
                        p.teleport(spawn);
                    }
                }
            }
            for(String UUID : OffLineMazePlayers.keySet())
            {
                OffLineMazePlayers.put(UUID,true);
            }

            //end of clear
            Runnable BlockUpdateRunnable_Concurrent = new Runnable() {
                @Override
                public void run() {
                    blockmeta = new ArrayList<BlockMeta>();
                    entitymeta = new ArrayList<EntityMeta>();
                    //if we change maze to smaller size
                    //clear extra blocks
                    if(lastxmax > xmax)
                    {
                        int vertical_limit = 6 * (lastxmax - xmax + 1);
                        int vertical_begin = 6 * xmax;
                        int horizontal_limit = 6 * (lastxmax + 1);
                        int ylimit = 3 * mazeHeight_Node + OriginY + 1;
                        for(int i = 3; i < vertical_limit;i++)
                        {
                            for(int j = 0; j < horizontal_limit;j++)
                            {
                                for(int k = OriginY;k < ylimit;k++)
                                {
                                    BlockMeta meta = new BlockMeta(Material.AIR,OriginX + vertical_begin + i,k,OriginZ + j , null);
                                    BlockMeta meta1 = new BlockMeta(Material.AIR,OriginZ + j ,k,OriginX + vertical_begin + i, null);
                                    blockmeta.add(meta);
                                    blockmeta.add(meta1);
                                }
                            }
                        }
                    }
                    //end of clear
                    int posx = 0;
                    int posy = 0;
                    int posz = 0;
                    int idz = 0;
                    int light = OriginY + 5;
                    total_gen_lines = 2 * xmax;
                    for (MazeNode[] nodes : maze.maze) {
                        for (MazeNode node : nodes) {
                            if (ReConstruct_Maze_On_Finish || (!ReConstruct_Maze_On_Finish && Admin_ReConstruct_trigger)) {
                                if (checkNodeIsEdge(node) && node.isWall) {
                                    //Generate Basic Walls
                                    for (int i = 0; i < mazeHeight_Node; i++) {
                                        for (int j = 0; j < 3; j++) { //3 X Outer Layer
                                            for (int k = 0; k < 3; k++) {   //3 X
                                                for (int l = 0; l < 3; l++) { //3 X
                                                    posx = OriginX + node.X * 3 + k;
                                                    posy = OriginY + i * 3 + j;
                                                    posz = OriginZ + node.Y * 3 + l;
                                                    BlockMeta meta = new BlockMeta(Maze_Material_Wall,posx,posy,posz , null);
                                                    blockmeta.add(meta);
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
                                            blockmeta.add(new BlockMeta(Maze_Material_Wall,posx, OriginY, posz,null));
                                            //assert we generate from 0,64,0 in practice
                                            for (int i = 0; i < 4; i++) {
                                                if (node.isWall) {
                                                    blockmeta.add(new BlockMeta(Maze_Material_Wall,posx,OriginY + 1 + i,posz , null));
                                                } else {
                                                    blockmeta.add(new BlockMeta(Material.AIR,posx,OriginY + 1 + i,posz , null));
                                                }
                                            }

                                            if (node.isWall) {
                                                blockmeta.add(new BlockMeta(Maze_Material_Light,posx,light,posz , null));
                                                blockmeta.add(new BlockMeta(Maze_Material_ROOF_B,posx, OriginY + mazeHeight_Node * 3, posz , null));
                                            } else {
                                                blockmeta.add(new BlockMeta(Material.AIR,posx,light,posz , null));
                                                blockmeta.add(new BlockMeta(Maze_Material_ROOF_A,posx, OriginY + mazeHeight_Node * 3, posz , null));
                                            }
                                        }
                                    }
                                    //Clear
                                    for (int j = 0; j < 3; j++)
                                    {
                                        for (int k = 0; k < 3; k++)
                                        {
                                            for (int i = 5; i < mazeHeight_Node * 3 - 2; i++) {
                                                posx = OriginX + node.X * 3 + j;
                                                posz = OriginZ + node.Y * 3 + k;
                                                blockmeta.add(new BlockMeta(Material.AIR,OriginY + 1 + i,light,posz , null));
                                            }
                                        }
                                    }
                                    //end of clear
                                }
                            }
                            else
                            {
                                //if we do not generate
                                //clear chests
                                //no affect to walls

                                        blockmeta.add(new BlockMeta(Material.AIR,OriginX + node.X * 3 - 2,OriginY + 1,OriginZ + node.Y * 3 - 2, null));
                            }

                        }
                        //System.out.printf("Generated line #" + idz);
                        random = new Random();
                        idz++;
                    }
                    for(MazeNode[] nodes : maze.maze)
                    {
                        GenMobs_Concurrent(nodes);
                    }
                    for(int i = 1; i < 4; i++)
                    {
                        for(int j = 1; j < 4; j++)
                        {
                            for(int k = 1; k < 4 ; k++)
                            {
                                blockmeta.add(new BlockMeta(Material.AIR,OriginX + (maze.maze.length - 1) * 3 - i,OriginY + j,OriginZ + (maze.maze.length - 1) * 3 - k, null));
                            }
                        }
                    }
                    double lastchance = chance_takaramono_rare;
                    chance_takaramono_rare *= 4;
                    blockmeta.add(setChest_Concurrent(OriginX + (maze.maze.length - 1) * 3 - 1, OriginY + 1, OriginZ + (maze.maze.length - 1) * 3 - 1));
                    blockmeta.add(setChest_Concurrent(OriginX + (maze.maze.length - 1) * 3 - 2, OriginY + 1, OriginZ + (maze.maze.length - 1) * 3 - 2));
                    blockmeta.add(setChest_Concurrent(OriginX + (maze.maze.length - 1) * 3 - 3, OriginY + 1, OriginZ + (maze.maze.length - 1) * 3 - 3));
                    blockmeta.add(setChest_Concurrent(OriginX + (maze.maze.length - 1) * 3 - 1, OriginY + 1, OriginZ + (maze.maze.length - 1) * 3 - 3));
                    blockmeta.add(setChest_Concurrent(OriginX + (maze.maze.length - 1) * 3 - 3, OriginY + 1, OriginZ + (maze.maze.length - 1) * 3 - 1));
                    blockmeta.add(setChest_Concurrent(OriginX + (maze.maze.length - 1) * 3 - 1, OriginY + 3, OriginZ + (maze.maze.length - 1) * 3 - 1));
                    blockmeta.add(setChest_Concurrent(OriginX + (maze.maze.length - 1) * 3 - 2, OriginY + 3, OriginZ + (maze.maze.length - 1) * 3 - 2));
                    blockmeta.add(setChest_Concurrent(OriginX + (maze.maze.length - 1) * 3 - 3, OriginY + 3, OriginZ + (maze.maze.length - 1) * 3 - 3));
                    blockmeta.add(setChest_Concurrent(OriginX + (maze.maze.length - 1) * 3 - 3, OriginY + 3, OriginZ + (maze.maze.length - 1) * 3 - 1));
                    blockmeta.add(setChest_Concurrent(OriginX + (maze.maze.length - 1) * 3 - 1, OriginY + 3, OriginZ + (maze.maze.length - 1) * 3 - 3));
                    chance_takaramono_rare = lastchance;
                    getServer().getScheduler().callSyncMethod(MazePlugin.this, new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            int ExitX = OriginX + maze.maze.length * 3 - 5;
                            int Exitz = OriginZ + maze.maze.length * 3 - 5;
                            //System.out.println("EXIT X,Z " + ExitX + " " + Exitz);
                            World w = getServer().getWorld(world);
                            //Convert Metadata to Blocks
                            for (int i = 0; i < blockmeta.size(); i++)
                            {
                                if(i % 100000 == 0)
                                {
                                    float percent = ((float)i) / blockmeta.size() * 100.0f;
                                    final String broadcast = "[" + format.format(percent) +  "%] 已经处理： " + i + " 块， 总共生成 ： " + blockmeta.size()+  " 服务器正在努力的生成迷宫啦>_<";
                                    for(Player p : getServer().getOnlinePlayers())
                                    {
                                        p.sendMessage(broadcast);
                                    }
                                    getLogger().info(broadcast);
                                }
                                BlockMeta meta = blockmeta.get(i);
                                w.getBlockAt(meta.x,meta.y,meta.z).setType(meta.type);
                                if(meta.type == Material.CHEST)
                                {
                                    Chest chest = (Chest) w.getBlockAt(meta.x,meta.y,meta.z).getState();
                                    Inventory inv = chest.getInventory();
                                    for(int j = 0; j < 27;j++)
                                    {
                                        if(meta.Inventories.get(j) != null)
                                        {
                                            inv.setItem(j,ItemStack.deserialize(meta.Inventories.get(j)));
                                        }
                                    }
                                }
                            }
                            //Spawn Entites by metadata
                            for(Player p : getServer().getOnlinePlayers())
                            {
                                p.sendMessage("服务器正在努力刷怪");
                            }

                            for (int i = 0; i < entitymeta.size(); i++) {
                                Entity entity = w.spawnEntity(new Location(w, entitymeta.get(i).x, entitymeta.get(i).y, entitymeta.get(i).z), entitymeta.get(i).type);
                                if (entity instanceof Zombie) {
                                    Zombie z = (Zombie) entity;
                                    z.setBaby((Boolean) entitymeta.get(i).Details.get("isBaby"));
                                    z.getEquipment().setItemInMainHand(ItemStack.deserialize((Map<String, Object>) entitymeta.get(i).Inventories.get("ItemInHand")));
                                    if (entitymeta.get(i).Details.get("ChestPlate") != null) {
                                        z.getEquipment().setItemInMainHand(ItemStack.deserialize((Map<String, Object>) entitymeta.get(i).Inventories.get("ChestPlate")));
                                        z.getEquipment().setItemInMainHand(ItemStack.deserialize((Map<String, Object>) entitymeta.get(i).Inventories.get("Helmet")));
                                        z.getEquipment().setItemInMainHand(ItemStack.deserialize((Map<String, Object>) entitymeta.get(i).Inventories.get("Leggings")));
                                        z.getEquipment().setItemInMainHand(ItemStack.deserialize((Map<String, Object>) entitymeta.get(i).Inventories.get("Boots")));
                                    }
                                } else if (entity instanceof Creeper) {
                                    Creeper c = (Creeper) entity;
                                    c.setPowered((Boolean) entitymeta.get(i).Details.get("Powered"));
                                } else if (entity instanceof Skeleton) {
                                    Skeleton s = (Skeleton) entity;
                                    if (entitymeta.get(i).Details.get("SkeletonType") != null) {
                                        s.setSkeletonType((Skeleton.SkeletonType) entitymeta.get(i).Details.get("SkeletonType"));
                                        s.getEquipment().setItemInMainHand(ItemStack.deserialize((Map<String, Object>) entitymeta.get(i).Inventories.get("ItemInHand")));
                                    }
                                } else if (entity instanceof Rabbit) {
                                    Rabbit r = (Rabbit) entity;
                                    r.setRabbitType(Rabbit.Type.THE_KILLER_BUNNY);
                                    r.setMaxHealth((Double) entitymeta.get(i).Details.get("MaxHealth"));
                                    r.setHealth((Double) entitymeta.get(i).Details.get("Health"));
                                    if ((Boolean) entitymeta.get(i).Details.get("isBaby")) {
                                        r.setBaby();
                                    }
                                }
                            }


                            //now lastxmax > xmax
                            //clear a larger area

                            for(Entity e : getServer().getWorld(world).getEntities()) {
                                if(checkIfInMaze(e.getLocation())) {
                                    if (e instanceof Item){
                                        //System.out.println("clearing " + ((Item)e).getName());
                                        Item item = (Item)e;
                                        item.remove();
                                    }
                                }
                            }
                            long elapsed = (System.currentTimeMillis() - begintime) / 1000;
                            for(Player p : getServer().getOnlinePlayers())
                            {
                                p.sendMessage("清理中----完成！");
                                p.sendMessage("耗时" + format.format(elapsed) + "秒");
                            }

                            if(RepeatingTaskId == -1) {
                                RepeatingTaskId = getServer().getScheduler().scheduleSyncRepeatingTask(MazePlugin.this, mCheckPlayerPositionRunnable, 0, 10);
                            }
                            if(RepeatingAntiHangId == -1) {
                                RepeatingAntiHangId = getServer().getScheduler().scheduleAsyncRepeatingTask(MazePlugin.this, Antihang, 0, 150);
                            }
                            if(RepeatingSerializeTaskId == -1)
                            {
                                RepeatingSerializeTaskId = getServer().getScheduler().scheduleAsyncRepeatingTask(MazePlugin.this, mSerializePlayerInfo,0,300);
                            }
                                System.out.println("return");
                            //change lastxmax to xmax
                            lastxmax = xmax;
                            FileConfiguration conf =  getConfig();
                            conf.set("lastWidthX",xmax);
                            conf.save(getDataFolder() + File.separator + "config.yml");
                            //reset trigger
                            if(Admin_ReConstruct_trigger)
                            {
                                Admin_ReConstruct_trigger = false;
                            }
                            blockmeta.clear();
                            entitymeta.clear();
                            blockmeta = null;
                            entitymeta = null;
                            return null;
                        }
                    });//end of callSync
                }
            }; //
            getServer().getScheduler().runTaskAsynchronously(MazePlugin.this,BlockUpdateRunnable_Concurrent);

        }
    };

    public String PlayerInfoSerializer()
    {
        Gson gson = new Gson();
        return gson.toJson(OffLineMazePlayers);
    }
    public Map<String, Boolean> PlayerInfoDeSerializer(File file)
    {
        String in = "";
        String read = null;
        Gson gson = new Gson();
        Map<String, Boolean> map;
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            do{

                read = br.readLine();
                if(read != null) {
                    in += read;
                }
            }while (read != null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        finally {
            try {

                return gson.fromJson(in, Map.class);
            }
            catch (JsonSyntaxException e)
            {
                System.out.println(in);
                e.printStackTrace();
                return null;
            }
        }
    }
    public String PlayerItemSerializer() {
        Gson gson = new Gson();
        return gson.toJson(MazePlayerItemStack);
    }

    public Map<String,ArrayList<Map<String,Object>>> PlayerItemDeserializer(File file)
    {
        String in = "";
        String read = null;
        Gson gson = new Gson();
        Map<String, Boolean> map;
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            do{
                read = br.readLine();
                if(read != null) {
                    in += read;
                }

            }while (read != null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        finally {
            try {
                return gson.fromJson(in, Map.class);
            }catch (JsonSyntaxException e)
            {
                System.out.println(in);
                e.printStackTrace();
                return null;
            }
        }
    }

    public MazeNode getNodePlayerStandOn(int X, int Z)
    {
        try {
            return maze.maze[(X - OriginX) / 3][(Z - OriginZ) / 3];
        }catch (ArrayIndexOutOfBoundsException e)
        {
            return null;
        }
    }
    public boolean checkNodeIsEdge(MazeNode node) {
        int nodeX = node.X;
        int nodeY = node.Y;
        if (nodeX == 0 || nodeY == 0 || nodeX == maze.maze.length - 1 || nodeY == maze.maze.length - 1)
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
        int xboundend = OriginX + 3 * (2 * lastxmax + 1);
        int zboundend = OriginZ + 3 * (2 * lastxmax + 1);
        // System.out.println(xboundend + " " + zboundend + " " + loc.getBlockX() + " " + loc.getBlockZ());
        if(loc.getX() > OriginX && loc.getX() < xboundend && loc.getZ() > OriginZ && loc.getZ() < zboundend)
        {
            return true;
        }
        else {
            return false;
        }
    }



    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        FileConfiguration config = this.getConfig();
        if(cmd.getName().equals("StartMazeTour"))
        {
            if(sender instanceof Player)
            {
                if(maze == null)
                {
                    sender.sendMessage("管理员还没有进行迷宫的初次产生");
                    return false;
                }
                ArrayList<Map<String,Object>> itemStack;
                Player p = ((Player)sender);
                if(p.getFireTicks() > 0)
                {
                    sender.sendMessage("你可能掉在岩浆里面，不允许执行这个指令");
                    return false;
                }
                Location loc = p.getLocation();
                if(loc.getWorld().getBlockAt(loc.getBlockX(),loc.getBlockY() - 1,loc.getBlockZ()).getType() == Material.AIR)
                {
                    sender.sendMessage("你可能正在下落，不允许执行这个指令");
                    return false;
                }
                if(MazePlayerItemStack.get(p.getUniqueId().toString()) == null) {
                    itemStack =new ArrayList<Map<String, Object>>();
                    MazePlayerItemStack.put(p.getUniqueId().toString(), itemStack);
                }
                else {
                    itemStack = MazePlayerItemStack.get(p.getUniqueId().toString());
                    itemStack.trimToSize();
                }
                int count = 0;
                ItemStack[] items = p.getInventory().getContents();
                for(int i = 0; i < items.length;i++)
                {
                    if(items[i] != null)
                    {
                        if(items[i].getType() != Material.AIR)
                        {
                            count++;
                        }
                    }
                }
                for(int i = 0; i < p.getEquipment().getArmorContents().length;i++)
                {
                    if(items[i] != null)
                    {
                        if(items[i].getType() != Material.AIR)
                        {
                            count++;
                        }
                    }
                }
                // System.out.println("" + count + " " + itemStack.size());
                if(itemStack.size() + count > 40)
                {
                    p.sendMessage("对不起迷宫最多帮你存41个东西，清理下物品栏再试");
                    return false;
                }
                p.teleport(new Location(this.getServer().getWorld(world),OriginX + 4, OriginY + 3,OriginZ + 3));
                p.sendMessage("欢迎来到tengge/dogeop设计的小迷宫，在这里开始你的冒险吧！");
                p.sendMessage("目标：走到迷宫的另一角。");
                p.sendMessage("如果下线前你在迷宫内部，而在你再次上线前迷宫被重置，那么你将被送回出生点并剥夺所有的收获物");
                p.sendMessage("建议组团搜刮");
                for (int i = 0 ; i < p.getInventory().getSize(); i++)
                {
                    if(p.getInventory().getItem(i) != null) {
                        if(p.getInventory().getItem(i).getType() != Material.AIR) {
                            itemStack.add(p.getInventory().getItem(i).serialize());
                            p.getInventory().setItem(i,null);
                        }
                    }
                }
                for (int i = 0 ; i < p.getInventory().getArmorContents().length; i++)
                {

                    if(p.getInventory().getArmorContents()[i] != null) {
                        if(p.getInventory().getArmorContents()[i].getType() != Material.AIR) {
                            itemStack.add(p.getInventory().getArmorContents()[i].serialize());
                        }
                    }
                }
                ItemStack iteminmainhand = p.getEquipment().getItemInMainHand();
                if(iteminmainhand != null) {
                    if(iteminmainhand.getType() != Material.AIR)
                    {
                        {
                            itemStack.add(iteminmainhand.serialize());
                        }
                    }
                }
                ItemStack iteminoffhand = p.getEquipment().getItemInOffHand();
                if(iteminoffhand != null) {
                    if(iteminoffhand.getType() != Material.AIR)
                    {
                        {
                            itemStack.add(iteminoffhand.serialize());
                        }
                    }
                }
                p.getEquipment().setBoots(null);
                p.getEquipment().setChestplate(null);
                p.getEquipment().setLeggings(null);
                p.getEquipment().setHelmet(null);
                p.getEquipment().setItemInMainHand(null);
                p.getEquipment().setItemInOffHand(null);
                p.getEquipment().clear();
                Inventory inv = p.getInventory();
                ItemStack boots = new ItemStack(Material.LEATHER_BOOTS,1);
                ItemStack helmets = new ItemStack(Material.LEATHER_HELMET,1);
                ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS,1);
                ItemStack chestp = new ItemStack(Material.LEATHER_CHESTPLATE,1);
                ItemStack sword = new ItemStack(Material.IRON_SWORD,1);
                ItemStack bow = new ItemStack(Material.BOW,1);
                ItemStack arrow = new ItemStack(Material.ARROW,16);
                ItemStack potato = new ItemStack(Material.BAKED_POTATO,24);
                inv.setItem(1,boots);
                inv.setItem(2,helmets);
                inv.setItem(3,leggings);
                inv.setItem(4,chestp);
                inv.setItem(5,sword);
                inv.setItem(6,bow);
                inv.setItem(7,arrow);
                inv.setItem(8,potato);
                HashMap<String,Object> status = (HashMap<String, Object>) Anti_hanging.get(p.getUniqueId().toString());
                status.put("JoinTime",System.currentTimeMillis() / 1000);
            }
        }
        else if(cmd.getName().equals("Abort"))
        {
            if(sender instanceof Player) {
                if(maze == null)
                {
                    sender.sendMessage("管理员还没有进行迷宫的初次产生");
                    return false;
                }

                int boundstart = OriginX;
                int boundend = OriginZ + 3 * maze.maze.length;
                Player e = (Player)sender;
                if(e.getLocation().getX() > boundstart && e.getLocation().getY() < boundend && e.getLocation().getZ() > boundstart && e.getLocation().getZ() < boundend) {
                    HashMap<String,Object> status = (HashMap<String, Object>) Anti_hanging.get(e.getUniqueId().toString());
                    if((long)status.get("JoinTime") - System.currentTimeMillis() / 1000 < 300)
                    {
                        if(!e.isOp()) {
                            sender.sendMessage("必须在迷宫停留五分钟以上才可以放弃");
                            return false;
                        }
                    }
                    ItemStack[] inv = e.getInventory().getContents();
                    for (int i = 0; i < inv.length; i++) {
                        e.getInventory().setItem(i,null);
                    }
                    for (int i = 0; i < e.getInventory().getContents().length; i++)
                    {
                        e.getInventory().setItem(i,null);
                    }
                    Location loc;
                    if (e.getBedSpawnLocation() != null) {
                        loc = e.getBedSpawnLocation();
                    } else {
                        loc = getServer().getWorld(world).getSpawnLocation();
                    }
                    if (MazePlayerItemStack.get(e.getUniqueId().toString()) != null) {
                        for (Map<String, Object> item : MazePlayerItemStack.get(e.getUniqueId().toString())) {
                            try {
                                getServer().getWorld(world).dropItem(loc, ItemStack.deserialize(item));
                            } catch (IllegalArgumentException ex) {

                            }
                        }
                        MazePlayerItemStack.remove(e.getUniqueId().toString());
                    }
                    e.teleport(loc);

                }
                else
                {
                    sender.sendMessage("你还不在迷宫");
                }

            }
        }
        else if(cmd.getName().equals("Finish"))
        {
            if(sender instanceof Player) {
                if(maze == null)
                {
                    sender.sendMessage("管理员还没有进行迷宫的初次产生");
                    return false;
                }
                Location loc = ((Player) sender).getLocation();
                if (loc.getX() >= OriginX +  xmax * 6 - 3  && loc.getX() <=OriginX + xmax * 6 && loc.getZ() >= OriginZ + xmax * 6 - 3 && loc.getZ() < OriginZ + xmax * 6 && loc.getY() > OriginY)
                {
                    mMazeRunnable_Concurrent.run();
                }
                else
                {
                    sender.sendMessage("你还没有走到终点");
                }
            }
        }
        else if(cmd.getName().equals("GenMaze"))
        {
            if(sender.isOp() && sender instanceof Player)
            {
                Admin_ReConstruct_trigger = true;
                sender.sendMessage("生成中，如果是第一次生成会卡服约10-20分钟，迷宫从无到有会消耗大量资源。如出现崩溃，清修改spigot.yml的timeout避免服务器报错。");
                mMazeRunnable_Concurrent.run();
            }
        }
        else if(cmd.getName().equals("SetSize"))
        {
            if(!sender.isOp())
            {
                return  false;
            }
            if(args.length == 1)
            {
                try {
                    int width = Integer.valueOf(args[0]);
                    if(width > 0)
                    {
                        config.set("currentWidthX",width);
                        config.save(getDataFolder() + File.separator + "config.yml");
                        xmax = width;
                        Admin_ReConstruct_trigger = true;
                        mMazeRunnable_Concurrent.run();
                    }
                    else
                    {
                        sender.sendMessage("需要输入正整数");
                    }
                }catch (NumberFormatException e)
                {
                    sender.sendMessage("需要输入正整数");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else
            {
                sender.sendMessage("只能有一个整形参数");
            }
        }
        else if(cmd.getName().equals("SetRebuildOnFinish"))
        {
            if(sender.isOp())
            {
                if(args.length == 1) {
                    try {
                        boolean ReConstruct_Maze_On_Finish = Boolean.valueOf(args[0]);
                        this.ReConstruct_Maze_On_Finish = ReConstruct_Maze_On_Finish;
                        config.set("ReConstruct_Maze_On_Finish",ReConstruct_Maze_On_Finish);
                        config.save(getDataFolder() + File.separator + "config.yml");
                    } catch (Exception e)
                    {
                        sender.sendMessage("用法 SetRebuildOnFinish <true|false>");
                    }
                }
            }
        }
        return true;
    }
    @Override
    public void onEnable() {
        System.out.println(getServer().getBukkitVersion());
        BufferedReader br = null;
        try {
            this.saveDefaultConfig();
            this.saveConfig();
            String DataFolder = getDataFolder().getPath();
            saveResource("enchantments.txt", false);
            saveResource("takaramono_rare.txt", false);
            saveResource("spawner_egg_entities.txt", false);
            saveResource("takaramono.txt", false);
            saveResource("banned_commands.txt",false);
            String line = null;
            br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(DataFolder + File.separator + "enchantments.txt"))));
            do{
                line = br.readLine();
                Enchantment e = Enchantment.getByName(line);
                if(e != null) {
                    //System.out.println(e.toString());
                    Enchantments.add(e);
                }
            }while (line != null);
            br.close();
            br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(DataFolder + File.separator + "spawner_egg_entities.txt"))));
            do {
                line = br.readLine();
                try {
                    EntityType e = EntityType.valueOf(line);
                    //       System.out.println(e.toString());
                    Monsters.add(e);
                }
                catch (NullPointerException ex)
                {
                    //     System.out.println("Read null");
                }
                catch (IllegalArgumentException ex)
                {
                    ex.printStackTrace();
                }
            }while (line != null);
            br.close();
            br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(DataFolder + File.separator + "takaramono.txt"))));
            do{
                try {
                    line = br.readLine();
                    Material e = Material.valueOf(line);
                    //   System.out.println(e.toString());
                    BonusItems.add(e);
                }
                catch (NullPointerException ex)
                {
                    // System.out.println("Read null");
                }
                catch (IllegalArgumentException ex)
                {
                    ex.printStackTrace();
                }
            }while (line != null);
            br.close();
            br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(DataFolder + File.separator + "takaramono_rare.txt"))));
            do{
                try {
                    line = br.readLine();
                    Material e = Material.valueOf(line);
                    //System.out.println(e.toString());
                    BonusItems_rare.add(e);
                }
                catch (NullPointerException ex)
                {
                    //System.out.println("Read null");
                }
                catch (IllegalArgumentException ex)
                {
                    ex.printStackTrace();
                }
            }while (line != null);
            String regex = "/(";
            br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(DataFolder + File.separator + "banned_commands.txt"))));
            do{
                line = br.readLine();
                if(line != null) {
                    //System.out.println(e.toString());
                    regex += (line + "|");
                }
            }while (line != null);
            regex = regex.substring(0,regex.length() - 1) + ")";
            System.out.println(regex);
            CommmandPattern = regex;
            br.close();
            FileConfiguration config = getConfig();
            chance_takaramono_rare = config.getDouble("Chance_Takaramono_rare");
            chance_takaramono = config.getDouble("Chance_Chest");
            chance_monster = config.getDouble("Chance_Monster");
            xmax = config.getInt("currentWidthX");
            lastxmax = config.getInt("lastWidthX");
            OriginX = config.getInt("OriginX");
            OriginY = config.getInt("OriginY");
            OriginZ = config.getInt("OriginZ");
            world = config.getString("world");
            ReConstruct_Maze_On_Finish = config.getBoolean("ReConstruct_Maze_On_Finish");
            try_count_takaramono = config.getInt("Takaramono_try_count");
            gen_count_takaramono = config.getInt("Takaramono_generation_count");
            Enable_Async_Block_Gen = config.getBoolean("ReConstruct_Maze_On_Finish");
            System.out.println("Enabling Maze");

            try {
                Maze_Material_Light = Material.valueOf(config.getString("Maze_Material_Light"));
                Maze_Material_ROOF_B = Material.valueOf(config.getString("Maze_Material_ROOF_B"));
                Maze_Material_ROOF_A = Material.valueOf(config.getString("Maze_Material_ROOF_A"));
                Maze_Material_Wall = Material.valueOf(config.getString("Maze_Material_Wall"));
            }catch (IllegalArgumentException e)
            {
                e.printStackTrace();
            }
            catch (NullPointerException e)
            {
                e.printStackTrace();
            }
            getServer().getScheduler().scheduleAsyncDelayedTask(this, new Runnable() {
                @Override
                public void run() {
                    Logger.getLogger("MazePlugin").info("Async Load Maze from disk");
                    Logger.getLogger("MazePlugin").info("Async Load PlayerData from disk");
                    Map<String,ArrayList<Map<String,Object>>> Mazeplayeritems = PlayerItemDeserializer(new File(DataFolder + File.separator + "MazePlayerItem.json"));
                    if(Mazeplayeritems != null)
                    {
                        MazePlayerItemStack = Mazeplayeritems;
                    }
                    Map<String,Boolean> OfflineMzPlayers = PlayerInfoDeSerializer(new File(DataFolder + File.separator + "OfflineMazePlayer.json"));
                    if(OfflineMzPlayers != null)
                    {
                        OffLineMazePlayers = OfflineMzPlayers;
                    }
                    Logger.getLogger("MazePlugin").info("End of Async Load PlayerData from disk");

                    maze = Maze.JSONDeSerialize(new File(DataFolder + File.separator + MazeSerialize_File));
                    if(maze != null)
                    {
                        RepeatingTaskId = getServer().getScheduler().scheduleSyncRepeatingTask(MazePlugin.this,mCheckPlayerPositionRunnable,0,10);
                        RepeatingSerializeTaskId = getServer().getScheduler().scheduleAsyncRepeatingTask(MazePlugin.this, mSerializePlayerInfo,0,300);
                        Logger.getLogger("MazePlugin").info("Starting Background Service : save Maze Data.....Done!");
                        RepeatingAntiHangId = getServer().getScheduler().scheduleSyncRepeatingTask(MazePlugin.this,Antihang,0,200);
                    }
                    else
                    {
                        Logger.getLogger("MazePlugin").info("Maze hasn't been generated");
                    }
                    Logger.getLogger("MazePlugin").info("End of Async Load Maze from disk");

                    //PlayerInfoDeSerializer
                }
            },0);
            getServer().getPluginManager().registerEvents(listener,this);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            Logger.getLogger("MazePlugin").warning("启动插件Mazelugin出错，请重新加载");
        }
        finally {
            if(br != null)
            {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        format.setMaximumFractionDigits(2);
        format.setMaximumIntegerDigits(3);

    }
    @Override
    public void onDisable()
    {
        getServer().getScheduler().cancelTask(RepeatingTaskId);
        getServer().getScheduler().cancelTask(RepeatingSerializeTaskId);
        getServer().getScheduler().cancelTask(RepeatingAntiHangId);
        mSerializePlayerInfo.run();
    }
}
