package me.kowalkowski.mummy.world;

import com.mojang.serialization.MapCodec;
import java.util.Arrays;
import java.util.Optional;
import me.kowalkowski.mummy.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * An abandoned zombie kingdom: a ruined village inside castle walls around a big cobblestone castle,
 * home to the Zombie King, the Zombie Princess and their knights and archers. Only generates on
 * fairly flat, dry land; the terrain is flattened under it (terrain_adaptation in the structure JSON).
 */
public class KingdomStructure extends Structure {
	public static final MapCodec<KingdomStructure> CODEC = simpleCodec(KingdomStructure::new);
	/** Largest height difference across the footprint that the kingdom will flatten. */
	private static final int MAX_SLOPE = 12;

	public KingdomStructure(final Structure.StructureSettings settings) {
		super(settings);
	}

	@Override
	public Optional<Structure.GenerationStub> findGenerationPoint(final Structure.GenerationContext context) {
		int centerX = context.chunkPos().getMiddleBlockX();
		int centerZ = context.chunkPos().getMiddleBlockZ();
		int[] heights = new int[9];
		int i = 0;
		for (int dx = -KingdomPiece.REACH; dx <= KingdomPiece.REACH; dx += KingdomPiece.REACH) {
			for (int dz = -KingdomPiece.REACH; dz <= KingdomPiece.REACH; dz += KingdomPiece.REACH) {
				int x = centerX + dx;
				int z = centerZ + dz;
				int surface = context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
				int floor = context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
				if (surface != floor) {
					return Optional.empty(); // water: no kingdoms in lakes or rivers
				}
				heights[i++] = surface;
			}
		}
		Arrays.sort(heights);
		if (heights[8] - heights[0] > MAX_SLOPE) {
			return Optional.empty();
		}
		BlockPos center = new BlockPos(centerX, heights[4] + 1, centerZ);
		long seed = context.random().nextLong();
		return Optional.of(new Structure.GenerationStub(center, builder -> builder.addPiece(new KingdomPiece(center, seed))));
	}

	@Override
	public StructureType<?> type() {
		return ModStructures.KINGDOM;
	}
}
