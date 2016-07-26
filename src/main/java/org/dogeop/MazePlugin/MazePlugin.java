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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.*;
import java.util.concurrent.Callable;
class Prefab
{
    static int prefab_nochange = 0;
    static int prefab_air = 1;
    static int prefab_hook = 2;
    static int prefab_line = 3;
    static int prefab_redstonetorch = 4;
    static int prefab_launcher = 5;
    static JSONObject prefab;

    static {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(Prefab.class.getClassLoader().getResourceAsStream("prefabs.json")));
            String s = "";
            String readline = "";
            while(readline != null)
            {
                readline = reader.readLine();
                s += readline;
            }
            prefab = new JSONObject(s);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static JSONObject getPrefab(String key)
    {
        return prefab.getJSONObject(key);
    }
}
public final class MazePlugin extends JavaPlugin
{
    static ArrayList<Material> BonusItems = new ArrayList<Material>();
    static ArrayList<Enchantment> Enchacntments = new ArrayList<Enchantment>();
    static ArrayList<EntityType> Monsters = new ArrayList<EntityType>();
    static Pattern pat = Pattern.compile("(SWORD|CHESTPLATE|HELMET|BOOTS|LEGGINGS|PICKAXE|BOW|BOOK)");
    static {
        BonusItems.add(Material.DIAMOND);
        BonusItems.add(Material.DIAMOND_SWORD);
        BonusItems.add(Material.DIAMOND_CHESTPLATE);
        BonusItems.add(Material.DIAMOND_BOOTS);
        BonusItems.add(Material.DIAMOND_HELMET);
        BonusItems.add(Material.DIAMOND_LEGGINGS);
        BonusItems.add(Material.MINECART);
        BonusItems.add(Material.DIAMOND_BLOCK);
        BonusItems.add(Material.IRON_BLOCK);
        BonusItems.add(Material.IRON_ORE);
        BonusItems.add(Material.GOLD_BLOCK);
        BonusItems.add(Material.GOLD_ORE);
        BonusItems.add(Material.GOLDEN_APPLE);
        BonusItems.add(Material.GOLDEN_CARROT);
        BonusItems.add(Material.CHAINMAIL_BOOTS);
        BonusItems.add(Material.CHAINMAIL_CHESTPLATE);
        BonusItems.add(Material.CHAINMAIL_HELMET);
        BonusItems.add(Material.CHAINMAIL_LEGGINGS);
        BonusItems.add(Material.ANVIL);

        BonusItems.add(Material.PISTON_BASE);
        BonusItems.add(Material.PISTON_STICKY_BASE);
        BonusItems.add(Material.MONSTER_EGG);
        for(int i = 0; i < 10;i++) {
            //rare items
            BonusItems.add(Material.REDSTONE);
            BonusItems.add(Material.REDSTONE_BLOCK);
            BonusItems.add(Material.REDSTONE_COMPARATOR);
            BonusItems.add(Material.REDSTONE_LAMP_ON);
            BonusItems.add(Material.POTATO);
            BonusItems.add(Material.CARROT);
            BonusItems.add(Material.TNT);
            BonusItems.add(Material.POTION);
            BonusItems.add(Material.ARROW);
            BonusItems.add(Material.POWERED_RAIL);
            BonusItems.add(Material.RAILS);
            BonusItems.add(Material.BOW);
            BonusItems.add(Material.COAL);
            BonusItems.add(Material.COAL_BLOCK);
            BonusItems.add(Material.BREAD);
            BonusItems.add(Material.EGG);
            BonusItems.add(Material.SUGAR);
            BonusItems.add(Material.MELON_BLOCK);
            BonusItems.add(Material.PUMPKIN);
            BonusItems.add(Material.BOOK);
            BonusItems.add(Material.BOOKSHELF);
        }


        Enchacntments.add(Enchantment.ARROW_FIRE);
        Enchacntments.add(Enchantment.ARROW_DAMAGE);
        Enchacntments.add(Enchantment.ARROW_INFINITE);
        Enchacntments.add(Enchantment.ARROW_KNOCKBACK);
        Enchacntments.add(Enchantment.ARROW_FIRE);
        Enchacntments.add(Enchantment.ARROW_FIRE);
        Enchacntments.add(Enchantment.DIG_SPEED);
        Enchacntments.add(Enchantment.DEPTH_STRIDER);
        Enchacntments.add(Enchantment.DURABILITY);
        Enchacntments.add(Enchantment.PROTECTION_ENVIRONMENTAL);
        Enchacntments.add(Enchantment.FIRE_ASPECT);
        Enchacntments.add(Enchantment.PROTECTION_EXPLOSIONS);
        Enchacntments.add(Enchantment.PROTECTION_FALL);
        Enchacntments.add(Enchantment.PROTECTION_FIRE);
        Enchacntments.add(Enchantment.PROTECTION_PROJECTILE);
        Enchacntments.add(Enchantment.SILK_TOUCH);
        Enchacntments.add(Enchantment.LUCK);
        Enchacntments.add(Enchantment.MENDING);
        Enchacntments.add(Enchantment.FROST_WALKER);
        Enchacntments.add(Enchantment.LOOT_BONUS_BLOCKS);
        Enchacntments.add(Enchantment.LOOT_BONUS_MOBS);
        Monsters.add(EntityType.OCELOT);
        Monsters.add(EntityType.MUSHROOM_COW);
        Monsters.add(EntityType.LIGHTNING);
        Monsters.add(EntityType.CAVE_SPIDER);
        Monsters.add(EntityType.CREEPER);
        Monsters.add(EntityType.SNOWMAN);
        Monsters.add(EntityType.SLIME);
        Monsters.add(EntityType.WITHER_SKULL);
        Monsters.add(EntityType.GHAST);
        Monsters.add(EntityType.SQUID);
        Monsters.add(EntityType.SKELETON);
        Monsters.add(EntityType.IRON_GOLEM);
        Monsters.add(EntityType.RABBIT);
        Monsters.add(EntityType.VILLAGER);
    }
    int xmax = 0;
    int Y;
    Maze maze = null;
    Runnable mMazeRunnable = new Runnable() {
        @Override
        public void run() {
            float chance_takaramono = 0.02f;
            float chance_monster = 0.1f;
            float chance_trap = 0.2f;
            final int chance_monster_zombiepigman = 1;
            final int chance_monster_skeleton = 2;
            final int chance_monster_slime = 3;
            final int chance_monster_blaze = 4;
            final int chance_monster_killerbunny = 5;
            final int chance_monster_powercreeper = 6;
            final int chance_monster_wither_skeleton = 7;
            final int chance_monster_witch = 8;

            getServer().getScheduler().callSyncMethod(MazePlugin.this, (Callable<Void>) () -> {
                FileConfiguration config = MazePlugin.this.getConfig();
                int lastWX = config.getInt("lastWidthX");
                int lastWZ = config.getInt("lastWidthZ");
                xmax = config.getInt("currentWidthX");
                int zmax = config.getInt("currentWidthZ");
                int startx = config.getInt("OriginX");
                int startz = config.getInt("OriginZ");
                int idx = 0;
                Y = config.getInt("OriginY");
                int idz = 0;
                int light = Y + 3;
                int roof = Y + 4;
                Random random = new Random();
                maze = new Maze(xmax);
                maze.init();
                maze.generate(1,1);
                maze.maze[0][1].isWall = false;
                int xboundstart = startx;
                int xboundend = startx + 3 * maze.maze.length;
                int zboundstart = startz;
                int zboundend = startz + 3 * maze.maze.length;
                Location locvoid = new Location(getServer().getWorld("world"),0,-64,0);
                for(Entity e : getServer().getWorld("world").getEntities()) {
                    if(e.getLocation().getX() > xboundstart && e.getLocation().getX() < xboundend && e.getLocation().getZ() > zboundstart && e.getLocation().getZ() < zboundend) {
                        if (e instanceof Monster) {
                            e.teleport(locvoid);
                        }else if (e instanceof Player) {
                            Player p = (Player) e;
                            p.sendMessage("本轮迷宫探险已经结束");
                                Location loc = p.getLocation();
                                if (loc.getX() >= startx + xmax * 6 - 3 && loc.getX() <= startx + xmax * 6 && loc.getZ() >= startz + xmax * 6 - 3 && loc.getZ() < startz + xmax * 6 && loc.getY() > Y) {
                                    System.out.println("Finish");
                                } else {
                                    Inventory inv = p.getInventory();
                                    ItemStack[] stacks = inv.getContents();
                                    System.out.println(stacks.length);
                                    for(int i = 0; i < stacks.length;i++)
                                    {
                                        if (stacks[i] == null) {
                                            System.out.println("MULL Stack");
                                        }
                                        else if (random.nextInt(10)< 7) {
                                            System.out.println("SET TO NULL");
                                            inv.setItem(i,null);
                                        }
                                    }
                                }

                                if (p.getBedSpawnLocation() != null) {
                                    p.teleport(p.getBedSpawnLocation());

                                } else {
                                    p.teleport(getServer().getWorld("world").getSpawnLocation());
                                }
                        }
                    }
                }
                for (MazeNode[] nodes : maze.maze) {
                    for (MazeNode node : nodes) {
                        for (int j = 0; j < 3; j++) {
                            for (int k = 0; k < 3; k++) {
                                int posx = startx + idx * 3 + j;
                                int posz = startz + idz * 3 + k;
                                getServer().getWorld("world").getBlockAt(posx, Y, posz).setType(Material.BEDROCK);
                                if (!node.isWall) {
                                    getServer().getWorld("world").getBlockAt(posx, Y + 3, posz).setType(Material.SEA_LANTERN);
                                } else {
                                    getServer().getWorld("world").getBlockAt(posx, light, posz).setType(Material.LEAVES);
                                }
                                getServer().getWorld("world").getBlockAt(posx, roof, posz).setType(Material.BARRIER);

                                //assert we generate from 0,64,0 in practice
                                for (int i = 0; i < 2; i++) {
                                    if (node.isWall) {
                                        getServer().getWorld("world").getBlockAt(posx, Y + 1 + i, posz).setType(Material.BEDROCK);
                                    } else {
                                        getServer().getWorld("world").getBlockAt(posx, Y + 1 + i, posz).setType(Material.AIR);
                                    }
                                }
                            }
                        }
                        idx++;
                    }
                    System.out.printf("Generated line #" + idz);
                    idx = 0;
                    idz++;
                }
                //after generation,we visit it
                System.out.println("Phase 2");

                    for (MazeNode[] nodes : maze.maze) {
                        for (MazeNode node : nodes) {
                            node.visited = false;
                            if (!node.isWall) {
                            //    System.out.println("Generation Block : "  + node.toString());
                                idx = startx + node.X * 3 + 1;
                                idz = startz + node.Y * 3 + 1;
                                float roll = random.nextFloat();
                                if(roll < chance_takaramono)
                                {
                                    getServer().getWorld("world").getBlockAt(idx, Y + 1, idz).setType(Material.CHEST);
                                    Chest chest = (Chest) getServer().getWorld("world").getBlockAt(idx, Y + 1, idz).getState();
                                    setChest(chest);

                                }
                                else if(roll > chance_takaramono && roll < chance_monster) {
                                    int decide = 1 + random.nextInt(8);
                                    int count = 1 + random.nextInt(3);
                                    for (int i = 0; i < count; i++)
                                    {
                                        switch (decide)
                                        {
                                            case chance_monster_zombiepigman:
                                            {
                                                Zombie zombie = (Zombie) getServer().getWorld("world").spawnEntity(new Location(getServer().getWorld("world"),idx, Y + 1, idz),EntityType.ZOMBIE);
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
                                                getServer().getWorld("world").spawnEntity(new Location(getServer().getWorld("world"),idx, Y + 1, idz),EntityType.BLAZE);
                                            }break;
                                            case chance_monster_wither_skeleton:
                                            {
                                                Skeleton s = (Skeleton) getServer().getWorld("world").spawnEntity(new Location(getServer().getWorld("world"),idx, Y + 1, idz),EntityType.SKELETON);
                                                s.setSkeletonType(Skeleton.SkeletonType.WITHER);
                                                s.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
                                            }break;
                                            case chance_monster_killerbunny:
                                            {
                                                Rabbit rabbit = (Rabbit) getServer().getWorld("world").spawnEntity(new Location(getServer().getWorld("world"),idx, Y + 1, idz),EntityType.RABBIT);
                                                rabbit.setRabbitType(Rabbit.Type.THE_KILLER_BUNNY);
                                                rabbit.setMaxHealth(20);
                                                rabbit.setHealth(20);
                                            }break;
                                            case chance_monster_powercreeper:
                                            {
                                                Creeper creeper = (Creeper) getServer().getWorld("world").spawnEntity(new Location(getServer().getWorld("world"),idx, Y + 1, idz),EntityType.CREEPER);
                                                creeper.setPowered(true);
                                            }break;
                                            case chance_monster_slime:
                                            {
                                                getServer().getWorld("world").spawnEntity(new Location(getServer().getWorld("world"),idx, Y + 1, idz),EntityType.SLIME);

                                            }break;
                                            case chance_monster_skeleton:
                                            {
                                                getServer().getWorld("world").spawnEntity(new Location(getServer().getWorld("world"),idx, Y + 1, idz),EntityType.SKELETON);
                                            }break;
                                            case chance_monster_witch:
                                            {
                                                getServer().getWorld("world").spawnEntity(new Location(getServer().getWorld("world"),idx, Y + 1, idz),EntityType.WITCH);
                                            }break;
                                        }
                                    }

                                }
                                else if(roll > chance_monster && roll < chance_trap)
                                {

                                }
                            }
                        }
                    }
                int ExitX =startx + maze.maze.length * 3 - 5;
                int Exitz =startz + maze.maze.length * 3 - 5;
                //System.out.println("EXIT X,Z " + ExitX + " " + Exitz);
                getServer().getWorld("world").getBlockAt(ExitX,Y + 1,Exitz - 2).setType(Material.WOOD_BUTTON);
                getServer().getWorld("world").getBlockAt(ExitX,Y + 1,Exitz).setType(Material.CHEST);
                getServer().getWorld("world").getBlockAt(ExitX,Y + 1,Exitz + 1).setType(Material.CHEST);
                getServer().getWorld("world").getBlockAt(ExitX,Y + 1,Exitz + 1).setType(Material.CHEST);
                for (int i = 0; i < 5;i++)
                {
                    setChest((Chest) getServer().getWorld("world").getBlockAt(ExitX,Y + 1,Exitz).getState());
                    setChest((Chest) getServer().getWorld("world").getBlockAt(ExitX,Y + 1,Exitz + 1).getState());
                }
                idx = config.getInt("OriginX");
                idz = config.getInt("OriginZ");
                getServer().getWorld("world").getBlockAt(idx + 5, Y + 1,idz + 2).setType(Material.CHEST);
                Chest chest = (Chest) getServer().getWorld("world").getBlockAt(idx + 5, Y + 1,idz + 2).getState();
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

                for(Entity e : getServer().getWorld("world").getEntities()) {
                    if(e.getLocation().getX() > xboundstart && e.getLocation().getX() < xboundend && e.getLocation().getZ() > zboundstart && e.getLocation().getZ() < zboundend) {
                        if (e instanceof Item){
                            System.out.println("clearing " + e.toString());
                            Item item = (Item)e;
                            item.remove();
                        }
                    }
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
        for(int j = 0; j < 2; j++) {
            for (int i = 0; i < 5; i++) {
                int invnumber = random.nextInt(maxsize);
                ItemStack item = inv.getItem(invnumber);
                Material m = BonusItems.get(random.nextInt(BonusItems.size()));
                if (item == null) {

                    if(m.toString().matches("SWORD|CHESTPLATE|HELMET|BOOTS|LEGGINGS|PICKAXE|BOW|BOOK"))
                    {
                        item = new ItemStack(m);
                        for (int k = 0; k < random.nextInt(4); k++) {
                            try {
                                item.addEnchantment(Enchacntments.get(random.nextInt(Enchacntments.size())), random.nextInt(5));
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
        int startx = config.getInt("OriginX");
        int Y = config.getInt("OriginY");
        int startz = config.getInt("OriginZ");
        if(cmd.getName().equals("StartMazeTour"))
        {
            if(sender instanceof Player)
            {
                ((Player) sender).teleport(new Location(this.getServer().getWorld("world"),startx + 4, Y + 1,startz + 3));
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
                int boundstart = startx;
                int boundend = startz + 3 * maze.maze.length;
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
                        e.teleport(getServer().getWorld("world").getSpawnLocation());
                    }
                }
            }
        }
        else if(cmd.getName().equals("Finish"))
        {
            if(sender instanceof Player) {
                Location loc = ((Player) sender).getLocation();
                if (loc.getX() >= startx +  xmax * 6 - 3  && loc.getX() <=startx + xmax * 6 && loc.getZ() >= startz + xmax * 6 - 3 && loc.getZ() < startz + xmax * 6 && loc.getY() > Y)
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
    public void onEnable()
    {
        this.saveDefaultConfig();
        this.saveConfig();
        System.out.println("Enabling Maze");
    }
    @Override
    public void onDisable()
    {

    }
}
