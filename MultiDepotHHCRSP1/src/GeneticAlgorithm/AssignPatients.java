package GeneticAlgorithm;

import Data.Caregiver;
import Data.InstancesClass;
import Data.Patient;
import Main.Main;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class AssignPatients {
    static InstancesClass instances = Main.instance;
    private final static Patient[] allPatients = instances.getPatients();
    private final static Caregiver[] caregivers = instances.getCaregivers();
    private final static double[][] distanceMatrix = instances.getDistances();
    private final static ThreadLocal<double[]> routeStartTime = ThreadLocal.withInitial(() -> new double[caregivers.length]);

    public static void evaluate(List<RouteInitializer> population) {
//        population.parallelStream().forEach( solution -> {
//            double[] routesCurrentTime = routeStartTime.get();
//            for (int i = 0; i < routesCurrentTime.length; i++) {
//                routesCurrentTime[i] = caregivers[i].getWorking_shift()[0];
//            }
//            evaluateChromosome(solution, routesCurrentTime);
//        });

        for (RouteInitializer solution : population) {
            double[] routesCurrentTime = routeStartTime.get();
            for (int i = 0; i < routesCurrentTime.length; i++) {
                routesCurrentTime[i] = caregivers[i].getWorking_shift()[0];
            }
            evaluateChromosome(solution, routesCurrentTime);
        }
    }

    private static void evaluateChromosome(RouteInitializer ch, double[] routesCurrentTime) {
        List<Integer> patients = ch.getAlleles();
        List<Integer>[] routes = ch.getCaregiversRoute();
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        boolean convHull = rand.nextBoolean();
        boolean wLoadHeuristic = rand.nextBoolean();

        ch.setTotalTravelCost(0.0);
        ch.setSolutionCost(Double.MAX_VALUE);
        ch.setHighestTardiness(0.0);
        ch.setTotalTardiness(0.0);
        ch.setWaitingTime(0.0);
        ch.setOvertime(0.0);
        ch.setHighestIdleTime(0.0);
        int caregiverLength = caregivers.length;
        double[] highestAndTotalTardiness = new double[2];
        double[] highestIdleTimeAndWaitingTime = new double[caregiverLength + 1];
        double[] startingWaitingTime = new double[caregiverLength];
        int[] routeEndPoint = new int[caregiverLength];
        double[] workload = new double[caregiverLength];
        Arrays.fill(routeEndPoint, -1);

        for (int i = 0; i < patients.size(); i++) {
            double currentCost = Double.POSITIVE_INFINITY;
            double travelCost = ch.getTotalTravelCost();
            CaregiverPair bestCP = null;
            int patientIndex = patients.get(i);
            Patient patient = allPatients[patientIndex];

            List<CaregiverPair> possibleCaregivers = patient.getAllPossibleCaregiverCombinations();

            for (CaregiverPair pair : possibleCaregivers) {
                double newCost = findInsertionCost(pair, travelCost, highestAndTotalTardiness, highestIdleTimeAndWaitingTime, routeEndPoint, routesCurrentTime, patient, convHull, caregiverLength);
                if (wLoadHeuristic) {
                    newCost += workload[pair.getFirst()];
                    if (patient.getRequired_caregivers().length > 1)
                        newCost += workload[pair.getSecond()];
                }
                if (newCost < currentCost || rand.nextBoolean() && newCost == currentCost) {
                    currentCost = newCost;
                    bestCP = pair;
                }
            }
            if (bestCP != null) {
                updateRoutes(ch, routes, highestAndTotalTardiness, highestIdleTimeAndWaitingTime, startingWaitingTime, routeEndPoint, routesCurrentTime, bestCP, patient, patientIndex, convHull, caregiverLength);
                workload[bestCP.getFirst()] += patient.getRequired_caregivers()[0].getDuration();

                if (bestCP.getSecond() != -1) {
                    workload[bestCP.getSecond()] += patient.getRequired_caregivers()[1].getDuration();
                }
            }
        }

        int route = 0;
        for (int routeEnd : routeEndPoint) {
            if (routeEnd != -1) {
                int depot = caregivers[route].getCacheStartingPoint();
                double distance = distanceMatrix[routeEnd][depot];
                if (!convHull) {
                    ch.updateTotalTravelCost(distance);
                }
                routesCurrentTime[route] += distance;
            }
            route++;
        }
        double overtime = 0;
        double highestIdleTime = 0;
        for (int i = 0; i < routesCurrentTime.length; i++) {
            double caregiverClosingTime = caregivers[i].getWorking_shift()[1];
            overtime += Math.max(0, (routesCurrentTime[i] - caregiverClosingTime));
            if (routesCurrentTime[i] > 0) {
                double routeIdleTime = highestIdleTimeAndWaitingTime[i] + Math.max(0, (caregiverClosingTime - routesCurrentTime[i])) + startingWaitingTime[i];
                highestIdleTime = Math.max(highestIdleTime, routeIdleTime);
            }
        }
        ch.setHighestTardiness(highestAndTotalTardiness[0]);
        ch.setTotalTardiness(highestAndTotalTardiness[1]);
        ch.setHighestIdleTime(highestIdleTime);
        ch.setWaitingTime(highestIdleTimeAndWaitingTime[caregiverLength]);
        ch.setOvertime(overtime);
        updateFitness(ch);
    }

    private static double findInsertionCost(CaregiverPair cp, double travelCost, double[] highestAndTotalTardiness, double[] highestIdleTimeAndWaitingTime, int[] routeEndPoint, double[] routesCurrentTime, Patient patient, boolean convHull, int waitingTimeIndex) {
        int c1 = cp.getFirst();
        Caregiver caregiver1 = caregivers[c1];
        int firstDepot = caregiver1.getCacheStartingPoint();
        int currentLocation1 = routeEndPoint[c1] == -1 ? firstDepot : routeEndPoint[c1];
        int nextLocation = patient.getDistance_matrix_index();
        double arrivalTime1 = routesCurrentTime[c1] + distanceMatrix[currentLocation1][nextLocation];
        double patientOpenTimeWindow = patient.getTime_window()[0];
        double patientCloseTimeWindow = patient.getTime_window()[1];
        double startTime1 = Math.max(arrivalTime1, patientOpenTimeWindow);
        if (patient.getRequired_caregivers().length > 1) {
            int c2 = cp.getSecond();
            Caregiver caregiver2 = caregivers[c2];
            int secondDepot = caregiver2.getCacheStartingPoint();
            int currentLocation2 = routeEndPoint[c2] == -1 ? secondDepot : routeEndPoint[c2];
            double arrivalTime2 = routesCurrentTime[c2] + distanceMatrix[currentLocation2][nextLocation];
            double startTime2 = Math.max(arrivalTime2, patientOpenTimeWindow);
            travelCost += distanceMatrix[currentLocation1][nextLocation] + distanceMatrix[currentLocation2][nextLocation];
            double tardiness, maxTardiness;
            if (convHull) {
                travelCost += (distanceMatrix[nextLocation][firstDepot] + distanceMatrix[nextLocation][secondDepot] - distanceMatrix[currentLocation1][firstDepot] - distanceMatrix[currentLocation2][secondDepot]);
            }
            double overtime;
            if (patient.getSynchronization().getType().equals("simultaneous")) {
                double startTime = Math.max(startTime1, startTime2);
                double finishingTime1 = startTime + patient.getRequired_caregivers()[0].getDuration() + distanceMatrix[nextLocation][firstDepot];
                double finishingTime2 = startTime + patient.getRequired_caregivers()[1].getDuration() + distanceMatrix[nextLocation][secondDepot];
                double overtime1 = Math.max(0, (finishingTime1 - caregiver1.getWorking_shift()[1]));
                double overtime2 = Math.max(0, (finishingTime2 - caregiver2.getWorking_shift()[1]));
                overtime = overtime1 + overtime2;
                tardiness = startTime - patientCloseTimeWindow;
                tardiness = Math.max(tardiness, 0);
                maxTardiness = Math.max(tardiness, highestAndTotalTardiness[0]);
                tardiness *= 2;
                tardiness += highestAndTotalTardiness[1];
                startTime1 = startTime2 = startTime;
            } else {
                startTime2 = Math.max(startTime2, startTime1 + patient.getSynchronization().getDistance()[0]);
                if (startTime2 - startTime1 > patient.getSynchronization().getDistance()[1]) {
                    startTime1 = startTime2 - patient.getSynchronization().getDistance()[1];
                }
                double finishingTime1 = startTime1 + patient.getRequired_caregivers()[0].getDuration() + distanceMatrix[nextLocation][firstDepot];
                double finishingTime2 = startTime2 + patient.getRequired_caregivers()[1].getDuration() + distanceMatrix[nextLocation][secondDepot];
                double overtime1 = Math.max(0, (finishingTime1 - caregiver1.getWorking_shift()[1]));
                double overtime2 = Math.max(0, (finishingTime2 - caregiver2.getWorking_shift()[1]));
                overtime = overtime1 + overtime2;
                double tardiness1 = Math.max(0, startTime1 - patientCloseTimeWindow);
                double tardiness2 = Math.max(0, startTime2 - patientCloseTimeWindow);
                maxTardiness = Math.max(tardiness1, tardiness2);
                maxTardiness = Math.max(maxTardiness, highestAndTotalTardiness[0]);
                tardiness = tardiness1 + tardiness2;
                tardiness += highestAndTotalTardiness[1];
            }

            double waitingTime1, startingWaitingTime1 = 0;
            if(routeEndPoint[c1] == -1){
                startingWaitingTime1 = Math.max(0, (startTime1 - arrivalTime1));
                waitingTime1 = 0;
            }else{
                waitingTime1 = Math.max(0, (startTime1 - arrivalTime1));
            }
            double waitingTime2, startingWaitingTime2 = 0;
            if(routeEndPoint[c2] == -1){
                startingWaitingTime2 = Math.max(0, (startTime2 - arrivalTime2));
                waitingTime2 = 0;
            } else{
                waitingTime2 = Math.max(0, (startTime2 - arrivalTime2));
            }

            double waitingTime = waitingTime1 + waitingTime2 + highestIdleTimeAndWaitingTime[waitingTimeIndex];
            double highestIdleTime = 0;
            for (int i = 0; i < waitingTimeIndex; i++) {
                if (i == c1) {
                    highestIdleTime = Math.max(highestIdleTime, (waitingTime1 + highestIdleTimeAndWaitingTime[i] + startingWaitingTime1));
                } else if (i == c2) {
                    highestIdleTime = Math.max(highestIdleTime, (waitingTime1 + highestIdleTimeAndWaitingTime[i] + startingWaitingTime2));
                } else {
                    highestIdleTime = Math.max(highestIdleTime, highestIdleTimeAndWaitingTime[i]);
                }
            }
            return travelCost + tardiness + maxTardiness + waitingTime + highestIdleTime + overtime;
        } else {
            double waitingTime, startingWaitingTime = 0;
            if(routeEndPoint[c1] == -1){
                startingWaitingTime = Math.max(0, (startTime1 - arrivalTime1));
                waitingTime = 0;
            }else {
                waitingTime = Math.max(0, (startTime1 - arrivalTime1));
            }
            double highestIdleTime = 0;
            //Checks for the highest idleness among all caregivers
            for (int i = 0; i < waitingTimeIndex; i++) {
                if (i == c1) {
                    highestIdleTime = Math.max(highestIdleTime, (waitingTime + highestIdleTimeAndWaitingTime[i] + startingWaitingTime));
                } else {
                    highestIdleTime = Math.max(highestIdleTime, highestIdleTimeAndWaitingTime[i]);
                }
            }
            double finishingTime = startTime1 + patient.getRequired_caregivers()[0].getDuration() + distanceMatrix[nextLocation][firstDepot];
            double overtime = Math.max(0, (finishingTime - caregiver1.getWorking_shift()[1]));
            //calculate the total waitingTime
            waitingTime += highestIdleTimeAndWaitingTime[waitingTimeIndex];

            double tardiness = startTime1 - patientCloseTimeWindow;
            tardiness = Math.max(0, tardiness);
            double maxTardiness = Math.max(tardiness, highestAndTotalTardiness[0]);
            tardiness += highestAndTotalTardiness[1];
            travelCost += distanceMatrix[currentLocation1][nextLocation];
            if (convHull) {
                travelCost += (distanceMatrix[nextLocation][0] - distanceMatrix[currentLocation1][0]);
            }
            return travelCost + tardiness + maxTardiness + waitingTime + highestIdleTime + overtime;
        }
    }

    private static void updateRoutes(RouteInitializer ch, List<Integer>[] routes, double[] highestAndTotalTardiness, double[] highestIdleTimeAndWaitingTime, double[] startingWaitingTime, int[] routeEndPoint, double[] routesCurrentTime, CaregiverPair cp, Patient p, int patientIndex, boolean convHull, int waitingTimeIndex) {
        int c1 = cp.getFirst();
        Caregiver caregiver1 = caregivers[c1];
        int firstDepot = caregiver1.getCacheStartingPoint();
        int currentLocation1 = routeEndPoint[c1] == -1 ? firstDepot : routeEndPoint[c1];
        int nextLocation = p.getDistance_matrix_index();
        double arrivalTime1 = routesCurrentTime[c1] + distanceMatrix[currentLocation1][nextLocation];
        double patientOpenTimeWindow = p.getTime_window()[0];
        double patientCloseTimeWindow = p.getTime_window()[1];
        double startTime1 = Math.max(arrivalTime1, patientOpenTimeWindow);

        if (p.getRequired_caregivers().length > 1) {
            int c2 = cp.getSecond();
            int secondDepot = caregivers[c2].getCacheStartingPoint();
            int currentLocation2 = routeEndPoint[c2] == -1 ? secondDepot : routeEndPoint[c2];
            double arrivalTime2 = routesCurrentTime[c2] + distanceMatrix[currentLocation2][nextLocation];
            double startTime2 = Math.max(arrivalTime2, patientOpenTimeWindow);
            if (p.getSynchronization().getType().equals("simultaneous")) {
                double startTime = Math.max(startTime1, startTime2);
                double tardiness = startTime - patientCloseTimeWindow;
                tardiness = 2 * Math.max(tardiness, 0);
                double highestTardiness = Math.max(tardiness / 2, highestAndTotalTardiness[0]);
                highestAndTotalTardiness[0] = highestTardiness;
                highestAndTotalTardiness[1] += tardiness;
                routesCurrentTime[c1] = startTime + p.getRequired_caregivers()[0].getDuration();
                routesCurrentTime[c2] = startTime + p.getRequired_caregivers()[1].getDuration();

                startTime1 = startTime2 = startTime;
            } else {
                startTime2 = Math.max(startTime2, startTime1 + p.getSynchronization().getDistance()[0]);
                if (startTime2 - startTime1 > p.getSynchronization().getDistance()[1])
                    startTime1 = startTime2 - p.getSynchronization().getDistance()[1];
                double tardiness1 = Math.max(0, startTime1 - patientCloseTimeWindow);
                double tardiness2 = Math.max(0, startTime2 - patientCloseTimeWindow);
                highestAndTotalTardiness[1] += (tardiness1 + tardiness2);
                double maxTardiness = Math.max(tardiness1, tardiness2);
                maxTardiness = Math.max(maxTardiness, highestAndTotalTardiness[0]);
                highestAndTotalTardiness[0] = maxTardiness;
                routesCurrentTime[c1] = startTime1 + p.getRequired_caregivers()[0].getDuration();
                routesCurrentTime[c2] = startTime2 + p.getRequired_caregivers()[1].getDuration();

            }
            double waitingTime1;
            if(routeEndPoint[c1] == -1){
                startingWaitingTime[c1] = Math.max(0, (startTime1 - arrivalTime1));
                waitingTime1 = 0;
            }else{
                waitingTime1 = Math.max(0, (startTime1 - arrivalTime1));
            }
            double waitingTime2;
            if(routeEndPoint[c2] == -1){
                startingWaitingTime[c2] = Math.max(0, (startTime2 - arrivalTime2));
                waitingTime2 = 0;
            } else{
                waitingTime2 = Math.max(0, (startTime2 - arrivalTime2));
            }

            highestIdleTimeAndWaitingTime[c1] += waitingTime1;
            highestIdleTimeAndWaitingTime[c2] += waitingTime2;
            highestIdleTimeAndWaitingTime[waitingTimeIndex] += (waitingTime1 + waitingTime2);

            double travelCost = distanceMatrix[currentLocation1][nextLocation] + distanceMatrix[currentLocation2][nextLocation];
            if (convHull) {
                travelCost += (distanceMatrix[nextLocation][firstDepot] + distanceMatrix[nextLocation][secondDepot] - distanceMatrix[currentLocation1][firstDepot] - distanceMatrix[currentLocation2][secondDepot]);
            }
            ch.updateTotalTravelCost(travelCost);
            routeEndPoint[c1] = nextLocation;
            routeEndPoint[c2] = nextLocation;
            routes[c1].add(patientIndex);
            routes[c2].add(patientIndex);
        } else {
            double waitingTime;
            if(routeEndPoint[c1] == -1){
                startingWaitingTime[c1] = Math.max(0, (startTime1 - arrivalTime1));
                waitingTime = 0;
            }else {
                waitingTime = Math.max(0, (startTime1 - arrivalTime1));
            }
            highestIdleTimeAndWaitingTime[c1] += waitingTime;
            highestIdleTimeAndWaitingTime[waitingTimeIndex] += waitingTime;
            double tardiness = startTime1 - patientCloseTimeWindow;
            tardiness = Math.max(0, tardiness);
            double maxTardiness = Math.max(tardiness, highestAndTotalTardiness[0]);
            highestAndTotalTardiness[0] = maxTardiness;
            highestAndTotalTardiness[1] += tardiness;
            double travelCost = distanceMatrix[currentLocation1][nextLocation];

            if (convHull) {
                travelCost += (distanceMatrix[nextLocation][firstDepot] - distanceMatrix[currentLocation1][firstDepot]);
            }
            ch.updateTotalTravelCost(travelCost);
            routesCurrentTime[c1] = startTime1 + p.getRequired_caregivers()[0].getDuration();
            routeEndPoint[c1] = nextLocation;
            routes[c1].add(patientIndex);
        }
    }

    private static void updateFitness(RouteInitializer ch) {
        double fitness = ch.getTotalTravelCost() + ch.getTotalTardiness() + ch.getHighestTardiness() + ch.getWaitingTime() + ch.getOvertime() + ch.getHighestIdleTime();
        ch.setSolutionCost(fitness);
    }
}

