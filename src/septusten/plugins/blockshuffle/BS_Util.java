package septusten.plugins.blockshuffle;

import java.text.DecimalFormat;

import org.bukkit.ChatColor;

public class BS_Util 
{
	private static DecimalFormat Formatter = new DecimalFormat("#.#");
	
	public static String getTimeLeftString(float secondsLeft)
	{
		int Minutes = (int)(secondsLeft / 60.f);
		float Seconds = secondsLeft - ((float)Minutes * 60.f);
		DecimalFormat Formatter = new DecimalFormat("#.#");
		String formattedSeconds = Formatter.format(Seconds);
		
		if (Minutes != 0 && Seconds != 0)
		{
			return ChatColor.GREEN + "You have " + ChatColor.GOLD + Minutes + ChatColor.GREEN + " minutes and " + ChatColor.GOLD + formattedSeconds + ChatColor.GREEN + " seconds to find your block!";
		}
		else if (Minutes != 0)
		{
			return ChatColor.GREEN + "You have " + ChatColor.GOLD + Minutes + ChatColor.GREEN + " minutes to find your block!";
		}
		else if (Seconds != 0)
		{
			return ChatColor.GREEN + "You have " + ChatColor.GOLD + formattedSeconds + ChatColor.GREEN + " seconds to find your block!";
		}
		
		// Should never get here
		return "";
	}
}
