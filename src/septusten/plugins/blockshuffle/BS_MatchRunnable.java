package septusten.plugins.blockshuffle;

import java.text.DecimalFormat;
import java.util.Collection;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.BlockFace;
import org.bukkit.potion.PotionEffectType;

import net.md_5.bungee.api.ChatColor;

public class BS_MatchRunnable implements Runnable 
{	
	private boolean IsNearly(float A, float B, float Eps)
	{
		return Math.abs(A - B) < Eps;
	}
	
	private int m_CurrentRound = 0;	
	private BS_Main MainGameReference;
	
	public BS_MatchRunnable(BS_Main GameRef)
	{
		MainGameReference = GameRef;
	}
	
	@Override
	public void run() 
	{
		// Go through all players
		Collection<BS_MatchPlayer> activePlayers = MainGameReference.getActivePlayers();
		int NumSearching = 0;
		for (BS_MatchPlayer p : activePlayers) 
		{
			if (p.m_FoundBlock || p.getTargetBlock() == null)
			{
				continue;
			}
			NumSearching++;
			
			// Check if we're on top of the block
			Material curBlock = p.m_PlayerRef.getLocation().getBlock().getRelative(BlockFace.DOWN).getBlockData().getMaterial();
			if (curBlock == p.getTargetBlock())
			{
				Bukkit.broadcastMessage("Player \"" + ChatColor.ITALIC + ChatColor.GREEN + p.m_PlayerRef.getDisplayName() +  ChatColor.RESET + "\" has found their target block!");
				p.m_FoundBlock = true;
				AttributeInstance maxHealth = p.m_PlayerRef.getAttribute(Attribute.GENERIC_MAX_HEALTH);
				double newBaseValue = maxHealth.getBaseValue() + 1;
				if (newBaseValue <= maxHealth.getDefaultValue())
				{
					maxHealth.setBaseValue(newBaseValue);
				}				
				p.m_Successes++;
				NumSearching--;
			}
		}
		
		// Check current time
		float SecondsLeft = MainGameReference.getRoundSecondsLeft();
		
		if (IsNearly(SecondsLeft, 30.f, 0.01f) &&
			m_CurrentRound < MainGameReference.getCurrentRound())
		{
			m_CurrentRound = MainGameReference.getCurrentRound();
			Bukkit.broadcastMessage(ChatColor.GOLD + "30" + ChatColor.GREEN + " seconds left!");			
		}
		
		//if (SecondsLeft <= 0.f || (NumSearching == 0))
		if (SecondsLeft <= 0.f || (NumSearching <= 0 && !activePlayers.isEmpty()))
		{
			MainGameReference.incrementRound();
		}
	}
}
