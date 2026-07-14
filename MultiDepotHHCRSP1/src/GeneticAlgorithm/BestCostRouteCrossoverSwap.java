package GeneticAlgorithm;

import Data.Caregiver;
import Data.InstancesClass;
import Data.Patient;
import Main.Main;

import java.util.*;

import static GeneticAlgorithm.EvaluationFunction.patientIsAssigned;
import static GeneticAlgorithm.GeneticAlgorithm.conflictCheck;

public class BestCostRouteCrossoverSwap implements Runnable {
    private final GeneticAlgorithm ga;
    private final int r;
    private final Chromosome p1, p2;
    private static InstancesClass dataset = Main.instance;
    private static Patient[] allPatients = dataset.getPatients();
    private static final Caregiver[] allCaregivers = dataset.getCaregivers();
    private static int numOfCaregivers = dataset.getCaregivers().length;
    private final static int numOfDepartingPoints = dataset.getDeparting_points().length;
    private static double[][] distances = dataset.getDistances();
    private final Random rand;
    private final int[] routeEndPoint;
    private final double[] routesCurrentTime;
    private final double[] highestAndTotalTardiness;
    private final double[] totalWaitingTime;
    private final double[] startingWaitingTime;
    private final double[] totalOvertime;
    private final Set<Integer> track;

    public BestCostRouteCrossoverSwap(GeneticAlgorithm ga, int r, Chromosome p1, Chromosome p2, Random rand) {
        this.ga = ga;
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

    @Override
    public void run() {
        ga.getCrossoverChromosomes().add(Crossover());

    }

    public Chromosome Crossover() {
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
        Collections.shuffle(route);
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
                        if (m > 0) {
                            int otherPatient = c1Routes[first].get(m);
                            c1Routes[first].set(m, patient);
                            c1Routes[first].set(m - 1, otherPatient);
                        }
                        c1Routes[second].add(0, patient);
                        for (int n = 0; n <= secondSize; n++) {
                            if (n > 0) {
                                int otherPatient = c1Routes[second].get(n);
                                c1Routes[second].set(n, patient);
                                c1Routes[second].set(n - 1, otherPatient);
                            }

                            if (noEvaluationConflicts(c1Routes[first], c1Routes[second], m, n)) {
                                double tempCost = calMoveCost(first, m, second, n, patient, cTemp, bestCost, shifts, isInvalid);
                                if (bestCost == Double.MAX_VALUE || bestCost - tempCost > 0.001 && bestCost != Double.POSITIVE_INFINITY && tempCost != Double.POSITIVE_INFINITY || tempCost <= bestCost && rand.nextBoolean()) {
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
                    isInvalid = cTemp.getFitness() == Double.POSITIVE_INFINITY;
                    cTemp = swap(cTemp, isInvalid, patient, bestFirst, bestSecond, bestM, bestN);
                } else {
                    System.out.println("no route found");
                }
            } else {
                for (CaregiverPair caregiverPair : caregiverPairs) {
                    int first = caregiverPair.getFirst();
                    int firstSize = c1Routes[first].size();
                    c1Routes[first].add(0, patient);
                    for (int k = 0; k <= firstSize; k++) {
                        if (k > 0) {
                            int otherPatient = c1Routes[first].get(k);
                            c1Routes[first].set(k, patient);
                            c1Routes[first].set(k - 1, otherPatient);
                        }
                        double tempCost = calMoveCost(first, k, -1, -1, patient, cTemp, bestCost, shifts, isInvalid);
                        if (bestCost == Double.MAX_VALUE || bestCost - tempCost > 0.001 && bestCost != Double.POSITIVE_INFINITY && tempCost != Double.POSITIVE_INFINITY || tempCost <= bestCost && rand.nextBoolean()
                        ) {
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
                    isInvalid = cTemp.getFitness() == Double.POSITIVE_INFINITY;
                    cTemp = swap(cTemp, isInvalid, patient, bestFirst, -1, bestM, -1);
                } else {
                    System.out.println("no route found " + caregiverPairs.size());
                }
            }
        }
        return cTemp;
    }

    private void applyMove(int first, int m, int second, int n, int patient, Chromosome cTemp, double bestCost, Shift[] shifts, boolean isInvalid) {
        Arrays.fill(routeEndPoint, -1);
        if (isInvalid) {
            EvaluationFunction.Evaluate(cTemp);
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

        removeAffectedPatient(patient, routeMove, positionMove, cTemp, routeEndPoint);
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

            if (routeEndPoint[i] != -1) {
                int index = routeEndPoint[i];
                routesCurrentTime[i] = currentTime.get(index);
                highestAndTotalTardiness[0] = Math.max(maxTardiness.get(index), highestAndTotalTardiness[0]);
                highestAndTotalTardiness[1] += tardiness.get(index);
                totalWaitingTime[i] = waitingTime.get(index);
                index++;
                currentTime.subList(index, currentTime.size()).clear();
                tardiness.subList(index, tardiness.size()).clear();
                maxTardiness.subList(index, maxTardiness.size()).clear();
                waitingTime.subList(index, waitingTime.size()).clear();
                overtime.subList(index, overtime.size()).clear();
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
                index++;
                travelCost.subList(index, travelCost.size()).clear();
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
                        int nextIndex = route.get(j) + numOfDepartingPoints;
                        double distance = distances[routeStartPoint][nextIndex];
                        totalTravelCost += distance;
                        shifts[i].updateTravelCost(distance);
                    } else if (j == route.size()) {
                        int prevIndex = route.get(j - 1) + numOfDepartingPoints;
                        double distance = distances[prevIndex][routeStartPoint];
                        totalTravelCost += distance;
                        shifts[i].updateTravelCost(distance);
                    } else {
                        int nextIndex = route.get(j) + numOfDepartingPoints;
                        int prevIndex = route.get(j - 1) + numOfDepartingPoints;
                        double distance = distances[prevIndex][nextIndex];
                        totalTravelCost += distance;
                        shifts[i].updateTravelCost(distance);
                    }

                }
            }
        }

    }


    private double calMoveCost(int first, int m, int second, int n, int patient, Chromosome cTemp, double bestCost, Shift[] shifts, boolean isInvalid) {
        Arrays.fill(routeEndPoint, -1);
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

        removeAffectedPatient(patient, routeMove, positionMove, cTemp, routeEndPoint);

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
                        int nextIndex = route.get(j) + numOfDepartingPoints;
                        totalTravelCost += distances[routeStartPoint][nextIndex];
                    } else if (j == route.size()) {
                        int prevIndex = route.get(j - 1) + numOfDepartingPoints;
                        totalTravelCost += distances[prevIndex][routeStartPoint];
                    } else {
                        int nextIndex = route.get(j) + numOfDepartingPoints;
                        int prevIndex = route.get(j - 1) + numOfDepartingPoints;
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
                    int current = j == 0 ? routeStartingPoint : route.get(j - 1) + numOfDepartingPoints;
                    solutionCost = patientIsAssigned(genes, i, current, route.get(j), totalTravelCost, routesCurrentTime, highestAndTotalTardiness, totalWaitingTime, startingWaitingTime, totalOvertime, routeEndPoint, track);
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
                int lastPatient = route.get(route.size() - 1) + numOfDepartingPoints;
                double distance = distances[lastPatient][routeStartingPoint];
                routesCurrentTime[i] += distance;
                double caregiverClosingTime = allCaregivers[i].getWorking_shift()[1];
                overallOvertime += Math.max(0, (routesCurrentTime[i] - caregiverClosingTime));
                double routeIdleTime = totalWaitingTime[i] + Math.max(0, (caregiverClosingTime - routesCurrentTime[i])) + startingWaitingTime[i];
                highestIdleTime = Math.max(highestIdleTime, routeIdleTime);
            }
        }

        return totalTravelCost + highestAndTotalTardiness[0] + highestAndTotalTardiness[1] + totalWaitingTime[numOfCaregivers] + highestIdleTime + overallOvertime;
    }


    private boolean noEvaluationConflicts(List<Integer> c1Route, List<Integer> c2Route, int m, int n) {
        return conflictCheck(c1Route, c2Route, m, n);
    }

    public static void removeAffectedPatient(int iPatient, int[] routeMove, int[] positionMove, Chromosome base, int[] routeEndPoint) {
        Map<Integer, Set<Integer>> patientToRoutesMap = base.getPatientToRoutesMap();
        List<Integer>[] genes = base.getGenes();
        for (int j = 0; j < routeMove.length; j++) {
            int first = routeMove[j];
            int firstPosition = positionMove[j];
            List<Integer> currentRoute = genes[first];
            for (int i = firstPosition; i < currentRoute.size(); i++) {
                int patient = currentRoute.get(i);
                if (patient == iPatient) {
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
                        removeAffectedPatient(iPatient, newRouteMove, newPositionMove, base, routeEndPoint);
                    }
                }
            }
        }
    }

    public static void removeAffectedPatientSwap(int iPatient, int[] routeMove, int[] positionMove, Chromosome base, int[] routeEndPoint) {
        Map<Integer, Set<Integer>> patientToRoutesMap = base.getPatientToRoutesMap();
        List<Integer>[] genes = base.getGenes();
        for (int j = 0; j < routeMove.length; j++) {
            int first = routeMove[j];
            int firstPosition = positionMove[j];
            List<Integer> currentRoute = genes[first];
            for (int i = firstPosition; i < currentRoute.size(); i++) {
                int patient = currentRoute.get(i);
//                if (patient == iPatient) {
//                    continue;
//                }
                Patient p = allPatients[patient];
                if (p.getRequired_caregivers().length > 1) {
                    int routeIndex = getRouteIndexMethod(first, patientToRoutesMap.get(patient));
                    int patientIndex = genes[routeIndex].indexOf(patient);
                    if (routeEndPoint[routeIndex] == -1 || routeEndPoint[routeIndex] > patientIndex) {
                        routeEndPoint[routeIndex] = patientIndex;
                        int[] newRouteMove = {routeIndex};
                        int[] newPositionMove = {patientIndex};
                        removeAffectedPatientSwap(iPatient, newRouteMove, newPositionMove, base, routeEndPoint);
                    }
                }
            }
        }
    }

    private static int getRouteIndexMethod(int first, Set<Integer> routes) {
        if (routes == null) return -1; // Patient not found
        for (int route : routes) {
            if (route != first) {
                return route; // Return the first alternative route
            }
        }
        return -1; // other route not found
    }

    private Chromosome swap(Chromosome ch, boolean isInvalid, int patient, int first, int second, int firstPosition, int secondPosition) {
        ch.buildPatientRouteMap();
        double bestCost = Double.MAX_VALUE;
        double currentCost = ch.getFitness();
        List<Integer>[] routes = ch.getGenes();
        Shift[] shifts = ch.getCaregiversRouteUp();

        if (second != -1) {
            List<Integer> route1 = routes[first];
            List<Integer> route2 = routes[second];
            int bestZ = -1;
            int bestFirstPatient = -1;
            int bestSecondPatient = -1;
            int bestL = -1;
            for (int z = 0; z < route1.size(); z++) {
                if (Math.abs(z - firstPosition) > 1) {
                    int p3 = route1.get(z);
//                    System.out.println("Yaa");
                    double tempCost = calSwapMoveCost(first, firstPosition, -1, -1, patient, p3, z, -1, -1, ch, bestCost, shifts, isInvalid);
                    if (bestCost == Double.MAX_VALUE || bestCost - tempCost > 0.001 && bestCost != Double.POSITIVE_INFINITY && tempCost != Double.POSITIVE_INFINITY || tempCost <= bestCost && rand.nextBoolean()) {
                        bestCost = tempCost;
                        bestZ = z;
                        bestFirstPatient = p3;
                    }
                }
            }

            for (int l = 0; l < route2.size(); l++) {
                if (Math.abs(l - secondPosition) > 1) {
                    int p4 = route2.get(l);
//                    System.out.println("Naa");
                    double tempCost = calSwapMoveCost(-1, -1, second, secondPosition, patient, -1, -1, p4, l, ch, bestCost, shifts, isInvalid);
                    if (bestCost == Double.MAX_VALUE || bestCost - tempCost > 0.001 && bestCost != Double.POSITIVE_INFINITY && tempCost != Double.POSITIVE_INFINITY || tempCost <= bestCost && rand.nextBoolean()) {
                        bestCost = tempCost;
                        bestL = l;
                        bestSecondPatient = p4;
                        bestZ = -1;
                        bestFirstPatient = -1;
                    }
                }
            }
            for (int z = 0; z < route1.size(); z++) {
                if (Math.abs(z - firstPosition) > 1) {
                    for (int l = 0; l < route2.size(); l++) {
                        if (Math.abs(l - secondPosition) > 1) {
                            int p3 = route1.get(z);
                            int p4 = route2.get(l);
//                            System.out.println("Maa");
                            double tempCost = calSwapMoveCost(first, firstPosition, second, secondPosition, patient, p3, z, p4, l, ch, bestCost, shifts, isInvalid);
                            if (bestCost == Double.MAX_VALUE || bestCost - tempCost > 0.001 && bestCost != Double.POSITIVE_INFINITY && tempCost != Double.POSITIVE_INFINITY || tempCost <= bestCost && rand.nextBoolean()) {
                                bestCost = tempCost;
                                bestL = l;
                                bestSecondPatient = p4;
                                bestZ = z;
                                bestFirstPatient = p3;
                            }
                        }
                    }
                }
            }

            if (bestCost != Double.MAX_VALUE && currentCost - bestCost > 0.001 || bestCost <= currentCost && bestCost != Double.MAX_VALUE && rand.nextBoolean()) {
                if (bestZ != -1) {
                    routes[first].set(firstPosition, bestFirstPatient);
                    routes[first].set(bestZ, patient);
                }
                if (bestL != -1) {
                    routes[second].set(secondPosition, bestSecondPatient);
                    routes[second].set(bestL, patient);
                }
                EvaluationFunction.Evaluate(ch);
            }
        } else {
            List<Integer> route1 = routes[first];
            int bestI = -1;
            int bestPatient = -1;
            for (int i = 0; i < route1.size(); i++) {
                if (Math.abs(i - firstPosition) > 1) {
                    int p2 = route1.get(i);
//                    System.out.println("Laa");
                    double tempCost = calSwapMoveCost(first, firstPosition, -1, -1, patient, p2, i, -1, -1, ch, bestCost, shifts, isInvalid);
                    if (bestCost == Double.MAX_VALUE || bestCost - tempCost > 0.001 && bestCost != Double.POSITIVE_INFINITY && tempCost != Double.POSITIVE_INFINITY || tempCost <= bestCost && rand.nextBoolean()) {
                        bestCost = tempCost;
                        bestI = i;
                        bestPatient = p2;
                    }
                }
            }

            if (bestCost != Double.MAX_VALUE && currentCost - bestCost > 0.001 || bestCost <= currentCost && bestCost != Double.MAX_VALUE && rand.nextBoolean()) {
                routes[first].set(firstPosition, bestPatient);
                routes[first].set(bestI, patient);
                EvaluationFunction.Evaluate(ch);
            }
        }
        return ch;
    }

    private double calSwapMoveCost(int first, int m, int second, int n, int patient, int patient1, int patient1Position, int patient2, int patient2Position, Chromosome cTemp, double bestCost, Shift[] shifts, boolean isInvalid) {
        Arrays.fill(routeEndPoint, -1);
        if (isInvalid) {
            List<Integer>[] c1Routes = cTemp.getGenes();
            if (patient1 != -1) {
                c1Routes[first].set(m, patient1);
                c1Routes[first].set(patient1Position, patient);
            }
            if (patient2 != -1) {
                c1Routes[second].set(n, patient2);
                c1Routes[second].set(patient2Position, patient);
            }
            Chromosome temp = new Chromosome(c1Routes, 0.0, false);
            EvaluationFunction.EvaluateFitness(temp);
            if (patient1 != -1) {
                c1Routes[first].set(m, patient);
                c1Routes[first].set(patient1Position, patient1);
            }
            if (patient2 != -1) {
                c1Routes[second].set(n, patient);
                c1Routes[second].set(patient2Position, patient2);
            }
            return temp.getFitness();
        }
        int size = 0;
        int newFirstPosition = Math.min(m, patient1Position);
        if (patient1 != -1) {
            routeEndPoint[first] = newFirstPosition;
            size++;
        }
        int newSecondPosition = Math.min(n, patient2Position);
        if (patient2 != -1) {
            routeEndPoint[second] = newSecondPosition;
            size++;
        }

        int[] routeMove = new int[size];
        int[] positionMove = new int[size];
        int counter = 0;
        if (patient1 != -1) {
            routeMove[counter] = first;
            positionMove[counter] = newFirstPosition;
            counter++;
        }
        if (patient2 != -1) {
            routeMove[counter] = second;
            positionMove[counter] = newSecondPosition;
        }
//        for (int i = 0; i < routeEndPoint.length; i++) {
//            System.out.println(i + " " + routeEndPoint[i]);
//        }
        removeAffectedPatientSwap(patient, routeMove, positionMove, cTemp, routeEndPoint);


//        for (List<Integer> c1Route : cTemp.getGenes()) {
//            System.out.println(c1Route);
//        }
//        System.out.println("Patient 1: "+patient1);
//        System.out.println("Patient 2: "+patient2);
//        for (int i = 0; i < routeMove.length; i++) {
//            System.out.println("Patient: "+patient+" Route: "+routeMove[i]+" Position: "+positionMove[i]);
//        }
//
//        for (int i = 0; i < routeEndPoint.length; i++) {
//            System.out.println(i + " " + routeEndPoint[i]);
//        }
//
//        System.exit(1);

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
                startingWaitingTime[i] =shift.getStartingWaitingTime();
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
        if(patient1 != -1){
            genes[first].set(m, patient1);
            genes[first].set(patient1Position, patient);
        }
        if(patient2 != -1){
            genes[second].set(n, patient2);
            genes[second].set(patient2Position, patient);
        }

        for (int i = 0; i < routeEndPoint.length; i++) {
            List<Integer> route = genes[i];
            int routeEnd = routeEndPoint[i];
            int routeStartPoint = allCaregivers[i].getDistance_matrix_index();
            if (i == first || i == second) {
                for (int j = routeEnd; j <= route.size(); j++) {
                    if (j == 0) {
                        int nextIndex = route.get(j) + numOfDepartingPoints;
                        totalTravelCost += distances[routeStartPoint][nextIndex];
                    } else if (j == route.size()) {
                        int prevIndex = route.get(j - 1) + numOfDepartingPoints;
                        totalTravelCost += distances[prevIndex][routeStartPoint];
                    } else {
                        int nextIndex = route.get(j) + numOfDepartingPoints;
                        int prevIndex = route.get(j - 1) + numOfDepartingPoints;
                        totalTravelCost += distances[prevIndex][nextIndex];
                    }

                }
            }
        }

        double solutionCost = totalTravelCost + highestAndTotalTardiness[0] + highestAndTotalTardiness[1] + totalWaitingTime[numOfCaregivers] + overallOvertime + highestIdleTime;
        if (solutionCost > bestCost) {
            if(patient1 != -1){
                genes[first].set(m, patient);
                genes[first].set(patient1Position, patient1);
            }
            if(patient2 != -1){
                genes[second].set(n, patient);
                genes[second].set(patient2Position, patient2);
            }
            return solutionCost;
        }

//        for (List<Integer> c1Route : cTemp.getGenes()) {
//            System.out.println(c1Route);
//        }
//        System.exit(1);

        track.clear();
        //Tardiness calculation
        for (int i = 0; i < routeEndPoint.length; i++) {
            List<Integer> route = genes[i];
            int routeEnd = routeEndPoint[i];
            if (routeEnd != -1) {
                int routeStartingPoint = allCaregivers[i].getDistance_matrix_index();
                for (int j = routeEnd; j < route.size(); j++) {
                    int current = j == 0 ? routeStartingPoint : route.get(j - 1) + numOfDepartingPoints;
                    solutionCost = patientIsAssigned(genes, i, current, route.get(j), totalTravelCost, routesCurrentTime, highestAndTotalTardiness, totalWaitingTime, startingWaitingTime, totalOvertime, routeEndPoint, track);
                    if (solutionCost == Double.POSITIVE_INFINITY || solutionCost > bestCost) {
                        if(patient1 != -1){
                            genes[first].set(m, patient);
                            genes[first].set(patient1Position, patient1);
                        }
                        if(patient2 != -1){
                            genes[second].set(n, patient);
                            genes[second].set(patient2Position, patient2);
                        }
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
                int lastPatient = route.get(route.size() - 1) + numOfDepartingPoints;
                double distance = distances[lastPatient][routeStartingPoint];
                routesCurrentTime[i] += distance;
                double caregiverClosingTime = allCaregivers[i].getWorking_shift()[1];
                overallOvertime += Math.max(0, (routesCurrentTime[i] - caregiverClosingTime));
                double routeIdleTime = totalWaitingTime[i] + Math.max(0, (caregiverClosingTime - routesCurrentTime[i])) + startingWaitingTime[i];
                highestIdleTime = Math.max(highestIdleTime, routeIdleTime);
            }
        }
        if(patient1 != -1){
            genes[first].set(m, patient);
            genes[first].set(patient1Position, patient1);
        }
        if(patient2 != -1){
            genes[second].set(n, patient);
            genes[second].set(patient2Position, patient2);
        }

        return totalTravelCost + highestAndTotalTardiness[0] + highestAndTotalTardiness[1] + totalWaitingTime[numOfCaregivers] + highestIdleTime + overallOvertime;
    }
}