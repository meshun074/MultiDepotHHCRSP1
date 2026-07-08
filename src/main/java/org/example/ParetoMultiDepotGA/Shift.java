package org.example.ParetoMultiDepotGA;

import org.example.Data.Caregiver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Shift {

    private Caregiver caregiver;
    private List<Integer> route;
    private final Set<Integer> routePatients;
    private List<Double> currentTime;
    private List<Double> travelCost;
    private List<Double> tardiness;
    private List<Double> maxTardiness;
    private List<Double> totalWaitingTime;
    private List<Double> Overtime;
    private double startingWaitingTime;
    private double idleTime;
    private double load;

    public Shift(Caregiver caregiver, List<Integer> route, double currentTime) {
        this.caregiver = caregiver;
        this.route = route;
        this.routePatients = new HashSet<>(route);
        this.currentTime = new ArrayList<>();
        this.currentTime.add(currentTime);
        this.travelCost = new ArrayList<>();
        this.tardiness = new ArrayList<>();
        this.maxTardiness = new ArrayList<>();
        this.totalWaitingTime = new ArrayList<>();
        this.Overtime = new ArrayList<>();
        startingWaitingTime = 0;
        this.load = 0.0;
        idleTime = 0.0;
        travelCost.add(0.0);
        tardiness.add(0.0);
        maxTardiness.add(0.0);
        totalWaitingTime.add(0.0);
        Overtime.add(0.0);
    }
    public Shift(Caregiver caregiver, List<Integer> route, List<Double> currentTime, List<Double> travelCost, List<Double> tardiness, List<Double> maxTardiness, List<Double> totalWaitingTime, List<Double> Overtime, double idleTime, double startingWaitingTime) {
        this.caregiver = caregiver;
        this.route = route;
        this.routePatients = new HashSet<>(route);
        this.currentTime = currentTime;
        this.travelCost = travelCost;
        this.tardiness = tardiness;
        this.maxTardiness = maxTardiness;
        this.totalWaitingTime = totalWaitingTime;
        this.Overtime = Overtime;
        this.idleTime = idleTime;
        this.startingWaitingTime = startingWaitingTime;
    }
    public void resetShift(){
        this.route.clear();
        this.routePatients.clear();
        this.currentTime.clear();
        this.travelCost.clear();
        this.tardiness.clear();
        this.maxTardiness.clear();
        this.totalWaitingTime.clear();
        this.Overtime.clear();
        this.currentTime.add(caregiver.getWorking_shift()[0]);
        this.load = 0.0;
        idleTime = 0.0;
        travelCost.add(0.0);
        tardiness.add(0.0);
        maxTardiness.add(0.0);
        totalWaitingTime.add(0.0);
        Overtime.add(0.0);
        startingWaitingTime = 0.0;
    }


    public Caregiver getCaregiver() {
        return caregiver;
    }

    public void setCaregiver(Caregiver caregiver) {
        this.caregiver = caregiver;
    }

    public List<Integer> getRoute() {
        return route;
    }

    public void setRoute(List<Integer> route) {
        this.route = route;
        this.routePatients.clear();
        this.routePatients.addAll(route);
    }
    public void updateRoute(int patient) {
        route.add(patient);
        routePatients.add(patient);
    }

    public boolean containsPatient(int patient) {
        return routePatients.contains(patient);
    }

    public List<Double> getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(double currentTime) {
        this.currentTime.add(currentTime);
    }

    public void updateCurrentTime(double currentTime) {
        double newValue = currentTime + this.getCurrentTime().get(this.getCurrentTime().size() - 1);
        this.currentTime.add(newValue);
    }

    public List<Double> getTardiness() {
        return tardiness;
    }

    public void setTardiness(double tardiness) {
        this.tardiness.add(tardiness);
    }

    public void updateTardiness(double tardiness) {
        double newValue = this.tardiness.get(this.tardiness.size()-1) + tardiness;
        this.tardiness.add(newValue);
        updateMaxTardiness(tardiness);
    }
    public void addTardiness(double tardiness) {
        this.tardiness.add(tardiness);
    }

    public double getLoad() {
        return load;
    }

    public void setLoad(double load) {
        this.load = load;
    }
    public void updateLoad(double load) {
        this.load += load;
    }
    public List<Double> getMaxTardiness() {
        return maxTardiness;
    }

    public void updateMaxTardiness(double maxTardiness) {
        double currentMax = this.maxTardiness.get(this.maxTardiness.size()-1);
        this.maxTardiness.add(Math.max(currentMax, maxTardiness));
    }

    public void initializeMaxTardiness(double maxTardiness) {
        this.maxTardiness.add(maxTardiness);
    }

    public List<Double> getTravelCost() {
        return travelCost;
    }

    public void setTravelCost(ArrayList<Double> travelCost) {
        this.travelCost= new ArrayList<>(travelCost);
    }

    public void updateTravelCost(double travelCost) {
        double newValue = this.travelCost.get(this.travelCost.size()-1) + travelCost;
        this.travelCost.add(newValue);
    }

    public void addTravelCost(double travelCost) {
        this.travelCost.add(travelCost);
    }

    public List<Double> getTotalWaitingTime() {
        return totalWaitingTime;
    }
    public void setTotalWaitingTime(ArrayList<Double> totalWaitingTime) {
        this.totalWaitingTime = new ArrayList<>(totalWaitingTime);
    }
    public void updateWaitingTime(double waitingTime) {
        double newValue = this.totalWaitingTime.get(this.totalWaitingTime.size()-1) + waitingTime;
        this.totalWaitingTime.add(newValue);
    }

    public double getIdleTime(){
        return idleTime;
    }
    public void setIdleTime(double idleTime) {
        this.idleTime = idleTime;
    }

    public void setStartingWaitingTime(double startingWaitingTime) {
        this.startingWaitingTime = startingWaitingTime;
    }
    public double getStartingWaitingTime() {
        return startingWaitingTime;
    }
    public void updateStartingWaitingTime(double startingWaitingTime) {

        this.startingWaitingTime += startingWaitingTime;
    }

    public void addWaitingTime(double waitingTime) {
        this.totalWaitingTime.add(waitingTime);
    }

    public List<Double> getOvertime() {return Overtime;}

    public void setOvertime(ArrayList<Double> overtime) {
        this.Overtime = new  ArrayList<>(overtime);
    }
    public void updateOvertime(double overtime) {
        double newValue = this.Overtime.get(this.Overtime.size()-1) + overtime;
        this.Overtime.add(newValue);
    }
    public void addOvertime(double overtime) {
        this.Overtime.add(overtime);
    }

    public void showInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Route -- ").append(route).append("\n")
                .append("Time -- ").append(currentTime).append("\n")
                .append("Tardiness -- ").append(tardiness).append("\n")
                .append("MaxTardiness -- ").append(maxTardiness).append("\n")
                .append("Travel cost -- ").append(travelCost);
        System.out.println(sb);
    }
}

