package me.kowalkowski.zombiekingdom.world;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import me.kowalkowski.zombiekingdom.entity.RoyalGuard;
import me.kowalkowski.zombiekingdom.entity.ZombieKnight;
import me.kowalkowski.zombiekingdom.registry.ModEntities;
import me.kowalkowski.zombiekingdom.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.equine.ZombieHorse;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import org.jspecify.annotations.Nullable;

/**
 * The whole kingdom as one piece, built block by block in local coordinates around {@link #center}
 * (x/z offsets, y = 0 is the courtyard's walking level, the ground layer is y = -1). Each chunk only
 * places its own share, so every decision is a pure function of the position and {@link #seed}.
 *
 * <pre>
 *   corner tower ── curtain wall (Chebyshev distance 35..36) ── corner tower
 *        |   houses (~26) · outer ring road (20..21) · houses              |
 *        |      inner ring road (15..16) · castle keep (11) + towers        |
 *   corner tower ──────────── gatehouse (south, +z) ───────────── corner tower
 * </pre>
 */
public class KingdomPiece extends StructurePiece {
	/** Distance from the centre to the outer edge of the corner towers. */
	public static final int REACH = 40;
	private static final int WALL = 36;
	private static final int TOP = 24;
	private static final int GUARD_HOME_RADIUS = 44;
	/** The stable: east side of the village, open front (fence and gates) facing west onto the outer ring road. */
	private static final int STABLE_FRONT_X = 23;
	private static final int STABLE_BACK_X = 29;
	private static final int STABLE_MIN_Z = -4;
	private static final int STABLE_MAX_Z = 16;
	/** First block of each 4-wide stall; stalls are separated by a fence. */
	private static final int[] STALLS = {-3, 2, 7, 12};

	private final BlockPos center;
	private final long seed;
	private final List<House> houses = new ArrayList<>();
	private final Set<Long> walks = new HashSet<>();

	private record House(int x, int z, int halfWidth, int halfDepth, Direction door, boolean cobblestone, float ruin) {
	}

	public KingdomPiece(final BlockPos center, final long seed) {
		super(ModStructures.KINGDOM_PIECE, 0, new BoundingBox(
			center.getX() - REACH - 1, center.getY(), center.getZ() - REACH - 1,
			center.getX() + REACH + 1, center.getY() + TOP, center.getZ() + REACH + 1
		));
		this.center = center;
		this.seed = seed;
		this.planVillage();
	}

	public KingdomPiece(final CompoundTag tag) {
		super(ModStructures.KINGDOM_PIECE, tag);
		this.center = new BlockPos(tag.getIntOr("CX", 0), tag.getIntOr("CY", 0), tag.getIntOr("CZ", 0));
		this.seed = tag.getLongOr("Seed", 0L);
		this.planVillage();
	}

	@Override
	protected void addAdditionalSaveData(final StructurePieceSerializationContext context, final CompoundTag tag) {
		tag.putInt("CX", this.center.getX());
		tag.putInt("CY", this.center.getY());
		tag.putInt("CZ", this.center.getZ());
		tag.putLong("Seed", this.seed);
	}

	// ------------------------------------------------------------------------------------- layout --

	/** Picks which house plots are built, and how; all from the seed, so every chunk agrees. */
	private void planVillage() {
		RandomSource random = RandomSource.create(this.seed);
		int[] sideRows = {-24, -12, 0, 12, 24};
		List<int[]> plots = new ArrayList<>();
		for (int z : sideRows) {
			plots.add(new int[] {-26, z, Direction.EAST.ordinal()});
			if (z < STABLE_MIN_Z || z > STABLE_MAX_Z) {
				plots.add(new int[] {26, z, Direction.WEST.ordinal()}); // the middle of the east side is the stable
			}
		}
		plots.add(new int[] {-12, -26, Direction.SOUTH.ordinal()});
		plots.add(new int[] {12, -26, Direction.SOUTH.ordinal()});
		plots.add(new int[] {-12, 26, Direction.NORTH.ordinal()});
		plots.add(new int[] {12, 26, Direction.NORTH.ordinal()});

		for (int[] plot : plots) {
			boolean built = random.nextFloat() < 0.8F;
			int halfWidth = 2 + random.nextInt(2);
			int halfDepth = 2 + random.nextInt(2);
			boolean cobblestone = random.nextBoolean();
			float ruin = 0.2F + random.nextFloat() * 0.6F;
			if (!built) {
				continue;
			}
			Direction door = Direction.values()[plot[2]];
			House house = new House(plot[0], plot[1], halfWidth, halfDepth, door, cobblestone, ruin);
			this.houses.add(house);
			// A short walk from the door to the outer ring road: straight out, then (for corner plots) along to the ring.
			int x = house.x() + door.getStepX() * (halfWidth + 1);
			int z = house.z() + door.getStepZ() * (halfDepth + 1);
			for (int step = 0; step < 32 && cheb(x, z) > 21; step++) {
				this.walks.add(key(x, z));
				boolean outAlongDoor = door.getStepX() != 0 ? Math.abs(x) > 21 : Math.abs(z) > 21;
				if (outAlongDoor) {
					x += door.getStepX();
					z += door.getStepZ();
				} else if (door.getStepX() != 0) {
					z -= Integer.signum(z);
				} else {
					x -= Integer.signum(x);
				}
			}
		}
	}

	private static long key(final int x, final int z) {
		return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
	}

	private static int cheb(final int x, final int z) {
		return Math.max(Math.abs(x), Math.abs(z));
	}

	private boolean isRoad(final int x, final int z) {
		int d = cheb(x, z);
		boolean innerRing = d == 15 || d == 16;
		boolean outerRing = d == 20 || d == 21;
		boolean spoke = (Math.abs(x) <= 1 || Math.abs(z) <= 1) && d >= 15 && d <= 21;
		boolean gateRoad = Math.abs(x) <= 1 && z >= 21 && z <= 34;
		return innerRing || outerRing || spoke || gateRoad || this.walks.contains(key(x, z));
	}

	// -------------------------------------------------------------------------------- generation --

	@Override
	public void postProcess(
		final WorldGenLevel level,
		final StructureManager structureManager,
		final ChunkGenerator generator,
		final RandomSource random,
		final BoundingBox chunkBB,
		final ChunkPos chunkPos,
		final BlockPos referencePos
	) {
		Builder builder = new Builder(level, chunkBB);
		builder.prepareGround();
		builder.roads();
		for (House house : this.houses) {
			builder.house(house);
		}
		builder.stable();
		builder.well(0, -26);
		builder.lampPosts();
		builder.curtainWall();
		builder.cornerTowers();
		builder.gatehouse();
		builder.castle();
		builder.residents();
	}

	/** Places blocks and mobs for one chunk. */
	private final class Builder {
		private final WorldGenLevel level;
		private final BoundingBox chunk;
		private final int minX;
		private final int maxX;
		private final int minZ;
		private final int maxZ;

		Builder(final WorldGenLevel level, final BoundingBox chunk) {
			this.level = level;
			this.chunk = chunk;
			this.minX = Math.max(-REACH, chunk.minX() - KingdomPiece.this.center.getX());
			this.maxX = Math.min(REACH, chunk.maxX() - KingdomPiece.this.center.getX());
			this.minZ = Math.max(-REACH, chunk.minZ() - KingdomPiece.this.center.getZ());
			this.maxZ = Math.min(REACH, chunk.maxZ() - KingdomPiece.this.center.getZ());
		}

		// ---- helpers ----

		private BlockPos pos(final int x, final int y, final int z) {
			return KingdomPiece.this.center.offset(x, y, z);
		}

		private boolean here(final int x, final int z) {
			return x >= this.minX && x <= this.maxX && z >= this.minZ && z <= this.maxZ;
		}

		private void set(final int x, final int y, final int z, final BlockState state) {
			if (this.here(x, z)) {
				this.level.setBlock(this.pos(x, y, z), state, 2);
			}
		}

		private BlockState get(final int x, final int y, final int z) {
			return this.level.getBlockState(this.pos(x, y, z));
		}

		/** A stable pseudo-random number in [0, 1) for a position, the same in every chunk. */
		private float noise(final int x, final int y, final int z, final int salt) {
			long h = KingdomPiece.this.seed ^ x * 0x9E3779B97F4A7C15L ^ y * 0xC2B2AE3D27D4EB4FL ^ z * 0x165667B19E3779F9L ^ salt * 0x27D4EB2F165667C5L;
			h = (h ^ h >>> 30) * 0xBF58476D1CE4E5B9L;
			h = (h ^ h >>> 27) * 0x94D049BB133111EBL;
			h ^= h >>> 31;
			return (h >>> 40) / (float) (1L << 24);
		}

		/** Weathered cobblestone: about one block in five is mossy. */
		private BlockState stone(final int x, final int y, final int z) {
			return this.noise(x, y, z, 1) < 0.2F ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState();
		}

		private void fill(final int x0, final int y0, final int z0, final int x1, final int y1, final int z1, final BlockState state) {
			for (int x = Math.max(x0, this.minX); x <= Math.min(x1, this.maxX); x++) {
				for (int z = Math.max(z0, this.minZ); z <= Math.min(z1, this.maxZ); z++) {
					for (int y = y0; y <= y1; y++) {
						this.set(x, y, z, state);
					}
				}
			}
		}

		private void wallBanner(final int x, final int y, final int z, final Direction facing) {
			this.set(x, y, z, Blocks.WALL_BANNER.pick(DyeColor.RED).defaultBlockState().setValue(WallBannerBlock.FACING, facing));
		}

		private void standingBanner(final int x, final int y, final int z) {
			this.set(x, y, z, Blocks.BANNER.pick(DyeColor.RED).defaultBlockState().setValue(BannerBlock.ROTATION, (int) (this.noise(x, y, z, 2) * 16)));
		}

		private void ladder(final int x, final int y0, final int y1, final int z, final Direction facing) {
			for (int y = y0; y <= y1; y++) {
				this.set(x, y, z, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, facing));
			}
		}

		// ---- ground and village ----

		/** Clears trees and hills above the courtyard and fills dips below it, then lays grass. */
		void prepareGround() {
			for (int x = this.minX; x <= this.maxX; x++) {
				for (int z = this.minZ; z <= this.maxZ; z++) {
					for (int y = 0; y <= TOP + 6; y++) {
						if (!this.get(x, y, z).isAir()) {
							this.set(x, y, z, Blocks.AIR.defaultBlockState());
						}
					}
					this.set(x, -1, z, this.noise(x, -1, z, 3) < 0.08F ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState());
					for (int y = -2; y >= -16; y--) {
						BlockState below = this.get(x, y, z);
						boolean soft = below.isAir() || !below.getFluidState().isEmpty() || below.canBeReplaced()
							|| below.is(BlockTags.LOGS) || below.is(BlockTags.LEAVES);
						if (!soft) {
							break;
						}
						this.set(x, y, z, Blocks.DIRT.defaultBlockState());
					}
				}
			}
		}

		void roads() {
			for (int x = this.minX; x <= this.maxX; x++) {
				for (int z = this.minZ; z <= this.maxZ; z++) {
					if (cheb(x, z) <= 34 && KingdomPiece.this.isRoad(x, z)) {
						float n = this.noise(x, -1, z, 4);
						BlockState road = n < 0.12F ? Blocks.GRAVEL.defaultBlockState()
							: n < 0.2F ? Blocks.COBBLESTONE.defaultBlockState()
							: Blocks.DIRT_PATH.defaultBlockState();
						this.set(x, -1, z, road);
					}
				}
			}
		}

		/** An abandoned house: broken walls, holes in the roof, cobwebs and a few leftovers. */
		void house(final House house) {
			int w = house.halfWidth();
			int d = house.halfDepth();
			int doorX = house.x() + house.door().getStepX() * w;
			int doorZ = house.z() + house.door().getStepZ() * d;
			for (int x = house.x() - w; x <= house.x() + w; x++) {
				for (int z = house.z() - d; z <= house.z() + d; z++) {
					boolean edgeX = Math.abs(x - house.x()) == w;
					boolean edgeZ = Math.abs(z - house.z()) == d;
					this.set(x, -1, z, Blocks.COBBLESTONE.defaultBlockState());
					for (int y = 0; y <= 3; y++) {
						BlockState state;
						if (edgeX && edgeZ) {
							state = Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
						} else if (edgeX || edgeZ) {
							boolean doorway = x == doorX && z == doorZ && y <= 1;
							boolean window = y == 1 && (edgeX ? (z - house.z()) % 2 != 0 : (x - house.x()) % 2 != 0) && Math.abs(edgeX ? z - house.z() : x - house.x()) == 1;
							boolean broken = this.noise(x, y, z, 5) < house.ruin() * (y == 3 ? 0.6F : 0.3F);
							if (doorway || broken) {
								state = Blocks.AIR.defaultBlockState();
							} else if (window) {
								state = this.noise(x, y, z, 6) < 0.5F ? Blocks.AIR.defaultBlockState() : Blocks.GLASS.defaultBlockState();
							} else {
								state = house.cobblestone() ? this.stone(x, y, z) : Blocks.OAK_PLANKS.defaultBlockState();
							}
						} else {
							state = Blocks.AIR.defaultBlockState();
						}
						this.set(x, y, z, state);
					}
					// The roof, with holes.
					if (this.noise(x, 4, z, 7) >= house.ruin() * 0.5F) {
						this.set(x, 4, z, edgeX || edgeZ ? Blocks.OAK_SLAB.defaultBlockState() : Blocks.OAK_PLANKS.defaultBlockState());
					}
				}
			}
			// Cobwebs in the corners and something left behind.
			int[][] corners = {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}};
			for (int[] c : corners) {
				int x = house.x() + c[0] * (w - 1);
				int z = house.z() + c[1] * (d - 1);
				if (this.noise(x, 3, z, 8) < 0.6F) {
					this.set(x, 3, z, Blocks.COBWEB.defaultBlockState());
				}
			}
			BlockState[] leftovers = {
				Blocks.CRAFTING_TABLE.defaultBlockState(), Blocks.BARREL.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(),
				Blocks.FURNACE.defaultBlockState(), Blocks.CAULDRON.defaultBlockState(), Blocks.HAY_BLOCK.defaultBlockState()
			};
			// In the back corner, away from the door.
			int lx = house.x() + (w - 1) * (house.door().getStepX() != 0 ? -house.door().getStepX() : 1);
			int lz = house.z() + (d - 1) * (house.door().getStepZ() != 0 ? -house.door().getStepZ() : 1);
			this.set(lx, 0, lz, leftovers[(int) (this.noise(lx, 0, lz, 9) * leftovers.length)]);
		}

		void well(final int cx, final int cz) {
			for (int x = cx - 2; x <= cx + 2; x++) {
				for (int z = cz - 2; z <= cz + 2; z++) {
					boolean rim = Math.abs(x - cx) == 2 || Math.abs(z - cz) == 2;
					this.set(x, -1, z, Blocks.COBBLESTONE.defaultBlockState());
					this.set(x, 0, z, rim ? this.stone(x, 0, z) : Blocks.WATER.defaultBlockState());
					if (!rim) {
						this.fill(x, -4, z, x, -2, z, Blocks.WATER.defaultBlockState());
					}
					if (Math.abs(x - cx) == 2 && Math.abs(z - cz) == 2) {
						this.fill(x, 1, z, x, 2, z, Blocks.OAK_FENCE.defaultBlockState());
					}
					this.set(x, 3, z, Blocks.COBBLESTONE_SLAB.defaultBlockState());
				}
			}
		}

		/** Lamp posts at the road corners; some have lost their lantern. */
		void lampPosts() {
			int[] corners = {17, 22}; // just outside each ring road's corner
			for (int r : corners) {
				for (int sx = -1; sx <= 1; sx += 2) {
					for (int sz = -1; sz <= 1; sz += 2) {
						int x = sx * r;
						int z = sz * r;
						this.fill(x, 0, z, x, 2, z, Blocks.OAK_FENCE.defaultBlockState());
						if (this.noise(x, 3, z, 10) < 0.6F) {
							this.set(x, 3, z, Blocks.LANTERN.defaultBlockState());
						}
					}
				}
			}
		}

		/** A timber stable with four stalls behind fences and closed gates; each has hay, a water trough and a zombie horse. */
		void stable() {
			BlockState log = Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
			BlockState planks = Blocks.OAK_PLANKS.defaultBlockState();
			BlockState fence = Blocks.OAK_FENCE.defaultBlockState();
			for (int x = STABLE_FRONT_X; x <= STABLE_BACK_X; x++) {
				for (int z = STABLE_MIN_Z; z <= STABLE_MAX_Z; z++) {
					boolean wall = x == STABLE_BACK_X || z == STABLE_MIN_Z || z == STABLE_MAX_Z;
					this.set(x, -1, z, wall ? Blocks.COBBLESTONE.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState());
					for (int y = 0; y <= 3; y++) {
						this.set(x, y, z, wall ? planks : Blocks.AIR.defaultBlockState());
					}
					this.set(x, 4, z, planks); // roof
				}
				this.set(x, 4, STABLE_MIN_Z - 1, Blocks.OAK_SLAB.defaultBlockState());
				this.set(x, 4, STABLE_MAX_Z + 1, Blocks.OAK_SLAB.defaultBlockState());
			}
			for (int z = STABLE_MIN_Z - 1; z <= STABLE_MAX_Z + 1; z++) {
				this.set(STABLE_FRONT_X - 1, 4, z, Blocks.OAK_SLAB.defaultBlockState()); // eaves over the front
				this.set(STABLE_FRONT_X - 1, -1, z, Blocks.DIRT_PATH.defaultBlockState());
			}
			// Posts: corners, and at both ends of every divider.
			for (int z : new int[] {STABLE_MIN_Z, STALLS[1] - 1, STALLS[2] - 1, STALLS[3] - 1, STABLE_MAX_Z}) {
				this.fill(STABLE_FRONT_X, 0, z, STABLE_FRONT_X, 3, z, log);
				this.fill(STABLE_BACK_X, 0, z, STABLE_BACK_X, 3, z, log);
				if (z != STABLE_MIN_Z && z != STABLE_MAX_Z) {
					this.fill(STABLE_FRONT_X + 1, 0, z, STABLE_BACK_X - 1, 1, z, fence); // divider
				}
			}
			for (int stall : STALLS) {
				int gateZ = stall + 1;
				for (int z = stall; z <= stall + 3 && z < STABLE_MAX_Z; z++) {
					this.set(STABLE_FRONT_X, 0, z, z == gateZ
						? Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.WEST)
						: fence);
				}
				this.set(STABLE_BACK_X - 1, 0, stall, Blocks.HAY_BLOCK.defaultBlockState());
				this.set(STABLE_BACK_X - 1, 0, stall + 2, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
				this.set(STABLE_FRONT_X + 3, 3, stall + 1, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
			}
			// Spare saddles for anyone who tames a horse.
			this.set(STABLE_BACK_X - 1, 0, STABLE_MAX_Z - 1, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.WEST));
			if (this.here(STABLE_BACK_X - 1, STABLE_MAX_Z - 1)
				&& this.level.getBlockEntity(this.pos(STABLE_BACK_X - 1, 0, STABLE_MAX_Z - 1)) instanceof ChestBlockEntity chest) {
				chest.setItem(0, new ItemStack(Items.SADDLE));
				chest.setItem(1, new ItemStack(Items.SADDLE));
				chest.setItem(2, new ItemStack(Items.LEAD, 2));
				chest.setItem(4, new ItemStack(Items.GOLDEN_CARROT, 6));
			}
		}

		// ---- fortifications ----

		void curtainWall() {
			for (int x = this.minX; x <= this.maxX; x++) {
				for (int z = this.minZ; z <= this.maxZ; z++) {
					int d = cheb(x, z);
					if (d < WALL - 1 || d > WALL || Math.abs(x) > WALL - 4 && Math.abs(z) > WALL - 4) {
						continue; // not the wall, or inside a corner tower
					}
					for (int y = -1; y <= 6; y++) {
						this.set(x, y, z, this.stone(x, y, z));
					}
					if (d == WALL && ((x + z) & 1) == 0) {
						this.set(x, 7, z, this.stone(x, 7, z)); // battlements
					}
				}
			}
			// Banners: outside facing out, inside facing the courtyard.
			for (int along = -24; along <= 24; along += 12) {
				for (Direction side : Direction.Plane.HORIZONTAL) {
					int ox = side.getStepX();
					int oz = side.getStepZ();
					if (side == Direction.SOUTH && Math.abs(along) < 9) {
						continue; // gatehouse
					}
					int x = ox != 0 ? ox * (WALL + 1) : along;
					int z = oz != 0 ? oz * (WALL + 1) : along;
					this.wallBanner(x, 5, z, side);
					int ix = ox != 0 ? ox * (WALL - 2) : along + 6;
					int iz = oz != 0 ? oz * (WALL - 2) : along + 6;
					if (Math.abs(along + 6) <= 24 && !(side == Direction.SOUTH && Math.abs(along + 6) < 9)) {
						this.wallBanner(ix, 5, iz, side.getOpposite());
					}
				}
			}
			// Ladders up to the wall walk.
			this.ladder(-(WALL - 2), 0, 6, 8, Direction.EAST);
			this.ladder(WALL - 2, 0, 6, -8, Direction.WEST);
			this.ladder(8, 0, 6, -(WALL - 2), Direction.SOUTH);
			this.ladder(-12, 0, 6, WALL - 2, Direction.NORTH);
		}

		void cornerTowers() {
			for (int sx = -1; sx <= 1; sx += 2) {
				for (int sz = -1; sz <= 1; sz += 2) {
					int cx = sx * WALL;
					int cz = sz * WALL;
					int ix = -sx;
					int iz = -sz;
					for (int dx = -4; dx <= 4; dx++) {
						for (int dz = -4; dz <= 4; dz++) {
							int x = cx + dx;
							int z = cz + dz;
							boolean ring = Math.max(Math.abs(dx), Math.abs(dz)) == 4;
							for (int y = -1; y <= 11; y++) {
								boolean room = !ring && y >= 7 && y <= 10;
								this.set(x, y, z, room ? Blocks.AIR.defaultBlockState() : this.stone(x, y, z));
							}
							if (ring && ((dx + dz) & 1) == 0) {
								this.set(x, 12, z, this.stone(x, 12, z));
							}
						}
					}
					// Doorways from the wall walk into the tower room.
					for (int off = 0; off <= 1; off++) {
						this.fill(cx + 4 * ix, 7, cz + off * iz, cx + 4 * ix, 8, cz + off * iz, Blocks.AIR.defaultBlockState());
						this.fill(cx + off * ix, 7, cz + 4 * iz, cx + off * ix, 8, cz + 4 * iz, Blocks.AIR.defaultBlockState());
					}
					// A ladder up through a hatch to the roof, and the kingdom's banner on top.
					int lx = cx - 3 * ix;
					this.ladder(lx, 7, 11, cz, ix > 0 ? Direction.EAST : Direction.WEST);
					this.standingBanner(cx, 12, cz);
					this.wallBanner(cx - 5 * ix, 9, cz, ix > 0 ? Direction.WEST : Direction.EAST);
					this.wallBanner(cx, 9, cz - 5 * iz, iz > 0 ? Direction.NORTH : Direction.SOUTH);
				}
			}
		}

		void gatehouse() {
			for (int side = -1; side <= 1; side += 2) {
				for (int x = 3; x <= 7; x++) {
					for (int z = WALL - 3; z <= WALL + 2; z++) {
						int gx = side * x;
						for (int y = -1; y <= 9; y++) {
							this.set(gx, y, z, this.stone(gx, y, z));
						}
						boolean edge = x == 3 || x == 7 || z == WALL - 3 || z == WALL + 2;
						if (edge && ((gx + z) & 1) == 0) {
							this.set(gx, 10, z, this.stone(gx, 10, z));
						}
					}
				}
				this.standingBanner(side * 5, 10, WALL);
				this.wallBanner(side * 5, 6, WALL + 3, Direction.SOUTH);
				this.wallBanner(side * 5, 6, WALL - 4, Direction.NORTH);
			}
			// The gate: an opening with the portcullis raised (its teeth showing at the top).
			this.fill(-2, 0, WALL - 1, 2, 3, WALL, Blocks.AIR.defaultBlockState());
			this.fill(-2, 4, WALL, 2, 4, WALL, Blocks.IRON_BARS.defaultBlockState());
			this.fill(-2, -1, WALL - 1, 2, -1, WALL, Blocks.COBBLESTONE.defaultBlockState());
		}

		// ---- the castle ----

		private int towerDistance(final int x, final int z) {
			int best = Integer.MAX_VALUE;
			for (int tx = -11; tx <= 11; tx += 22) {
				for (int tz = -11; tz <= 11; tz += 22) {
					best = Math.min(best, Math.max(Math.abs(x - tx), Math.abs(z - tz)));
				}
			}
			return best;
		}

		void castle() {
			BlockState planks = Blocks.OAK_PLANKS.defaultBlockState();
			BlockState air = Blocks.AIR.defaultBlockState();
			for (int x = Math.max(-14, this.minX); x <= Math.min(14, this.maxX); x++) {
				for (int z = Math.max(-14, this.minZ); z <= Math.min(14, this.maxZ); z++) {
					int keep = cheb(x, z);
					int tower = this.towerDistance(x, z);
					if (keep > 11 && tower > 3) {
						continue;
					}
					this.set(x, -1, z, Blocks.COBBLESTONE.defaultBlockState());
					if (tower <= 3) {
						// Corner tower: three storeys and a roof with battlements.
						for (int y = 0; y <= 19; y++) {
							boolean shell = tower == 3 || y == 19;
							boolean floor = y == 7 || y == 14;
							this.set(x, y, z, shell ? this.stone(x, y, z) : floor ? planks : air);
						}
						if (tower == 3 && ((x + z) & 1) == 0) {
							this.set(x, 20, z, this.stone(x, 20, z));
						}
					} else {
						// The keep: throne room, upper hall, walkable roof.
						for (int y = 0; y <= 14; y++) {
							boolean shell = keep == 11 || y == 14;
							this.set(x, y, z, shell ? this.stone(x, y, z) : y == 7 ? planks : air);
						}
						if (keep == 11 && ((x + z) & 1) == 0) {
							this.set(x, 15, z, this.stone(x, 15, z));
						}
						// Arrow-slit windows.
						int along = Math.abs(x) == 11 ? z : x;
						if (keep == 11 && along % 4 == 0 && Math.abs(along) <= 8 && !(z == 11 && Math.abs(x) <= 1)) {
							this.fill(x, 2, z, x, 3, z, air);
							this.fill(x, 9, z, x, 10, z, air);
						}
					}
				}
			}
			// Entrance.
			this.fill(-1, 0, 11, 1, 3, 11, air);
			this.wallBanner(-3, 4, 12, Direction.SOUTH);
			this.wallBanner(3, 4, 12, Direction.SOUTH);
			// A banner on every face, and on top of every tower.
			this.wallBanner(0, 12, -12, Direction.NORTH);
			this.wallBanner(12, 12, 0, Direction.EAST);
			this.wallBanner(-12, 12, 0, Direction.WEST);
			this.wallBanner(0, 12, 12, Direction.SOUTH);
			for (int tx = -11; tx <= 11; tx += 22) {
				for (int tz = -11; tz <= 11; tz += 22) {
					this.standingBanner(tx, 20, tz);
				}
			}
			this.throneRoom();
			this.upperHall();
			// Ladder from the throne room through the upper hall to the roof.
			this.ladder(10, 0, 14, 2, Direction.WEST);
		}

		private void throneRoom() {
			BlockState carpet = Blocks.CARPET.pick(DyeColor.RED).defaultBlockState();
			this.fill(-1, 0, -6, 1, 0, 10, carpet);
			// The dais, two thrones with gold backs, and the royal treasure chest.
			this.fill(-5, 0, -10, 5, 0, -7, Blocks.COBBLESTONE.defaultBlockState());
			this.fill(-5, 1, -10, 5, 1, -7, carpet);
			for (int side = -1; side <= 1; side += 2) {
				this.set(side * 2, 1, -9, Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
				this.fill(side * 2, 1, -10, side * 2, 2, -10, Blocks.GOLD_BLOCK.defaultBlockState());
			}
			this.set(0, 1, -10, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH));
			if (this.here(0, -10)) {
				RandomizableContainer.setBlockEntityLootTable(
					this.level, this.level.getRandom(), this.pos(0, 1, -10), BuiltInLootTables.VILLAGE_WEAPONSMITH
				);
			}
			for (int x = -6; x <= 6; x += 3) {
				this.wallBanner(x, 4, -10, Direction.SOUTH);
			}
			// Pillars, hanging lanterns and torches.
			for (int side = -1; side <= 1; side += 2) {
				this.fill(side * 6, 0, -3, side * 6, 6, -3, this.stone(side * 6, 0, -3));
				this.fill(side * 6, 0, 4, side * 6, 6, 4, this.stone(side * 6, 0, 4));
				this.set(side * 3, 6, 0, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
				this.set(side * 3, 6, 6, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
				for (int z = -6; z <= 6; z += 6) {
					this.set(side * 10, 3, z, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, side < 0 ? Direction.EAST : Direction.WEST));
				}
			}
			this.set(0, 6, -5, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
		}

		private void upperHall() {
			for (int x = -10; x <= 10; x++) {
				for (int z = -10; z <= 10; z++) {
					boolean corner = Math.abs(x) >= 9 && Math.abs(z) >= 9;
					if (corner && this.noise(x, 13, z, 11) < 0.7F) {
						this.set(x, 13, z, Blocks.COBWEB.defaultBlockState());
					}
				}
			}
			for (int z = -8; z <= 8; z += 2) {
				this.set(-10, 8, z, this.noise(-10, 8, z, 12) < 0.5F ? Blocks.BOOKSHELF.defaultBlockState() : Blocks.BARREL.defaultBlockState());
			}
			this.fill(-3, 8, -3, 3, 8, 3, Blocks.CARPET.pick(DyeColor.RED).defaultBlockState());
			this.set(0, 13, 0, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
		}

		// ---- residents ----

		void residents() {
			this.spawn(ModEntities.ZOMBIE_KING, -2, 1, -8, 0.0F);
			this.spawn(ModEntities.ZOMBIE_PRINCESS, 2, 1, -8, 0.0F);
			int[][] knights = {
				{-5, 0, -3}, {5, 0, -3}, {-5, 0, 4}, {5, 0, 4},  // throne room
				{-3, 0, 13}, {3, 0, 13},                         // castle door
				{-3, 0, 32}, {3, 0, 32}                          // gate
			};
			for (int[] k : knights) {
				this.spawn(ModEntities.ZOMBIE_KNIGHT, k[0], k[1], k[2], 0.0F);
			}
			int[][] cavalry = {{-18, 0, 0}, {18, 0, 0}, {0, 0, -18}, {-18, 0, -18}, {18, 0, 18}}; // mounted patrols
			for (int[] k : cavalry) {
				if (this.spawn(ModEntities.ZOMBIE_KNIGHT, k[0], k[1], k[2], 0.0F) instanceof ZombieKnight knight) {
					knight.mountHorse(this.level, true);
				}
			}
			for (int stall : STALLS) {
				this.spawnStableHorse(STABLE_FRONT_X + 3, stall + 1);
			}
			int[][] archers = {
				{0, 7, -35}, {-35, 7, 0}, {35, 7, 0}, {-12, 7, 35}, {12, 7, 35}, // wall walk
				{-34, 12, -34}, {34, 12, -34}, {-34, 12, 34}, {34, 12, 34},     // corner towers
				{-6, 15, -6}, {6, 15, 6}                                         // castle roof
			};
			for (int[] a : archers) {
				this.spawn(ModEntities.ZOMBIE_ARCHER, a[0], a[1], a[2], 0.0F);
			}
		}

		/** Creates a resident if its spot is in this chunk; the caller may still mount it before it is added. */
		private @Nullable Zombie spawn(final EntityType<? extends Zombie> type, final int x, final int y, final int z, final float yaw) {
			BlockPos pos = this.pos(x, y, z);
			if (!this.chunk.isInside(pos)) {
				return null;
			}
			Zombie mob = type.create(this.level.getLevel(), EntitySpawnReason.STRUCTURE);
			if (mob == null) {
				return null;
			}
			mob.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0.0F);
			mob.finalizeSpawn(this.level, this.level.getCurrentDifficultyAt(pos), EntitySpawnReason.STRUCTURE, new Zombie.ZombieGroupData(false, false));
			mob.setPersistenceRequired();
			if (mob instanceof RoyalGuard) {
				mob.setHomeTo(KingdomPiece.this.center, GUARD_HOME_RADIUS);
			}
			this.level.addFreshEntityWithPassengers(mob);
			return mob;
		}

		/** An untamed zombie horse waiting in its stall, in the kingdom's red armour. */
		private void spawnStableHorse(final int x, final int z) {
			BlockPos pos = this.pos(x, 0, z);
			if (!this.chunk.isInside(pos)) {
				return;
			}
			ZombieHorse horse = EntityTypes.ZOMBIE_HORSE.create(this.level.getLevel(), EntitySpawnReason.STRUCTURE);
			if (horse != null) {
				horse.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 90.0F, 0.0F);
				horse.finalizeSpawn(this.level, this.level.getCurrentDifficultyAt(pos), EntitySpawnReason.STRUCTURE, null);
				horse.setItemSlot(EquipmentSlot.BODY, ZombieKnight.kingdomHorseArmour());
				horse.setPersistenceRequired();
				this.level.addFreshEntity(horse);
			}
		}
	}
}
