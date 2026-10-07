package me.kowalkowski.mummy.registry;

import me.kowalkowski.mummy.MummyMod;
import me.kowalkowski.mummy.world.KingdomPiece;
import me.kowalkowski.mummy.world.KingdomStructure;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

/** The kingdom's structure type and piece type. Where and how often it generates is data: see data/mummy/worldgen. */
public final class ModStructures {
	public static final StructureType<KingdomStructure> KINGDOM = Registry.register(
		BuiltInRegistries.STRUCTURE_TYPE, MummyMod.id("kingdom"), () -> KingdomStructure.CODEC
	);
	public static final StructurePieceType KINGDOM_PIECE = Registry.register(
		BuiltInRegistries.STRUCTURE_PIECE, MummyMod.id("kingdom"), (StructurePieceType.ContextlessType) KingdomPiece::new
	);

	private ModStructures() {
	}

	public static void init() {
	}
}
