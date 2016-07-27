package org.dogeop.MazePlugin;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Chest;
import org.bukkit.block.CommandBlock;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.SpawnEgg;
import org.json.JSONObject;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.Buffer;
import java.text.NumberFormat;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.*;
import java.util.concurrent.Callable;
public final class MazePlugin extends JavaPlugin
{
    ArrayList<Material> BonusItems = new ArrayList<Material>();
    ArrayList<Material> BonusItems_rare = new ArrayList<Material>();
    ArrayList<Enchantment> Enchantments = new ArrayList<Enchantment>();
    ArrayList<EntityType> Monsters = new ArrayList<EntityType>();
    Pattern pat = Pattern.compile("(SWORD|CHESTPLATE|HELMET|BOOTS|LEGGINGS|PICKAXE|BOW|BOOK)");
    double chance_takaramono_rare = 0.01f;
    double chance_takaramono = 0.02f;
    double chance_monster = 0.1f;
    float chance_trap = 0.2f;
    int xmax = 0;
    int OriginX = 0;
    int OriginZ = 0;
    int OriginY = 0;
    int mazeHeight_Node = 8;
    int try_count_takaramono = 0;
    int gen_count_takaramono = 0;
    int total_gen_lines = 0;
    int RepeatingTaskId = -1;
    final int chance_monster_zombiepigman = 1;
    final int chance_monster_skeleton = 2;
    final int chance_monster_slime = 3;
    final int chance_monster_blaze = 4;
    final int chance_monster_killerbunny = 5;
    final int chance_monster_powercreeper = 6;
    final int chance_monster_wither_skeleton = 7;
    final int chance_monster_witch = 8;
    final int chance_monster_ghast = 9;
    Material Maze_Material_Wall = Material.BEDROCK;
    Material Maze_Material_ROOF_A = Material.OBSIDIAN;
    Material Maze_Material_ROOF_B = Material.GLASS;
    Material Maze_Material_Light = Material.SEA_LANTERN;
    String MazeSerialize_File = "Maze.json";
    String world = "world";
    NumberFormat format = NumberFormat.getInstance();
    Random random = new Random();
    Maze maze = null;
    public MazeNode getNodePlayerStandOn(int X, int Z)
    {
        return maze.maze[(X - OriginX)/3][(Z-OriginZ)/3];
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
    public void GenMobs(MazeNode[] nodes)
    {
        int idx = 0;
        int idz = 0;
        for (MazeNode node : nodes) {
            node.visited = false;
            idx = OriginX + node.X * 3 + 1;
            idz = OriginZ + node.Y * 3 + 1;
            float roll = random.nextFloat();
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
                    switch (decide)
                    {
                        case chance_monster_zombiepigman:
                        {
                            Zombie zombie = (Zombie) getServer().getWorld(world).spawnEntity(new Location(getServer().getWorld(world),idx, spawnY, idz),EntityType.ZOMBIE);
                            ItemStack sword = new ItemStack(Material.GOLD_SWORD);
                            if(random.nextFloat() < 0.3)
                            {
                                sword.setType(Material.DIAMOND_SWORD);
                                sword.addEnchantment(Enchantment.DAMAGE_ALL,4);
                                sword.addEnchantment(Enchantment.FIRE_ASPECT,2);
                                sword.addEnchantment(Enchantment.DURABILITY,3);
                            }
                            if(random.nextFloat() < 0.5)
                            {
                                zombie.setBaby(true);
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
                                zombie.getEquipment().setHelmet(helmet);
                                zombie.getEquipment().setChestplate(chestplate);
                                zombie.getEquipment().setLeggings(legging);
                                zombie.getEquipment().setBoots(boots);
                            }
                            zombie.getEquipment().setItemInMainHand(sword);
                            roll = random.nextFloat();

                        }break;
                        case chance_monster_blaze:
                        {
                            getServer().getWorld(world).spawnEntity(new Location(getServer().getWorld(world),idx, spawnY, idz),EntityType.BLAZE);
                        }break;
                        case chance_monster_wither_skeleton:
                        {
                            Skeleton s = (Skeleton) getServer().getWorld(world).spawnEntity(new Location(getServer().getWorld(world),idx, spawnY, idz),EntityType.SKELETON);
                            s.setSkeletonType(Skeleton.SkeletonType.WITHER);
                            s.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
                        }break;
                        case chance_monster_killerbunny:
                        {
                            Rabbit rabbit = (Rabbit) getServer().getWorld(world).spawnEntity(new Location(getServer().getWorld(world),idx, spawnY, idz),EntityType.RABBIT);
                            rabbit.setRabbitType(Rabbit.Type.THE_KILLER_BUNNY);
                            rabbit.setMaxHealth(20);
                            rabbit.setHealth(20);
                        }break;
                        case chance_monster_powercreeper:
                        {
                            Creeper creeper = (Creeper) getServer().getWorld(world).spawnEntity(new Location(getServer().getWorld(world),idx, spawnY, idz),EntityType.CREEPER);
                            creeper.setPowered(true);
                        }break;
                        case chance_monster_slime:
                        {
                            getServer().getWorld(world).spawnEntity(new Location(getServer().getWorld(world),idx, spawnY, idz),EntityType.SLIME);

                        }break;
                        case chance_monster_skeleton:
                        {
                            getServer().getWorld(world).spawnEntity(new Location(getServer().getWorld(world),idx, spawnY, idz),EntityType.SKELETON);
                        }break;
                        case chance_monster_witch:
                        {
                            getServer().getWorld(world).spawnEntity(new Location(getServer().getWorld(world),idx, spawnY, idz),EntityType.WITCH);
                        }break;
                        case chance_monster_ghast:
                        {
                            spawnY = OriginY + 12;
                            if(random.nextFloat() < 0.1)
                            {
                                Ghast ghast = (Ghast) getServer().getWorld(world).spawnEntity(new Location(getServer().getWorld(world),idx, spawnY, idz),EntityType.GHAST);
                            }
                        }
                    }
                }

            }
            else if (!node.isWall) {
                //    System.out.println("Generation Block : "  + node.toString());

                if(roll < chance_takaramono)
                {
                    //System.out.printf("Chest X %d Y %d Z %d", idx, 193 ,idz);
                    getServer().getWorld(world).getBlockAt(idx, OriginY + 1, idz).setType(Material.CHEST);
                    Chest chest = (Chest) getServer().getWorld(world).getBlockAt(idx, OriginY + 1, idz).getState();
                    setChest(chest);

                }
            }

        }
    }
    Runnable mCheckPlayerPositionRunnable = new Runnable() {
        @Override
        public void run() {
            for (Player p : getServer().getOnlinePlayers())
            {
                try {
                    Location loc = p.getLocation();
                    MazeNode node = getNodePlayerStandOn(loc.getBlockX(),loc.getBlockZ());
                    if(node.isWall)
                    {
                        loc.setY(loc.getY() - 1);
                        if(getServer().getWorld(world).getBlockAt(loc).getType() != Material.AIR)
                        {
                            //Player on the wall
                            if (!p.isOp())
                            {
                                //check whether Node at x-1 y-1 is not wall
                                int X = node.X - 1;
                                int Y = node.Y - 1;
                                while(maze.maze[X][Y].isWall) {
                                    X--;
                                    Y--;
                                }
                                node = maze.maze[X][Y];
                                loc.setX(OriginX + X * 3);
                                loc.setZ(OriginZ + Y * 3);
                                loc.setY(OriginY + 1);
                                p.sendMessage("禁止爬墙");
                                p.teleport(loc);
                            }
                        }
                    }
                }catch (IndexOutOfBoundsException e)
                {
                    //suppress the warning
                }
            }
        }
    };
    Runnable mMazeRunnable = new Runnable() {
        @Override
        public void run() {

            getServer().getScheduler().callSyncMethod(MazePlugin.this, (Callable<Void>) () -> {
                FileConfiguration config = MazePlugin.this.getConfig();
                int idz = 0;
                int light = OriginY + 5;
                maze = new Maze(xmax);
                maze.init();
                maze.generate(1,1);
                int xboundstart = OriginX;
                int xboundend = OriginX + 3 * maze.maze.length;
                int zboundstart = OriginZ;
                int zboundend = OriginZ + 3 * maze.maze.length;
                Location locvoid = new Location(getServer().getWorld(world),0,-64,0);
                for(Entity e : getServer().getWorld(world).getEntities()) {
                    if(e.getLocation().getX() > xboundstart && e.getLocation().getX() < xboundend && e.getLocation().getZ() > zboundstart && e.getLocation().getZ() < zboundend) {
                        if (e instanceof Monster) {
                            e.teleport(locvoid);
                        }else if (e instanceof Player) {
                            Player p = (Player) e;
                            p.sendMessage("本轮迷宫探险已经结束");
                            Location loc = p.getLocation();
                            if (loc.getX() >= OriginX + xmax * 6 - 3 && loc.getX() <= OriginX + xmax * 6 && loc.getZ() >= OriginZ + xmax * 6 - 3 && loc.getZ() < OriginZ + xmax * 6 && loc.getY() > OriginY) {
                                System.out.println("Finish");
                            } else {
                                Inventory inv = p.getInventory();
                                ItemStack[] stacks = inv.getContents();
                                System.out.println(stacks.length);
                                for(int i = 0; i < stacks.length;i++)
                                {
                                    if (stacks[i] == null) {
                                        //         System.out.println("MULL Stack");
                                    }
                                    else if (random.nextInt(10)< 7) {
                                        //        System.out.println("SET TO NULL");
                                        inv.setItem(i,null);
                                    }
                                }
                            }

                            if (p.getBedSpawnLocation() != null) {
                                p.teleport(p.getBedSpawnLocation());

                            } else {
                                p.teleport(getServer().getWorld(world).getSpawnLocation());
                            }
                        }
                    }
                }
                int posx = 0;
                int posy = 0;
                int posz = 0;
                for (MazeNode[] nodes : maze.maze) {
                    for (MazeNode node : nodes) {
                        if(checkNodeIsEdge(node) && node.isWall)
                        {
                            for(int i = 0; i < mazeHeight_Node;i++) {
                                for (int j = 0; j < 3; j++) { //3 X Outer Layer
                                    for (int k = 0; k < 3; k++) {   //3 X
                                        for (int l = 0; l < 3; l++) { //3 X
                                            posx = OriginX + node.X * 3 + k;
                                            posy = OriginY + i * 3 + j;
                                            posz = OriginZ + node.Y * 3 + l;
                                            getServer().getWorld(world).getBlockAt(posx, posy, posz).setType(Maze_Material_Wall);
                                        }
                                    }
                                }
                            }
                        }
                        else {
                            for (int j = 0; j < 3; j++) {
                                for (int k = 0; k < 3; k++) {
                                    posx = OriginX + node.X * 3 + j;
                                    posz = OriginZ + node.Y * 3 + k;
                                    getServer().getWorld(world).getBlockAt(posx, OriginY, posz).setType(Maze_Material_Wall);
                                    //assert we generate from 0,64,0 in practice
                                    for (int i = 0; i < 4; i++) {
                                        if (node.isWall) {
                                            getServer().getWorld(world).getBlockAt(posx, OriginY + 1 + i, posz).setType(Maze_Material_Wall);
                                        } else {
                                            getServer().getWorld(world).getBlockAt(posx, OriginY + 1 + i, posz).setType(Material.AIR);
                                        }
                                    }
                                    if (node.isWall) {
                                        getServer().getWorld(world).getBlockAt(posx, light, posz).setType(Maze_Material_Light);
                                        getServer().getWorld(world).getBlockAt(posx, OriginY + mazeHeight_Node * 3, posz).setType(Maze_Material_ROOF_B);
                                    } else {
                                        getServer().getWorld(world).getBlockAt(posx, light, posz).setType(Material.AIR);
                                        getServer().getWorld(world).getBlockAt(posx, OriginY + mazeHeight_Node * 3, posz).setType(Maze_Material_ROOF_A);
                                    }
                                }
                            }
                        }
                    }
                    //System.out.printf("Generated line #" + idz);
                    GenMobs(nodes);
                    random = new Random();
                    float percent = ((float)idz) / total_gen_lines * 100.0f;
                    for(Player p : getServer().getOnlinePlayers())
                    {
                        p.sendMessage("[" + format.format(percent) +  "%] 服务器正在努力的生成迷宫啦>_<");
                    }
                    getLogger().info("[" + format.format(percent) +  "%] 服务器正在努力的生成迷宫啦>_<");
                    idz++;
                }
                int ExitX =OriginX + maze.maze.length * 3 - 5;
                int Exitz =OriginZ + maze.maze.length * 3 - 5;
                //System.out.println("EXIT X,Z " + ExitX + " " + Exitz);
                getServer().getWorld(world).getBlockAt(ExitX,OriginY + 1,Exitz - 2).setType(Material.WOOD_BUTTON);
                getServer().getWorld(world).getBlockAt(ExitX,OriginY + 1,Exitz).setType(Material.CHEST);
                getServer().getWorld(world).getBlockAt(ExitX,OriginY + 1,Exitz + 1).setType(Material.CHEST);
                getServer().getWorld(world).getBlockAt(ExitX,OriginY + 1,Exitz + 1).setType(Material.CHEST);
                for (int i = 0; i < 5;i++)
                {
                    setChest((Chest) getServer().getWorld(world).getBlockAt(ExitX,OriginY + 1,Exitz).getState());
                    setChest((Chest) getServer().getWorld(world).getBlockAt(ExitX,OriginY + 1,Exitz + 1).getState());
                }
                getServer().getWorld(world).getBlockAt(OriginX + 5, OriginY + 1,OriginZ + 2).setType(Material.CHEST);
                getServer().getWorld(world).getBlockAt(OriginX + 5, OriginY + 2,OriginZ + 2).setType(Material.AIR);
                Chest chest = (Chest) getServer().getWorld(world).getBlockAt(OriginX + 5, OriginY + 1,OriginZ + 2).getState();
                Inventory inv = chest.getInventory();
                ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
                boots.setAmount(64);
                ItemStack helmets = new ItemStack(Material.LEATHER_HELMET);
                helmets.setAmount(64);
                ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS);
                leggings.setAmount(64);
                ItemStack chestp = new ItemStack(Material.LEATHER_CHESTPLATE);
                chestp.setAmount(64);
                ItemStack sword = new ItemStack(Material.IRON_SWORD);
                sword.setAmount(64);
                inv.setItem(1,boots);
                inv.setItem(2,helmets);
                inv.setItem(3,leggings);
                inv.setItem(4,chestp);
                inv.setItem(5,sword);

                for(Entity e : getServer().getWorld(world).getEntities()) {
                    if(e.getLocation().getX() > xboundstart && e.getLocation().getX() < xboundend && e.getLocation().getZ() > zboundstart && e.getLocation().getZ() < zboundend) {
                        if (e instanceof Item){
                            //System.out.println("clearing " + ((Item)e).getName());
                            Item item = (Item)e;
                            item.remove();
                        }
                    }
                }
                if(RepeatingTaskId == -1) {
                    RepeatingTaskId = getServer().getScheduler().scheduleSyncRepeatingTask(MazePlugin.this, mCheckPlayerPositionRunnable, 0, 10);
                }
                System.out.println("return");
                return null;
            });
        }
    };
    public void setChest(Chest chest)
    {
        Inventory inv = chest.getBlockInventory();
        Random random = new Random();
        int maxsize = 27;
        for(int j = 0; j < try_count_takaramono; j++) {
            for (int i = 0; i < gen_count_takaramono; i++) {
                int invnumber = random.nextInt(maxsize);
                ItemStack item = inv.getItem(invnumber);
                Material m;
                if (random.nextFloat() < chance_takaramono_rare)
                {
                    m = BonusItems_rare.get(random.nextInt(BonusItems_rare.size()));
                }
                else
                {
                    m = BonusItems.get(random.nextInt(BonusItems.size()));
                }
                if (item == null) {

                    if(m.toString().matches("SWORD|CHESTPLATE|HELMET|BOOTS|LEGGINGS|PICKAXE|BOW|BOOK"))
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
                } else {

                    if (item.getType() == m)
                    {
                        item.setAmount(item.getAmount() + random.nextInt(3));
                    }
                }
                inv.setItem(invnumber, item);
            }
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
                ((Player)sender).teleport(new Location(this.getServer().getWorld(world),OriginX + 4, OriginY + 1,OriginZ + 3));
                ((Player)sender).sendMessage("欢迎来到tengge的小迷宫，在这里开始你的冒险吧！");
                ((Player)sender).sendMessage("目标：走到迷宫的另一角。");
                ((Player)sender).sendMessage("说明：/Abort 放弃，身上所有物品将被剥夺，并被传回出生点");
                ((Player)sender).sendMessage("/Finish 当来到迷宫对角（相对入口）时，执行此命令将结束本局，所有人传送到出生位置并重置迷宫，最先完成的人和他的伙伴将得到身上的所有宝物，其他人身上的宝物有70%机会消失掉");
                ((Player)sender).sendMessage("建议组团搜刮");
            }
        }
        else if(cmd.getName().equals("Abort"))
        {
            if(sender instanceof Player) {
                int boundstart = OriginX;
                int boundend = OriginZ + 3 * maze.maze.length;
                Player e = (Player)sender;
                if(e.getLocation().getX() > boundstart && e.getLocation().getY() < boundend && e.getLocation().getZ() > boundstart && e.getLocation().getZ() < boundend)
                {
                    Inventory inv = e.getInventory();
                    for(int i = 0; i < inv.getSize();i++)
                    {
                        inv.setItem(i,new ItemStack(Material.AIR));
                    }
                    e.getEquipment().setItemInMainHand(new ItemStack(Material.AIR));
                    e.getEquipment().setBoots(new ItemStack(Material.AIR));
                    e.getEquipment().setChestplate(new ItemStack(Material.AIR));
                    e.getEquipment().setLeggings(new ItemStack(Material.AIR));
                    e.getEquipment().setHelmet(new ItemStack(Material.AIR));
                    e.getEquipment().setItemInOffHand(new ItemStack(Material.AIR));
                    if(e.getBedSpawnLocation() != null)
                    {
                        e.teleport(e.getBedSpawnLocation());
                    }
                    else
                    {
                        e.teleport(getServer().getWorld(world).getSpawnLocation());
                    }
                }
            }
        }
        else if(cmd.getName().equals("Finish"))
        {
            if(sender instanceof Player) {
                Location loc = ((Player) sender).getLocation();
                if (loc.getX() >= OriginX +  xmax * 6 - 3  && loc.getX() <=OriginX + xmax * 6 && loc.getZ() >= OriginZ + xmax * 6 - 3 && loc.getZ() < OriginZ + xmax * 6 && loc.getY() > OriginY)
                {
                    mMazeRunnable.run();
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
                sender.sendMessage("生成中，如果是第一次生成会卡服约10-20分钟，迷宫从无到有会消耗大量资源。如出现崩溃，清修改spigot.yml的timeout避免服务器报错。");
                mMazeRunnable.run();
            }
        }
        return true;
    }
    @Override
    public void onEnable() {
        BufferedReader br = null;
        try {
            this.saveDefaultConfig();
            this.saveConfig();
            String DataFolder = getDataFolder().getPath();
            saveResource("enchantments.txt", false);
            saveResource("takaramono_rare.txt", false);
            saveResource("spawner_egg_entities.txt", false);
            saveResource("takaramono.txt", false);
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
                    System.out.println(e.toString());
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
            FileConfiguration config = getConfig();
            chance_takaramono_rare = config.getDouble("Chance_Takaramono_rare");
            chance_takaramono = config.getDouble("Chance_Chest");
            chance_monster = config.getDouble("Chance_Monster");
            xmax = config.getInt("currentWidthX");
            OriginX = config.getInt("OriginX");
            OriginY = config.getInt("OriginY");
            OriginZ = config.getInt("OriginZ");
            world = config.getString("world");
            total_gen_lines = 2 * xmax;
            try_count_takaramono = config.getInt("Takaramono_try_count");
            gen_count_takaramono = config.getInt("Takaramono_generation_count");
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
                    maze = Maze.JSONDeSerialize(new File(DataFolder + File.separator + MazeSerialize_File));
                    if(maze != null)
                    {
                        RepeatingTaskId = getServer().getScheduler().scheduleSyncRepeatingTask(MazePlugin.this,mCheckPlayerPositionRunnable,0,10);
                    }
                    Logger.getLogger("MazePlugin").info("End of Async Load Maze from disk");
                }
            },0);

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
}
