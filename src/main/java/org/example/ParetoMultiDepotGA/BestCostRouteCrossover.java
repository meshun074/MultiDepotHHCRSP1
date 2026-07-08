package org.example.ParetoMultiDepotGA;

import org.example.Data.Caregiver;
import org.example.Data.CaregiverPair;
import org.example.Data.InstancesClass;
import org.example.Data.Patient;

import java.util.*;

import static org.example.ParetoMultiDepotGA.EvaluationFunction.patientIsAssigned;
import static org.example.ParetoMultiDepotGA.GeneticAlgorithm.conflictCheck;


public class BestCostRouteCrossover {
    private final int r;
    private final Chromosome p1, p2;
    private static Patient[] allPatients;
    private static Caregiver[] allCaregivers;
    private static int numOfCaregivers;
    private static int[] patientMatrixIndexes;
    private static double[][] distances;
    private final Random rand;
    private final int[] routeEndPoint;
    private final double[] routesCurrentTime;
    private final double[] highestAndTotalTardiness;
    private final double[] totalWaitingTime;
    private final double[] startingWaitingTime;
    private final double[] totalOvertime;
    private final Set<Integer> track;

    public BestCostRouteCrossover(int r, Chromosome p1, Chromosome p2, Random rand) {
        this.r = r;
        this.p1 = p1;
        this.p2 = p2;
        this.rand = rand;
        this.routeEndPoint = new int[numOfCaregivers];
        this.routesCurrentTime = new double[numOfCaregivers];
        this.highestAndTotalTardiness = new double[2];
        this.totalWaitingTime = new double[numOfCaregivers + 1];
        this.startingWaitingTime = new double[numOfCaregivers];
        this.totalOvertime = new double[numOfCaregivers];
        this.track = new HashSet<>(100);
    }

    public static void initialize(InstancesClass instance) {
        allPatients = instance.getPatients();
        distances = instance.getDistances();
        allCaregivers = instance.getCaregivers();
        numOfCaregivers = instance.getCaregivers().length;
        patientMatrixIndexes = new int[allPatients.length];
        for (int i = 0; i < allPatients.length; i++) {
            patientMatrixIndexes[i] = allPatients[i].getDistance_matrix_index();
        }
    }

    public Chromosome Crossover(){
        // Initialize variables
        Set<Integer> selectRoute = new HashSet<>(p2.getGenes()[r]);
        List<Integer>[] p1Routes = p1.getGenes();
        List<Integer>[] c1Routes = new ArrayList[p1Routes.length];

        //Remove patients of selected route from parents
        for (int i = 0; i < p1Routes.length; i++) {
            List<Integer> route = new ArrayList<>(p1Routes[i].size());
            for (int j = 0; j < p1Routes[i].size(); j++) {
                int patient = p1Routes[i].get(j);
                if (!selectRoute.contains(patient)) {
                    route.add(patient);
                }
            }
            c1Routes[i] = route;
        }
        List<Integer> route = new ArrayList<>(selectRoute);
        Collections.shuffle(route, rand);
        Chromosome cTemp = new Chromosome(c1Routes, 0.0, false);
        EvaluationFunction.Evaluate(cTemp);
        for (int i = 0; i < route.size(); i++) {
            int patient = route.get(i);
            Patient p = allPatients[patient];
            double bestCost = Double.MAX_VALUE;
            int bestFirst = -1;
            int bestSecond = -1;
            int bestM = -1;
            int bestN = -1;
            cTemp.buildPatientRouteMap();
            Shift[] shifts = cTemp.getCaregiversRouteUp();
            boolean isInvalid = cTemp.getFitness() == Double.POSITIVE_INFINITY;
            Set<CaregiverPair> caregiverPairs = p.getAllPossibleCaregiverCombinationsCrossover();
            if (p.getRequired_caregivers().length > 1) {
                for (CaregiverPair caregiverPair : caregiverPairs) {
                    int first = caregiverPair.getFirst();
                    int second = caregiverPair.getSecond();
                    int firstSize = c1Routes[first].size();
                    int secondSize = c1Routes[second].size();
                    c1Routes[first].add(0, patient);
                    for (int m = 0; m <= firstSize; m++) {
                        if(m>0){
                            int otherPatient = c1Routes[first].get(m);
                            c1Routes[first].set(m, patient);
                            c1Routes[first].set(m-1, otherPatient);
                        }
                        c1Routes[second].add(0, patient);
                        for (int n = 0; n <= secondSize; n++) {
                            if(n>0){
                                int otherPatient = c1Routes[second].get(n);
                                c1Routes[second].set(n, patient);
                                c1Routes[second].set(n - 1, otherPatient);
                            }

                            if (noEvaluationConflicts(c1Routes[first], c1Routes[second], m, n)) {
                                double tempCost = calMoveCost(first, m, second, n, patient, cTemp, bestCost, shifts, isInvalid);
                                if (shouldReplaceBestCost(bestCost, tempCost)) {
                                    bestCost = tempCost;
                                    bestFirst = first;
                                    bestSecond = second;
                                    bestM = m;
                                    bestN = n;
                                }
                            }
                        }
                        c1Routes[second].remove(Integer.valueOf(patient));
                    }
                    c1Routes[first].remove(Integer.valueOf(patient));
                }
                if (bestCost != Double.MAX_VALUE) {
                    c1Routes = cTemp.getGenes();
                    c1Routes[bestFirst].add(bestM, patient);
                    c1Routes[bestSecond].add(bestN, patient);
                    EvaluationFunction.Evaluate(cTemp);
                } else {
                    if (!restorePatientFromParent(patient, cTemp, c1Routes)) {
                        cTemp.setFitness(Double.POSITIVE_INFINITY);
                        return cTemp;
                    }
                }
            } else {
                for (CaregiverPair caregiverPair : caregiverPairs) {
                    int first = caregiverPair.getFirst();
                    int firstSize = c1Routes[first].size();
                    c1Routes[first].add(0, patient);
                    for (int k = 0; k <= firstSize; k++) {
                        if(k>0){
                            int otherPatient = c1Routes[first].get(k);
                            c1Routes[first].set(k, patient);
                            c1Routes[first].set(k - 1, otherPatient);
                        }
                        double tempCost = calMoveCost(first,k,-1,-1,patient,cTemp,bestCost,shifts,isInvalid);
                        if (shouldReplaceBestCost(bestCost, tempCost)) {
                            bestCost = tempCost;
                            bestFirst = first;
                            bestM = k;
                        }
                    }
                    c1Routes[first].remove(Integer.valueOf(patient));
                }
                if (bestCost != Double.MAX_VALUE) {
                    c1Routes = cTemp.getGenes();
                    c1Routes[bestFirst].add(bestM, patient);
                    EvaluationFunction.Evaluate(cTemp);
                } else {
                    if (!restorePatientFromParent(patient, cTemp, c1Routes)) {
                        cTemp.setFitness(Double.POSITIVE_INFINITY);
                        return cTemp;
                    }
                }
            }
        }
        return cTemp;
    }

    private double calMoveCost(int first, int m, int second, int n, int patient, Chromosome cTemp, double bestCost, Shift[] shifts, boolean isInvalid) {
        Arrays.fill(routeEndPoint, -1);
        Arrays.fill(routesCurrentTime, 0.0);
        Arrays.fill(startingWaitingTime, 0.0);
        Arrays.fill(totalOvertime, 0.0);
        if (isInvalid) {
            List<Integer>[] c1Routes = cTemp.getGenes();
            Chromosome temp = new Chromosome(c1Routes, 0.0, false);
            EvaluationFunction.EvaluateFitness(temp);
            return temp.getFitness();
        }
        int size = 1;
        routeEndPoint[first] = m;
        if (second != -1) {
            size++;
            routeEndPoint[second] = n;
        }

        int[] routeMove = new int[size];
        int[] positionMove = new int[size];
        routeMove[0] = first;
        positionMove[0] = m;
        if (size > 1) {
            routeMove[1] = second;
            positionMove[1] = n;
        }

        removeAffectedPatient(patient,routeMove, positionMove, cTemp, routeEndPoint);

        double totalTravelCost = 0.0;
        double highestIdleTime = 0.0;
        Arrays.fill(highestAndTotalTardiness, 0);
        Arrays.fill(totalWaitingTime, 0);
        double overallOvertime = 0.0;
        for (int i = 0; i < routeEndPoint.length; i++) {
            Shift shift = shifts[i];
            List<Double> currentTime = shift.getCurrentTime();
            List<Double> travelCost = shift.getTravelCost();
            List<Double> tardiness = shift.getTardiness();
            List<Double> maxTardiness = shift.getMaxTardiness();
            List<Double> waitingTime = shift.getTotalWaitingTime();
            List<Double> overtime = shift.getOvertime();

            if(routeEndPoint[i] != 0){
                startingWaitingTime[i] = shift.getStartingWaitingTime();
            }

            if (routeEndPoint[i] != -1) {
                int index = routeEndPoint[i];
                routesCurrentTime[i] = currentTime.get(index);
                highestAndTotalTardiness[0] = Math.max(maxTardiness.get(index), highestAndTotalTardiness[0]);
                highestAndTotalTardiness[1] += tardiness.get(index);
                totalWaitingTime[i] = waitingTime.get(index);
            } else {
                highestAndTotalTardiness[0] = Math.max(maxTardiness.get(maxTardiness.size() - 1), highestAndTotalTardiness[0]);
                highestAndTotalTardiness[1] += tardiness.get(tardiness.size() - 1);
                totalWaitingTime[i] = waitingTime.get(tardiness.size() - 1);
                totalOvertime[i] = overtime.get(overtime.size() - 1);
                overallOvertime += overtime.get(overtime.size() - 1);
                highestIdleTime = Math.max(shift.getIdleTime(), highestIdleTime);
            }
            totalWaitingTime[numOfCaregivers] += totalWaitingTime[i];
            if (i == first || i == second) {
                int index = routeEndPoint[i];
                totalTravelCost += travelCost.get(index);
            } else {
                totalTravelCost += travelCost.get(travelCost.size() - 1);
            }
        }

        //Distance calculation
        List<Integer>[] genes = cTemp.getGenes();
        for (int i = 0; i < routeEndPoint.length; i++) {
            List<Integer> route = genes[i];
            int routeEnd = routeEndPoint[i];
            int routeStartPoint = allCaregivers[i].getDistance_matrix_index();
            if (i == first || i == second) {
                for (int j = routeEnd; j <= route.size(); j++) {
                    if (j == 0) {
                        int nextIndex = patientMatrixIndexes[route.get(j)];
                        totalTravelCost += distances[routeStartPoint][nextIndex];
                    } else if (j == route.size()) {
                        int prevIndex = patientMatrixIndexes[route.get(j-1)];
                        totalTravelCost += distances[prevIndex][routeStartPoint];
                    } else {
                        int nextIndex = patientMatrixIndexes[route.get(j)];
                        int prevIndex = patientMatrixIndexes[route.get(j-1)];
                        totalTravelCost += distances[prevIndex][nextIndex];
                    }

                }
            }
        }

        double solutionCost = totalTravelCost + highestAndTotalTardiness[0] + highestAndTotalTardiness[1] + totalWaitingTime[numOfCaregivers] + overallOvertime + highestIdleTime;
        if (solutionCost > bestCost) {
            return solutionCost;
        }

        track.clear();
        //Tardiness calculation
        for (int i = 0; i < routeEndPoint.length; i++) {
            List<Integer> route = genes[i];
            int routeEnd = routeEndPoint[i];
            if (routeEnd != -1) {
                int routeStartingPoint = allCaregivers[i].getDistance_matrix_index();
                for (int j = routeEnd; j < route.size(); j++) {
                    int current = j == 0 ? routeStartingPoint : patientMatrixIndexes[route.get(j - 1)];
                    solutionCost = patientIsAssigned(genes, i, current, route.get(j), totalTravelCost, routesCurrentTime, highestAndTotalTardiness, totalWaitingTime, startingWaitingTime, totalOvertime,routeEndPoint, track);
                    if (solutionCost == Double.POSITIVE_INFINITY || solutionCost > bestCost) {
                        return solutionCost;
                    }
                    track.clear();
                }
            }
        }

        for (int i = 0; i < routeEndPoint.length; i++) {
            List<Integer> route = genes[i];
            int routeEnd = routeEndPoint[i];
            if (routeEnd != -1) {
                int routeStartingPoint = allCaregivers[i].getDistance_matrix_index();
                int lastPatient = patientMatrixIndexes[route.get(route.size()-1)];
                double distance = distances[lastPatient][routeStartingPoint];
                routesCurrentTime[i] += distance;
                double caregiverClosingTime = allCaregivers[i].getWorking_shift()[1];
                overallOvertime += Math.max(0, (routesCurrentTime[i] - caregiverClosingTime));
                double routeIdleTime = totalWaitingTime[i] + Math.max(0, (caregiverClosingTime - routesCurrentTime[i])) +startingWaitingTime[i];
                highestIdleTime = Math.max(highestIdleTime, routeIdleTime);
            }
        }

        return totalTravelCost + highestAndTotalTardiness[0] + highestAndTotalTardiness[1] + totalWaitingTime[numOfCaregivers] + highestIdleTime + overallOvertime;
    }


    private boolean noEvaluationConflicts(List<Integer> c1Route, List<Integer> c2Route, int m, int n) {
        return conflictCheck(c1Route, c2Route, m, n);
    }

    private boolean shouldReplaceBestCost(double bestCost, double candidateCost) {
        if (Double.isFinite(candidateCost) && !Double.isFinite(bestCost)) {
            return true;
        }
        if (bestCost - candidateCost > 0.001) {
            return true;
        }
        return candidateCost <= bestCost && rand.nextBoolean();
    }

    private boolean restorePatientFromParent(
            int patient, Chromosome child, List<Integer>[] childRoutes) {
        boolean restored = false;
        List<Integer>[] sourceRoutes = p2.getGenes();
        for (int routeIndex = 0; routeIndex < sourceRoutes.length; routeIndex++) {
            if (sourceRoutes[routeIndex].contains(patient)) {
                childRoutes[routeIndex].add(patient);
                restored = true;
            }
        }
        if (restored) {
            EvaluationFunction.Evaluate(child);
        }
        return restored;
    }

    public static void removeAffectedPatient(int iPatient, int[] routeMove, int[] positionMove, Chromosome base, int[] routeEndPoint) {
        Map<Integer, Set<Integer>> patientToRoutesMap = base.getPatientToRoutesMap();
        List<Integer>[] genes = base.getGenes();
        for(int j = 0; j < routeMove.length; j++){
            int first = routeMove[j];
            int firstPosition = positionMove[j];
            List<Integer> currentRoute = genes[first];
            for (int i = firstPosition; i < currentRoute.size(); i++) {
                int patient = currentRoute.get(i);
                if(patient==iPatient){
                    continue;
                }
                Patient p = allPatients[patient];
                if (p.getRequired_caregivers().length > 1) {
                    int routeIndex = getRouteIndexMethod(first, patientToRoutesMap.get(patient));
                    int patientIndex = genes[routeIndex].indexOf(patient);
                    if (routeEndPoint[routeIndex] == -1 || routeEndPoint[routeIndex] > patientIndex) {
                        routeEndPoint[routeIndex] = patientIndex;
                        int[] newRouteMove = {routeIndex};
                        int[] newPositionMove = {patientIndex};
                        removeAffectedPatient(iPatient,newRouteMove,newPositionMove, base, routeEndPoint);
                    }
                }
            }
        }
    }

    private static int getRouteIndexMethod(int first, Set<Integer> routes) {
        if(routes==null) return -1; // Patient not found
        for(int route : routes){
            if(route != first){
                return route; // Return the first alternative route
            }
        }
        return -1; // other route not found
    }
}
