package dev.hephaestus.fiblib;

import com.google.common.collect.Iterables;
import dev.hephaestus.fiblib.blocks.BlockFib;
import dev.hephaestus.fiblib.mixin.blocks.BlockMixin;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.PersistentState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

@SuppressWarnings("ALL")
public class FibLib {
	public static final String MOD_ID = "fiblib";
	private static final String MOD_NAME = "FibLib";

	private static final Logger LOGGER = LogManager.getLogger();
	private static final String SAVE_KEY = "fiblib";
    public static boolean DEBUG = FabricLoader.getInstance().isDevelopmentEnvironment();

    static void log(String msg) {
		log("%s", msg);
	}

	static void log(String format, Object... args) {
		LOGGER.info(String.format("[%s] %s", MOD_NAME, String.format(format, args)));
	}

	static void debug(String msg) {
		debug("%s", msg);
	}

	static void debug(String format, Object... args) {
		if (DEBUG) LOGGER.info(String.format("[%s] %s", MOD_NAME, String.format(format, args)));
	}

	public static class Blocks extends PersistentState {
    	private ServerWorld world;
		private static final String SAVE_KEY = FibLib.SAVE_KEY + "_blocks";

		private static final HashMap<Block, BlockFib> fibs = new HashMap<>();
		private final HashMap<Block, LongSet> trackedBlocks = new HashMap<>();

		// Construction methods
		private Blocks(ServerWorld world) {
			super(SAVE_KEY);
			this.world = world;

			this.markDirty();
		}

		private static FibLib.Blocks getInstance(ServerWorld world) {
			FibLib.Blocks instance = world.getPersistentStateManager().getOrCreate(() ->
					new FibLib.Blocks(world), SAVE_KEY
			);

			return instance;
		}

		// Convenience
		private static FibLib.Blocks getInstance(ServerPlayerEntity player) {
			return getInstance(player.getServerWorld());
		}

		@Internal
		@Override
		public void fromTag(CompoundTag tag) {
			trackedBlocks.clear();

			CompoundTag blockSaveTag = tag.getCompound(SAVE_KEY);

			for (String k : blockSaveTag.getKeys()) {
				trackedBlocks.put(Registry.BLOCK.get(new Identifier(k)), new LongOpenHashSet(blockSaveTag.getLongArray(k)));
			}
		}

		@Internal
		@Override
		public CompoundTag toTag(CompoundTag tag) {
			CompoundTag blockSaveTag = new CompoundTag();
			for (Map.Entry<Block, LongSet> e : trackedBlocks.entrySet()) {
				blockSaveTag.put(Registry.BLOCK.getId(e.getKey()).toString(), new LongArrayTag(e.getValue()));
			}

			tag.put(SAVE_KEY, blockSaveTag);

			return tag;
		}

		// Instance methods. These are private to make the API simpler.

		// Because we only actually begin tracking the block if we have a fib that references it, it's safe to call put()
		// whenever and wherever we want.




		// API methods

		/**
		 * Updates all tracked blocks in a given world. Somewhat expensive, and should probably not really be called. If you
		 * need to update multiple kinds of blocks, see the methods below
		 *
		 * @param world the world to update in
		 */
		public static void update(ServerWorld world) {
			FibLib.Blocks instance = FibLib.Blocks.getInstance(world);

			int i = 0;
			for (Long l : Iterables.concat(FibLib.Blocks.getInstance(world).trackedBlocks.values())) {
				world.getChunkManager().markForUpdate(BlockPos.fromLong(l));
				++i;
			}
			FibLib.log("Updated %d blocks", i);
		}

		/**
		 * Updates all of one kind of block.
		 *
		 * @param world the world to update in
		 * @param block the block type to update
		 */
		public static void update(ServerWorld world, Block block) {
			FibLib.Blocks instance = FibLib.Blocks.getInstance(world);

			if (instance.trackedBlocks.containsKey(block)) {
				int i = 0;
				for (Long l : instance.trackedBlocks.get(block)) {
					world.getChunkManager().markForUpdate(BlockPos.fromLong(l));
					++i;
				}
				FibLib.log("Updated %d blocks", i);
			}
		}

		/**
		 * Helper function for updating multiple kinds of blocks
		 *
		 * @param world  the world to update in
		 * @param blocks the blocks to update
		 */
		public static void update(ServerWorld world, Block... blocks) {
			FibLib.Blocks instance = FibLib.Blocks.getInstance(world);

			int i = 0;
			for (Block a : blocks) {
				if (instance.trackedBlocks.containsKey(a)) {
					for (Long l : instance.trackedBlocks.get(a)) {
						world.getChunkManager().markForUpdate(BlockPos.fromLong(l));
						++i;
					}
				}
			}

			FibLib.log("Updated %d blocks", i);
		}

		/**
		 * Helper function for updating multiple kinds of blocks
		 *
		 * @param world  the world to update in
		 * @param blocks the blocks to update
		 */
		public static void update(ServerWorld world, Collection<Block> blocks) {
			FibLib.Blocks instance = FibLib.Blocks.getInstance(world);

			int i = 0;
			for (Block a : blocks) {
				if (instance.trackedBlocks.containsKey(a)) {
					for (Long l : instance.trackedBlocks.get(a)) {
						world.getChunkManager().markForUpdate(BlockPos.fromLong(l));
						++i;
					}
				}
			}

			FibLib.log("Updated %d blocks", i);
		}


		/**
		 * Register a fib so it can be used.
		 *
		 * @param block the block to be fibbed
		 * @param fib   the fib itself. Can be a lambda expression for simpler fibs, or an implementation of BlockFib for
		 *              fibs that need some more complex processing
		 */
		public static void register(Block block, BlockFib fib) {
			Blocks.fibs.put(block,fib);
			FibLib.log("Registered a BlockFib for %s", block.getTranslationKey());
		}

		/**
		 * Looks up what a block should look like for a player.
		 *
		 * @param state  the state of the block we're inquiring about. Note that because this is passed to a BlockFib, other
		 *               aspects of the state than the Block may be used in determining the output
		 * @param player the player who we will be fibbing to
		 * @return the result of the fib. This is what the player will get told the block is
		 */
		@Internal
		public static BlockState get(BlockState state, ServerPlayerEntity player) {
			if (player == null) {
				return state;
			}
			return fibs.get(state.getBlock()).get(state,player);
		}

		private void track(Block block, BlockPos pos) {
			if (fibs.containsKey(block)) {
				trackedBlocks.putIfAbsent(block, new LongOpenHashSet());
				trackedBlocks.get(block).add(pos.asLong());
			}
		}

		/**
		 * Begins tracking a block for updates. You shouldn't have to call this manually
		 *
		 * @param world the world that this block is in
		 * @param block the block we care about. used for selective updating
		 * @param pos   the position of the block we are going to keep track of
		 */
		public static void track(ServerWorld world, Block block, BlockPos pos) {
			FibLib.Blocks.getInstance(world).track(block, pos);
		}

		/**
		 * Begins tracking a block for updates. You shouldn't have to call this manually
		 *
		 * @param world the world that this block is in
		 * @param state the block state we care about; note that only the actual Block is used, state info is disregarded
		 * @param pos   the position of the block we are going to keep track of
		 */
		public static void track(ServerWorld world, BlockState state, BlockPos pos) {
			FibLib.Blocks.track(world, state.getBlock(), pos);
		}

		private void stopTracking(BlockPos pos) {
			Block block = world.getBlockState(pos).getBlock();
			if (trackedBlocks.containsKey(block))
				trackedBlocks.get(block).remove(pos.asLong());
		}

		/**
		 * Removes a block from tracking. Automatically called on block removal, see {@link BlockMixin}; you shouldn't have to call this manually
		 *
		 * @param world the world that this block is in
		 * @param pos   the position of the block to be removed
		 */
		public static void stopTracking(ServerWorld world, BlockPos pos) {
			FibLib.Blocks.getInstance(world).stopTracking(pos);
		}
	}
}
