package septusten.plugins.blockshuffle;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class BS_MatchPlayer 
{
	public BS_MatchPlayer(Material mat, Player player)
	{
		m_PlayerRef = player;
		setTargetBlock(mat);		
	}
	
	public void setTargetBlock(Material mat)
	{
		if (mat == null)
		{
			return;
		}
		
		m_TargetBlock = mat;
		sendTargetBlockMessage();
	}
	
	public void sendTargetBlockMessage()
	{
		if (m_TargetBlock == null)
		{
			return;
		}
		
		String blockString = m_TargetBlock.toString();
		blockString = blockString.replace('_',  ' ');
		blockString = blockString.toLowerCase();
		m_PlayerRef.sendMessage(ChatColor.GREEN + "Your target block is: \"" + ChatColor.GOLD + blockString + ChatColor.GREEN + "\"");
	}
	
	public Material getTargetBlock() 
	{
		return m_TargetBlock;
	}
	
	private Material m_TargetBlock	= null;
	int m_Successes 				= 0;
	int m_Fails 					= 0;
	double m_MaxHealth				= 0;
	boolean m_FoundBlock			= false;
	Player m_PlayerRef				= null;
}
