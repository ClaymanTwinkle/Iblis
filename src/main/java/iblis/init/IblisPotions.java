package iblis.init;

import iblis.IblisMod;
import iblis.player.SharedIblisAttributes;
import iblis.potion.IblisPotion;
import net.minecraft.potion.Potion;

public class IblisPotions {
	public static Potion OVERHEATING = new IblisPotion(true, 0);
	
	public static void init(){
		OVERHEATING.setPotionName("overheating")
			.registerPotionAttributeModifier(SharedIblisAttributes.FIRE_DAMAGE_REDUCTION, "A111A5E-17EE5-AA0D1F1E7-F01-1770B5", -0.1, 0)
			.setRegistryName(IblisMod.MODID, "overheating");
		registerPotion(OVERHEATING);
	}
	
	private static void registerPotion(Potion item) {
		RegistryEventHandler.potions.add(item);
	}
}
