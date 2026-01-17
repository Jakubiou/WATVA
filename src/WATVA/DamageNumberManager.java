
package WATVA;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages all damage numbers in the game independently from enemies.
 * This ensures damage numbers remain visible even after enemy death.
 */
public class DamageNumberManager {
    private List<DamageNumber> damageNumbers;

    public DamageNumberManager() {
        this.damageNumbers = new ArrayList<>();
    }

    /**
     * Adds a new damage number at the specified position.
     */
    public void addDamageNumber(int x, int y, int damage) {
        damageNumbers.add(new DamageNumber(x, y, damage));
    }

    /**
     * Updates all damage numbers and removes inactive ones.
     */
    public void update() {
        for (int i = damageNumbers.size() - 1; i >= 0; i--) {
            DamageNumber damageNumber = damageNumbers.get(i);
            damageNumber.update();
            if (!damageNumber.isActive()) {
                damageNumbers.remove(i);
            }
        }
    }

    /**
     * Draws all active damage numbers.
     */
    public void draw(Graphics g) {
        for (DamageNumber damageNumber : damageNumbers) {
            damageNumber.draw(g);
        }
    }

    /**
     * Returns the count of active damage numbers.
     */
    public int getCount() {
        return damageNumbers.size();
    }
}