package iblis.client.renderer.tileentity;

import org.lwjgl.opengl.GL11;

import iblis.IblisMod;
import iblis.block.BlockLabTable;
import iblis.block.BlockLabTable.SubBox;
import iblis.client.gui.GuiLabTable;
import iblis.client.particle.ParticleFlame;
import iblis.init.IblisBlocks;
import iblis.tileentity.TileEntityLabTable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.client.model.pipeline.LightUtil;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class TileEntityLabTableSpecialRenderer extends TileEntitySpecialRenderer<TileEntityLabTable> {

	private final ResourceLocation particleTexture = new ResourceLocation(IblisMod.MODID, "textures/particle/particles.png");
	private static final VertexFormat VERTEX_FORMAT = (new VertexFormat()).addElement(DefaultVertexFormats.POSITION_3F)
			.addElement(DefaultVertexFormats.TEX_2F)
			.addElement(DefaultVertexFormats.COLOR_4UB)
			.addElement(DefaultVertexFormats.TEX_2S);
	private static IBakedModel staticGlass;
	private static IBakedModel reactor;
	private static IBakedModel reactorOut;
	private static IBakedModel separatorOut;
	private static IBakedModel filterOut;
	private SubBox selectedSubBox = null;
	private BlockPos prevPos;
	private TileEntity tile;
	
	public static void bakeModels() {
		staticGlass = bake("static_glass");
		reactor = bake("reactor");
		reactorOut = bake("reactor_out");
		separatorOut = bake("separator_out");
		filterOut = bake("filter_out");
	}

	private static IBakedModel bake(String model) {
		IModel raw;
		try {
			raw = OBJLoader.INSTANCE.loadModel(
					new ResourceLocation(IblisMod.MODID, "models/block/chemical_lab_installation_" + model + ".obj"));
			return raw.bake(raw.getDefaultState(), DefaultVertexFormats.ITEM, ModelLoader.defaultTextureGetter());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void render(TileEntityLabTable te, double x, double y, double z, float partialTicks, int destroyStage,
			float alpha) {
		super.render(te, x, y, z, partialTicks, destroyStage, alpha);
		if (te.getWorld() == null)
			return;
		Minecraft mc = Minecraft.getMinecraft();
		mc.renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		int light = te.getWorld().getCombinedLight(te.getPos(), 0);
		RenderHelper.enableStandardItemLighting();
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light >> 8, light & 255);
		GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GlStateManager.pushMatrix();
		GlStateManager.translate(x, y, z);
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferbuilder = tessellator.getBuffer();
		bufferbuilder.begin(7, DefaultVertexFormats.ITEM);
		this.render(staticGlass);
		if(te.hasReactor())
			this.render(reactor);
		if(te.hasReactorOut())
			this.render(reactorOut);
		if(te.hasFilterOut())
			this.render(filterOut);
		if(te.hasSeparatorOut())
			this.render(separatorOut);
		tessellator.draw();
		GlStateManager.popMatrix();
		BlockPos pos = te.getPos();
		this.renderBB(partialTicks, pos);
		GL11.glDisable(GL11.GL_BLEND);
	}

	public void render(IBakedModel model) {
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferbuilder = tessellator.getBuffer();
		for (BakedQuad quad : model.getQuads(null, null, 0L))
			LightUtil.renderQuadColor(bufferbuilder, quad, -1);
	}

	public void renderBB(float partialTick, BlockPos pos) {
		Entity player = Minecraft.getMinecraft().getRenderViewEntity();
		GL11.glEnable(GL11.GL_BLEND);
		OpenGlHelper.glBlendFunc(770, 771, 1, 0);
		GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.8F);
		GL11.glLineWidth(2.0F);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDepthMask(false);
		double offsetX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTick;
		double offsetY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTick;
		double offsetZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTick;
		if (selectedSubBox != null)
			RenderGlobal.drawSelectionBoundingBox(selectedSubBox.getBBAt(pos).offset(-offsetX, -offsetY, -offsetZ),
					1.0f, 0.0f, 0.0f, 1.0f);
		GL11.glDepthMask(true);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_BLEND);
	}
	
	public void renderFlame(float partialTick, BlockPos pos, int particleTextureIndexX, int particleTextureIndexY, double x, double y, double z) {
		GL11.glDepthMask(false);
	    GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
		Minecraft mc = Minecraft.getMinecraft();
		mc.renderEngine.bindTexture(particleTexture);
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		buffer.begin(7, VERTEX_FORMAT);
        float f = (float)particleTextureIndexX / 16.0F;
        float f1 = f + 0.0624375F;
        float f2 = (float)particleTextureIndexY / 16.0F;
        float f3 = f2 + 0.0624375F;
        float f4 = 0.1F; // Scale

        float f5 = (float)x;
        float f6 = (float)y;
        float f7 = (float)z;
        int j = 65535;
        int k = 65535;
        Vec3d[] avec3d = new Vec3d[] {new Vec3d((double)(-rotationX * f4 - rotationXY * f4), (double)(-rotationZ * f4), (double)(-rotationYZ * f4 - rotationXZ * f4)), new Vec3d((double)(-rotationX * f4 + rotationXY * f4), (double)(rotationZ * f4), (double)(-rotationYZ * f4 + rotationXZ * f4)), new Vec3d((double)(rotationX * f4 + rotationXY * f4), (double)(rotationZ * f4), (double)(rotationYZ * f4 + rotationXZ * f4)), new Vec3d((double)(rotationX * f4 - rotationXY * f4), (double)(-rotationZ * f4), (double)(rotationYZ * f4 - rotationXZ * f4))};

        if (this.particleAngle != 0.0F)
        {
            float f8 = this.particleAngle + (this.particleAngle - this.prevParticleAngle) * partialTicks;
            float f9 = MathHelper.cos(f8 * 0.5F);
            float f10 = MathHelper.sin(f8 * 0.5F) * (float)cameraViewDir.x;
            float f11 = MathHelper.sin(f8 * 0.5F) * (float)cameraViewDir.y;
            float f12 = MathHelper.sin(f8 * 0.5F) * (float)cameraViewDir.z;
            Vec3d vec3d = new Vec3d((double)f10, (double)f11, (double)f12);

            for (int l = 0; l < 4; ++l)
            {
                avec3d[l] = vec3d.scale(2.0D * avec3d[l].dotProduct(vec3d)).add(avec3d[l].scale((double)(f9 * f9) - vec3d.dotProduct(vec3d))).add(vec3d.crossProduct(avec3d[l]).scale((double)(2.0F * f9)));
            }
        }

        buffer.pos((double)f5 + avec3d[0].x, (double)f6 + avec3d[0].y, (double)f7 + avec3d[0].z).tex((double)f1, (double)f3).color(1.0f,1.0f,1.0f,1.0f).lightmap(j, k).endVertex();
        buffer.pos((double)f5 + avec3d[1].x, (double)f6 + avec3d[1].y, (double)f7 + avec3d[1].z).tex((double)f1, (double)f2).color(1.0f,1.0f,1.0f,1.0f).lightmap(j, k).endVertex();
        buffer.pos((double)f5 + avec3d[2].x, (double)f6 + avec3d[2].y, (double)f7 + avec3d[2].z).tex((double)f, (double)f2).color(1.0f,1.0f,1.0f,1.0f).lightmap(j, k).endVertex();
        buffer.pos((double)f5 + avec3d[3].x, (double)f6 + avec3d[3].y, (double)f7 + avec3d[3].z).tex((double)f, (double)f3).color(1.0f,1.0f,1.0f,1.0f).lightmap(j, k).endVertex();
        tessellator.draw();
    	GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthMask(true);

	}
	
	@SubscribeEvent
	public void drawBlockSelectionBox(DrawBlockHighlightEvent event) {
		BlockPos pos = event.getTarget().getBlockPos();
		EntityPlayer playerIn = event.getPlayer();
		if (pos == null)
			return;
		if (prevPos != pos) {
			prevPos = pos;
			tile = playerIn.world.getTileEntity(pos);
		}
		if (!(tile instanceof TileEntityLabTable)) {
			GuiLabTable.instance.setSelectedSubBox(null, 0, null);
			return;
		}
		Vec3d aim = playerIn.getLookVec();
		float blockReachDistance = BlockLabTable.BLOCK_REACH_DISTANCE;
		Vec3d vec3d = new Vec3d(playerIn.posX, playerIn.posY + playerIn.eyeHeight, playerIn.posZ);
		Vec3d vec3d2 = vec3d.addVector(aim.x * blockReachDistance, aim.y * blockReachDistance,
				aim.z * blockReachDistance);
		boolean hasSubBlockSelected = false;
		for (SubBox subBox : SubBox.values()) {
			RayTraceResult traceResult = subBox.intersectsAtPos(vec3d, vec3d2, pos);
			if (traceResult != null) {
				selectedSubBox = subBox;
				GuiLabTable.instance.setSelectedSubBox(subBox, traceResult.subHit, (TileEntityLabTable)tile);
				hasSubBlockSelected = true;
				break;
			}
		}
		if (!hasSubBlockSelected) {
			GuiLabTable.instance.setSelectedSubBox(null, 0, null);
			selectedSubBox = null;
		}
	}
}
