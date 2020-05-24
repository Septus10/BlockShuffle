package septusten.plugins.blockshuffle;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;

// https://drive.google.com/drive/folders/1-s7SnMHnDyZpznmGNBOsRlN6FosYI6IK
// To see how this dude made his

public class BS_Main extends JavaPlugin 
{
	static {
		ConfigurationSerialization.registerClass(BS_Block.class, "BS_Block");
	}
	
	//=========================================================================
	// VIRTUAL FUNCTIONS
	//=========================================================================
	@Override
	public void onEnable() 
	{
		// Register an event listener
		getServer().getPluginManager().registerEvents(new BS_EventListener(this),  this);
		
		// Get the configuration
		FileConfiguration config = getConfig();
		saveDefaultConfig();		
		// Get the block whitelist from the configuration
		m_Blocks = (ArrayList<BS_Block>)config.getList("whitelisted-blocks");
	}
	
	@Override
	public void onDisable() 
	{
		m_Blocks = null;
		m_Players = null;
		m_bMatchHasStarted = false;
		m_RoundNumber = 1;
		BukkitScheduler scheduler = getServer().getScheduler();
		scheduler.cancelTask(m_TickTaskID);
		scheduler.cancelTask(m_GraceTaskID);
	}
	
	
	@Override
	public boolean onCommand(CommandSender sender,
						     Command command,
						     String label,
						     String[] args) 
	{
		if (args.length <= 0)
		{
			sender.sendMessage("Command not executed, please provide arguments with the command");
			return false;
		}
		
		if (command.getName().equalsIgnoreCase("blockshuffle")) 
		{		
			if (args[0].equalsIgnoreCase("start"))
			{
				if (!sender.isOp())
				{
					return false;
				}
				
				if (args.length > 1)
				{
					m_FirstRoundTime = Float.parseFloat(args[1]);
				}
				else
				{
					m_FirstRoundTime = (float)getConfig().getDouble("first-round-time");
				}
				
				if (args.length > 2)
				{
					m_MinRoundTime = Float.parseFloat(args[2]);
				}
				else
				{
					m_MinRoundTime = (float)getConfig().getDouble("min-round-time");
				}
				
				if (args.length > 3)
				{
					m_MaxRounds = Integer.parseInt(args[3]);
				}
				else
				{
					m_MaxRounds = getConfig().getInt("max-rounds");
				}
				
				if (args.length > 4)
				{
					m_GracePeriod = Float.parseFloat(args[4]);
				}
				else 
				{
					m_GracePeriod = (float)getConfig().getDouble("grace-period");
				}

				float m_TimeDiff = m_FirstRoundTime - m_MinRoundTime;
				m_RoundTimeDelta = m_TimeDiff / (m_MaxRounds - 1);
				
				startGracePeriod();
				return true;
			}
			else if (args[0].equalsIgnoreCase("stop"))
			{
				if (!sender.isOp()) 
				{
					return false;
				}
				stopMatch();
				return true;
			}
			else if (args[0].equalsIgnoreCase("join"))
			{
				if (sender instanceof Player)
				{
					addPlayerToMatch((Player)sender);
				}
				return true;
			}
			else if (args[0].equalsIgnoreCase("block"))
			{
				if (sender instanceof Player)
				{
					Player p = (Player)sender;
					UUID id = p.getUniqueId();
					if (m_Players.containsKey(id))
					{
						BS_MatchPlayer matchPlayer = m_Players.get(id);
						if (matchPlayer.getTargetBlock() != null)
						{
							String blockName = matchPlayer.getTargetBlock().toString();
							blockName = blockName.replace('_', ' ');
							blockName = blockName.toLowerCase();
							BS_MessageSender.sendMessage(p, ChatColor.GREEN + "Your current block is: " + ChatColor.GOLD + blockName);
						}
					}
					return true;
				}
			}
			else if (args[0].equalsIgnoreCase("timeleft"))
			{
				if (sender instanceof Player)
				{
					Player p = (Player)sender;
					float secondsLeft = getRoundSecondsLeft();
					BS_MessageSender.sendMessage(p, BS_Util.getTimeLeftString(secondsLeft));					
				}
				return true;
			}
		}
		return false;
	}
	
	public void onPlayerJoin(Player player)
	{
		AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
		maxHealth.setBaseValue(maxHealth.getDefaultValue());
		
		if (!m_bMatchHasStarted)
		{
			return;
		}
		
		UUID id = player.getUniqueId();
		if (m_InactivePlayers.containsKey(id))
		{
			// Grab match player class from inactive players list
			BS_MatchPlayer temp = m_InactivePlayers.get(id);
			m_InactivePlayers.remove(id); // remove it from inactive players list
			
			// Set new player reference
			temp.m_PlayerRef = player;
			
			// Set current player max health to what they had before they left.
			maxHealth.setBaseValue(temp.m_MaxHealth);
			
			// Send target block message
			temp.sendTargetBlockMessage();			
			
			// Put the player in the active players list
			m_Players.put(id, temp);			
			return;
		}
		
		BS_MessageSender.sendMessage(player, "You have joined during a round of blockshuffle, you can still join by using the command \"\\blockshuffle join\"");
	}
	
	public void onPlayerQuit(Player player)
	{
		if (!m_bMatchHasStarted)
		{
			return;
		}
		
		UUID id = player.getUniqueId();
		if (m_Players.containsKey(id))
		{
			m_InactivePlayers.put(id, m_Players.get(id));
			m_Players.remove(id);
		}
	}
	
	public boolean addPlayerToMatch(Player player)
	{
		if (!m_bIsInGracePeriod)
		{
			return false;
		}
		
		UUID id = player.getUniqueId();
		if (m_Players.containsKey(id))
		{
			return false;
		}
		
		BS_MatchPlayer matchPlayer = new BS_MatchPlayer(null, player);
		
		AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
		matchPlayer.m_MaxHealth = maxHealth.getDefaultValue();
		
		m_Players.put(id, matchPlayer);
		BS_MessageSender.sendMessage(player, "You have joined the match!");
		
		for (Player onlinePlayer :getServer().getOnlinePlayers())
		{
			UUID onlinePlayerId = onlinePlayer.getUniqueId();
			if (!m_Players.containsKey(onlinePlayerId))
			{
				return true;
			}
		}
		
		BS_MessageSender.sendBroadcast("Everyone has joined!");
		
		// If we get here it means that everyone joined
		// Stop the grace period
		BukkitScheduler scheduler = getServer().getScheduler();
		scheduler.cancelTask(m_GraceTaskID);
		
		// Start the match
		startMatch();
		
		return true;
	}
	
	public boolean removePlayerFromMatch(Player player)
	{
		UUID id = player.getUniqueId();
		if (!m_Players.containsKey(id))
		{
			return false;
		}
		
		AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
		maxHealth.setBaseValue(maxHealth.getDefaultValue());
		
		m_Players.remove(id);
		
		return true;
	}
	
	public void incrementRound()
	{
		m_RoundsPassed++;		
		for (BS_MatchPlayer p: getActivePlayers())
		{
			if (!p.m_FoundBlock)
			{
				AttributeInstance maxHealth = p.m_PlayerRef.getAttribute(Attribute.GENERIC_MAX_HEALTH);
				double newBaseValue = maxHealth.getDefaultValue() - (m_RoundNumber * 2);
				if (newBaseValue >= 1)
				{
					maxHealth.setBaseValue(newBaseValue);
					p.m_MaxHealth = newBaseValue;
				}
				else
				{
					maxHealth.setBaseValue(1);
				}
				
				double curHealth = p.m_PlayerRef.getHealth();
				if (curHealth > newBaseValue)
				{
					p.m_PlayerRef.setHealth(newBaseValue);
				}
				
				p.m_Fails++;
				p.m_PlayerRef.sendMessage(ChatColor.RED + "You didn't manage to get to your block in time!");
			}
			
			p.m_FoundBlock = false;
		}		
		
		if (m_RoundsPassed == m_MaxRounds)
		{
			finishMatch();
			return;
		}
		
		m_RoundNumber++;
		
		m_CurRoundTime -= m_RoundTimeDelta;
		
		for (BS_MatchPlayer p: getActivePlayers())
		{
			p.setTargetBlock(getRandomBlock());
			p.m_PlayerRef.setFoodLevel(20);
		}
		
		m_RoundStartTime = Instant.now();
		
		BS_MessageSender.sendBroadcast(ChatColor.BLUE + "Round " + ChatColor.LIGHT_PURPLE + m_RoundNumber + ChatColor.BLUE + " has started!");
		BS_MessageSender.sendBroadcast(BS_Util.getTimeLeftString(m_CurRoundTime));		
	}
	
	//=========================================================================
	// PRIVATE FUNCTIONS
	//=========================================================================
	private void startGracePeriod()
	{
		BukkitScheduler scheduler = getServer().getScheduler();
		BS_MessageSender.sendBroadcast("Starting grace period, match will start in " + ChatColor.GOLD + (int)m_GracePeriod + ChatColor.RESET + " minutes");
		BS_MessageSender.sendBroadcast("You can join the upcoming match by typing: " + ChatColor.GOLD + "\"/blockshuffle join\"");
		m_GraceTaskID = scheduler.scheduleSyncDelayedTask(this, new BS_GracePeriodRunnable(this), (long)((m_GracePeriod * 60.f) * 20.f));
		m_bIsInGracePeriod = true;
	}
	
	public void startMatch()
	{
		if (m_bMatchHasStarted) return;

		
		for (BS_MatchPlayer P: m_Players.values())
		{
			Material randomBlock = getRandomBlock();			
			P.setTargetBlock(randomBlock);
			P.m_PlayerRef.setFoodLevel(20);
		}
		
		BukkitScheduler scheduler = getServer().getScheduler();
		m_TickTaskID = scheduler.scheduleSyncRepeatingTask(this, new BS_MatchRunnable(this), 0L, 1L);
		
		m_bMatchHasStarted = true;
		m_bIsInGracePeriod = false;
		m_RoundStartTime = Instant.now();
		m_CurRoundTime = m_FirstRoundTime;
		
		BS_MessageSender.sendBroadcast(BS_Util.getTimeLeftString(m_CurRoundTime));	
	}
	
	private Material getRandomBlock()
	{
		Random random = new Random();
		
		int weightSum = 0;
		// Calculate weight sum
		for (int i = 0; i < m_Blocks.size(); ++i)
		{
			BS_Block block = m_Blocks.get(i);
			float exponent = (block.m_Weight - 1) / m_MaxRounds;
			weightSum += Math.max(1, (int)(block.m_Weight - (m_RoundNumber * exponent)));
		}
		
		int randomWeight = random.nextInt(weightSum);
		
		for (int i = 0; i < m_Blocks.size(); ++i)
		{
			BS_Block block = m_Blocks.get(i);
			if (randomWeight < block.m_Weight)
			{
				return block.m_Block;
			}
			randomWeight -= block.m_Weight;			
		}
		
		return null;
	}
	
	private void stopMatch()
	{
		if (!m_bMatchHasStarted) return;
		
		m_bMatchHasStarted = false;
		BS_MessageSender.sendBroadcast(ChatColor.DARK_RED + "Match was stopped!");
		
		handleEndMatchLogic();
	}
	
	private void finishMatch()
	{
		if (!m_bMatchHasStarted) return;
		
		m_bMatchHasStarted = false;
		BS_MessageSender.sendBroadcast(ChatColor.GOLD + "Match has finished!");
		
		handleEndMatchLogic();
	}
	
	enum MatchPlayerComparator implements Comparator<BS_MatchPlayer>
	{
		BY_WINS 
		{
			public int compare(BS_MatchPlayer p1, BS_MatchPlayer p2)
			{
				return p1.m_Successes - p2.m_Successes;
			}
		}
	}
	
	public static Comparator<BS_MatchPlayer> getComparator()
	{
		return new Comparator<BS_MatchPlayer>() {
			public int compare(BS_MatchPlayer p1, BS_MatchPlayer p2) {
				return p1.m_Successes + p2.m_Successes;
			}
		};
	}
	
	private void handleEndMatchLogic()
	{
		m_bIsInGracePeriod = false;
		
		BukkitScheduler scheduler = getServer().getScheduler();
		scheduler.cancelTask(m_TickTaskID);
		scheduler.cancelTask(m_GraceTaskID);
		
		// Sort the list by top player first
		Collection<BS_MatchPlayer> players = m_Players.values();
		List<BS_MatchPlayer> playersList = new ArrayList<BS_MatchPlayer>(players);
		Collections.sort(playersList, getComparator());
		
		BS_MessageSender.sendBroadcast(ChatColor.GREEN + "Here are the scores!");
		BS_MessageSender.sendBroadcast(ChatColor.GREEN + "============================================");
		
		//TODO Get top 3 players
		for (int i = 0; i < playersList.size(); ++i)
		{
			BS_MatchPlayer p = playersList.get(i);
			BS_MessageSender.sendBroadcast(ChatColor.DARK_GREEN + "#" + i + " " + ChatColor.YELLOW + p.m_PlayerRef.getDisplayName() + ChatColor.DARK_GREEN + " got " + ChatColor.GOLD + p.m_Successes + ChatColor.DARK_GREEN + " wins and " + ChatColor.DARK_RED + p.m_Fails + ChatColor.DARK_GREEN + " losses");
			AttributeInstance maxHealth = p.m_PlayerRef.getAttribute(Attribute.GENERIC_MAX_HEALTH);
			maxHealth.setBaseValue(maxHealth.getDefaultValue());
		}
		
		BS_MessageSender.sendBroadcast(ChatColor.GREEN + "============================================");
		
		m_Players.clear();
		m_InactivePlayers.clear();
	}
	
	//=========================================================================
	// PRIVATE MEMBERS
	//=========================================================================
	private boolean m_bMatchHasStarted = false;
	private boolean m_bIsInGracePeriod = false;
	private float m_GracePeriod = 5.f;
	private float m_FirstRoundTime = 10.f;
	private float m_MinRoundTime = 3.f;
	private float m_CurRoundTime;
	private float m_RoundTimeDelta;
	private Instant m_RoundStartTime;
	private int m_TickTaskID = -1;
	private int m_GraceTaskID = -1;
	private int m_RoundNumber = 1;
	private int m_RoundsPassed = 0;
	private int m_MaxRounds = 15;
	private ArrayList<BS_Block> m_Blocks = new ArrayList<BS_Block>();
	private Map<UUID, BS_MatchPlayer> m_InactivePlayers = new HashMap<UUID, BS_MatchPlayer>();
	private Map<UUID, BS_MatchPlayer> m_Players = new HashMap<UUID, BS_MatchPlayer>();
	
	//=========================================================================
	// PUBLIC GETTERS
	//=========================================================================
	public Collection<BS_MatchPlayer> getActivePlayers()
	{
		return m_Players.values();
	}
	
	public float getRoundSecondsLeft()
	{
		Duration timeElapsed = Duration.between(m_RoundStartTime, Instant.now());
		float SecondsElapsed = (float)timeElapsed.toMillis() / 1000.f;
		return (m_CurRoundTime * 60.f) - SecondsElapsed;
	}
	
	public float getRoundSeconds()
	{
		return m_CurRoundTime * 60.f;
	}
	
	public int getCurrentRound()
	{
		return m_RoundNumber;
	}
}
