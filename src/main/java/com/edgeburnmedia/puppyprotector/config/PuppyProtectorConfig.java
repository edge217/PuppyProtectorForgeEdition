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

package com.edgeburnmedia.puppyprotector.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;

public class PuppyProtectorConfig {
	public static final ForgeConfigSpec GENERAL_SPEC;
	public static ForgeConfigSpec.ConfigValue<List<? extends String>> protectedEntities;
	public static ForgeConfigSpec.ConfigValue<List<? extends String>> peacefullyProtectedEntities;
	public static ForgeConfigSpec.BooleanValue protectNamedEntities;
	public static ForgeConfigSpec.BooleanValue protectNamedEntitiesPeacefully;

	static {
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
		setupConfig(builder);
		GENERAL_SPEC = builder.build();
	}

	private static void setupConfig(ForgeConfigSpec.Builder builder) {
		builder.comment("Puppy Protector Config");
		builder.push("Puppy Protector Config");
		protectedEntities = builder
				.comment("List of entities that are protected from being attacked by players")
				.defineList("protected_entities", List.of("minecraft:wolf", "minecraft:cat"), o -> o instanceof String);

		peacefullyProtectedEntities = builder
				.comment("List of entities that will be protected from player damage, but the player will not be killed if they try to harm these")
				.defineListAllowEmpty(List.of("peacefully_protected"), ArrayList::new, o -> o instanceof String && protectedEntities.get().contains(o));

		protectNamedEntities = builder
				.comment("Protect entities that have name tags.", "Not recommended as some mods add named hostile mobs that can spawn randomly.")
				.define("protected_named", false);

		protectNamedEntitiesPeacefully = builder
				.comment("If set to false, players attacking named entities will not be killed for trying to do so.")
						.define("peacefully_protect_named_entities", true);

		builder.pop();
	}
}
