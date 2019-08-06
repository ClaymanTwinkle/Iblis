package iblis;

import iblis.client.ClientGameEventHandler;
import iblis.client.ClientRenderEventHandler;
import iblis.client.ItemTooltipEventHandler;
import iblis.client.gui.GuiEventHandler;
import iblis.client.particle.ParticleDecal;
import iblis.client.particle.ParticleFlame;
import iblis.client.particle.ParticleSliver;
import iblis.client.particle.ParticleSpark;
import iblis.client.util.DecalHelper;
import iblis.init.IblisParticles;
import iblis.player.PlayerSkills;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class ClientProxy extends ServerProxy {

	@SubscribeEvent
	void onWorldLoadedEvent(WorldEvent.Load event) {
		if (!event.getWorld().isRemote)
			return;
		event.getWorld().addEventListener(ClientGameEventHandler.instance);
	}

	@Override
	void load() {
		OBJLoader.INSTANCE.addDomain(IblisMod.MODID);
		MinecraftForge.EVENT_BUS.register(GuiEventHandler.instance);
		MinecraftForge.EVENT_BUS.register(new ItemTooltipEventHandler());
		MinecraftForge.EVENT_BUS.register(ClientGameEventHandler.instance);
		MinecraftForge.EVENT_BUS.register(new ClientRenderEventHandler());
	}

	@Override
	public void registerRenders() {

	}

	@SubscribeEvent
	public void onModelBake(ModelBakeEvent event) {
		ParticleSliver.bakeModels();
	}

	@Override
	public void init() {
		registerItemRenders();
		registerBlockRenders();
	}
	
	private void registerItemRenders() {

	}
	
	private void registerBlockRenders() {
	}
	
	private static void registerRender(Block block, int metadata, ResourceLocation modelResourceLocation) {
		Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(Item.getItemFromBlock(block), metadata,
				new ModelResourceLocation(modelResourceLocation, "inventory"));
	}
	
	@Override
	public double getPlayerSkillValue(PlayerSkills sensitiveSkill, InventoryCrafting inv) {
		double serverValue = super.getPlayerSkillValue(sensitiveSkill, inv);
		if (serverValue != 0d)
			return serverValue;
		EntityPlayerSP player = Minecraft.getMinecraft().player;
		return sensitiveSkill.getFullSkillValue(player);
	}

	@Override
	public EntityPlayer getPlayer(InventoryCrafting inv) {
		EntityPlayer serverPlayer = super.getPlayer(inv);
		if (serverPlayer != null)
			return serverPlayer;
		return Minecraft.getMinecraft().player;
	}

	public void spawnParticle(@Nonnull IblisParticles particle, double posX, double posY, double posZ, double xSpeedIn,
			double ySpeedIn, double zSpeedIn) {
		@Nonnull
		Particle entityParticle;
		Minecraft mc = Minecraft.getMinecraft();
		Entity renderViewEntity = mc.getRenderViewEntity();
		switch (particle) {
		case SPARK:
			entityParticle = new ParticleSpark(mc.world, posX, posY, posZ, xSpeedIn, ySpeedIn, zSpeedIn, -0.04f);
			break;
		case SLIVER:
			entityParticle = new ParticleSliver(mc.getTextureManager(), mc.world, posX, posY, posZ, xSpeedIn, ySpeedIn,
					zSpeedIn, 1.0f);
			break;
		case FLAME:
			entityParticle = new ParticleFlame(mc.getTextureManager(), mc.world, posX, posY, posZ, xSpeedIn, ySpeedIn,
					zSpeedIn, 0.01f);
			entityParticle.setMaxAge(16);
			entityParticle.multipleParticleScaleBy(0.2f);
			break;
		default:
			entityParticle = mc.effectRenderer.spawnEffectParticle(10, posX, posY, posZ, xSpeedIn, ySpeedIn, zSpeedIn,
					new int[] { 0 });
			break;
		}
		mc.effectRenderer.addEffect(entityParticle);
	}

	public void addDecal(@Nonnull IblisParticles decalIn, double posX, double posY, double posZ,
			@Nonnull EnumFacing facingIn, int colourIn, float size) {
		Minecraft mc = Minecraft.getMinecraft();
		int spriteIndexX = 0;
		switch (decalIn) {
		case BULLET_HOLE:
			spriteIndexX = 0;
			break;
		case BLOOD_SPLATTER:
			spriteIndexX = 1;
			break;
		case TRACE_OF_SHOT:
			spriteIndexX = 2;
			break;
		default:
			spriteIndexX = 0;
			IblisMod.log.error("Incorrect/unhandled decal recieved on client: " + decalIn.name());
		}

		AxisAlignedBB particleBB = new AxisAlignedBB(posX - size / 2, posY - size / 2, posZ - size / 2, posX + size / 2,
				posY + size / 2, posZ + size / 2);
		List<AxisAlignedBB> collidingBoxes = new ArrayList<AxisAlignedBB>();
		int x1 = MathHelper.floor(posX - size / 2);
		int y1 = MathHelper.floor(posY - size / 2);
		int z1 = MathHelper.floor(posZ - size / 2);
		int x2 = MathHelper.ceil(posX - size / 2);
		int y2 = MathHelper.ceil(posY - size / 2);
		int z2 = MathHelper.ceil(posZ - size / 2);
		for (int x = x1; x <= x2; x++)
			for (int y = y1; y <= y2; y++)
				for (int z = z1; z <= z2; z++) {
					BlockPos pos = new BlockPos(x, y, z);
					IBlockState bstate = mc.world.getBlockState(pos);
					// For a custom cases when collision box not match display
					// borders.
					DecalHelper.addDecalDisplayBoxToList(mc.world, pos, particleBB, collidingBoxes, bstate);
					if (!collidingBoxes.isEmpty()) {
						int packedLight = bstate.getPackedLightmapCoords(mc.world, pos.offset(facingIn));
						int layer = ClientGameEventHandler.instance.getDecalLayer(pos);
						for (AxisAlignedBB cbb : collidingBoxes) {
							double posX1 = posX;
							double posY1 = posY;
							double posZ1 = posZ;
							switch (facingIn) {
							case DOWN:
								posY1 = Math.max(posY, cbb.minY);
								break;
							case UP:
								posY1 = Math.min(posY, cbb.maxY);
								break;
							case NORTH:
								posZ1 = Math.max(posZ, cbb.minZ);
								break;
							case SOUTH:
								posZ1 = Math.min(posZ, cbb.maxZ);
								break;
							case WEST:
								posX1 = Math.max(posX, cbb.minX);
								break;
							case EAST:
								posX1 = Math.min(posX, cbb.maxX);
								break;
							}
							int colour = colourIn;
							if (colourIn == -1)
								colour = DecalHelper.getDecalColour(mc.world, pos, bstate);
							ParticleDecal decal = new ParticleDecal(mc.getTextureManager(), mc.world, posX1, posY1,
									posZ1, facingIn, cbb, size, colour, packedLight, layer, spriteIndexX);
							mc.effectRenderer.addEffect(decal);
							ClientGameEventHandler.instance.attachParticleToBlock(decal, pos);
						}
						collidingBoxes.clear();
					}
				}
	}
	
	@Override
	public InputStream getResourceInputStream(ResourceLocation location) {
		try {
			return Minecraft.getMinecraft().getResourceManager().getResource(location).getInputStream();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return super.getResourceInputStream(location);
	}
	
	public void setToggleSprintByKeyBindSprint(boolean value) {
		ClientGameEventHandler.instance.toggleSprintByKeyBindSprint = value;
	}
	
	@Override
	public void setHPRender(boolean renderHPIn) {
		GuiEventHandler.renderHP = renderHPIn;
	}
}
