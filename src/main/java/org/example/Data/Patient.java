package org.example.Data;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class Patient {
    private String id;
    private double[] location;
    private double[] time_window;
    private Required_Caregiver[] required_caregivers;
    private Synchronization synchronization;
    private int distance_matrix_index = -1;
    private String[] incompatible_caregivers;
    private Set<Integer> possibleFirstCaregiver;
    private Set<Integer> possibleSecondCaregiver;
    private Set<Integer> allCaregiversForDoubleService;
    private List<CaregiverPair> allPossibleCaregiverCombinations;
    private Set<CaregiverPair> allPossibleCaregiverCombinationsCrossover;
    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    public Set<Integer> getPossibleFirstCaregiver() {
        return possibleFirstCaregiver;
    }

    public void setPossibleFirstCaregiver(Set<Integer> possibleFirstCaregiver) {
        this.possibleFirstCaregiver = new HashSet<>(possibleFirstCaregiver);
    }

    public Set<Integer> getPossibleSecondCaregiver() {
        return possibleSecondCaregiver;
    }

    public void setPossibleSecondCaregiver(Set<Integer> possibleSecondCaregiver) {
        this.possibleSecondCaregiver = new HashSet<>(possibleSecondCaregiver);
    }

    public Set<CaregiverPair> getAllPossibleCaregiverCombinationsCrossover() {
        return allPossibleCaregiverCombinationsCrossover;
    }

    public void setAllPossibleCaregiverCombinationsCrossover(Set<CaregiverPair> allPossibleCaregiverCombinationsCrossover) {
        this.allPossibleCaregiverCombinationsCrossover = allPossibleCaregiverCombinationsCrossover;
    }

    public List<CaregiverPair> getAllPossibleCaregiverCombinations() {
        return allPossibleCaregiverCombinations;
    }

    public void setAllPossibleCaregiverCombinations(List<CaregiverPair> allPossibleCaregiverCombinations) {
        this.allPossibleCaregiverCombinations = allPossibleCaregiverCombinations;
    }

    public Set<Integer> getAllCaregiversForDoubleService() {
        return allCaregiversForDoubleService;
    }

    public void setAllCaregiversForDoubleService(Set<Integer> allCaregiversForDoubleService) {
        this.allCaregiversForDoubleService = allCaregiversForDoubleService;
    }

    public CaregiverPair getRandomCaregiverPair() {
        if (allPossibleCaregiverCombinations == null || allPossibleCaregiverCombinations.isEmpty()) {
            throw new IllegalStateException("No feasible caregiver combination for patient '" + id + "'");
        }
        return allPossibleCaregiverCombinations.get(
                random.nextInt(allPossibleCaregiverCombinations.size()));
    }

    public String getId() {
        return id;
    }

    public double[] getLocation() {
        return location;
    }

    public double[] getTime_window() {
        return time_window;
    }

    public Required_Caregiver[] getRequired_caregivers() {
        return required_caregivers;
    }

    public Synchronization getSynchronization() {
        return synchronization;
    }

    public int getDistance_matrix_index() {
        return distance_matrix_index;
    }

    public String[] getIncompatible_caregivers() {
        return incompatible_caregivers;
    }

    void assignDistanceMatrixIndex(int distanceMatrixIndex) {
        this.distance_matrix_index = distanceMatrixIndex;
    }

}
