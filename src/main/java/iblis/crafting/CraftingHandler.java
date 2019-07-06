package iblis.crafting;

import iblis.IblisMod;
import iblis.player.PlayerSkills;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.*;
import net.minecraft.item.*;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.RecipeRepairItem;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.registries.IForgeRegistryModifiable;

import java.util.*;

public class CraftingHandler  implements IContainerListener{
	
	static List<PlayerSensitiveShapedRecipeWrapper> replacements = new ArrayList<PlayerSensitiveShapedRecipeWrapper>();
	private static List<ShapedRecipeRaisingSkillWrapper> replacements2 = new ArrayList<ShapedRecipeRaisingSkillWrapper>();
	
	@SubscribeEvent
	public void registerRecipes(RegistryEvent.Register<IRecipe> event) {
		IForgeRegistryModifiable<IRecipe> recipeRegistry = (IForgeRegistryModifiable<IRecipe>) ForgeRegistries.RECIPES;
		Iterator<IRecipe> irecipes = recipeRegistry.iterator();
		List<ResourceLocation> vanillaRecipesToRemove = new ArrayList<ResourceLocation>();
		while (irecipes.hasNext()) {
			IRecipe recipe = irecipes.next();
			if(recipe instanceof RecipeRepairItem) {
				vanillaRecipesToRemove.add(recipe.getRegistryName());
			}
			ItemStack is = recipe.getRecipeOutput();
			if (is != null) {
				if (isArmor(is)) {
					this.wrapRecipe(is, vanillaRecipesToRemove, recipe, PlayerSkills.ARMORSMITH);
				} else if (isWeapon(is)) {
					this.wrapRecipe(is, vanillaRecipesToRemove, recipe, PlayerSkills.WEAPONSMITH);
				} else if (is.getItem() instanceof ItemBow) {
					this.wrapRecipe(is, vanillaRecipesToRemove, recipe, PlayerSkills.MECHANICS);
				} else if (is.getItem() instanceof ItemShield) {
					ShapedRecipeRaisingSkillWrapper shieldsWrapper = new ShapedRecipeRaisingSkillWrapper(recipe);
					shieldsWrapper.setSensitiveTo(PlayerSkills.ARMORSMITH, 2);
					shieldsWrapper.setRegistryName(recipe.getRegistryName());
					vanillaRecipesToRemove.add(recipe.getRegistryName());
					replacements2.add(shieldsWrapper);
				} else if (isMechanism(is)) {
					ShapedRecipeRaisingSkillWrapper mechanismWrapper = new ShapedRecipeRaisingSkillWrapper(recipe);
					mechanismWrapper.setSensitiveTo(PlayerSkills.MECHANICS, 1);
					mechanismWrapper.setRegistryName(recipe.getRegistryName());
					vanillaRecipesToRemove.add(recipe.getRegistryName());
					replacements2.add(mechanismWrapper);
				}
			}
		}
		for (ResourceLocation key : vanillaRecipesToRemove)
			recipeRegistry.remove(key);
		this.addRecipes(event);
	}
	
	private boolean isMechanism(ItemStack is) {
		Item item = is.getItem();
		return item == Item.getItemFromBlock(Blocks.PISTON) 
				|| item == Items.CLOCK
				|| item == Item.getItemFromBlock(Blocks.NOTEBLOCK)
				|| item == Item.getItemFromBlock(Blocks.DISPENSER)
				|| item == Item.getItemFromBlock(Blocks.JUKEBOX);
	}

	private void wrapRecipe(ItemStack is, List<ResourceLocation> vanillaRecipesToRemove, IRecipe recipe, PlayerSkills sensitiveSkill){
		PlayerSensitiveShapedRecipeWrapper recipeReplacement = new PlayerSensitiveShapedRecipeWrapper(recipe);
		double requiredskill = getWeaponCraftingRequiredSkill(is) + getArmorCraftingRequiredSkill(is);
		double skillXP = requiredskill + 1.0d;
		if(is.getItem() instanceof ItemTool){
			ItemTool isit = (ItemTool) is.getItem();
			String material = isit.getToolMaterialName();
			if(material.equalsIgnoreCase("wood"))
				skillXP = 0.2d; // Nerf tools XP for easily obtained materials
			if(material.equalsIgnoreCase("stone"))
				skillXP = 0.2d;
		}
		recipeReplacement.setSensitiveTo(sensitiveSkill, requiredskill, skillXP);
		recipeReplacement.setRegistryName(recipe.getRegistryName());
		vanillaRecipesToRemove.add(recipe.getRegistryName());
		replacements.add(recipeReplacement);

	}

	private void addRecipes(RegistryEvent.Register<IRecipe> event) {
		PlayerSensitiveRecipeRepairItem rri = new PlayerSensitiveRecipeRepairItem();
		rri.setRegistryName(new ResourceLocation(IblisMod.MODID,"recipe_repair_item"));

		event.getRegistry().register(rri);

		for(PlayerSensitiveShapedRecipeWrapper recipeReplacement: replacements)
			event.getRegistry().register(recipeReplacement);
		for(ShapedRecipeRaisingSkillWrapper recipeReplacement: replacements2)
			event.getRegistry().register(recipeReplacement);
	}
	
	private final List<ContainerRepair> openedContainers = new ArrayList<ContainerRepair>();
	private boolean skipNextUpdate = false;
	
	@SubscribeEvent
	public void onAnvilUpdate(AnvilUpdateEvent event) {
		// Hackish way to retrieve output after event.
		if(skipNextUpdate)
			return;
		// First find container responsible for event
		ContainerRepair container = null;
		assert !openedContainers.isEmpty();
		for(ContainerRepair containerIn: openedContainers)
			if (containerIn.inventorySlots.get(0).getStack() == event.getLeft()) {
				event.setCanceled(true);
				skipNextUpdate = true;
				containerIn.updateRepairOutput();
				container = containerIn;
				break;
			}
		if(container == null)
			return;
		// Second - find player
		EntityPlayer player = null; 
		for (IContainerListener listener : container.listeners) {
			if (listener instanceof EntityPlayerMP) {
				player = (EntityPlayer) listener;
				break;
			}
		}
		// Third - find a recipe output
		ItemStack repairableStack = container.getSlot(2).getStack();
		for (PlayerSensitiveShapedRecipeWrapper recipeReplacement : replacements) {
			if (CraftingHandler.itemMatches(repairableStack, recipeReplacement.getRecipeOutput())) {
				double skillValue = recipeReplacement.sensitiveSkill.getFullSkillValue(player) - recipeReplacement.minimalSkill;
				PlayerSensitiveShapedRecipeWrapper.getCraftingResult(repairableStack, skillValue, true);
				break;
			}
		}
		container.detectAndSendChanges();
		skipNextUpdate = false;		
	}
	
	@SubscribeEvent
	public void onPlayerOpenContainerEvent(PlayerContainerEvent.Open event) {
		if(event.getContainer() instanceof ContainerRepair)
			openedContainers.add((ContainerRepair) event.getContainer());
		if(event.getContainer() instanceof ContainerWorkbench) {
			event.getContainer().listeners.add(this);
			this.sendSlotContents(event.getContainer(), 0, ItemStack.EMPTY);
		}
	}

	@SubscribeEvent
	public void onPlayerOpenContainerEvent(PlayerContainerEvent.Close event) {
		if(event.getContainer() instanceof ContainerRepair)
			openedContainers.remove(event.getContainer());
	}

	static boolean isArmor(ItemStack is) {
		if(is.getMaxStackSize()!=1)
			return false;
		String armorkey = SharedMonsterAttributes.ARMOR.getName();
		if(is.getAttributeModifiers(EntityEquipmentSlot.CHEST).keySet().contains(armorkey))
			return true;
		if(is.getAttributeModifiers(EntityEquipmentSlot.FEET).keySet().contains(armorkey))
			return true;
		if(is.getAttributeModifiers(EntityEquipmentSlot.HEAD).keySet().contains(armorkey))
			return true;
		if(is.getAttributeModifiers(EntityEquipmentSlot.LEGS).keySet().contains(armorkey))
			return true;
		return false;
	}
	
	private static double getArmorCraftingRequiredSkill(ItemStack is) {
		if(is.getItem() instanceof ItemArmor){
			ItemArmor ia = (ItemArmor)is.getItem();
			switch(ia.getArmorMaterial()){
			case DIAMOND:
				return 10d;
			case IRON:
				return 6d;
			case GOLD:
				return 4d;
			case CHAIN:
				return 2d;
			case LEATHER:
				return 1d;
			default:
				break;
			}
		}
		double minimalSkill = 0;
		for(AttributeModifier am :is.getAttributeModifiers(EntityEquipmentSlot.CHEST).get(SharedMonsterAttributes.ARMOR.getName()))
			minimalSkill+=am.getAmount();
		for(AttributeModifier am :is.getAttributeModifiers(EntityEquipmentSlot.FEET).get(SharedMonsterAttributes.ARMOR.getName()))
			minimalSkill+=am.getAmount();
		for(AttributeModifier am :is.getAttributeModifiers(EntityEquipmentSlot.HEAD).get(SharedMonsterAttributes.ARMOR.getName()))
			minimalSkill+=am.getAmount();
		for(AttributeModifier am :is.getAttributeModifiers(EntityEquipmentSlot.LEGS).get(SharedMonsterAttributes.ARMOR.getName()))
			minimalSkill+=am.getAmount();
		for(AttributeModifier am :is.getAttributeModifiers(EntityEquipmentSlot.CHEST).get(SharedMonsterAttributes.ARMOR_TOUGHNESS.getName()))
			minimalSkill+=am.getAmount();
		for(AttributeModifier am :is.getAttributeModifiers(EntityEquipmentSlot.FEET).get(SharedMonsterAttributes.ARMOR_TOUGHNESS.getName()))
			minimalSkill+=am.getAmount();
		for(AttributeModifier am :is.getAttributeModifiers(EntityEquipmentSlot.HEAD).get(SharedMonsterAttributes.ARMOR_TOUGHNESS.getName()))
			minimalSkill+=am.getAmount();
		for(AttributeModifier am :is.getAttributeModifiers(EntityEquipmentSlot.LEGS).get(SharedMonsterAttributes.ARMOR_TOUGHNESS.getName()))
			minimalSkill+=am.getAmount();
		return minimalSkill;
	}
	
	static boolean isWeapon(ItemStack is) {
		Set<String> am = is.getAttributeModifiers(EntityEquipmentSlot.MAINHAND).keySet();
		return am.contains(SharedMonsterAttributes.ATTACK_DAMAGE.getName());
	}
	
	private static double getWeaponCraftingRequiredSkill(ItemStack is) {
		double minimalSkill = 0;
		if(is.getItem() instanceof ItemBow) {
			minimalSkill = 1;
		}
		Collection<AttributeModifier> am = is.getAttributeModifiers(EntityEquipmentSlot.MAINHAND).get(SharedMonsterAttributes.ATTACK_DAMAGE.getName());
		Iterator<AttributeModifier> ami = am.iterator();
		while(ami.hasNext()) {
			minimalSkill+=ami.next().getAmount();
		}
		minimalSkill*=2;
		return minimalSkill;
	}
	
	@Override
	public void sendSlotContents(Container containerToSend, int slotInd, ItemStack stack) {
		if (!(containerToSend instanceof ContainerWorkbench))
			return;
		EntityPlayerMP player = (EntityPlayerMP) IblisMod.proxy.getPlayer(((ContainerWorkbench)containerToSend).craftMatrix);
		if(player!=null)
			IblisMod.network.sendRefreshTrainCraftButton(player);
	}

	@Override
	public void sendWindowProperty(Container containerIn, int varToUpdate, int newValue) {}
	@Override
	public void sendAllWindowProperties(Container containerIn, IInventory inventory) {
	}
	@Override
	public void sendAllContents(Container containerToSend, NonNullList<ItemStack> itemsList) {
	}

	public static boolean itemMatches(ItemStack stack1, ItemStack stack2) {
		return stack1.getItem().getHasSubtypes() && OreDictionary.itemMatches(stack1, stack2, false)
				|| stack1.getItem().equals(stack2.getItem());
	}
}
