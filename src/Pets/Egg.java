package Pets;

import java.util.Arrays;
import java.util.Random;

/**
 * Egg types sold in the shop – each has a cost and weighted rarity pool.
 * Opening an egg gives one copy via PetInventory.addCopy().
 */
public class Egg {
    public enum EggType {
        BASIC_EGG    ("Basic Egg",      200, "Common pets only",
                new double[]{1.00, 0.00, 0.00, 0.00, 0.00}),
        FOREST_EGG   ("Forest Egg",     500, "Common & Uncommon",
                new double[]{0.55, 0.45, 0.00, 0.00, 0.00}),
        MAGIC_EGG    ("Magic Egg",     1200, "Up to Rare",
                new double[]{0.20, 0.40, 0.40, 0.00, 0.00}),
        DARK_EGG     ("Dark Egg",      3000, "Up to Epic",
                new double[]{0.05, 0.20, 0.45, 0.30, 0.00}),
        LEGENDARY_EGG("Legendary Egg", 8000, "Any rarity!",
                new double[]{0.00, 0.10, 0.30, 0.35, 0.25});

        public final String displayName;
        public final int    cost;
        public final String description;
        /** Probability weights: [COMMON, UNCOMMON, RARE, EPIC, LEGENDARY] */
        public final double[] rarityWeights;

        EggType(String dn, int cost, String desc, double[] w) {
            displayName=dn; this.cost=cost; description=desc; rarityWeights=w;
        }
    }

    private static final Random RNG = new Random();

    /**
     * Open one egg, add the result to the inventory, and return the Pet type received.
     */
    public static Pet.PetType openEgg(EggType eggType, PetInventory inventory) {
        Pet.Rarity rarity = rollRarity(eggType.rarityWeights);
        Pet.PetType petType = randomPetOfRarity(rarity);
        inventory.addCopy(petType);
        return petType;
    }

    private static Pet.Rarity rollRarity(double[] weights) {
        Pet.Rarity[] rarities = Pet.Rarity.values();
        double roll = RNG.nextDouble(), cumulative = 0;
        for (int i = 0; i < weights.length; i++) {
            cumulative += weights[i];
            if (roll < cumulative) return rarities[i];
        }
        return Pet.Rarity.COMMON;
    }

    private static Pet.PetType randomPetOfRarity(Pet.Rarity rarity) {
        Pet.PetType[] matching = Arrays.stream(Pet.PetType.values())
                .filter(t -> t.rarity == rarity)
                .toArray(Pet.PetType[]::new);
        if (matching.length == 0) return Pet.PetType.SLIMELING;
        return matching[RNG.nextInt(matching.length)];
    }
}