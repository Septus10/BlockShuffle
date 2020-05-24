package septusten.plugins.blockshuffle;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class BS_EventListener implements Listener 
{
	private BS_Main MainGameReference;
	public BS_EventListener(BS_Main GameRef)
	{
		MainGameReference = GameRef;
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		MainGameReference.onPlayerJoin(event.getPlayer());
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event)
	{
		MainGameReference.onPlayerQuit(event.getPlayer());
	}
}
