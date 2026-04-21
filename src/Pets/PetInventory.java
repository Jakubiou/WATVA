package Pets;

import java.io.*;
import java.util.*;

/**
 * Stores all owned pets (as stacks by PetType), the active pet for the current game,
 * and persists to disk.
 *
 * Key design:
 *  - One "master" Pet per PetType tracks the level.
 *  - A separate int map tracks how many copies (duplicates) the player owns.
 *  - To upgrade from Lv N to Lv N+1, the player needs Pet.getDupesRequired() extra copies
 *    (on top of the 1 they already "own" as the levelled pet) PLUS the coin cost.
 */
public class PetInventory implements Serializable {
    private static final long serialVersionUID = 2L;
    private static final String SAVE_FILE = "pet_inventory.dat";

    /** One Pet instance per type (holds the level). Null = not unlocked yet. */
    private final Map<Pet.PetType, Pet> ownedPets = new LinkedHashMap<>();
    /** How many extra copies (beyond the 1 base) the player holds, per type */
    private final Map<Pet.PetType, Integer> duplicateCounts = new LinkedHashMap<>();
    /** Which pet is selected for the NEXT run (persisted) */
    private Pet.PetType selectedPetType = null;

    public static PetInventory load() {
        File f = new File(SAVE_FILE);
        if (!f.exists()) return new PetInventory();
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            return (PetInventory) ois.readObject();
        } catch (Exception e) {
            System.err.println("Could not load pet inventory: " + e.getMessage());
            return new PetInventory();
        }
    }

    public void save() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SAVE_FILE))) {
            oos.writeObject(this);
        } catch (Exception e) {
            System.err.println("Could not save pet inventory: " + e.getMessage());
        }
    }

    /**
     * Add one copy of the given pet type.
     * If not yet owned, unlocks it at level 1 (this copy IS the base copy).
     * If already owned, increments the duplicate counter.
     */
    public void addCopy(Pet.PetType type) {
        if (!ownedPets.containsKey(type)) {
            ownedPets.put(type, new Pet(type));
            duplicateCounts.put(type, 0);
        } else {
            duplicateCounts.merge(type, 1, Integer::sum);
        }
    }

    /**
     * Returns true if we can upgrade the given pet type right now
     * (enough dupes + enough coins check is done externally for the coin part).
     */
    public boolean hasEnoughDupes(Pet.PetType type) {
        Pet p = ownedPets.get(type);
        if (p == null || !p.canUpgrade()) return false;
        int needed = p.getDupesRequired();
        return getDuplicateCount(type) >= needed;
    }

    /**
     * Consume duplicates and upgrade the pet.
     * Caller must already have checked coins and deducted them.
     * @return true on success
     */
    public boolean upgradePet(Pet.PetType type) {
        Pet p = ownedPets.get(type);
        if (p == null || !p.canUpgrade()) return false;
        int needed = p.getDupesRequired();
        int have   = getDuplicateCount(type);
        if (have < needed) return false;
        duplicateCounts.put(type, have - needed);
        p.upgrade();
        return true;
    }

    public void selectPet(Pet.PetType type) {
        if (isUnlocked(type)) selectedPetType = type;
    }
    public void deselectPet() { selectedPetType = null; }
    public Pet.PetType getSelectedPetType() { return selectedPetType; }

    /** Returns the live Pet instance for the selected pet, or null. */
    public Pet getSelectedPet() {
        if (selectedPetType == null) return null;
        return ownedPets.get(selectedPetType);
    }

    public boolean isUnlocked(Pet.PetType type) { return ownedPets.containsKey(type); }
    public Pet getPet(Pet.PetType type)          { return ownedPets.get(type); }
    public int getDuplicateCount(Pet.PetType type) {
        return duplicateCounts.getOrDefault(type, 0);
    }
    /** Total owned copies = 1 (base) + duplicates */
    public int getTotalCopies(Pet.PetType type) {
        if (!isUnlocked(type)) return 0;
        return 1 + getDuplicateCount(type);
    }
    public int getUnlockedCount() { return ownedPets.size(); }
    public Collection<Pet> getUnlockedPets() { return ownedPets.values(); }
}