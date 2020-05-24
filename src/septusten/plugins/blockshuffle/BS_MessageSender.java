package septusten.plugins.blockshuffle;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class BS_MessageSender 
{
	static String MessageStart = ChatColor.DARK_BLUE + "[" + ChatColor.GREEN + "BlockShuffle" + ChatColor.DARK_BLUE + "]" + ChatColor.RESET + " ";
	
	static void sendBroadcast(String message)
	{
		Bukkit.broadcastMessage(MessageStart + message);
	}
	
	static void sendMessage(Player player, String message)
	{
		player.sendMessage(MessageStart + message);
	}
}
