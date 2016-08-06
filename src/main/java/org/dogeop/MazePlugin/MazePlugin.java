package org.dogeop.MazePlugin;
import com.boydti.fawe.bukkit.wrapper.AsyncWorld;
import com.boydti.fawe.util.TaskManager;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;
import org.dogeop.MazePlugin.IMaze.MazeFactory;
import java.io.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;
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
public final class MazePlugin extends JavaPlugin
{
    Map<String, Object> settings = new HashMap<String,Object>();
    ArrayList<BlockMeta> chestmeta = new ArrayList<BlockMeta>();
    Map<String, Object> Anti_hanging = new HashMap<String,Object>();
    Map<String,Boolean> OffLineMazePlayers = new HashMap<String,Boolean>();
    Map<String,ArrayList<Map<String,Object>>> MazePlayerItemStack = new HashMap<String,ArrayList<Map<String,Object>>>();
    private static Map<String,String> MazeTypes = new HashMap<String,String>();
    int lastxmax = 0;
    int xmax = 0;
    int OriginY = 0;
    int OriginX = 0;
    int OriginZ = 0;
    public static int RepeatingTaskId = -1;
    public static int RepeatingAntiHangId = -1;
    public static int RepeatingSerializeTaskId = -1;
    public static boolean busy = false;
    String MazeSerialize_File = "Maze.json";
    String CommmandPattern = "";
    String world = "world";
    NumberFormat format = NumberFormat.getInstance();
    Random random = new Random();
    IMaze maze = null;
    MazeListener listener = new MazeListener();

    class MazeListener implements Listener {
        @EventHandler
        public void onBlockPlace(BlockPlaceEvent event)
        {
            if(maze != null) {
                Location loc = event.getBlock().getLocation();
                //System.out.println(maze.checkIfInMaze(loc));
                if (maze.checkIfInMaze(loc)) {
                    if (event.getBlock().getY() > OriginY + 4) {
                        event.getPlayer().sendMessage("在这个位置放置方块将被视为作弊！");
                        event.setCancelled(true);
                        event.getPlayer().setHealth(0);
                        getServer().getWorld(world).createExplosion(loc, 5, true);
                    } else if (event.getBlock().getType() == Material.ENDER_CHEST) {
                        event.setCancelled(true);
                        event.getPlayer().sendMessage("不允许在迷宫设置这个方块");
                    }
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
            if(maze.checkIfInMaze(p.getLocation()))
            {
                OffLineMazePlayers.put(p.getUniqueId().toString(),false);
            }
        }
        @EventHandler
        public void onInterceptCommand(PlayerCommandPreprocessEvent event)
        {
            if(event.getMessage().split(" ")[0].matches(CommmandPattern))
            {
                //System.out.println(event.getMessage());
                if(!event.getPlayer().isOp())
                {
                    if(maze.checkIfInMaze(event.getPlayer().getLocation())) {
                        event.setMessage("/");
                        event.getPlayer().sendMessage("你的命令在迷宫禁止使用");
                    }
                }
            }
        }
        @EventHandler
        public void OnGhastProjectileLaunch(ProjectileLaunchEvent event)
        {
            if(maze != null) {
                ProjectileSource e = event.getEntity().getShooter();
                if (e instanceof Ghast && maze.checkIfInMaze(((Ghast) e).getLocation())) {
                    Fireball ball = (Fireball) event.getEntity();
                    float roll = 1 + 3 * random.nextFloat();
                    ball.setYield(roll);
                }
            }
        }
        @EventHandler
        public void OnDamage(EntityDamageByEntityEvent event)
        {
            if(maze != null) {
                //System.out.println("Hit!");
                if (event.getDamager() instanceof Player) {
                    Player p = (Player) event.getDamager();
                    if (maze.checkIfInMaze(p.getLocation())) {
                        Map<String, Object> status = (Map<String, Object>) Anti_hanging.get(p.getUniqueId().toString());
                        //System.out.println("interrupting");
                        status.put("interrupted", true);
                    }
                }
            }
        }
        @EventHandler
        public void onInventoryOpen(InventoryOpenEvent e)
        {
            if(maze != null)
            {
            //System.out.println("OpneInv");
                if(maze.checkIfInMaze(e.getPlayer().getLocation()))
                {
                    HashMap<String,Object> status = (HashMap<String, Object>) Anti_hanging.get(e.getPlayer().getUniqueId().toString());
                    status.put("interrupted",true);
                }
            }
        }
        public void AntiHang_onJoin (Player p) {
            if(Anti_hanging.get(p.getUniqueId().toString()) != null || maze == null) {
                return;
            }
            HashMap<String, Object> status = new HashMap<String,Object>();
            status.put("lastX",OriginX);
            status.put("lastZ",OriginZ);
            status.put("JoinTime",System.currentTimeMillis() / 1000);
            status.put("times",0);
            status.put("interrupted",false);
            if(status.get("WeaponTime") == null) {
                status.put("WeaponTime", 0L);
            }
            Anti_hanging.put(p.getUniqueId().toString(),status);
        }
    }

    Runnable Antihang = new Runnable() {
        @Override
        public void run() { //interval = 5s
            World w = getServer().getWorld(world);
            for(Player p : getServer().getOnlinePlayers())
            {
                if(!p.isDead() && maze.checkIfInMaze(p.getLocation()))
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
                        if(!interrupt) {
                            //System.out.println("Detected a hang!");
                            if ((X - lastX <= 9 && Z - lastZ <= 9) && times < 5) {
                                float roll = random.nextFloat();
                                if (roll < 0.4) {
                                    Creeper c = (Creeper) w.spawnEntity(p.getLocation(), EntityType.CREEPER);
                                    c.setPowered(true);
                                    status.put("times",times + 1);
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
                else if(busy)
                {
                    sender.sendMessage("新的一轮迷宫正在产生，还不可以开始游戏");
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
                HashMap<String,Object> status = (HashMap<String, Object>) Anti_hanging.get(p.getUniqueId().toString());
                status.put("JoinTime",System.currentTimeMillis() / 1000);
                p.getEquipment().setBoots(null);
                p.getEquipment().setChestplate(null);
                p.getEquipment().setLeggings(null);
                p.getEquipment().setHelmet(null);
                p.getEquipment().setItemInMainHand(null);
                p.getEquipment().setItemInOffHand(null);
                p.getEquipment().clear();
                if(System.currentTimeMillis() / 1000 - (long)status.get("WeaponTime")  > 150)
                {
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
                    status.put("WeaponTime",System.currentTimeMillis() / 1000);
                }


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
                Player e = (Player)sender;
                if(maze.checkIfInMaze(e.getLocation())) {
                    HashMap<String,Object> status = (HashMap<String, Object>) Anti_hanging.get(e.getUniqueId().toString());
                    if(System.currentTimeMillis() / 1000 - (long)status.get("JoinTime")  < 300)
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
                    TaskManager.IMP.async(new Runnable() {
                        @Override
                        public void run() {
                            AsyncWorld w = AsyncWorld.wrap(getServer().getWorld(world));
                            if (MazePlayerItemStack.get(e.getUniqueId().toString()) != null) {
                                synchronized (MazePlayerItemStack.get(e.getUniqueId().toString())) {
                                    for (Map<String, Object> item : MazePlayerItemStack.get(e.getUniqueId().toString())) {
                                        try {
                                            w.dropItem(loc, ItemStack.deserialize(item));
                                        } catch (IllegalArgumentException ex) {

                                        }
                                    }
                                    MazePlayerItemStack.remove(e.getUniqueId().toString());
                                }
                                w.commit();
                            }
                        }
                    });
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
                    TaskManager.IMP.async(new Runnable() {
                        @Override
                        public void run() {
                            maze = getMaze(xmax);
                            maze.GenBukkitWorld(MazePlugin.this, settings,true);
                        }
                    });
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
                if(busy)
                {
                    sender.sendMessage("新的一轮迷宫正在产生，不能在这期间重复执行");
                    return false;
                }
                sender.sendMessage("生成中，如果是第一次生成会卡服约10-20分钟，迷宫从无到有会消耗大量资源。如出现崩溃，清修改spigot.yml的timeout避免服务器报错。");
                TaskManager.IMP.async(new Runnable() {
                    @Override
                    public void run() {
                        maze = getMaze(xmax);
                        //System.out.println(maze.getClass());
                        maze.GenBukkitWorld(MazePlugin.this, settings,true);
                    }
                });
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
                        if(busy)
                        {
                            sender.sendMessage("新的一轮迷宫正在产生，不能在这期间重复执行");
                            return false;
                        }
                        config.set("currentWidthX",width);
                        config.save(getDataFolder() + File.separator + "config.yml");
                        xmax = width;
                        TaskManager.IMP.async(new Runnable() {
                            @Override
                            public void run() {
                                maze = getMaze(xmax);
                                maze.GenBukkitWorld(MazePlugin.this, settings,true);
                            }
                        });
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
        return true;
    }
    public IMaze getMaze(int xmax) {
        Object[] arr = MazeTypes.values().toArray();
        try {
            return ((Class<MazeFactory>)Class.forName(arr[random.nextInt(arr.length)] + "$Factory")).newInstance().GenMaze(xmax);
        } catch (InstantiationException e) {
            return null;
        } catch (IllegalAccessException e) {
            return null;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
    public void LoadConfig()
    {
        MazeTypes.put("DFS_RecursiveBT", "org.dogeop.MazePlugin.DFS_Recursive_Backtrack_Maze");
        MazeTypes.put("Aldous_Broder", "org.dogeop.MazePlugin.Aldous_Broder_Maze");
        MazeTypes.put("Random_Kruskal", "org.dogeop.MazePlugin.Random_Kruskal_Maze");
        ArrayList<Material> BonusItems = new ArrayList<Material>();
        ArrayList<Material> BonusItems_rare = new ArrayList<Material>();
        ArrayList<Enchantment> Enchantments = new ArrayList<Enchantment>();
        ArrayList<EntityType> Monsters = new ArrayList<EntityType>();
        BufferedReader br = null;
        try {
            this.saveDefaultConfig();
            this.saveConfig();
            //Beginning of Load ListConfig
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
           //System.out.println(regex);
            CommmandPattern = regex;
            br.close();

            //end of loading ListConfig
            FileConfiguration config = getConfig();


            xmax = config.getInt("currentWidthX");
            world = config.getString("world");
            //Global settings

            //new things
            //put to settings hashmap
            settings.put("BonusItems",BonusItems);
            settings.put("BonusItems_rare",BonusItems_rare);
            settings.put("Enchantments", Enchantments);
            settings.put("Monsters",Monsters);
            settings.put("Chance_Takaramono_rare",config.getDouble("Chance_Takaramono_rare"));
            settings.put("Chance_Chest",config.getDouble("Chance_Chest"));
            settings.put("Chance_Monster",config.getDouble("Chance_Monster"));
            settings.put("lastxmax",config.getInt("lastWidthX"));
            OriginX = config.getInt("OriginX");
            OriginY = config.getInt("OriginY");
            OriginZ = config.getInt("OriginZ");
            settings.put("OriginX",OriginX);
            settings.put("OriginZ",OriginZ);
            settings.put("OriginY",OriginY);
            settings.put("try_count_takaramono",config.getInt("Takaramono_try_count"));
            settings.put("gen_count_takaramono",config.getInt("Takaramono_generation_count"));
            settings.put("Maze_Material_Wall" , Material.BEDROCK);
            settings.put("Maze_Material_ROOF_B",  Material.GLASS);
            settings.put("Maze_Material_Light" ,Material.PUMPKIN);
            settings.put("world",config.getString("world"));


            //Beginning of Load Material of Maze
            System.out.println("Enabling Maze");
            try {
                settings.put("Maze_Material_Wall" , Material.valueOf(config.getString("Maze_Material_Wall")));
                settings.put("Maze_Material_ROOF_A", Material.valueOf(config.getString("Maze_Material_ROOF_A")));
                settings.put("Maze_Material_ROOF_B",  Material.valueOf(config.getString("Maze_Material_ROOF_B")));
                settings.put("Maze_Material_Light" ,Material.valueOf(config.getString("Maze_Material_Light")));
            }catch (IllegalArgumentException e)
            {
                e.printStackTrace();
            }
            catch (NullPointerException e)
            {
                e.printStackTrace();
            }
            //End

            //Async Load Maze on the Disk
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

                    maze = (IMaze)Abstract2DMaze.JSONDeSerialize(new File(DataFolder + File.separator + MazeSerialize_File));
                    maze.GenBukkitWorld(MazePlugin.this,settings,false);
                    //System.out.println(maze.getNode(30,30).isWall);
                    if(maze != null)
                    {
                        if(RepeatingTaskId == -1) {
                            RepeatingTaskId = getServer().getScheduler().scheduleSyncRepeatingTask(MazePlugin.this, new Runnable() {
                                @Override
                                public void run() {
                                    maze.CheckPlayerPosition(MazePlugin.this);
                                }
                            }, 0, 10);
                        }
                        if(RepeatingSerializeTaskId == -1) {
                            RepeatingSerializeTaskId = getServer().getScheduler().scheduleAsyncRepeatingTask(MazePlugin.this, mSerializePlayerInfo, 0, 150);
                            Logger.getLogger("MazePlugin").info("Starting Background Service : save Maze Data.....Done!");

                        }
                        if(RepeatingAntiHangId == -1 )
                        {
                            RepeatingAntiHangId = getServer().getScheduler().scheduleSyncRepeatingTask(MazePlugin.this,Antihang,0,400);
                        }
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
    public void onEnable() {
        System.out.println(getServer().getBukkitVersion());
        LoadConfig();

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
