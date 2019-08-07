package iblis.init;

import iblis.IblisMod;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;

public class IblisSounds {
	public static SoundEvent book_reading;
	public static SoundEvent book_closing;
	public static SoundEvent opening_medkit;
	public static SoundEvent closing_medkit;
	public static SoundEvent full_bottle_shaking;
	public static SoundEvent scissors_clicking;
	public static SoundEvent tearing_bandage;
	public static SoundEvent boulder_impact;
	public static SoundEvent knife_impact;
	public static SoundEvent knife_impact_stone;
	public static SoundEvent knife_fall;

	public static SoundEvent crossbow_cock;
	public static SoundEvent crossbow_putting_bolt;
	public static SoundEvent crossbow_shot;
	
	public static void register() {
		book_reading = registerSound("book_reading");
		book_closing = registerSound("book_closing");
		opening_medkit = registerSound("opening_medkit");
		closing_medkit = registerSound("closing_medkit");
		full_bottle_shaking = registerSound("full_bottle_shaking");
		scissors_clicking = registerSound("scissors_clicking");
		tearing_bandage = registerSound("tearing_bandage");
		boulder_impact = registerSound("boulder_impact");
		knife_impact = registerSound("knife_impact");
		knife_impact_stone = registerSound("knife_impact_stone");
		knife_fall = registerSound("knife_fall");
		crossbow_cock = registerSound("crossbow_cock");
		crossbow_putting_bolt = registerSound("crossbow_putting_bolt");
		crossbow_shot = registerSound("crossbow_shot");
	}

	private static SoundEvent registerSound(String soundNameIn) {
		ResourceLocation sound = new ResourceLocation(IblisMod.MODID, soundNameIn);
		SoundEvent soundEvent = new SoundEvent(sound).setRegistryName(sound);
		RegistryEventHandler.sounds.add(soundEvent);
		return soundEvent;
	}
}
