package iblis;

import iblis.applecore_integration.AplleCoreHungerEventHandler;
import iblis.command.CommandGetAttribute;
import iblis.command.CommandSetAttribute;
import iblis.command.CommandShowNBT;
import iblis.crafting.CraftingHandler;
import iblis.ebwizardy_integration.EBWizardyEventHandler;
import iblis.entity.EntityPlayerZombie;
import iblis.event.IblisDeathPenaltyHandler;
import iblis.event.IblisEventHandler;
import iblis.init.*;
import iblis.loot.LootTableParsingEventHandler;
import iblis.tconstruct_integration.TConstructCraftingEventHandler;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.RangedAttribute;
import net.minecraft.init.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.VillagerRegistry.VillagerCareer;
import net.minecraftforge.fml.common.registry.VillagerRegistry.VillagerProfession;
import org.apache.logging.log4j.Logger;

@Mod(modid = IblisMod.MODID, version = IblisMod.VERSION, guiFactory = IblisMod.GUI_FACTORY, dependencies = IblisMod.DEPENDENCIES)
public class IblisMod {
	public static final String MODID = "iblis";
	public static final String VERSION = "0.5.13";
	public static final String GUI_FACTORY = "iblis.client.gui.IblisGuiFactory";
	public static final String DEPENDENCIES = "after:landcore;after:hardcorearmor;after:tconstruct;after:silentgems";

	@SidedProxy(clientSide = "iblis.ClientProxy", serverSide = "iblis.ServerProxy")
	public static ServerProxy proxy= new ServerProxy();
	@SidedProxy(clientSide = "iblis.ClientNetworkHandler", serverSide = "iblis.ServerNetworkHandler")
	public static ServerNetworkHandler network = new ServerNetworkHandler();
	public static Logger log;
	public static IblisModConfig config;
	public static IblisEventHandler eventHandler;
	public static IblisDeathPenaltyHandler deathPenaltyHandler;
	public static boolean isRPGHUDLoaded = false;
	public static boolean isAppleCoreLoaded = false;
	public static boolean isAppleskinLoaded = false;
	public static boolean isEBWizardyLoaded = false;
	private static final int LIBRARIAN_CAREER_ID = 0;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		isAppleCoreLoaded = Loader.isModLoaded("applecore");
		isEBWizardyLoaded = Loader.isModLoaded("ebwizardry");
		isAppleskinLoaded = Loader.isModLoaded("appleskin");
		// Oh, so original and fresh! ^_^
		eventHandler = new IblisEventHandler();
		deathPenaltyHandler = new IblisDeathPenaltyHandler();
		log = event.getModLog();
		IblisBlocks.init();
		IblisItems.init();
		IblisPotions.init();
		IblisSounds.register();
		if (isEBWizardyLoaded) {
			EBWizardyEventHandler.initSkills();
			MinecraftForge.EVENT_BUS.register(new EBWizardyEventHandler());
		}
		config = new IblisModConfig(new Configuration(event.getSuggestedConfigurationFile()));
		MinecraftForge.EVENT_BUS.register(config);
		RangedAttribute toughness = (RangedAttribute) SharedMonsterAttributes.ARMOR_TOUGHNESS;
		RangedAttribute armor = (RangedAttribute) SharedMonsterAttributes.ARMOR;
		// Max armor and armor toughness upper limit is removed
		toughness.maximumValue = Double.MAX_VALUE;
		armor.maximumValue = Double.MAX_VALUE;
		Items.SHIELD.setMaxDamage(200);
		EntityRegistry.registerModEntity(new ResourceLocation(MODID, "player_zombie"), EntityPlayerZombie.class,
				"player_zombie", 0, this, 80, 3, true);

		VillagerProfession librarian = ForgeRegistries.VILLAGER_PROFESSIONS.getValue(new ResourceLocation("minecraft:librarian"));
		VillagerCareer librarianCareer = null;
		if (librarian != null) {
			librarianCareer = librarian.getCareer(LIBRARIAN_CAREER_ID);
		}
		proxy.load();
		network.load();

		MinecraftForge.EVENT_BUS.register(new CraftingHandler());
		MinecraftForge.EVENT_BUS.register(eventHandler);
		MinecraftForge.EVENT_BUS.register(deathPenaltyHandler);
		MinecraftForge.EVENT_BUS.register(proxy);
		MinecraftForge.EVENT_BUS.register(new LootTableParsingEventHandler());
		MinecraftForge.EVENT_BUS.register(new RegistryEventHandler());

		if (Loader.isModLoaded("tconstruct")) {
			MinecraftForge.EVENT_BUS.register(new TConstructCraftingEventHandler());
		}
		if (isAppleCoreLoaded) {
			MinecraftForge.EVENT_BUS.register(new AplleCoreHungerEventHandler());
		}
	}

	@EventHandler
	public void init(FMLPostInitializationEvent event) {
		proxy.init();
		isRPGHUDLoaded = Loader.isModLoaded("rpghud");
	}

	@EventHandler
	public void serverStarting(FMLServerStartingEvent event) {
		proxy.setServer(event.getServer());
		network.setServer(event.getServer());
		event.registerServerCommand(new CommandSetAttribute());
		event.registerServerCommand(new CommandGetAttribute());
		event.registerServerCommand(new CommandShowNBT());
	}
}
