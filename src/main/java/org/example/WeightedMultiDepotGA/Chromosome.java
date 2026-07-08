package org.example.WeightedMultiDepotGA;

import org.example.Data.Caregiver;
import org.example.Data.InstancesClass;

import java.util.*;

public class Chromosome {
    private List<Integer>[] genes;
    private int caregivers;
    private int crossIndex = -1;
    private int first=-1;
    private int second=-1;
    private int third=-1;
    private int fourth=-1;
    private int firstPosition =-1;
    private int secondPosition =-1;
    private int thirdPosition =-1;
    private int fourthPosition =-1;
    private int rank = -1;
    private double fitness;
    private double totalTravelCost;
    private double totalTardiness;
    private double highestTardiness;
    private double totalWaitingTime;
    private double HighestIdleTime;
    private double Overtime;
    private Shift[] caregiversRouteUp;
    private final double[] objectives = new double[6];
    private final Map<Integer, Set<Integer>> patientToRoutesMap = new HashMap<>();

    public Chromosome(List<Integer>[] genes, boolean newChromosome) {
        this(genes, 0.0, newChromosome, null);
    }

    public Chromosome(List<Integer>[] genes, double fitness, boolean newChromosome) {
        this(genes, fitness, newChromosome, null);
    }

    public Chromosome(List<Integer>[] genes, double fitness, boolean newChromosome, Shift[] routes) {
        this.genes = copyGenesIfNeeded(genes, newChromosome);
        this.caregivers = genes.length;
        this.fitness = fitness;
        caregiversRouteUp = routes == null ? new Shift[caregivers] : routes;
        this.totalTravelCost = 0;
        this.totalTardiness = 0;
        this.highestTardiness = 0;
        this.totalWaitingTime = 0;
        this.HighestIdleTime = 0;
        this.Overtime = 0;
    }

    @SuppressWarnings("unchecked")
    private static List<Integer>[] copyGenesIfNeeded(
            List<Integer>[] genes, boolean copyGenes) {
        if (!copyGenes) {
            return genes;
        }
        List<Integer>[] copy = new ArrayList[genes.length];
        for (int i = 0; i < genes.length; i++) {
            copy[i] = new ArrayList<>(genes[i]);
        }
        return copy;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public int getCrossIndex() {
        return crossIndex;
    }

    public void setCrossIndex(int crossIndex) {
        this.crossIndex = crossIndex;
    }

    public int getFirst() {
        return first;
    }

    public void setFirst(int first) {
        this.first = first;
    }

    public int getSecond() {
        return second;
    }

    public void setSecond(int second) {
        this.second = second;
    }

    public int getFirstPosition() {
        return firstPosition;
    }

    public void setFirstPosition(int firstPosition) {
        this.firstPosition = firstPosition;
    }

    public int getSecondPosition() {
        return secondPosition;
    }

    public void setSecondPosition(int secondPosition) {
        this.secondPosition = secondPosition;
    }

    public int getThird() {
        return third;
    }

    public void setThird(int third) {
        this.third = third;
    }

    public int getFourth() {
        return fourth;
    }

    public void setFourth(int fourth) {
        this.fourth = fourth;
    }

    public int getThirdPosition() {
        return thirdPosition;
    }

    public void setThirdPosition(int thirdPosition) {
        this.thirdPosition = thirdPosition;
    }

    public int getFourthPosition() {
        return fourthPosition;
    }

    public void setFourthPosition(int fourthPosition) {
        this.fourthPosition = fourthPosition;
    }

    public void initializeCaregiversRoute(InstancesClass instance) {
        for(int i =0; i<caregivers;i++){
            Caregiver caregiver = instance.getCaregivers()[i];
            Shift s = new Shift(caregiver,new ArrayList<>(), caregiver.getWorking_shift()[0]);
            caregiversRouteUp[i] = s;
        }
    }

    public int getCaregivers() {
        return caregivers;
    }
    // Call this once when `genes` is initialized or updated
    public void buildPatientRouteMap() {
        patientToRoutesMap.clear();
        for (int i = 0; i < genes.length; i++) {
            for (int patient : genes[i]) {
                patientToRoutesMap.computeIfAbsent(patient, k -> new HashSet<>()).add(i);
            }
        }
    }

    public  Map<Integer, Set<Integer>> getPatientToRoutesMap(){
        return patientToRoutesMap;
    }

    public Set<Integer> getPatientRoutes(int patient) {
        return patientToRoutesMap.get(patient);
    }

    public void setCaregivers(int caregivers) {
        this.caregivers = caregivers;
    }

    public double getFitness() {
        return fitness;
    }

    public void setFitness(double fitness) {
        this.fitness = fitness;
    }

    public List<Integer>[] getGenes() {
        return genes;
    }

    public void setGenes(List<Integer>[] genes) {
        this.genes = genes;
        patientToRoutesMap.clear();
    }

    public void setCaregiversRouteUp(Shift[] caregiversRouteUp) {
        this.caregiversRouteUp = caregiversRouteUp;
    }
    public Shift[] getCaregiversRouteUp() {
        return caregiversRouteUp;
    }

    public double getTotalTravelCost() {
        return totalTravelCost;
    }

    public void setTotalTravelCost(double totalTravelCost) {
        this.totalTravelCost = totalTravelCost;
    }
    public void updateTotalTravelCost(double totalTravelCost) {
        this.totalTravelCost += totalTravelCost;
    }

    public double getTotalTardiness() {
        return totalTardiness;
    }

    public void setTotalTardiness(double totalTardiness) {
        this.totalTardiness = totalTardiness;
    }

    public double getHighestTardiness() {
        return highestTardiness;
    }

    public void setHighestTardiness(double highestTardiness) {
        this.highestTardiness = highestTardiness;
    }
    public void updateTotalTardiness(double totalTardiness) {
        this.totalTardiness += totalTardiness;
    }

    public double getTotalWaitingTime() {return totalWaitingTime;}
    public void setTotalWaitingTime(double totalWaitingTime) {this.totalWaitingTime = totalWaitingTime;}
    public void updateWaitingTime(double waitingTime) {this.totalWaitingTime += waitingTime;}

    public double getHighestIdleTime() {return HighestIdleTime;}
    public void setHighestIdleTime(double highestIdleTime) {this.HighestIdleTime = highestIdleTime;}
    public void updateHighestIdleTime(double highestIdleTime) {this.HighestIdleTime = Math.max(highestIdleTime,this.HighestIdleTime);}

    public double getOvertime() {return Overtime;}
    public void setOvertime(double overtime) {this.Overtime = overtime;}
    public void updateOvertime(double overtime) {this.Overtime += overtime;}

    public void showSolution(int index) {
        System.out.print("\n Best Solution : "+index+"\n");
        for (int i =0; i< genes.length; i++) {
            List<Integer> route = genes[i];
            Shift Caregiver = caregiversRouteUp[i];
            System.out.println(Caregiver.getCaregiver().getId() +" - "+ route);
            System.out.println("Travel Cost to patients\n"+Caregiver.getTravelCost());
            System.out.println("Service completed time at patients\n"+Caregiver.getCurrentTime());
//            System.out.println("Route total tardiness: "+Caregiver.getTardiness());
            System.out.println("Route total tardiness: "+Caregiver.getTardiness().get(Caregiver.getTardiness().size()-1)+" Route Highest tardiness: "+Caregiver.getMaxTardiness().get(Caregiver.getMaxTardiness().size()-1));
            System.out.println("Waiting Time: "+Caregiver.getTotalWaitingTime());
            System.out.println("Overtime: "+Caregiver.getOvertime());
            System.out.println();
        }

    }
    @Override
    public String toString() {
        StringBuilder genesStrings= new StringBuilder();
        for(List<Integer> c :genes){
            genesStrings.append(c.toString());
        }
        return genesStrings.toString();
    }

    public double[] getObjective() {
        objectives[0] = totalTravelCost;
        objectives[1] = totalTardiness;
        objectives[2] = highestTardiness;
        objectives[3] = totalWaitingTime;
        objectives[4] = HighestIdleTime;
        objectives[5] = Overtime;
        return objectives;
    }
}
