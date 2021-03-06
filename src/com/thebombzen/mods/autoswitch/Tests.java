package com.thebombzen.mods.autoswitch;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.thebombzen.mods.autoswitch.configuration.Configuration;
import com.thebombzen.mods.thebombzenapi.ComparableTuple;
import com.thebombzen.mods.thebombzenapi.FieldNotFoundException;
import com.thebombzen.mods.thebombzenapi.ThebombzenAPI;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.IShearable;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class Tests {
	
	private static final Minecraft mc = Minecraft.getMinecraft();
	
	public static final ItemStack EMPTY_ITEMSTACK;
	
	private static ItemStack prevHeldItem;
	
	static {
		ItemStack tempEmpty;
		try {
			tempEmpty = ItemStack.EMPTY;
		} catch (NoSuchFieldError err) {
			tempEmpty = null;
		}
		EMPTY_ITEMSTACK = tempEmpty;
		prevHeldItem = EMPTY_ITEMSTACK;
	}
	
	private static Random prevRandom = null;
	
	private static final String[] randomNames = {"rand", "field_73012_v", "v"};
	private static final String[] getSilkTouchDropNames = {"getSilkTouchDrop", "createStackedBlock", "func_180643_i", "i"};
	
	/**
	 * Anything strictly greater than this is considered to be "standard"
	 */
	public static final ComparableTuple<Integer> standardThreshold = new ComparableTuple<Integer>(0, 0);
	
	public static ItemStack getSilkTouchDrop(Block block, IBlockState state) {
		return ThebombzenAPI.invokePrivateMethod(block, Block.class, getSilkTouchDropNames,
				new Class<?>[] { IBlockState.class }, state);
	}
	

	public static boolean doesFortuneWorkOnBlock(World world, BlockPos pos) {

		IBlockState blockState = world.getBlockState(pos);

		int state = AutoSwitch.instance.getConfiguration().getFortuneOverrideState(blockState);
		if (state == Configuration.OVERRIDDEN_NO){
			return false;
		} else if (state == Configuration.OVERRIDDEN_YES){
			return true;
		}

		Random maxRandom = new NotSoRandom(false);
		Random zeroRandom = new NotSoRandom(true);

		List<ItemStack> defaultMaxRandom;
		List<ItemStack> defaultZeroRandom;
		List<ItemStack> fortuneMaxRandom;
		List<ItemStack> fortuneZeroRandom;

		fakeRandomForWorld(world, maxRandom);
		defaultMaxRandom = getDrops(world, pos, 0);
		fortuneMaxRandom = getDrops(world, pos, 3);
		unFakeRandomForWorld(world);

		fakeRandomForWorld(world, zeroRandom);
		defaultZeroRandom = getDrops(world, pos, 0);
		fortuneZeroRandom = getDrops(world, pos, 3);
		unFakeRandomForWorld(world);

		if (!ThebombzenAPI.areItemStackCollectionsEqual(defaultMaxRandom,
				fortuneMaxRandom)
				|| !ThebombzenAPI.areItemStackCollectionsEqual(
						defaultZeroRandom, fortuneZeroRandom)) {
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean doesItemStackMimicSilk(World world, BlockPos pos, ItemStack itemstack) {
		
		IBlockState blockState = world.getBlockState(pos);
		
		if (blockState.getBlock() instanceof IShearable && !Tests.isItemStackEmpty(itemstack) && itemstack.getItem() instanceof ItemShears) {
			IShearable shearable = (IShearable) blockState.getBlock();
			if (shearable.isShearable(itemstack, world, pos)) {
				List<ItemStack> drops = shearable.onSheared(itemstack, world, pos, 0);
				if (ThebombzenAPI.areItemStackCollectionsEqual(drops, Collections.singletonList(Tests.getSilkTouchDrop(blockState.getBlock(), blockState)))) {
					return true;
				}
			}
		}

		return false;
		
	}

	@SuppressWarnings("deprecation")
	public static List<ItemStack> getDrops(World world, BlockPos pos, int fortune){
		IBlockState blockState = world.getBlockState(pos);
		try {
			NonNullList<ItemStack> ret = NonNullList.<ItemStack>create();
			blockState.getBlock().getDrops(ret, world, pos, blockState, fortune);
			return ret;
		} catch (NoClassDefFoundError | NoSuchMethodError error) {
			return blockState.getBlock().getDrops(world, pos, blockState, fortune);
		}
	}

	public static boolean doesSilkTouchWorkOnBlock(World world, BlockPos pos) {

		IBlockState blockState = world.getBlockState(pos);

		int state = AutoSwitch.instance.getConfiguration().getSilkTouchOverrideState(blockState);
		
		if (state == Configuration.OVERRIDDEN_NO){
			return false;
		} else if (state == Configuration.OVERRIDDEN_YES){
			return true;
		}

		boolean silkHarvest = blockState.getBlock().canSilkHarvest(world, pos, blockState, mc.player);
		if (!silkHarvest){
			return false;
		}
		
		Random zeroRandom = new NotSoRandom(true);
		Random maxRandom = new NotSoRandom(false);
		
		ItemStack stackedBlock = getSilkTouchDrop(blockState.getBlock(), blockState);
		List<ItemStack> stackedBlockList = Collections.singletonList(stackedBlock);
		
		List<ItemStack> defaultMaxRandom;
		List<ItemStack> defaultZeroRandom;
		
		fakeRandomForWorld(world, maxRandom);
		defaultMaxRandom = getDrops(world, pos, 0);
		unFakeRandomForWorld(world);

		fakeRandomForWorld(world, zeroRandom);
		defaultZeroRandom = getDrops(world, pos, 0);
		unFakeRandomForWorld(world);
		
		if (!ThebombzenAPI.areItemStackCollectionsEqual(stackedBlockList, defaultMaxRandom) || !ThebombzenAPI.areItemStackCollectionsEqual(stackedBlockList, defaultZeroRandom)){
			return true;
		} else {
			return false;
		}
	}

	private static void fakeItemForPlayer(ItemStack itemstack) {
		prevHeldItem = mc.player.inventory.getStackInSlot(mc.player.inventory.currentItem);
		mc.player.inventory.setInventorySlotContents(mc.player.inventory.currentItem, itemstack);
		if (!Tests.isItemStackEmpty(prevHeldItem)) {
			mc.player.getAttributeMap().removeAttributeModifiers(prevHeldItem.getAttributeModifiers(EntityEquipmentSlot.MAINHAND));
		}
		if (!Tests.isItemStackEmpty(itemstack)) {
			mc.player.getAttributeMap().applyAttributeModifiers(itemstack.getAttributeModifiers(EntityEquipmentSlot.MAINHAND));
		}
	}

	private static void fakeRandomForWorld(World world, final Random random) {
		prevRandom = world.rand;
		for (String name : randomNames) {
			try {
				Field field = World.class.getDeclaredField(name);
				field.setAccessible(true);
				try {
					Field modifiersField = Field.class.getDeclaredField("modifiers");
				    modifiersField.setAccessible(true);
				    modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
					field.set(world, random);
					modifiersField.setInt(field, field.getModifiers() | Modifier.FINAL);
					return;
				} catch (Exception e) {
					throw new FieldNotFoundException("Error setting field", e);
				}
			} catch (NoSuchFieldException nsfe) {
				continue;
			}
		}
	}

	public static int getAdjustedBlockStr(double blockStr){
		if (blockStr <= 0){
			return Integer.MIN_VALUE;
		} else {
			return -MathHelper.ceil(1D / blockStr);
		}
	}
	
	public static float getBlockHardness(World world, BlockPos pos) {
		IBlockState blockState = world.getBlockState(pos);
		if (blockState == null) {
			return 0;
		} else {
			return blockState.getBlockHardness(world, pos);
		}
	}

	public static float getBlockStrength(ItemStack itemstack, World world, BlockPos pos) {
		IBlockState blockState = world.getBlockState(pos);
		fakeItemForPlayer(itemstack);
		float str = blockState.getPlayerRelativeBlockHardness(mc.player, world, pos);
		unFakeItemForPlayer();
		return str;
	}
	
	public static float getEff(float str, ItemStack itemstack) {
		if (str <= 1.5F) {
			return str;
		}
		fakeItemForPlayer(itemstack);
		float effLevel = EnchantmentHelper.getEfficiencyModifier(mc.player);
		unFakeItemForPlayer();
		if (effLevel == 0) {
			return str;
		}
		return str + effLevel * effLevel + 1;
	}
	
	public static int getHarvestLevel(ItemStack itemstack, World world, BlockPos pos) {
		IBlockState blockState = world.getBlockState(pos);
		int state = AutoSwitch.instance.getConfiguration().getHarvestOverrideState(itemstack, blockState);
		if (state == Configuration.OVERRIDDEN_NO){
			return -2;
		} else if (state == Configuration.OVERRIDDEN_YES){
			return 2;
		}
		fakeItemForPlayer(Tests.EMPTY_ITEMSTACK);
		boolean noTool = mc.player.canHarvestBlock(blockState);
		unFakeItemForPlayer();
		if (noTool){
			return 0;
		}
		fakeItemForPlayer(itemstack);
		boolean can = blockState.getBlock().canHarvestBlock(world, pos, mc.player);
		unFakeItemForPlayer();
		return can ? 1 : -1;
	}
	
	public static int getFullTinkersConstructItemStackDamage(ItemStack stack,
			EntityLivingBase entity) throws ClassNotFoundException {
		NBTTagCompound tags = stack.getTagCompound();
		NBTTagCompound toolTags = stack.getTagCompound().getCompoundTag(
				"InfiTool");
		int damage = toolTags.getInteger("Attack");
		boolean broken = toolTags.getBoolean("Broken");
		int durability = tags.getCompoundTag("InfiTool").getInteger("Damage");
		float stonebound = tags.getCompoundTag("InfiTool").getFloat("Shoddy");
		float stoneboundDamage = (float) Math.log(durability / 72f + 1) * -2
				* stonebound;
		int earlyModDamage = 0;
		Iterable<?> activeModifiers = ThebombzenAPI.getPrivateField(null,
				Class.forName("tconstruct.library.TConstructRegistry"),
				"activeModifiers");
		Class<?> activeToolModClass = Class
				.forName("tconstruct.library.ActiveToolMod");
		Class<?> toolCoreClass = Class
				.forName("tconstruct.library.tools.ToolCore");
		Class<?>[] attackModClasses = new Class<?>[] { int.class, int.class,
				toolCoreClass, NBTTagCompound.class, NBTTagCompound.class,
				ItemStack.class, EntityLivingBase.class, Entity.class };
		Item tool = stack.getItem();
		for (Object activeToolMod : activeModifiers) {
			earlyModDamage = ThebombzenAPI.invokePrivateMethod(activeToolMod,
					activeToolModClass, "baseAttackDamage", attackModClasses,
					earlyModDamage, damage, tool, tags, toolTags, stack,
					mc.player, entity);
		}
		damage += earlyModDamage;
		if (mc.player.isPotionActive(Potion.getPotionFromResourceLocation("strength"))) {
			damage += 3 << mc.player.getActivePotionEffect(Potion.getPotionFromResourceLocation("strength")).getAmplifier();
		}
		if (mc.player.isPotionActive(Potion.getPotionFromResourceLocation("weakness"))) {
			damage -= 2 << mc.player.getActivePotionEffect(Potion.getPotionFromResourceLocation("weakness")).getAmplifier();
		}
		float enchantDamage = 0;
		if (entity instanceof EntityLivingBase) {
			enchantDamage = EnchantmentHelper.getModifierForCreature(stack, entity.getCreatureAttribute());
		}
		damage += stoneboundDamage;
		if (damage < 1) {
			damage = 1;
		}
		if (mc.player.isSprinting()) {
			float lunge = ThebombzenAPI.invokePrivateMethod(tool,
					toolCoreClass, "chargeAttack", new Class<?>[] {});
			if (lunge > 1f) {
				damage *= lunge;
			}
		}
		int modDamage = 0;
		for (Object activeToolMod : activeModifiers) {
			modDamage = ThebombzenAPI.invokePrivateMethod(activeToolMod,
					activeToolModClass, "attackDamage", attackModClasses,
					modDamage, damage, tool, tags, toolTags, stack,
					mc.player, entity);
		}
		damage += modDamage;
		if (damage > 0 || enchantDamage > 0) {
			boolean criticalHit = mc.player.fallDistance > 0.0F
					&& !mc.player.onGround && !mc.player.isOnLadder()
					&& !mc.player.isInWater()
					&& !mc.player.isPotionActive(Potion.getPotionFromResourceLocation("blindness"))
					&& !mc.player.isRiding();
			for (Object activeToolMod : activeModifiers) {
				if (ThebombzenAPI.<Boolean>invokePrivateMethod(activeToolMod, activeToolModClass, "doesCriticalHit",
						new Class<?>[] { toolCoreClass, NBTTagCompound.class, NBTTagCompound.class, ItemStack.class,
								EntityLivingBase.class, Entity.class },
						tool, tags, toolTags, stack, mc.player, entity)) {
					criticalHit = true;
				}
			}
			if (criticalHit) {
				damage *= 1.5F; // This is not actually accurate. It just makes
								// it fully objective. Also, this is how it is
								// in vanilla now.
			}
			damage += enchantDamage;
			float damageModifier = ThebombzenAPI.invokePrivateMethod(tool,
					toolCoreClass, "getDamageModifier", new Class<?>[] {});
			if (damageModifier != 1f) {
				damage *= damageModifier;
			}
			if (broken) {
				damage = 1;
			}
			return damage;
		} else {
			return 0;
		}
	}
	
	public static boolean isItemStackEmpty(ItemStack itemstack) {
		if (Tests.EMPTY_ITEMSTACK == null) {
			return itemstack == null;
		} else {
			return itemstack.isEmpty();
		}
	}
	
	public static double getFullRegularItemStackDamage(ItemStack itemStack,
			EntityLivingBase entity) {
		fakeItemForPlayer(itemStack);
		double damage = AutoSwitch.instance.getConfiguration()
				.getCustomWeaponDamage(itemStack, entity);
		if (damage < 0) {
			damage = mc.player.getEntityAttribute(
					SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
		}
		// getEnchantmentModifierDamage or getEnchantmentModifierLiving
		double enchDamage = EnchantmentHelper.getModifierForCreature(itemStack, entity.getCreatureAttribute());

		if (damage > 0.0D || enchDamage > 0.0D) {
			boolean critical = mc.player.fallDistance > 0.0F
					&& !mc.player.onGround && !mc.player.isOnLadder()
					&& !mc.player.isInWater()
					&& !mc.player.isPotionActive(Potion.getPotionFromResourceLocation("blindness"))
					&& !mc.player.isRiding();

			if (critical && damage > 0) {

				damage *= 1.5D;
			}
			damage += enchDamage;
		}
		unFakeItemForPlayer();
		return damage;
	}
	
	public static double getFullItemStackDamage(ItemStack itemStack, EntityLivingBase entity){
		try {
			if (!Tests.isItemStackEmpty(itemStack) && itemStack.getTagCompound() != null && Loader.isModLoaded("TConstruct")){
				Class<?> clazz = Class.forName("tconstruct.library.tools.ToolCore");
				if (clazz.isAssignableFrom(itemStack.getItem().getClass())){
					return Tests.getFullTinkersConstructItemStackDamage(itemStack, entity);
				}
			}
		} catch (ClassNotFoundException e){
			AutoSwitch.instance.throwException("Error in Tinkers Construct Compatability", e, false);
		}
		return Tests.getFullRegularItemStackDamage(itemStack, entity);
	}
	
	public static Set<Enchantment> getNonstandardNondamageEnchantmentsOnBothStacks(
			ItemStack stack1, ItemStack stack2) {
		
		Set<Enchantment> bothItemsEnchantments = new HashSet<Enchantment>();

		if (!Tests.isItemStackEmpty(stack1)) {
			bothItemsEnchantments.addAll(EnchantmentHelper.getEnchantments(
					stack1).keySet());
		}
		if (!Tests.isItemStackEmpty(stack2)) {
			bothItemsEnchantments.addAll(EnchantmentHelper.getEnchantments(
					stack2).keySet());
		}

		Iterator<Enchantment> iterator = bothItemsEnchantments.iterator();
		while (iterator.hasNext()) {
			Enchantment enchantment = iterator.next();
			if (enchantment == null) {
				iterator.remove();
				continue;
			}
			ResourceLocation location = Enchantment.REGISTRY.getNameForObject(enchantment);
			if (location == null || "minecraft".equals(location.getResourceDomain())) {
				iterator.remove();
			}
		}

		return bothItemsEnchantments;
	}
	
	private static int getToolOverrideStandardness(ItemStack itemstack, World world, BlockPos pos) {
		IBlockState blockState = world.getBlockState(pos);
		int state = AutoSwitch.instance.getConfiguration().getStandardToolOverrideState(itemstack, blockState);
		if (state == Configuration.OVERRIDDEN_NO){
			return -3;
		} else if (state == Configuration.OVERRIDDEN_YES){
			return 3;
		}
		return 0;
	}
	
	public static ComparableTuple<Integer> getToolStandardness(ItemStack itemstack, World world, BlockPos pos){
		int override = getToolOverrideStandardness(itemstack, world, pos);
		int harvest = getHarvestLevel(itemstack, world, pos);
		return new ComparableTuple<Integer>(override, harvest);
	}
	
	public static ComparableTuple<Integer> getToolEffectiveness(ItemStack itemStack, World world, BlockPos pos){
		int weakStrength = getToolSpeedEffectiveness(itemStack, world, pos);
		int forgeStandard = (!Tests.isItemStackEmpty(itemStack) && ForgeHooks.isToolEffective(world, pos, itemStack)) ? 1 : 0;
		return new ComparableTuple<Integer>(weakStrength, forgeStandard);
	}
	
	public static ComparableTuple<Integer> getToolDamageability(ItemStack itemStack, World world, BlockPos pos){
		int damageable = Tests.isItemStackDamageableOnBlock(itemStack, world, pos) ? -1 : 0;
		return new ComparableTuple<Integer>(damageable);
	}
	
	public static int getToolSpeedEffectiveness(ItemStack itemstack, World world, BlockPos pos){
		IBlockState blockState = world.getBlockState(pos);
		float hardness = Tests.getBlockHardness(world, pos);
		if (hardness <= 0F){
			return 0;
		}
		
		float blockStrForNull = Tests.getBlockStrength(Tests.EMPTY_ITEMSTACK, world, pos);
		fakeItemForPlayer(Tests.EMPTY_ITEMSTACK);
		boolean harvestable = blockState.getBlock().canHarvestBlock(world, pos, mc.player);
		unFakeItemForPlayer();
		
		float blockStr = Tests.getBlockStrength(itemstack, world, pos);
		fakeItemForPlayer(itemstack);
		boolean harvest = blockState.getBlock().canHarvestBlock(world, pos, mc.player);
		unFakeItemForPlayer();
		
		if (harvest && !harvestable){
			blockStr *= 0.3F;
		}
		
		if (blockStr > blockStrForNull * 1.5F){
			return 1;
		} else {
			return 0;
		}
	}

	public static boolean isItemStackDamageable(ItemStack itemstack) {
		return !Tests.isItemStackEmpty(itemstack) && itemstack.isItemStackDamageable();
	}

	public static boolean isItemStackDamageableOnBlock(ItemStack itemstack,
			World world, BlockPos pos) {
		IBlockState blockState = world.getBlockState(pos);
		int state = AutoSwitch.instance.getConfiguration().getDamageableOverrideState(itemstack, blockState);
		if (state == Configuration.OVERRIDDEN_NO){
			return false;
		} else if (state == Configuration.OVERRIDDEN_YES){
			return true;
		}
		if (!isItemStackDamageable(itemstack)) {
			return false;
		}
		return getBlockHardness(world, pos) != 0.0F;
	}

	public static boolean isSword(ItemStack itemstack) {
		if (Tests.isItemStackEmpty(itemstack)){
			return false;
		}
		if (itemstack.getItem() instanceof ItemSword){
			return true;
		}
		String name = Item.REGISTRY.getNameForObject(itemstack.getItem()).toString();
		if (name.endsWith("sword")){
			return true;
		}
		return false;
	}

	private static void unFakeItemForPlayer() {
		ItemStack fakedStack = mc.player.inventory.getStackInSlot(mc.player.inventory.currentItem);
		mc.player.inventory.setInventorySlotContents(mc.player.inventory.currentItem, prevHeldItem);
		if (!Tests.isItemStackEmpty(fakedStack)) {
			mc.player.getAttributeMap().removeAttributeModifiers(
					fakedStack.getAttributeModifiers(EntityEquipmentSlot.MAINHAND));
		}
		if (!Tests.isItemStackEmpty(prevHeldItem)) {
			mc.player.getAttributeMap().applyAttributeModifiers(
					prevHeldItem.getAttributeModifiers(EntityEquipmentSlot.MAINHAND));
		}
	}
	
	private static void unFakeRandomForWorld(World world) {
		for (String name : randomNames) {
			try {
				Field field = World.class.getDeclaredField(name);
				field.setAccessible(true);
				try {
					Field modifiersField = Field.class.getDeclaredField("modifiers");
				    modifiersField.setAccessible(true);
				    modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
					field.set(world, prevRandom);
					modifiersField.setInt(field, field.getModifiers() | Modifier.FINAL);
					return;
				} catch (Exception e) {
					throw new FieldNotFoundException("Error setting field", e);
				}
			} catch (NoSuchFieldException nsfe) {
				continue;
			}
		}
		prevRandom = null;
	}
	
	private Tests() {
		
	}
}
