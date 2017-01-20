package net.cpprograms.minecraft.TravelPortals;

import net.cpprograms.minecraft.General.CommandHandler;
import net.cpprograms.minecraft.General.PermissionsHandler;
import net.cpprograms.minecraft.General.PluginBase;
import java.io.*;
import java.util.ArrayList;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;

/**
 * TravelPortals Bukkit port.
 *
 * @author cppchriscpp
 */
public class TravelPortals extends PluginBase {
	/**
	 * A Player listener
	 */
	private final TravelPortalsPlayerListener playerListener = new TravelPortalsPlayerListener(this);

	/**
	 * A Block listener.
	 */
	private final TravelPortalsBlockListener blockListener = new TravelPortalsBlockListener(this);

	/**
	 * All user warp points
	 */
	public ArrayList<WarpLocation> warpLocations = new ArrayList<WarpLocation>();

	/**
	 * The type of block portals must be made from. (Default is 49 (Obsidian))
	 */
	protected Material blocktype = Material.OBSIDIAN;

	/**
	 * A friendly name for the construction block type.
	 */
	protected String strBlocktype = "obsidian";

	/**
	 * Whether to automatically export the list of portals to travelportals.txt.
	 */
	protected boolean autoExport = false;

	/**
	 * Server access!
	 */
	public static Server server;

	/**
	 * The numerical type of block used inside the portal. This would be 90, but portal blocks stink.
	 */
	protected Material portaltype = Material.WATER;

	/**
	 * The list of types of blocks used for the doorway
	 */
	protected List<Material> doortypes = Arrays.asList(
			Material.WOODEN_DOOR,
			Material.IRON_DOOR_BLOCK,
			Material.ACACIA_DOOR,
			Material.BIRCH_DOOR,
			Material.DARK_OAK_DOOR,
			Material.JUNGLE_DOOR,
			Material.SPRUCE_DOOR
	);

	/**
	 *  A friendly name for the door block type.
	 */
	protected String strDoortype = "door";

	/**
	 * A friendly name for the type of block used for the torch.
	 */
	protected String strTorchtype = "redstone torch";

	/**
	 * The type of block used for the torch at the bottom.
	 */
	protected Material torchtype = Material.REDSTONE_TORCH_ON;

	/**
	 * Do we want to use the permissions plugin?
	 */
	protected boolean usepermissions = false;

	/**
	 * How long until a portal reactivates?
	 */
	protected int cooldown = 5000;

	/**
	 * How many backup saves should there be? (3)
	 */
	protected int numsaves = 3;

	/**
	 * Ticks for a sync repeated task to check all players for if they are in a portal.
	 */
	protected int mainTicks = 17;

	/**
	 * Ticks for checking for the target desination and the teleport after the player was found in a portal.
	 * Set to <= 0 for immediate teleport.
	 */
	protected int followTicks = 7;

	/**
	 * Flag indicating if playerMove should be used or not.
	 */
	protected boolean usePlayerMove = true;

	/**
	 * The task for the alternate checking method for if players are in a portal etc.
	 */
	private PortalUseTask useTask = null;


	/**
	 * Called upon enabling the plugin
	 */
	@Override
	@SuppressWarnings({ "unchecked" })
	public void onEnable() {

		server = getServer();

		// Read in the YAML config stuff
		try
		{
			FileConfiguration conf = getConfig();
			if (conf.contains("frame")) {
				int frameId = conf.getInt("frame");
				if (frameId != 0) {
					blocktype = Material.getMaterial(frameId);
				} else {
					blocktype = Material.valueOf(conf.getString("frame").toUpperCase());
				}
			}
			if (conf.contains("framename"))
				strBlocktype = conf.getString("framename");
			if (conf.contains("fill")) {
				int fillId = conf.getInt("fill");
				if (fillId != 0) {
					portaltype = Material.getMaterial(fillId);
				} else {
					portaltype = Material.valueOf(conf.getString("fill").toUpperCase());
				}
			}
			if (conf.contains("doorlist")) {
				List<Material> doorList = new ArrayList<Material>();
				for (int doorId : conf.getIntegerList("doorlist")) {
					Material door = Material.getMaterial(doorId);
					if (door != null) {
						doorList.add(door);
					}
				}
				for (String doorType : conf.getStringList("doorlist")) {
					doorList.add(Material.valueOf(doorType.toUpperCase()));
				}
				if (!doorList.isEmpty()) {
					doortypes = doorList;
				}
			} else {
				if (conf.contains("door")) {
					int doorId = conf.getInt("door");
					if (doorId != 0) {
						doortypes.add(Material.getMaterial(doorId));
					} else {
						doortypes.add(Material.valueOf(conf.getString("door").toUpperCase()));
					}
				}
				if (conf.contains("door2")) {
					int doorId = conf.getInt("door2");
					if (doorId != 0) {
						doortypes.add(Material.getMaterial(doorId));
					} else {
						doortypes.add(Material.valueOf(conf.getString("door2").toUpperCase()));
					}
				}
				// Yes, this is a bit lame. I'd like to save updated config, but at the same time I don't want to wipe out comments and make
				// the file extremely unclear. 
				logWarning("Old style door configuration found. Config loaded correctly, but you may want to update it.");
				logWarning("The plugin now supports an integer list \"doorlist\", to allow use of the new wooden door types.");
				logWarning("Example configuration here: https://github.com/cppchriscpp/TravelPortals/blob/master/config.yml");
			}
			if (conf.contains("doorname"))
				strDoortype = conf.getString("doorname");
			if (conf.contains("torch")) {
				int torchId = conf.getInt("torch");
				if (torchId != 0) {
					torchtype = Material.getMaterial(torchId);
				} else {
					torchtype = Material.valueOf(conf.getString("torch").toUpperCase());
				}
			}
			if (conf.contains("torchname"))
				strTorchtype = conf.getString("torchname");
			if (conf.contains("permissions"))
				usepermissions = conf.getBoolean("permissions");
			if (conf.contains("autoexport"))
				autoExport = conf.getBoolean("autoexport");
			if (conf.contains("cooldown"))
				cooldown = 1000 * conf.getInt("cooldown");
			if (conf.contains("numsaves"))
				numsaves = conf.getInt("numsaves");

			if (conf.contains("useplayermove"))
				usePlayerMove = conf.getBoolean("useplayermove");
			if (conf.contains("polling-mainticks"))
				mainTicks = conf.getInt("polling-mainticks");
			if (conf.contains("polling-followticks"))
				followTicks = conf.getInt("polling-followticks");

		}
		catch (java.lang.NumberFormatException i)
		{
			logSevere("An exception occurred when trying to read your config file.");
			logSevere("Check your config.yml!");
			return;
		}
		catch (java.lang.IllegalArgumentException e) {
			logSevere("An exception occurred when trying to read a block type from your config file.");
			logSevere("Check your config.yml!");
			return;
		}

		if (!this.getDataFolder().exists())
		{
			logSevere("Could not read plugin's data folder! Please put the TravelPortals folder in the plugins folder with the plugin!");
			logSevere("Aborting plugin load");
			return;
		}

		try
		{

			// Save backup directory?
			if (!(new File(this.getDataFolder(), "backups")).exists())
				(new File(this.getDataFolder(), "backups")).mkdir();

			// Move the save file to where it belongs.
			// if it's done the old way.
			if (!(new File(this.getDataFolder(), "TravelPortals.ser").exists()))
			{
				// Moving time. Otherwise we just need a new file.
				if ((new File("TravelPortals.ser")).exists())
				{
					(new File("TravelPortals.ser")).renameTo(new File(this.getDataFolder(), "TravelPortals.ser"));
				}
			}
		}
		catch (SecurityException i)
		{
			logSevere("Could not read/write TravelPortals data folder! Aborting.");
			return;
		}



		// Attempt to read in the current version's save data.
		try
		{
			FileInputStream fIn = new FileInputStream(new File(this.getDataFolder(), "TravelPortals.ser"));
			ObjectInputStream oIn = new ObjectInputStream(fIn);
			warpLocations = (ArrayList<net.cpprograms.minecraft.TravelPortals.WarpLocation>)oIn.readObject();
			oIn.close();
			fIn.close();

			doBackup();
		}
		catch (IOException i)
		{
			logWarning("Could not load TravelPortals location file!");
			logWarning("If this is your first time running the plugin, you can ignore this message.");
			logWarning("If this is not your first run, STOP YOUR SERVER NOW! You could lose your portals!");
			logWarning("The file plugins/TravelPortals/TravelPortals.ser is missing or unreadable.");
			logWarning("Please check that this file exists and is in the right directory.");
			logWarning("If it is not, there should be a backup in the plugins/TravelPortals/backups folder.");
			logWarning("Copy this, place it in the plugins/TravelPortals folder, and rename it to TravelPortals.ser and restart the server.");
			logWarning("If this does not fix the problem, or if something strange went wrong, please report this issue.");
		}
		catch (java.lang.ClassNotFoundException i) 
		{
			logSevere("TravelPortals: Something has gone very wrong. Please contact admin@cpprograms.net!");
			return;
		}

		// Test to see if we need to do conversion.
		try 
		{
			if (!warpLocations.isEmpty()) {
				WarpLocation w = warpLocations.get(0);
				w.getName();
			}
		} 
		catch (java.lang.ClassCastException i)
		{
			{
				logInfo("Importing old pre-2.0 portals...");
				try 
				{
					warpLocations = new ArrayList<net.cpprograms.minecraft.TravelPortals.WarpLocation>();
					FileInputStream fIn = new FileInputStream(new File(this.getDataFolder(), "TravelPortals.ser"));
					ObjectInputStream oIn = new ObjectInputStream(fIn);
					ArrayList<com.bukkit.cppchriscpp.TravelPortals.WarpLocation> oldloc = (ArrayList<com.bukkit.cppchriscpp.TravelPortals.WarpLocation>)oIn.readObject();
					for (com.bukkit.cppchriscpp.TravelPortals.WarpLocation wl : oldloc) 
					{
						// Yes, this blows.
						net.cpprograms.minecraft.TravelPortals.WarpLocation temp = new WarpLocation(wl.getX(), wl.getY(), wl.getZ(), wl.getDoorPosition(), wl.getWorld(), wl.getOwner());
						temp.setName(wl.getName());
						temp.setDestination(wl.getDestination());
						temp.setHidden(wl.getHidden());
						warpLocations.add(temp);
					}
					oIn.close();
					fIn.close();

					this.savedata();

					this.doBackup();
					logInfo("Imported old portals sucecessfully!");
				} 
				catch (IOException e) {
					logWarning("Importing old portals failed.");
					return;
				}
				catch (ClassNotFoundException e) 
				{
					logSevere("Something has gone horribly wrong. Contact owner@cpprograms.net!");
					return;
				}
			}
		}

		// Register our events
		PluginManager pm = getServer().getPluginManager();

		if (this.usePlayerMove)
			pm.registerEvents(playerListener, this);
		else
		{
			if (this.useTask != null)
				this.useTask.cancel();

			this.useTask = new PortalUseTask(this);
			if (!this.useTask.register())
			{
				logSevere("Failed to register portal use task. Falling back to old PlayerMove style.");
				pm.registerEvents(playerListener, this);
				this.useTask = null;
			}

		}

		pm.registerEvents(blockListener, this);

		super.onEnable();
		// Override PermissionsHandler with our variable. Have to do this after the parent method
		permissions = new PermissionsHandler(usepermissions);
		commandHandler = new CommandHandler(this, PortalCommandSet.class);
	}

	/**
	 * Called upon disabling the plugin.
	 */
	@Override
	public void onDisable() {
		savedata();
		super.onDisable();
	}

	/**
	 * Saves all door warp stuff to disk.
	 */
	public void savedata()
	{
		savedata(false);
	}

	/**
	 * Saves all portals to disk.
	 * @param backup Whether to save to a backup file or not.
	 */
	public void savedata(boolean backup)
	{
		try
		{
			File file = new File(this.getDataFolder(), "TravelPortals.ser");
			if (backup)
			{
				DateFormat df = new SimpleDateFormat("yyyy-MM-dd--kk_mm");
				file = new File(this.getDataFolder(), "backups/TravelPortals--" + df.format(Calendar.getInstance().getTime()));
			}
			FileOutputStream fOut = new FileOutputStream(file);
			ObjectOutputStream oOut = new ObjectOutputStream(fOut);
			oOut.writeObject(warpLocations);
			oOut.close();
			fOut.close();
		}
		catch (IOException i)
		{
			logWarning("Could not save TravelPortals data!");
		}

		if (autoExport)
			dumpPortalList();
		if (!backup)
			doBackup();
	}

	/**
	 * Makes a backup of the TravelPortals file in case something goes wrong.
	 * - This is for the paranoid, though there are a few isolated cases of files disappearing.
	 *   I can't find a source, so this is at least a quick fix.
	 */
	public void doBackup()
	{
		try
		{
			File dir = new File(this.getDataFolder(), "backups");
			if (!dir.isDirectory())
			{
				logSevere("Cannot save backups!");
				return;
			}
			String[] list = dir.list();
			if (numsaves > 0 && list.length+1 > numsaves)
			{
				java.util.Arrays.sort(list);
				for (int i = 0; i < list.length+1-numsaves; i++)
					(new File(dir, list[i])).delete();
			}

		}
		catch (SecurityException i)
		{
			logWarning("Saving a backup of the TravelPortals file failed.");
		}
		savedata(true);
	}

	/**
	 * Quick helper function to add warp points.
	 * @param w The warp point to add to warpLocations.
	 */
	public void addWarp(WarpLocation w)
	{
		warpLocations.add(w);
	}

	/**
	 * Get the index of a warp object from its name.
	 * @param name The name of the portal to find.
	 * @return The index of the portal in plugin.warpLocations, or -1 if it is not found.
	 */
	public int getWarp(String name)
	{
		// Test all warps to see if they have the name we're looking for.
		for (int i = 0; i < this.warpLocations.size(); i++)
		{
			if (this.warpLocations.get(i).getName().equals(name))
				return i;
		}
		return -1;
	}

	/**
	 * Find a warp point from a relative location. (Within 1 block)
	 * @param worldname The world name to get the warp from.
	 * @param x X coordinate to search near.
	 * @param y Y coordinate to search near.
	 * @param z Z coordinate to search near.
	 * @return The index of a nearby portal in plugin.warpLocations, or -1 if it is not found.
	 */
	public int getWarpFromLocation(String worldname, int x, int y, int z)
	{
		// Iterate through all warps and check how close they are
		for (int i = 0; i < this.warpLocations.size(); i++)
		{
			WarpLocation wd = this.warpLocations.get(i);
			if (wd.getY() == y && wd.getWorld().equals(worldname))
			{
				// We found one!!
				if (Math.abs(wd.getX() - x) <= 1 && Math.abs(wd.getZ() - z) <= 1)
					return i;
			}
		}
		return -1;
	}

	/**
	 * This is a function to rename a world for all existing portals. 
	 * @param oldworld The name of the old world.
	 * @param newworld The name of the new world.
	 */
	public void renameWorld(String oldworld, String newworld)
	{
		for (int i = 0; i < this.warpLocations.size(); i++) 
		{
			if (this.warpLocations.get(i).getWorld().equals(oldworld))
				this.warpLocations.get(i).setWorld(newworld);
		}
	}

	/**
	 * Delete all portals linking to a world.
	 * @param oldworld The world to delete all portals to.
	 */
	public void deleteWorld(String oldworld) 
	{
		for (int i = 0; i < this.warpLocations.size(); i++)
		{
			if (this.warpLocations.get(i).getWorld().equals(oldworld))
			{
				this.warpLocations.remove(i);
				i--;
			}
		}
	}

	/**
	 * Dump the portal list to a text file for parsing.
	 */
	public void dumpPortalList()
	{
		try
		{
			FileOutputStream fOut = new FileOutputStream(new File(this.getDataFolder(), "travelportals.txt"));
			PrintStream pOut = new PrintStream(fOut);
			for (WarpLocation w : this.warpLocations)
				pOut.println(w.getX() + "," + w.getY() + "," + w.getZ() + "," + w.getName() + "," + w.getDestination() + "," + w.isHidden() +"," + w.getWorld() + "," + w.getOwner());

			pOut.close();
			fOut.close();
		}
		catch (Exception e)
		{
		}
	}

	/**
	 * Import existing portals from a given txt file into the world. THIS WIPES OUT EXISTING PORTALS. You have been warned.
	 * @param file The file to import from.
	 */
	public void importPortalList(File file)
	{
		try
		{
			this.warpLocations = new ArrayList<WarpLocation>();
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;
			while ((line = br.readLine()) != null)
			{
				String[] data = line.split(",");
				if (data.length != 8)
					continue;
				WarpLocation warp = new WarpLocation(Integer.parseInt(data[0]), Integer.parseInt(data[1]), Integer.parseInt(data[2]), 0, data[6], data[7]);
				warp.setHidden(data[5] == "1");
				warp.setDestination(data[4]);
				warp.setName(data[3]);
				this.warpLocations.add(warp);
			}
			br.close();
		}
		catch (Exception e)
		{
			this.logSevere("Unable to load portal list from "+file.getName()+"!");
			this.logDebug(e.getMessage());
		}
	}

	/**
	 * Gets the destination of this teleport, but only if the user can use it.
	 * @param player The player in question.
	 * @param disablePortal Disable this portal (set its cooldown).
	 * @return The location to warp to, or null if there is no warping to be done.
	 */
	public Location getWarpLocationIfAllowed(Player player, boolean disablePortal)
	{
		if (!permissions.hasPermission(player, "travelportals.portal.use"))
			return null;

		Location playerLoc = player.getLocation();
		playerLoc = playerLoc.add(playerLoc.getDirection());
		playerLoc.setY(player.getLocation().getY());
		Block blk = player.getWorld().getBlockAt(player.getLocation());

		Material blockType = player.getWorld().getBlockAt(playerLoc).getType();
		// Is the user actually in portal material?
		if (blockType == blocktype || doortypes.contains(blockType))
		{
			// Find nearby warp.
			int w = getWarpFromLocation(player.getWorld().getName(), blk.getLocation().getBlockX(), blk.getLocation().getBlockY(), blk.getLocation().getBlockZ());
			if (w == -1)
				return null;

			if (!warpLocations.get(w).isUsable(cooldown))
			{
				return null;
			}

			// Ownership check
			if (usepermissions) 
			{
				if (!permissions.hasPermission(player, "travelportals.portal.use")) 
				{
					player.sendMessage(ChatColor.DARK_RED + "You do not have permission to use portals.");
					return null;
				}

				if (!warpLocations.get(w).getOwner().equals("") && !warpLocations.get(w).getOwner().equals(player.getName()))
				{
					if (!permissions.hasPermission(player, "travelportals.admin.portal.use")) 
					{
						player.sendMessage(ChatColor.DARK_RED + "You do not own this portal, so you cannot use it.");
						return null;
					}
				}
			}

			// Complain if this isn't usable
			if (!warpLocations.get(w).hasDestination())
			{
				if ((!permissions.hasPermission(player, "travelportals.command.warp") || (!warpLocations.get(w).getOwner().equals("") && !warpLocations.get(w).getOwner().equals(player.getName()))))
				{
					player.sendMessage(ChatColor.DARK_RED + "This portal has no destination.");
				}
				else
				{
					player.sendMessage(ChatColor.DARK_RED + "You need to set this portal's destination first!");
					player.sendMessage(ChatColor.DARK_GREEN + "See /portal help for more information.");
				}
				warpLocations.get(w).setLastUsed();
				return null;
			}
			else // send the user on his way!
			{
				// Find the warp this one points to
				int loc = getWarp(warpLocations.get(w).getDestination());

				if (loc == -1)
				{
					player.sendMessage(ChatColor.DARK_RED + "This portal's destination (" + warpLocations.get(w).getDestination() + ") does not exist.");
					if (!(permissions.hasPermission(player, "travelportals.command.warp")))
						player.sendMessage(ChatColor.DARK_GREEN + "See /portal help for more information.");

					warpLocations.get(w).setLastUsed();
					return null;
				}
				else
				{

					// Another permissions check...
					if (!permissions.hasPermission(player, "travelportals.admin.portal.use")) 
					{

						if (!warpLocations.get(w).getOwner().equals("") && !warpLocations.get(w).getOwner().equals(player.getName()))
						{
							player.sendMessage(ChatColor.DARK_RED + "You do not own the destination portal, and do not have permission to use it.");
							return null;
						}
					}
					int x = warpLocations.get(loc).getX();
					int y = warpLocations.get(loc).getY();
					int z = warpLocations.get(loc).getZ();
					float rotation = 180.0f; // c

					// Use rotation to place the player correctly.
					int d = warpLocations.get(loc).getDoorPosition();

					if (d > 0)
					{
						if (d == 1)
							rotation = 270.0f;
						else if (d == 2)
							rotation = 0.0f;
						else if (d == 3)
							rotation = 90.0f;
						else
							rotation = 180.0f;
					}
					// Guess.
					else if (doortypes.contains(player.getWorld().getBlockAt(x + 1, y, z).getTypeId()))
					{
						rotation = 270.0f;
						warpLocations.get(loc).setDoorPosition(1);
					}
					else if (doortypes.contains(player.getWorld().getBlockAt(x, y, z+1).getTypeId()))
					{
						rotation = 0.0f;
						warpLocations.get(loc).setDoorPosition(2);
					}
					else if (doortypes.contains(player.getWorld().getBlockAt(x - 1, y, z).getTypeId()))
					{
						rotation = 90.0f;
						warpLocations.get(loc).setDoorPosition(3);
					}
					else if (doortypes.contains(player.getWorld().getBlockAt(x, y, z-1).getTypeId()))
					{
						rotation = 180.0f;
						warpLocations.get(loc).setDoorPosition(4);
					}
					else
					{
						// oh noes :<
					}
					// Create the location for the user to warp to
					Location locy = new Location(player.getWorld(), x + 0.50, y + 0.1, z + 0.50, rotation, 0);
					if (warpLocations.get(loc).getWorld() != null && !warpLocations.get(loc).getWorld().equals(""))
					{
						World wo = WorldCreator.name(warpLocations.get(loc).getWorld()).createWorld();
						locy.setWorld(wo);
					}
					else
					{
						logWarning("World name not set for portal " + warpLocations.get(loc).getName() + " - consider running the following command from the console:");
						logWarning("portal fixworld " + TravelPortals.server.getWorlds().get(0).getName());
						logWarning("Replacing the world name with the world this portal should link to, if it is incorrect.");
						locy.setWorld(TravelPortals.server.getWorlds().get(0));
					}

					if (disablePortal)
					{
						warpLocations.get(loc).setLastUsed();
						warpLocations.get(w).setLastUsed();
					}

					return locy;
				}
			}
		}
		return null;
	}

	/**
	 * Get a location for the user to warp to, if they are allowed to use it.
	 * @param player The player to warp.
	 * @return A location, or null if the user does not need to be teleported.
	 */
	public Location getWarpLocationIfAllowed(Player player) 
	{ 
		return getWarpLocationIfAllowed(player, true); 
	}
}

