import StrongholdGenerator.StrongholdPieces;
import com.seedfinding.mcbiome.source.OverworldBiomeSource;
import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.util.data.Pair;
import com.seedfinding.mccore.util.pos.BPos;
import com.seedfinding.mccore.util.pos.CPos;
import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcfeature.loot.LootContext;
import com.seedfinding.mcfeature.loot.MCLootTables;
import com.seedfinding.mcfeature.loot.item.Items;
import com.seedfinding.mcfeature.structure.DesertPyramid;
import com.seedfinding.mcfeature.structure.Stronghold;
import com.seedfinding.mcseed.rand.JRand;
import com.seedfinding.mcfeature.loot.item.ItemStack;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.util.math.BlockPos;
import nl.jellejurre.seedchecker.ReflectionUtils;
import nl.jellejurre.seedchecker.SeedChecker;
import nl.jellejurre.seedchecker.SeedCheckerDimension;
import nl.jellejurre.seedchecker.TargetState;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.io.FileWriter;

// Uses mc_feature_java locally and seedchecker via gradle. ik its stupid but i couldnt get gradle to work for the mc_feature_java version i need cuz its a newer one

// To run out of a new thing
// Create intellij file
// Copy gradle file
// Copy stronghold stuff
// Copy util folder
// Copy combinedFilter
// Copy seedlists (input and output)
// Change directories in file paths for seedlists
// Should be done and ready to go

class LootResults { // Used for comparing temple loot with the 3 strongholds separately
    boolean templeMendingOrUnbreaking;
    boolean templeChanneling;
    boolean templePiercing4;
    int templePiercing3Count;
}

public class finderMain {
        public static void main(String[] args) { // IO Handling. Def a better way to do error handling but idk how


        int seedCounter = 0;
        int taskSize = 0;

        try (BufferedReader lineReader = new BufferedReader(new FileReader("D:/Seedfinding/AassgBookFinder/src/main/java/inputSeeds.txt"));
             PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter("D:/Seedfinding/AassgBookFinder/src/main/java/outputSeeds.txt", false))))
        {


            while (lineReader.readLine() != null)
                taskSize++;
            lineReader.close();

            String line;
            BufferedReader reader = new BufferedReader(new FileReader("D:/Seedfinding/AassgBookFinder/src/main/java/inputSeeds.txt"));

            while ((line = reader.readLine()) != null) { // Goes thru every line and runs the whole filter, prints passable seeds to file
                try {
                    long seed = Long.parseLong(line.trim());
                    seedCounter++;
                    if (processSeed(seed)) {
                        writer.println(seed);
                        writer.flush();
                        System.out.println(seed);
                    }
                    writeBoinc(seedCounter, taskSize);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void writeBoinc(int seedCounter, int taskSize) {
        try (FileWriter writer = new FileWriter("./boincpoint")) {

            writer.write(String.valueOf(seedCounter));
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Write fraction done to file for the wrapper to read
        try (FileWriter writer = new FileWriter("./boinc_frac")) {
            writer.write(String.format("%.6f", (double) seedCounter/(taskSize)));
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean processSeed(long seed) {

        // Initialization

        Stronghold stronghold = new Stronghold(MCVersion.v1_16_1);

        CPos[] positions = stronghold.getStarts(new OverworldBiomeSource(MCVersion.v1_16_1, seed), 3, new JRand(seed)); // Getting the positions of the first ring strongholds
        SeedChecker seedChecker = new SeedChecker(seed, TargetState.STRUCTURES, SeedCheckerDimension.OVERWORLD); // Initialize seedChecker
        ChunkRand rand = new ChunkRand();

        OverworldBiomeSource biomeSource = new OverworldBiomeSource(MCVersion.v1_16_1, seed);
        BlockPos spawn = seedChecker.getSpawnPos();
        DesertPyramid desertPyramid = new DesertPyramid(MCVersion.v1_16_1);

        // Temple filtering

        BPos closestPyramid = null;
        double minDistance = Double.MAX_VALUE;

        for (int regionX = -1; regionX <= 0; regionX++) {
            for (int regionZ = -1; regionZ <= 0; regionZ++) {
                CPos pyramidChunk = desertPyramid.getInRegion(seed, regionX, regionZ, rand);
                if (pyramidChunk != null && desertPyramid.canSpawn(pyramidChunk.getX(), pyramidChunk.getZ(), biomeSource)) {
                    BPos pyramidPos = new BPos(pyramidChunk.getX() * 16, 0, pyramidChunk.getZ() * 16);
                    double distance = Math.pow((Math.abs(pyramidPos.getX() - spawn.getX())) + Math.abs((pyramidPos.getZ() - spawn.getZ())), 2);
                    if (distance < minDistance) { // Just finding the closest temple. The seed list is guaranteed within 96 of spawn, so it should always find the closest one
                        minDistance = distance;
                        closestPyramid = pyramidPos;
                    }
                }
            }
        }

        if (closestPyramid == null) {
            // System.out.println("Error finding temple"); // Really should never happen cuz the input list has them close already
            closestPyramid = new BPos(0, 0, 0);
        }

        List<ItemStack> templeLoot = new ArrayList<>(); // Master list for temple loot
        List<BPos> templeChestPositions = new ArrayList<>(); // Stupid way to get chest positions but it works
        templeChestPositions.add(new BPos(closestPyramid.getX() + 10, 53, closestPyramid.getZ() + 8));
        templeChestPositions.add(new BPos(closestPyramid.getX() + 12, 53, closestPyramid.getZ() + 10));
        templeChestPositions.add(new BPos(closestPyramid.getX() + 10, 53, closestPyramid.getZ() + 12));
        templeChestPositions.add(new BPos(closestPyramid.getX() + 8, 53, closestPyramid.getZ() + 10));

        for (BPos chestPos : templeChestPositions) { // Getting loot from each chest
            Items.ENCHANTED_BOOK.getEnchantments().clear();
            ChestBlockEntity chest = (ChestBlockEntity) seedChecker.getBlockEntity(chestPos.getX(), chestPos.getY(), chestPos.getZ());
            try {
                long lootSeed = (Long) ReflectionUtils.getValueFromField(chest, "lootTableSeed"); // Derive loot seed
                List<ItemStack> items = MCLootTables.DESERT_PYRAMID_CHEST.get().generate(new LootContext(lootSeed)); // Get loot from loot seed
                templeLoot.addAll(items);
            } catch (Exception ex) {
                // System.out.printf("ERROR - Seed: %d, Chest Position: (%d, %d, %d)\n", seed, chestPos.getX(), chestPos.getY(), chestPos.getZ());
                // Extreme edge case where the temple doesn't generate. unsure why but just a false positive from a previous filter
            }
        }

        LootResults results = new LootResults();
        testTempleLoot(templeLoot, results); // Testing temple loot
        Items.ENCHANTED_BOOK.getEnchantments().clear();

        // Stronghold filtering

        for (CPos pos : positions) { // For each stronghold
            StrongholdGenerator.StrongholdGenerator generator = new StrongholdGenerator.StrongholdGenerator();
            generator.generate(seed, pos.getX(), pos.getZ());

            List<ItemStack> combinedLoot = new ArrayList<>(); // Master list for all loot

            for (StrongholdGenerator.Util.StructurePiece piece : generator.pieces) {
                if (piece instanceof StrongholdPieces.Library library) { // Checks the chest loot of the library
                    for (StrongholdGenerator.Util.BPos chestPos : library.getChestPos()) {
                        Items.ENCHANTED_BOOK.getEnchantments().clear();
                        ChestBlockEntity chest = (ChestBlockEntity) seedChecker.getBlockEntity(chestPos.toSeedCheckerBlockPos().getX(), chestPos.toSeedCheckerBlockPos().getY(), chestPos.toSeedCheckerBlockPos().getZ());
                        try {
                            long lootSeed = (Long) ReflectionUtils.getValueFromField(chest, "lootTableSeed"); // Derive loot seed
                            List<ItemStack> items = MCLootTables.STRONGHOLD_LIBRARY_CHEST.get().generate(new LootContext(lootSeed)); // Get loot from loot seed
                            combinedLoot.addAll(items);
                        } catch (Exception ex) {
                            // System.out.printf("ERROR - Seed: %d, Chest Position: (%d, %d, %d)\n", seed, chestPos.toSeedCheckerBlockPos().getX(), chestPos.toSeedCheckerBlockPos().getY(), chestPos.toSeedCheckerBlockPos().getZ()); // Failsafe in case the chest can't generate (extreme edge case)
                        }
                    }
                } else if (piece instanceof StrongholdPieces.ChestCorridor chestCorridor) { // Checks the chest loot of the altar chest
                    StrongholdGenerator.Util.BPos chestPos = chestCorridor.getChestPos();
                    ChestBlockEntity chest = (ChestBlockEntity) seedChecker.getBlockEntity(chestPos.toSeedCheckerBlockPos().getX(), chestPos.toSeedCheckerBlockPos().getY(), chestPos.toSeedCheckerBlockPos().getZ());
                    try {
                        long lootSeed = (Long) ReflectionUtils.getValueFromField(chest, "lootTableSeed"); // Derive loot seed
                        List<ItemStack> items = MCLootTables.STRONGHOLD_CORRIDOR_CHEST.get().generate(new LootContext(lootSeed)); // Get loot from loot seed
                        combinedLoot.addAll(items);
                    } catch (Exception ex) {
                        // System.out.printf("ERROR - Seed: %d, Chest Position: (%d, %d, %d)\n", seed, chestPos.toSeedCheckerBlockPos().getX(), chestPos.toSeedCheckerBlockPos().getY(), chestPos.toSeedCheckerBlockPos().getZ()); // Failsafe in case the chest can't generate (extreme edge case)
                    }
                } else if (piece instanceof StrongholdPieces.SquareRoom squareRoom) { // Checks the chest loot of the wooden 4 way
                    StrongholdGenerator.Util.BPos chestPos = squareRoom.getChestPos();

                    if (chestPos == null) // Skip rooms without chests
                        continue;
                    ChestBlockEntity chest = (ChestBlockEntity) seedChecker.getBlockEntity(chestPos.toSeedCheckerBlockPos().getX(), chestPos.toSeedCheckerBlockPos().getY(), chestPos.toSeedCheckerBlockPos().getZ());
                    try {
                        long lootSeed = (Long) ReflectionUtils.getValueFromField(chest, "lootTableSeed"); // Derive loot seed
                        List<ItemStack> items = MCLootTables.STRONGHOLD_CROSSING_CHEST.get().generate(new LootContext(lootSeed)); // Get loot from loot seed
                        combinedLoot.addAll(items); // Adds all the items from the chest into the master list
                    } catch (Exception ex) {
                        // System.out.printf("ERROR - Seed: %d, Chest Position: (%d, %d, %d)\n", seed, chestPos.toSeedCheckerBlockPos().getX(), chestPos.toSeedCheckerBlockPos().getY(), chestPos.toSeedCheckerBlockPos().getZ()); // Failsafe in case the chest can't generate (extreme edge case)
                    }
                }
            }

//            `System.out.println(seed);`
            // Test the combined loot for the seed
            if (testLoot(combinedLoot, results.templeMendingOrUnbreaking, results.templeChanneling, results.templePiercing4, results.templePiercing3Count)) {
                Items.ENCHANTED_BOOK.getEnchantments().clear();
                return true;
            }
        }
        return false;
    }

    private static void testTempleLoot(List<ItemStack> templeLoot, LootResults results) { // Same as testing for stronghold loot but using global variables so each stronghold can independently take in account the temple loot
        results.templeMendingOrUnbreaking = false;
        results.templeChanneling = false;
        results.templePiercing4 = false;
        results.templePiercing3Count = 0;

        List<ItemStack> availableBooks = new ArrayList<>();

        for (ItemStack itemStack : templeLoot) {
            if (itemStack.getItem().equalsName(Items.ENCHANTED_BOOK)) {
                availableBooks.add(itemStack);
            }
        }

        for (ItemStack book : availableBooks) {
            List<Pair<String, Integer>> enchantments = book.getItem().getEnchantments();
            boolean used = false;

            for (Pair<String, Integer> enchantment : enchantments) {
                String enchant = enchantment.getFirst();
                int level = enchantment.getSecond();

                if (!used) {
                    if ((enchant.equals("unbreaking") && level == 3 && !results.templeMendingOrUnbreaking) ||
                            (enchant.equals("mending") && !results.templeMendingOrUnbreaking)) {
                        results.templeMendingOrUnbreaking = true;
                        used = true;
                    } else if (enchant.equals("piercing")) {
                        if (level == 4 && !results.templePiercing4) {
                            results.templePiercing4 = true;
                            used = true;
                        } else if (level == 3 && !results.templePiercing4) {
                            results.templePiercing3Count++;
                            used = true;
                        }
                    } else if (enchant.equals("channeling") && !results.templeChanneling) {
                        results.templeChanneling = true;
                        used = true;
                    }
                }
            }
        }
    }

    private static boolean testLoot(List<ItemStack> loot, boolean templeMendingOrUnbreaking, boolean templeChanneling, boolean templePiercing4, int templePiercing3Count) {
        boolean foundUnbreakingOrMending = false;
        boolean foundChanneling = false;
        boolean foundPiercing4 = false;
        int piercing3Count = 0;

        List<ItemStack> availableBooks = new ArrayList<>();

        for (ItemStack itemStack : loot) { // Takes only the enchanted books from all the loot and puts them on a new list
            if (itemStack.getItem().equalsName(Items.ENCHANTED_BOOK)) {
                availableBooks.add(itemStack);
            }
        }

        for (ItemStack book : availableBooks) { // Checks each enchanted book one by one
            List<Pair<String, Integer>> enchantments = book.getItem().getEnchantments();

            boolean used = false; // Can't have two of the desired enchantments on the same book

            for (Pair<String, Integer> enchantment : enchantments) { // Checks each enchantment one by one
                String enchant = enchantment.getFirst();
                int level = enchantment.getSecond();

                if (!used) {
                    if ((enchant.equals("unbreaking") && level == 3 && !foundUnbreakingOrMending) || (enchant.equals("mending") && !foundUnbreakingOrMending)) { // Only need mending or unbreaking, not both
                        foundUnbreakingOrMending = true;
                        used = true;
                    } else if (enchant.equals("piercing")) { // Allowing 2 piercing 3 books or a piercing 4 book
                        if (level == 4 && !foundPiercing4) {
                            foundPiercing4 = true;
                            used = true;
                        } else if (level == 3 && !foundPiercing4) {
                            piercing3Count++;
                            used = true;
                        }
                    } else if (enchant.equals("channeling") && !foundChanneling) {
                        foundChanneling = true;
                        used = true;
                    }
                }
            }

            if ((foundUnbreakingOrMending || templeMendingOrUnbreaking) && (foundChanneling || templeChanneling) && ((foundPiercing4 || piercing3Count >= 2) || (templePiercing4 || templePiercing3Count >= 2))) {
                return true;
            }
        }
//        System.out.println(foundUnbreakingOrMending);
//        System.out.println(templeMendingOrUnbreaking);
//        System.out.println(foundChanneling);
//        System.out.println(templeChanneling);
//        System.out.println(foundPiercing4);
//        System.out.println(piercing3Count);
//        System.out.println(templePiercing4);
//        System.out.println(templePiercing3Count);

        return false;
    }


}
