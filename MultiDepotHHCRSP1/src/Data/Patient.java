package Data;

import GeneticAlgorithm.CaregiverPair;

import java.util.ArrayList;
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
    private int distance_matrix_index;
    private String[] incompatible_caregivers;
    private Set<Integer> possibleFirstCaregiver;
    private Set<Integer> possibleSecondCaregiver;
    private List<Integer> possibleFirstCaregiverList;
    private List<Integer> possibleSecondCaregiverList;
    private Set<Integer> allCaregiversForDoubleService;
    private List<CaregiverPair> allPossibleCaregiverCombinations;
    private Set<CaregiverPair> allPossibleCaregiverCombinationsCrossover;
    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    public Set<Integer> getPossibleFirstCaregiver() {
        return possibleFirstCaregiver;
    }

    public void setPossibleFirstCaregiver(Set<Integer> possibleFirstCaregiver) {
        this.possibleFirstCaregiver = new HashSet<>(possibleFirstCaregiver);
        this.possibleFirstCaregiverList = new ArrayList<>(possibleFirstCaregiver);
    }

    public Set<Integer> getPossibleSecondCaregiver() {
        return possibleSecondCaregiver;
    }

    public void setPossibleSecondCaregiver(Set<Integer> possibleSecondCaregiver) {
        this.possibleSecondCaregiver = new HashSet<>(possibleSecondCaregiver);
        this.possibleSecondCaregiverList = new ArrayList<>(possibleSecondCaregiver);
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
        int firstCaregiver = possibleFirstCaregiverList.get(random.nextInt(possibleFirstCaregiver.size()));

        if (required_caregivers.length == 0) {
            return new CaregiverPair(firstCaregiver, -1);
        }
        int secondCaregiver;
        int count = 1;
        do {
            if (count % 3 == 0) {
                firstCaregiver = possibleFirstCaregiverList.get(random.nextInt(possibleFirstCaregiver.size()));
            }
            secondCaregiver = possibleSecondCaregiverList.get(random.nextInt(possibleSecondCaregiver.size()));
            count++;
        }
        while (firstCaregiver == secondCaregiver);

        return new CaregiverPair(firstCaregiver, secondCaregiver);
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

}
