/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package dso.concurrency;

import java.util.Random;

/**
 * Created by Alan Brown
 * Date: May 17, 2005
 * Time: 1:48:02 PM
 */
public class Shuffler {

    private int shuffleSize = 0;
    private Random random = new Random();
    int[] positions;

    public Shuffler(int shuffleSize) {
        this.shuffleSize = shuffleSize;
        positions = new int[shuffleSize];
        for (int i=0; i<shuffleSize; i++) {
            positions[i] = i;
        }
    }

    public int[] shuffleOrder() {

        for (int i=0; i<shuffleSize; i++) {
            int changingPosition = random.nextInt(shuffleSize);
            int swappedValue = positions[i];
            positions[i] = positions[changingPosition];
            positions[changingPosition] = swappedValue;
        }
        return positions;
    }
}
