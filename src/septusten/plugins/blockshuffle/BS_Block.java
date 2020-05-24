package septusten.plugins.blockshuffle;

import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

public class BS_Block implements ConfigurationSerializable
{
	Material 	m_Block = null;
	int 		m_Weight = 1;
	
	@Override
	public Map<String, Object> serialize() 
	{
		LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
		result.put("block", m_Block.toString());
		result.put("weight", m_Weight);
		return result;
	}
	
	public static BS_Block deserialize(Map<String, Object> args) 
	{
		BS_Block result = new BS_Block();
		if (args.containsKey("block"))
		{
			result.m_Block = Material.valueOf((String)args.get("block"));
		}
		
		if (args.containsKey("weight"))
		{
			result.m_Weight = (int)args.get("weight");
		}
		
		return result;
	}
}
