/*
 * Copyright (c) 2022 Edgeburn Media
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later
 * version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>
 */

package com.edgeburnmedia.puppyprotector;

import com.edgeburnmedia.puppyprotector.config.PuppyProtectorConfig;
import com.edgeburnmedia.puppyprotector.item.ProtectingWandItem;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.ArrayList;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(PuppyProtector.MODID)
public class PuppyProtector {

	// Define mod id in a common place for everything to reference
	public static final String MODID = "puppyprotector";
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
	public static final RegistryObject<Item> PROTECTING_WAND = ITEMS.register("protecting_wand", ProtectingWandItem::new);
	// Directly reference a slf4j logger
	private static final Logger LOGGER = LogUtils.getLogger();
	private static Registry<EntityType<?>> entityTypeRegistryCache;

	private static ArrayList<ResourceLocation> cachedProtectedEntities = new ArrayList<>();
	private static ArrayList<ResourceLocation> cachedPeacefullyProtectedEntities = new ArrayList<>();
	private static boolean hasCached = false;

	public PuppyProtector() {
		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

		// Register the commonSetup method for modloading
		modEventBus.addListener(this::commonSetup);

		ITEMS.register(modEventBus);

		// Register ourselves for server and other game events we are interested in
		MinecraftForge.EVENT_BUS.register(this);

		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, PuppyProtectorConfig.GENERAL_SPEC, "puppyprotector.toml");

	}

	private void commonSetup(final FMLCommonSetupEvent event) {
		// Some common setup code

	}

	@SubscribeEvent
	public void onEntityAttack(AttackEntityEvent event) {
		// Do something when the player attacks an entity
		Entity target = event.getTarget();
		Player attacker = event.getEntity();

		if (!target.getLevel().isClientSide()) {
			if (!hasCached) {
				entityTypeRegistryCache = target.getLevel().registryAccess().registryOrThrow(Registry.ENTITY_TYPE_REGISTRY);
				PuppyProtectorConfig.protectedEntities.get().forEach(entity -> cachedProtectedEntities.add(new ResourceLocation(entity)));
				PuppyProtectorConfig.peacefullyProtectedEntities.get().forEach(entity -> cachedPeacefullyProtectedEntities.add(new ResourceLocation(entity)));
				hasCached = true;
			}

			ResourceLocation targetResourceLocation = entityTypeRegistryCache.getKey(target.getType());

			boolean protectedWithNbt = isProtectedWithNbt(target);
			if (protectedWithNbt) {
				if (target instanceof LivingEntity livingEntity) {

					// make sure the target is being protected from any further attempts to damage as well as lightning
					addProtectionEffects(livingEntity);

					event.setCanceled(true);

					String description = getEntityDescription(target);
					attacker.sendSystemMessage(Component.literal("You are a monster for trying to hurt " + description + "!").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));

					if (!cachedPeacefullyProtectedEntities.contains(targetResourceLocation)) {
						smite((ServerPlayer) attacker);
					}
				} else {
					LOGGER.warn("Entity {} has puppyprotector:is_protected data but isn't a LivingEntity", targetResourceLocation);
				}
			}

			if (PuppyProtectorConfig.protectNamedEntities.get() && target.hasCustomName()) {
				if (target instanceof LivingEntity e) {
					addProtectionEffects(e);
				} else {
					LOGGER.warn("Entity {} has a custom name but is not a LivingEntity", targetResourceLocation);
					return;
				}
				attacker.sendSystemMessage(Component.literal("You are a monster for trying to hurt " + target.getName().getString() + "!").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
				if (!PuppyProtectorConfig.protectNamedEntitiesPeacefully.get()) {
					smite((ServerPlayer) attacker);
				}
				event.setCanceled(true);
				return;
			}

			if (cachedProtectedEntities.contains(targetResourceLocation)) {
				if (target instanceof LivingEntity livingEntity) {

					// make sure the target is being protected from any further attempts to damage as well as lightning
					addProtectionEffects(livingEntity);

					event.setCanceled(true);

					attacker.sendSystemMessage(Component.literal("You are a monster for trying to hurt a " + target.getType().getDescription().getString() + "!").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));

					if (!cachedPeacefullyProtectedEntities.contains(targetResourceLocation)) {
						smite((ServerPlayer) attacker);
					}
				} else {
					LOGGER.warn("Entity {} is in the protectedEntities list but is not a LivingEntity", targetResourceLocation);
				}
			}
		}
	}

	public static boolean isProtectedWithNbt(Entity target) {
		CompoundTag tag = target.getPersistentData();
        return tag.getBoolean(MODID + ":is_protected");
	}

	@NotNull
	public static String getEntityDescription(Entity target) {
        return target.hasCustomName() ? target.getCustomName().getString() : target.getType().getDescription().getString();
	}

	private void addProtectionEffects(LivingEntity entity) {
		entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 72000, 9));
		entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 72000, 9));
		entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 72000, 9));
	}

	private void smite(ServerPlayer player) {
		LightningBolt lightningBolt = EntityType.LIGHTNING_BOLT.create(player.getLevel());
		if (lightningBolt != null) {
			lightningBolt.moveTo(player.getX(), player.getY(), player.getZ());
			lightningBolt.setVisualOnly(true);
			player.getLevel().addFreshEntity(lightningBolt);
			player.hurt(DamageSource.LIGHTNING_BOLT, Float.MAX_VALUE);
		}
		if (!player.isDeadOrDying()) {
			player.kill();
		}
	}

}
