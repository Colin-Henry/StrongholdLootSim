import StrongholdGenerator.StrongholdPieces;
import com.seedfinding.mcbiome.source.OverworldBiomeSource;
import com.seedfinding.mccore.util.data.Pair;
import com.seedfinding.mccore.util.pos.CPos;
import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcfeature.loot.LootContext;
import com.seedfinding.mcfeature.loot.MCLootTables;
import com.seedfinding.mcfeature.loot.item.ItemStack;
import com.seedfinding.mcfeature.loot.item.Items;
import com.seedfinding.mcfeature.structure.Stronghold;
import com.seedfinding.mcseed.rand.JRand;
import nl.jellejurre.seedchecker.SeedChecker;
import com.seedfinding.mccore.rand.ChunkRand;

import java.util.List;

public class combinedFilter
{
    public static void main(String[] args)
    {

        long seed = 1; // TODO: automatically take seeds from file

        ChunkRand rand = new ChunkRand().asChunkRandDebugger();

        Stronghold stronghold = new Stronghold(MCVersion.v1_16_1);
        CPos[] positions = stronghold.getStarts(new OverworldBiomeSource(MCVersion.v1_16_1, seed), 3, new JRand(seed)); // Getting the positions of the first ring strongholds

        SeedChecker seedChecker = new SeedChecker(seed);
        for (CPos pos : positions)
        {
            StrongholdGenerator.StrongholdGenerator generator = new StrongholdGenerator.StrongholdGenerator();
            generator.generate(seed, pos.getX(), pos.getZ());

            for (StrongholdGenerator.Util.StructurePiece piece: generator.pieces)
            {
                if (piece instanceof StrongholdPieces.Library library) // Checks the chest loot of the library
                {
                    for (StrongholdGenerator.Util.BPos chestPos : library.getChestPos())
                    {
                        long decoratorSeed = rand.setPopulationSeed(seed, chestPos.toSeedCheckerBlockPos().getX(), chestPos.toSeedCheckerBlockPos().getZ(), MCVersion.v1_16_1);
                        rand.setDecoratorSeed(decoratorSeed, 2, 2, MCVersion.v1_16_1); // TODO: double check these are the correct offsets
                        long lootTableSeed = rand.nextLong();

                        LootContext context = new LootContext(lootTableSeed, MCVersion.v1_16_1);
                        Items.ENCHANTED_BOOK.getEnchantments().clear();
                        List<ItemStack> chestLoot = MCLootTables.STRONGHOLD_LIBRARY_CHEST.get().generate(context);

                        if (testLoot(chestLoot))
                        {
                            System.out.println(testLoot(chestLoot));
                        }
                    }
                }

                if (piece instanceof StrongholdPieces.ChestCorridor chestCorridor) // Checks the chest loot of the chest corridor
                {
                    StrongholdGenerator.Util.BPos chestPos = chestCorridor.getChestPos();
                    long decoratorSeed = rand.setPopulationSeed(seed, chestPos.toSeedCheckerBlockPos().getX(), chestPos.toSeedCheckerBlockPos().getZ(), MCVersion.v1_16_1);
                    rand.setDecoratorSeed(decoratorSeed, 2, 2, MCVersion.v1_16_1); // TODO: double check these are the correct offsets
                    long lootTableSeed = rand.nextLong();

                    LootContext context = new LootContext(lootTableSeed, MCVersion.v1_16_1);
                    Items.ENCHANTED_BOOK.getEnchantments().clear();
                    List<ItemStack> chestLoot = MCLootTables.STRONGHOLD_CORRIDOR_CHEST.get().generate(context);

                    if (testLoot(chestLoot))
                    {
                        System.out.println(testLoot(chestLoot));
                    }
                }

                if (piece instanceof StrongholdPieces.SquareRoom squareRoom) // Checks the chest loot of the square room
                {
                    StrongholdGenerator.Util.BPos chestPos = squareRoom.getChestPos();
                    if (chestPos == null) // Program doesn't build without this because some square rooms don't have chests
                        continue;

                    long decoratorSeed = rand.setPopulationSeed(seed, chestPos.toSeedCheckerBlockPos().getX(), chestPos.toSeedCheckerBlockPos().getZ(), MCVersion.v1_16_1);
                    rand.setDecoratorSeed(decoratorSeed, 2, 2, MCVersion.v1_16_1); // TODO: double check these are the correct offsets
                    long lootTableSeed = rand.nextLong();

                    LootContext context = new LootContext(lootTableSeed, MCVersion.v1_16_1);
                    Items.ENCHANTED_BOOK.getEnchantments().clear();
                    List<ItemStack> chestLoot = MCLootTables.STRONGHOLD_CROSSING_CHEST.get().generate(context);

                    if (testLoot(chestLoot))
                    {
                        System.out.println(testLoot(chestLoot));
                    }
                }
            }
        }
    }

    private static boolean testLoot(List<ItemStack> loot)
    {
        boolean hasUnbreaking = false;
        boolean hasPiercing = false;
        boolean hasChanneling = false;

        for (ItemStack itemStack : loot) {
            if (!itemStack.getItem().equalsName(Items.ENCHANTED_BOOK))
                continue;
            for (Pair<String, Integer> enchantment : itemStack.getItem().getEnchantments()) {
                if (enchantment.getFirst().equals("unbreaking") && enchantment.getSecond() == 3)
                    hasUnbreaking = true;
                if (enchantment.getFirst().equals("piercing") && enchantment.getSecond() == 4)
                    hasPiercing = true;
                if (enchantment.getFirst().equals("channeling"))
                    hasChanneling = true;
            }
        }

        return hasUnbreaking && hasPiercing && hasChanneling;
    }
}