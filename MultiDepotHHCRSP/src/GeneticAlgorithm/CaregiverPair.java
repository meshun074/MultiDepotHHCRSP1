package GeneticAlgorithm;

import java.util.Objects;

public class CaregiverPair {
    private final int first;
    private final int second;
    private final int hash;
    public CaregiverPair(int first, int second) {
        this.first = first;
        this.second = second;
        this.hash = Objects.hash(first, second);
    }
    public int getFirst() {
        return first;
    }
    public int getSecond() {
        return second;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CaregiverPair other)) return false;
        return (this.first == other.first && this.second == other.second) ||
                (this.first == other.second && this.second == other.first);
    }

    // Symmetric hash: order doesn't matter
    @Override
    public int hashCode() {
        // unordered hash (x,y same as y,x)
        return hash;
    }
}
