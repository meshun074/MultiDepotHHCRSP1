package org.example.Data;

public class CaregiverPair {
    private final int first;
    private final int second;
    private final int hash;

    public CaregiverPair(int first, int second) {
        this.first = first;
        this.second = second;
        this.hash = 31 * Math.min(first, second) + Math.max(first, second);
    }

    public int getFirst() {
        return first;
    }

    public int getSecond() {
        return second;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof CaregiverPair other)) {
            return false;
        }
        return (first == other.first && second == other.second)
                || (first == other.second && second == other.first);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
