package org.example.ParetoMultiDepotGA;

import java.util.ArrayList;
import java.util.List;

public class RouteInitializer {

    private final List<Integer> alleles;
    private double solutionCost;
    private double totalTravelCost;
    private double totalTardiness;
    private double highestTardiness;
    private double waitingTime;
    private double highestIdleTime;
    private double overtime;
    private final List<Integer>[] caregiversRoute;

    public RouteInitializer(List<Integer> patientOrder, int caregiverNumber, double solutionCost) {
        this.alleles = patientOrder;
        this.solutionCost = solutionCost;
        totalTravelCost = 0;
        totalTardiness = 0;
        highestTardiness = 0;
        waitingTime = 0;
        highestIdleTime = 0;
        overtime = 0;
        caregiversRoute = new ArrayList[caregiverNumber];
        int size = (patientOrder.size()*2+1)/3;
        for (int i = 0; i < caregiverNumber; i++) {
            caregiversRoute[i] = new ArrayList<>(size);
        }
    }

    public List<Integer> getAlleles() {
        return alleles;
    }

    public double getSolutionCost() {
        return solutionCost;
    }

    public void setSolutionCost(double solutionCost) {
        this.solutionCost = solutionCost;
    }

    public List<Integer>[] getCaregiversRoute() {
        return caregiversRoute;
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

    public double getWaitingTime() {return waitingTime;}
    public void setWaitingTime(double waitingTime) {this.waitingTime = waitingTime;}

    public double getHighestIdleTime() {return highestIdleTime;}
    public void setHighestIdleTime(double highestIdleTime) {this.highestIdleTime = highestIdleTime;}

    public double getOvertime() {return overtime;}
    public void setOvertime(double overtime) {this.overtime = overtime;}

    public void showString(){
        for (List<Integer> shift : caregiversRoute) {
            System.out.println(shift);
        }
        System.out.println("Done\n");
    }
}

