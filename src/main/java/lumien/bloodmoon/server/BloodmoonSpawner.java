package lumien.bloodmoon.server;

import java.util.List;
import java.util.Random;
import java.util.Set;

import com.google.common.collect.Sets;

import lumien.bloodmoon.Bloodmoon;
import lumien.bloodmoon.config.BloodmoonConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.util.WeightedRandom;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;

public final class BloodmoonSpawner
{
	private static final int MOB_COUNT_DIV = (int) Math.pow(17.0D, 2.0D);
	private final Set<ChunkPos> eligibleChunksForSpawning = Sets.<ChunkPos> newHashSet();

	/**
	 * adds all chunks within the spawn radius of the players to
	 * eligibleChunksForSpawning. pars: the world, hostileCreatures,
	 * passiveCreatures. returns number of eligible chunks.
	 */
	public int findChunksForSpawning(WorldServer worldServerIn, boolean spawnHostileMobs, boolean spawnPeacefulMobs, boolean spawnOnSetTickRate)
	{
		if (!spawnHostileMobs && !spawnPeacefulMobs)
		{
			return 0;
		}
		else
		{
			this.eligibleChunksForSpawning.clear();
			int i = 0;

			for (EntityPlayer entityplayer : worldServerIn.playerEntities)
			{
				if (!entityplayer.isSpectator())
				{
					int j = MathHelper.floor(entityplayer.posX / 16.0D);
					int k = MathHelper.floor(entityplayer.posZ / 16.0D);
					int l = 8;

					for (int i1 = -l; i1 <= l; ++i1)
					{
						for (int j1 = -l; j1 <= l; ++j1)
						{
							boolean flag = i1 == -l || i1 == l || j1 == -l || j1 == l;
							ChunkPos chunkpos = new ChunkPos(i1 + j, j1 + k);

							if (!this.eligibleChunksForSpawning.contains(chunkpos))
							{
								++i;

								if (!flag && worldServerIn.getWorldBorder().contains(chunkpos))
								{
									PlayerChunkMapEntry playermanager$playerinstance = worldServerIn.getPlayerChunkMap().getEntry(chunkpos.x, chunkpos.z);

									if (playermanager$playerinstance != null && playermanager$playerinstance.isSentToPlayers())
									{
										this.eligibleChunksForSpawning.add(chunkpos);
									}
								}
							}
						}
					}
				}
			}

			int j4 = 0;
			BlockPos blockpos1 = worldServerIn.getSpawnPoint();

			for (EnumCreatureType enumcreaturetype : EnumCreatureType.values())
			{
				if ((!enumcreaturetype.getPeacefulCreature() || spawnPeacefulMobs) && (enumcreaturetype.getPeacefulCreature() || spawnHostileMobs) && (!enumcreaturetype.getAnimal() || spawnOnSetTickRate))
				{
					int k4 = worldServerIn.countEntities(enumcreaturetype, true);
					int spawnLimit = enumcreaturetype.getMaxNumberOfCreature() * i / MOB_COUNT_DIV;

					spawnLimit *= BloodmoonConfig.SPAWNING.SPAWN_LIMIT_MULT;

					if (k4 <= spawnLimit)
					{
						java.util.ArrayList<ChunkPos> shuffled = com.google.common.collect.Lists.newArrayList(this.eligibleChunksForSpawning);
						java.util.Collections.shuffle(shuffled);
						BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
						label415:

						for (ChunkPos chunkcoordintpair1 : shuffled)
						{
							BlockPos blockpos = getRandomChunkPosition(worldServerIn, chunkcoordintpair1.x, chunkcoordintpair1.z);
							int k1 = blockpos.getX();
							int l1 = blockpos.getY();
							int i2 = blockpos.getZ();
							IBlockState iblockstate = worldServerIn.getBlockState(blockpos);

							if (!iblockstate.isNormalCube())
							{
								int j2 = 0;

								for (int k2 = 0; k2 < 3; ++k2)
								{
									int l2 = k1;
									int i3 = l1;
									int j3 = i2;
									int k3 = 6;
									Biome.SpawnListEntry biomegenbase$spawnlistentry = null;
									IEntityLivingData ientitylivingdata = null;
									int l3 = MathHelper.ceil(Math.random() * 4.0D);

									for (int i4 = 0; i4 < l3; ++i4)
									{
										l2 += worldServerIn.rand.nextInt(k3) - worldServerIn.rand.nextInt(k3);
										i3 += worldServerIn.rand.nextInt(1) - worldServerIn.rand.nextInt(1);
										j3 += worldServerIn.rand.nextInt(k3) - worldServerIn.rand.nextInt(k3);
										blockpos$mutableblockpos.setPos(l2, i3, j3);
										float f = (float) l2 + 0.5F;
										float f1 = (float) j3 + 0.5F;

										if (worldServerIn.canBlockSeeSky(blockpos$mutableblockpos) && !worldServerIn.isAnyPlayerWithinRangeAt((double) f, (double) i3, (double) f1, BloodmoonConfig.SPAWNING.SPAWN_RANGE) && blockpos1.distanceSq((double) f, (double) i3, (double) f1) >= (BloodmoonConfig.SPAWNING.SPAWN_DISTANCE * BloodmoonConfig.SPAWNING.SPAWN_DISTANCE))
										{
											if (biomegenbase$spawnlistentry == null)
											{
												biomegenbase$spawnlistentry = worldServerIn.getSpawnListEntryForTypeAt(enumcreaturetype, blockpos$mutableblockpos);

												if (biomegenbase$spawnlistentry == null || !BloodmoonConfig.canSpawn(biomegenbase$spawnlistentry.entityClass))
												{
													biomegenbase$spawnlistentry = null;
													break;
												}
											}

											if (worldServerIn.canCreatureTypeSpawnHere(enumcreaturetype, biomegenbase$spawnlistentry, blockpos$mutableblockpos) && canCreatureTypeSpawnAtLocation(EntitySpawnPlacementRegistry.getPlacementForEntity(biomegenbase$spawnlistentry.entityClass), worldServerIn, blockpos$mutableblockpos))
											{
												EntityLiving entityliving;

												try
												{
													entityliving = (EntityLiving) biomegenbase$spawnlistentry.entityClass.getConstructor(new Class[] { World.class }).newInstance(new Object[] { worldServerIn });
												}
												catch (Exception exception)
												{
													exception.printStackTrace();
													return j4;
												}

												entityliving.setLocationAndAngles((double) f, (double) i3, (double) f1, worldServerIn.rand.nextFloat() * 360.0F, 0.0F);

												net.minecraftforge.fml.common.eventhandler.Event.Result canSpawn = net.minecraftforge.event.ForgeEventFactory.canEntitySpawn(entityliving, worldServerIn, f, i3, f1, false);
												if (canSpawn == net.minecraftforge.fml.common.eventhandler.Event.Result.ALLOW || (canSpawn == net.minecraftforge.fml.common.eventhandler.Event.Result.DEFAULT && (entityliving.getCanSpawnHere() && entityliving.isNotColliding())))
												{
													if (!net.minecraftforge.event.ForgeEventFactory.doSpecialSpawn(entityliving, worldServerIn, f, l3, f1))
														ientitylivingdata = entityliving.onInitialSpawn(worldServerIn.getDifficultyForLocation(new BlockPos(entityliving)), ientitylivingdata);

													if (entityliving.isNotColliding())
													{
														++j2;
														entityliving.getEntityData().setBoolean("bloodmoonSpawned", true);

														worldServerIn.spawnEntity(entityliving);
													}
													else
													{
														entityliving.setDead();
													}

													if (i2 >= net.minecraftforge.event.ForgeEventFactory.getMaxSpawnPackSize(entityliving))
													{
														continue label415;
													}
												}

												j4 += j2;
											}
										}
									}
								}
							}
						}
					}
				}
			}

			return j4;
		}
	}

	protected static BlockPos getRandomChunkPosition(World worldIn, int x, int z)
	{
		Chunk chunk = worldIn.getChunkFromChunkCoords(x, z);
		int i = x * 16 + worldIn.rand.nextInt(16);
		int j = z * 16 + worldIn.rand.nextInt(16);
		int k = MathHelper.roundUp(chunk.getHeight(new BlockPos(i, 0, j)) + 1, 16);
		int l = worldIn.rand.nextInt(k > 0 ? k : chunk.getTopFilledSegment() + 16 - 1);
		return new BlockPos(i, l, j);
	}

	public static boolean func_185331_a(IBlockState p_185331_0_)
	{
		return p_185331_0_.isBlockNormalCube() ? false : (p_185331_0_.canProvidePower() ? false : (p_185331_0_.getMaterial().isLiquid() ? false : !BlockRailBase.isRailBlock(p_185331_0_)));
	}

	public static boolean canCreatureTypeSpawnAtLocation(EntityLiving.SpawnPlacementType spawnPlacementTypeIn, World worldIn, BlockPos pos)
	{
		if (!worldIn.getWorldBorder().contains(pos))
		{
			return false;
		}
		else
		{
			IBlockState iblockstate = worldIn.getBlockState(pos);

			if (spawnPlacementTypeIn == EntityLiving.SpawnPlacementType.IN_WATER)
			{
				return iblockstate.getMaterial().isLiquid() && worldIn.getBlockState(pos.down()).getMaterial().isLiquid() && !worldIn.getBlockState(pos.up()).isNormalCube();
			}
			else
			{
				BlockPos blockpos = pos.down();
				IBlockState state = worldIn.getBlockState(blockpos);

				if (!state.getBlock().canCreatureSpawn(state, worldIn, blockpos, spawnPlacementTypeIn))
				{
					return false;
				}
				else
				{
					Block block = worldIn.getBlockState(blockpos).getBlock();
					boolean flag = block != Blocks.BEDROCK && block != Blocks.BARRIER;
					return flag && func_185331_a(iblockstate) && func_185331_a(worldIn.getBlockState(pos.up()));
				}
			}
		}
	}

	/**
	 * Called during chunk generation to spawn initial creatures.
	 */
	public static void performWorldGenSpawning(World worldIn, Biome biomeIn, int p_77191_2_, int p_77191_3_, int p_77191_4_, int p_77191_5_, Random randomIn)
	{
		List<Biome.SpawnListEntry> list = biomeIn.getSpawnableList(EnumCreatureType.CREATURE);

		if (!list.isEmpty())
		{
			while (randomIn.nextFloat() < biomeIn.getSpawningChance())
			{
				Biome.SpawnListEntry biomegenbase$spawnlistentry = (Biome.SpawnListEntry) WeightedRandom.getRandomItem(worldIn.rand, list);
				int i = biomegenbase$spawnlistentry.minGroupCount + randomIn.nextInt(1 + biomegenbase$spawnlistentry.maxGroupCount - biomegenbase$spawnlistentry.minGroupCount);
				IEntityLivingData ientitylivingdata = null;
				int j = p_77191_2_ + randomIn.nextInt(p_77191_4_);
				int k = p_77191_3_ + randomIn.nextInt(p_77191_5_);
				int l = j;
				int i1 = k;

				for (int j1 = 0; j1 < i; ++j1)
				{
					boolean flag = false;

					for (int k1 = 0; !flag && k1 < 4; ++k1)
					{
						BlockPos blockpos = worldIn.getTopSolidOrLiquidBlock(new BlockPos(j, 0, k));

						if (canCreatureTypeSpawnAtLocation(EntityLiving.SpawnPlacementType.ON_GROUND, worldIn, blockpos))
						{
							EntityLiving entityliving;

							try
							{
								entityliving = (EntityLiving) biomegenbase$spawnlistentry.entityClass.getConstructor(new Class[] { World.class }).newInstance(new Object[] { worldIn });
							}
							catch (Exception exception)
							{
								exception.printStackTrace();
								continue;
							}

							entityliving.setLocationAndAngles((double) ((float) j + 0.5F), (double) blockpos.getY(), (double) ((float) k + 0.5F), randomIn.nextFloat() * 360.0F, 0.0F);
							worldIn.spawnEntity(entityliving);
							ientitylivingdata = entityliving.onInitialSpawn(worldIn.getDifficultyForLocation(new BlockPos(entityliving)), ientitylivingdata);
							flag = true;
						}

						j += randomIn.nextInt(5) - randomIn.nextInt(5);

						for (k += randomIn.nextInt(5) - randomIn.nextInt(5); j < p_77191_2_ || j >= p_77191_2_ + p_77191_4_ || k < p_77191_3_ || k >= p_77191_3_ + p_77191_4_; k = i1 + randomIn.nextInt(5) - randomIn.nextInt(5))
						{
							j = l + randomIn.nextInt(5) - randomIn.nextInt(5);
						}
					}
				}
			}
		}
	}
}