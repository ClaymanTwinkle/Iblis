package iblis;

import iblis.player.PlayerSkills;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class IblisModConfig {
	
	public IblisModConfig(Configuration configuration) {
		loadConfig(configuration);
		syncConfig();
	}

	public static String getNicelyFormattedName(String name) {
		StringBuffer out = new StringBuffer();
		char char_ = '_';
		char prevchar = 0;
		for (char c : name.toCharArray()) {
			if (c != char_ && prevchar != char_) {
				out.append(String.valueOf(c).toLowerCase());
			} else if (c != char_) {
				out.append(String.valueOf(c));
			}
			prevchar = c;
		}
		return out.toString();
	}

	public Configuration configuration;

	void loadConfig(Configuration configuration) {
		this.configuration = configuration;
	}

	@SubscribeEvent
	public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent eventArgs) {
		if (eventArgs.getModID().equals(IblisMod.MODID)) {
			IblisMod.config.syncConfig();
		}
	}

	void syncConfig() {
		for(PlayerSkills skill:PlayerSkills.values()){
			skill.enabled = configuration.getBoolean("enable_"+skill.name().toLowerCase()+"_skill",
					Configuration.CATEGORY_GENERAL, true, "Turn off to disable skill. Disabled skills always counts as equal to zero.");
		}
		IblisMod.eventHandler.spawnPlayerZombie = configuration.getBoolean("spawn_player_zombie",
				Configuration.CATEGORY_GENERAL, true, "Spawn player zombie on players death with all inventory.");
		IblisMod.eventHandler.noDeathPenalty = configuration.getBoolean("no_death_penalty",
				Configuration.CATEGORY_GENERAL, false, "No death penalty to all skills and characteristics.");
		if (configuration.hasChanged()) {
			configuration.save();
		}
	}
}