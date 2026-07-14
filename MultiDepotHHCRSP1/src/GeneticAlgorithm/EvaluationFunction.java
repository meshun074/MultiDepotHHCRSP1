package GeneticAlgorithm;

import Data.*;
import Main.Main;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EvaluationFunction {
    private static final InstancesClass dataset = Main.instance;
    private static final double[][] distances = dataset.getDistances();
    private static final Patient[] allPatients = dataset.getPatients();
    private static final Caregiver[] allCaregivers = dataset.getCaregivers();
    private static final int numOfDepartingPoints = dataset.getDeparting_points().length;
    private static final ThreadLocal<Set<Integer>> trackHolder = ThreadLocal.withInitial(() -> new HashSet<>(150));
    private final static ThreadLocal<double[]> routeStartTime = ThreadLocal.withInitial(() -> new double[allCaregivers.length]);

    public static void Evaluate(Chromosome ch) {
        Shift[] routes = ch.getCaregiversRouteUp();
        if (routes[0] == null) {
            ch.initializeCaregiversRoute();
        }
        for (int i = 0; i < routes.length; i++) {

            routes[i].resetShift();
        }
        ch.setCaregiversRouteUp(routes);
        ch.setHighestTardiness(0);
        ch.setTotalTardiness(0);
        ch.setTotalTravelCost(0);
        ch.setTotalWaitingTime(0.0);
        ch.setHighestIdleTime(0.0);
        ch.setOvertime(0.0);
        ch.setFitness(Double.POSITIVE_INFINITY);

        Set<Integer> track = trackHolder.get(); // Initial capacity for small patient sets
        List<Integer>[] genes = ch.getGenes();

        try {
            for (int i = 0; i < routes.length; i++) {
                List<Integer> route = genes[i];
                Shift caregiver1 = routes[i];

                for (int patient : route) {
                    if (!caregiver1.getRoute().contains(patient)) {
                        if (patientAssignment(ch, patient, caregiver1, routes, i, track)) {
                            ch.setFitness(Double.POSITIVE_INFINITY);
                            return;
                        }
                        track.clear();
                    }
                }
            }
        } finally {
            track.clear();
        }

        // Final cost calculations
        int departingPoints = dataset.getDeparting_points().length;
        for (Shift s : routes) {
            int depot = s.getCaregiver().getCacheStartingPoint();
            int lastLocationId = s.getRoute().isEmpty() ? depot : s.getRoute().get(s.getRoute().size() - 1) + departingPoints;
            double returnCost = distances[lastLocationId][depot];
            double[] workingShift = s.getCaregiver().getWorking_shift();
            double caregiverClosingTime = workingShift[1];
            double caregiverOpeningTime = workingShift[0];
            s.updateCurrentTime(returnCost);
            List<Double> routeTimeInformation = s.getCurrentTime();
            double routeCurrentTime = routeTimeInformation.get(routeTimeInformation.size() - 1);
            double overtime = Math.max(0, (routeCurrentTime - caregiverClosingTime));
            if (routeCurrentTime > caregiverOpeningTime) {
                double highestIdleTime = s.getTotalWaitingTime().get(s.getTotalWaitingTime().size() - 1) + Math.max(0, (caregiverClosingTime - routeCurrentTime)) + s.getStartingWaitingTime();
                s.setIdleTime(highestIdleTime);
                ch.updateHighestIdleTime(highestIdleTime);
            }
            ch.updateTotalTravelCost(returnCost);
            ch.updateOvertime(overtime);
            s.updateTravelCost(returnCost);
            s.addOvertime(overtime);
        }

        UpdateCost(ch);
    }


    public static boolean patientAssignment(Chromosome ch, int patient, Shift caregiver1, Shift[] routes, int i, Set<Integer> track) {
        Patient p = allPatients[patient];
        double[] timeWindow = p.getTime_window();
        int firstDepot = caregiver1.getCaregiver().getCacheStartingPoint();
        int currentLocation1 = caregiver1.getRoute().isEmpty() ? firstDepot : allPatients[caregiver1.getRoute().get(caregiver1.getRoute().size() - 1)].getDistance_matrix_index();
        int nextLocation = p.getDistance_matrix_index();
        double patientOpenTimeWindow = timeWindow[0];
        double patientCloseTimeWindow = timeWindow[1];
        double arrivalTime1 = caregiver1.getCurrentTime().get(caregiver1.getCurrentTime().size() - 1) + distances[currentLocation1][nextLocation];
        double startTime1 = Math.max(arrivalTime1, patientOpenTimeWindow);

        if (p.getRequired_caregivers().length > 1) {
            if (!track.add(patient)) { // Combined contains check and add
                return true;
            }

            int index = findSecondCaregiver(patient, i, routes, ch, track);
            if (index > allCaregivers.length - 1) {
                return true;
            }

            Shift caregiver2 = routes[index];
            Required_Caregiver[] requiredCaregivers = p.getRequired_caregivers();
            int secondDepot = caregiver2.getCaregiver().getCacheStartingPoint();

            // Check and potentially swap caregivers based on qualifications
            if (shouldSwapCaregivers(p, caregiver1, caregiver2)) {
                Shift temp = caregiver1;
                caregiver1 = caregiver2;
                caregiver2 = temp;

                int depot = firstDepot;
                firstDepot = secondDepot;
                secondDepot = depot;

                // Recalculate after swap
                currentLocation1 = caregiver1.getRoute().isEmpty() ? firstDepot : allPatients[caregiver1.getRoute().get(caregiver1.getRoute().size() - 1)].getDistance_matrix_index();
                arrivalTime1 = caregiver1.getCurrentTime().get(caregiver1.getCurrentTime().size() - 1) + distances[currentLocation1][nextLocation];
                startTime1 = Math.max(arrivalTime1, patientOpenTimeWindow);
            }

            int currentLocation2 = caregiver2.getRoute().isEmpty() ? secondDepot : allPatients[caregiver2.getRoute().get(caregiver2.getRoute().size() - 1)].getDistance_matrix_index();
            double arrivalTime2 = caregiver2.getCurrentTime().get(caregiver2.getCurrentTime().size() - 1) + distances[currentLocation2][nextLocation];
            double startTime2 = Math.max(arrivalTime2, patientOpenTimeWindow);

            processSynchronization(ch, p, caregiver1, caregiver2, requiredCaregivers,
                    startTime1, arrivalTime1, startTime2, arrivalTime2, patientOpenTimeWindow, patientCloseTimeWindow, distances[nextLocation][firstDepot], distances[nextLocation][secondDepot]);

            double travelCost = distances[currentLocation1][nextLocation] +
                    distances[currentLocation2][nextLocation];
            updateCaregiverRoutes(ch, caregiver1, caregiver2, patient,
                    distances[currentLocation1][nextLocation],
                    distances[currentLocation2][nextLocation],
                    travelCost);
        } else {
            double waitingTime;
            double startingWaitingTime = 0;
            if (caregiver1.getRoute().isEmpty()){
                waitingTime = 0;
                startingWaitingTime = Math.max(0, (startTime1 - arrivalTime1));;
            }else {
                waitingTime = Math.max(0, (startTime1 - arrivalTime1));
            }
            processSingleCaregiver(ch, caregiver1, p, patient, startTime1, patientCloseTimeWindow, waitingTime, startingWaitingTime,
                    distances[currentLocation1][nextLocation], distances[nextLocation][firstDepot]);
        }
        return false;
    }

    private static void updateCaregiverRoutes(Chromosome ch, Shift caregiver1,
                                              Shift caregiver2, int patientId, double cost1, double cost2, double totalCost) {
        ch.updateTotalTravelCost(totalCost);
        caregiver1.updateRoute(patientId);
        caregiver1.updateTravelCost(cost1);
        caregiver2.updateRoute(patientId);
        caregiver2.updateTravelCost(cost2);
    }

    private static void processSingleCaregiver(Chromosome ch, Shift caregiver,
                                               Patient p, int patient, double startTime, double timeWindowEnd, double waitingTime, double startingWatingTime, double travelCost, double finishingTravelCost) {
        double tardiness = Math.max(0, startTime - timeWindowEnd);

        double finishingTime = startTime + p.getRequired_caregivers()[0].getDuration() + finishingTravelCost;
        double overtime = Math.max(0, (finishingTime - caregiver.getCaregiver().getWorking_shift()[1]));

        ch.setHighestTardiness(Math.max(tardiness, ch.getHighestTardiness()));
        ch.updateTotalTardiness(tardiness);
        ch.updateTotalTravelCost(travelCost);
        ch.updateWaitingTime(waitingTime);

        caregiver.setCurrentTime(startTime + p.getRequired_caregivers()[0].getDuration());
        caregiver.updateStartingWaitingTime(startingWatingTime);
        caregiver.updateRoute(patient);
        caregiver.updateTravelCost(travelCost);
        caregiver.updateTardiness(tardiness);
        caregiver.updateWaitingTime(waitingTime);
        caregiver.addOvertime(overtime);
    }

    private static void processSynchronization(Chromosome ch, Patient p, Shift caregiver1,
                                               Shift caregiver2, Required_Caregiver[] requiredCaregivers, double startTime1, double arrivalTime1,
                                               double startTime2, double arrivalTime2, double timeWindowOpen, double timeWindowEnd, double finishingTravelCost1, double finishingTravelCost2) {

        double tardiness1, tardiness2, overtime1, overtime2;
        if (p.getSynchronization().getType().equals("sequential")) {
            double[] syncDistances = p.getSynchronization().getDistance();
            startTime2 = Math.max(startTime2, startTime1 + syncDistances[0]);
            if (startTime2 - startTime1 > syncDistances[1]) {
                startTime1 = startTime2 - syncDistances[1];
            }

            double finishingTime1 = startTime1 + p.getRequired_caregivers()[0].getDuration() + finishingTravelCost1;
            double finishingTime2 = startTime2 + p.getRequired_caregivers()[1].getDuration() + finishingTravelCost2;
            overtime1 = Math.max(0, (finishingTime1 - caregiver1.getCaregiver().getWorking_shift()[1]));
            overtime2 = Math.max(0, (finishingTime2 - caregiver2.getCaregiver().getWorking_shift()[1]));

            tardiness1 = Math.max(0, startTime1 - timeWindowEnd);
            tardiness2 = Math.max(0, startTime2 - timeWindowEnd);
        } else {
            double startTime = Math.max(startTime1, startTime2);
            tardiness1 = tardiness2 = Math.max(0, startTime - timeWindowEnd);
            startTime1 = startTime2 = startTime;


            double finishingTime1 = startTime + p.getRequired_caregivers()[0].getDuration() + finishingTravelCost1;
            double finishingTime2 = startTime + p.getRequired_caregivers()[1].getDuration() + finishingTravelCost2;
            overtime1 = Math.max(0, (finishingTime1 - caregiver1.getCaregiver().getWorking_shift()[1]));
            overtime2 = Math.max(0, (finishingTime2 - caregiver2.getCaregiver().getWorking_shift()[1]));
        }

        double waitingTime1;
        double startingWaitingTime1 = 0;
        if (caregiver1.getRoute().isEmpty()) {
            waitingTime1 = 0;
            startingWaitingTime1 = Math.max(0, startTime1 - arrivalTime1);
        }else {
            waitingTime1 = Math.max(0, startTime1 - arrivalTime1);
        }
        double waitingTime2;
        double startingWaitingTime2 =0;
        if(caregiver2.getRoute().isEmpty()){
            waitingTime2 = 0;
            startingWaitingTime2 = Math.max(0, startTime2 - arrivalTime2);
        }else{
            waitingTime2 = Math.max(0, startTime2 - arrivalTime2);
        }

        double maxTardiness = Math.max(tardiness1, tardiness2);
        ch.updateTotalTardiness(tardiness1 + tardiness2);
        ch.setHighestTardiness(Math.max(maxTardiness, ch.getHighestTardiness()));
        ch.updateWaitingTime(waitingTime1 + waitingTime2);

        caregiver1.setCurrentTime(startTime1 + requiredCaregivers[0].getDuration());
        caregiver1.updateStartingWaitingTime(startingWaitingTime1);
        caregiver1.updateTardiness(tardiness1);
        caregiver1.updateWaitingTime(waitingTime1);
        caregiver1.addOvertime(overtime1);

        caregiver2.setCurrentTime(startTime2 + requiredCaregivers[1].getDuration());
        caregiver2.updateStartingWaitingTime(startingWaitingTime2);
        caregiver2.updateTardiness(tardiness2);
        caregiver2.updateWaitingTime(waitingTime2);
        caregiver2.addOvertime(overtime2);
    }

    private static int findSecondCaregiver(int pIndex, int route1, Shift[] routes, Chromosome ch, Set<Integer> track) {
        List<Integer>[] genes = ch.getGenes();

        for (int i = 0; i < genes.length; i++) {
            if (i != route1 && genes[i].contains(pIndex)) {
                List<Integer> route = genes[i];
                Shift caregiver = routes[i];
                int patientPosition = route.indexOf(pIndex);

                // Process patients up to the target position
                int j = caregiver.getRoute().size();
                while (caregiver.getRoute().size() != patientPosition && j < route.size()) {
                    int patient = route.get(j);
                    if (patientAssignment(ch, patient, caregiver, routes, i, track))
                        return Integer.MAX_VALUE;
                    j++;
                }
                return i;
            }
        }
        return Integer.MAX_VALUE;
    }

    private static boolean shouldSwapCaregivers(Patient p, Shift caregiver1, Shift caregiver2) {
        Required_Caregiver[] requiredCaregivers = p.getRequired_caregivers();
        String service1 = requiredCaregivers[0].getService();
        String service2 = requiredCaregivers[1].getService();

        Set<Integer> service1Routes = dataset.getQualifiedCaregiver(service1);
        Set<Integer> service2Routes = dataset.getQualifiedCaregiver(service2);

        int caregiver2Id = caregiver2.getCaregiver().getCacheId();
        int caregiver1Id = caregiver1.getCaregiver().getCacheId();

        return (service1Routes.contains(caregiver2Id) && !service2Routes.contains(caregiver2Id)) ||
                (service2Routes.contains(caregiver1Id) && !service1Routes.contains(caregiver1Id)) ||
                (service1Routes.contains(caregiver2Id) && !service1Routes.contains(caregiver1Id));
    }

    static void UpdateCost(Chromosome ch) {
        double cost = ch.getTotalTravelCost() + ch.getTotalTardiness() + ch.getHighestTardiness() + ch.getTotalWaitingTime() + ch.getHighestIdleTime() + ch.getOvertime();
        ch.setFitness(cost);
    }

    public static void EvaluateFitness(List<Chromosome> population) {
        for (Chromosome chromosome : population) {
            EvaluateFitness(chromosome);
        }
    }
    public static void EvaluateFitness(Chromosome ch) {
        ch.setHighestTardiness(0);
        ch.setTotalTardiness(0);
        ch.setTotalTravelCost(0);
        ch.setTotalWaitingTime(0.0);
        ch.setHighestIdleTime(0.0);
        ch.setOvertime(0.0);
        ch.setFitness(Double.POSITIVE_INFINITY);
        double travelCost = 0;
        int caregiverLength = allCaregivers.length;
        double[] highestAndTotalTardiness = new double[2];
        double[] TotalWaitingTime = new double[caregiverLength + 1];
        double[] startingWaitingTime = new double[caregiverLength];
        int[] routeEndPoint = new int[caregiverLength];
        double[] overtime = new double[caregiverLength];
        double[] routesCurrentTime = routeStartTime.get();
        for (int i = 0; i < caregiverLength; i++) {
            routesCurrentTime[i] = allCaregivers[i].getWorking_shift()[0];
        }

        Set<Integer> track = trackHolder.get();
        List<Integer>[] genes = ch.getGenes();
        for (int i = 0; i < genes.length; i++) {
            List<Integer> route = genes[i];
            int routeStartingPoint = allCaregivers[i].getDistance_matrix_index();
            for (int j = 0; j < route.size(); j++) {
                if (j == 0) {
                    int nextIndex = route.get(j) + numOfDepartingPoints;
                    travelCost += distances[routeStartingPoint][nextIndex];
                    if (route.size() == 1) {
                        travelCost += distances[nextIndex][routeStartingPoint];
                    }
                } else if (j == route.size() - 1) {
                    int prevIndex = route.get(j - 1) + numOfDepartingPoints;
                    int nextIndex = route.get(j) + numOfDepartingPoints;
                    travelCost += distances[prevIndex][nextIndex];
                    travelCost += distances[nextIndex][routeStartingPoint];
                } else {
                    int prevIndex = route.get(j - 1) + numOfDepartingPoints;
                    int nextIndex = route.get(j) + numOfDepartingPoints;
                    travelCost += distances[prevIndex][nextIndex];
                }
            }
        }

        ch.setTotalTravelCost(travelCost);

        try {
            double solutionCost;
            for (int i = 0; i < routeEndPoint.length; i++) {
                List<Integer> route = genes[i];
                int routeEnd = routeEndPoint[i];
                if (routeEnd != -1) {
                    int routeStartingPoint = allCaregivers[i].getDistance_matrix_index();
                    for (int j = routeEnd; j < route.size(); j++) {
                        int current = j == 0 ? routeStartingPoint : route.get(j - 1)+numOfDepartingPoints;
                        solutionCost = patientIsAssigned(genes, i, current, route.get(j), travelCost, routesCurrentTime, highestAndTotalTardiness, TotalWaitingTime, startingWaitingTime, overtime, routeEndPoint, track);
                        if (solutionCost == Double.POSITIVE_INFINITY) {
                            ch.setFitness(Double.POSITIVE_INFINITY);
                            return;
                        }
                        track.clear();
                    }
                }
            }

            double overtimeCost = 0;
            double highestIdleTime = 0;
            for (int i = 0; i < routeEndPoint.length; i++) {
                List<Integer> route = genes[i];
                if (!route.isEmpty()) {
                    int routeStartingPoint = allCaregivers[i].getDistance_matrix_index();
                    int lastPatient = route.get(route.size()-1) + numOfDepartingPoints;
                    double distance = distances[lastPatient][routeStartingPoint];
                    routesCurrentTime[i] += distance;
                    double caregiverClosingTime = allCaregivers[i].getWorking_shift()[1];
                    overtimeCost += Math.max(0, (routesCurrentTime[i] - caregiverClosingTime));
                    double routeIdleTime = TotalWaitingTime[i] + Math.max(0, (caregiverClosingTime - routesCurrentTime[i])) + startingWaitingTime[i];
                    highestIdleTime = Math.max(highestIdleTime, routeIdleTime);
                }
            }

            ch.setHighestTardiness(highestAndTotalTardiness[0]);
            ch.setTotalTardiness(highestAndTotalTardiness[1]);
            ch.setTotalWaitingTime(TotalWaitingTime[caregiverLength]);
            ch.setOvertime(overtimeCost);
            ch.setHighestIdleTime(highestIdleTime);
            UpdateCost(ch);

        } finally {
            track.clear();
        }
    }

    public static double patientIsAssigned(List<Integer>[] genes, int route1, int curPatientIndex1, int nextPatientIndex, double totalTravelCost, double[] routesCurrentTime, double[] highestAndTotalTardiness, double[] totalWaitingTime, double[] startingWaitingTime, double[] overtime, int[] routeEndPoint, Set<Integer> track) {
        Patient nextPatient = allPatients[nextPatientIndex];
        Caregiver caregiver1 = allCaregivers[route1];
        double[] timeWindow = nextPatient.getTime_window();
        double patientOpenTimeWindow = timeWindow[0];
        int currentLocation1 = curPatientIndex1;
        int nextLocation = nextPatient.getDistance_matrix_index();

        double arrivalTime1 = routesCurrentTime[route1] + distances[currentLocation1][nextLocation];
        double startTime1 = Math.max(arrivalTime1, patientOpenTimeWindow);

        double highestIdleTime = 0;
        double totalOvertime = 0;
        if (nextPatient.getRequired_caregivers().length > 1) {
            if (!track.add(nextPatientIndex)) {
                return Double.POSITIVE_INFINITY;
            }

            int route2 = findOtherCaregiver(nextPatientIndex, route1, genes, totalTravelCost, routesCurrentTime, highestAndTotalTardiness, totalWaitingTime, startingWaitingTime, overtime, routeEndPoint, track);
            if (route2 > allCaregivers.length - 1) {
                return Double.POSITIVE_INFINITY;
            }

            Caregiver caregiver2 = allCaregivers[route2];
            int position2 = routeEndPoint[route2] - 1;
            int curPatient2;
            if (position2 == -1) {
                curPatient2 = caregiver2.getDistance_matrix_index();
            } else {
                curPatient2 = genes[route2].get(position2)+numOfDepartingPoints;
            }

            if (SwapRoutes(nextPatient, route1, route2)) {
                int temp = route1;
                route1 = route2;
                route2 = temp;

                temp = curPatientIndex1;
                curPatientIndex1 = curPatient2;
                curPatient2 = temp;

                Caregiver caregiver3 = caregiver1;
                caregiver1 = caregiver2;
                caregiver2 = caregiver3;

                currentLocation1 = curPatientIndex1;
                arrivalTime1 = routesCurrentTime[route1] + distances[currentLocation1][nextLocation];
                startTime1 = Math.max(arrivalTime1, patientOpenTimeWindow);
            }

            int currentLocation2 = curPatient2;
            double arrivalTime2 = routesCurrentTime[route2] + distances[currentLocation2][nextLocation];
            double startTime2 = Math.max(arrivalTime2, patientOpenTimeWindow);

            if (nextPatient.getSynchronization().getType().equals("sequential")) {
                double[] syncDistances = nextPatient.getSynchronization().getDistance();
                startTime2 = Math.max(startTime2, startTime1 + syncDistances[0]);
                if (startTime2 - startTime1 > syncDistances[1]) {
                    startTime1 = startTime2 - syncDistances[1];
                }
                double tardiness = Math.max(0, startTime1 - timeWindow[1]);
                highestAndTotalTardiness[0] = Math.max(highestAndTotalTardiness[0], tardiness);
                highestAndTotalTardiness[1] += tardiness;
                tardiness = Math.max(0, startTime2 - timeWindow[1]);
                highestAndTotalTardiness[0] = Math.max(highestAndTotalTardiness[0], tardiness);
                highestAndTotalTardiness[1] += tardiness;
            } else {
                double startTime = Math.max(startTime1, startTime2);
                double tardiness = Math.max(0, startTime - timeWindow[1]);
                highestAndTotalTardiness[0] = Math.max(highestAndTotalTardiness[0], tardiness);
                highestAndTotalTardiness[1] += (2 * tardiness);
                startTime1 = startTime2 = startTime;

            }

            double finishingTime1 = startTime1 + nextPatient.getRequired_caregivers()[0].getDuration();
            double finishingTime2 = startTime2 + nextPatient.getRequired_caregivers()[1].getDuration();
            overtime[route1] = Math.max(0, (finishingTime1 - caregiver1.getWorking_shift()[1]));
            overtime[route2] = Math.max(0, (finishingTime2 - caregiver2.getWorking_shift()[1]));

            double waitingTime1;
            if(currentLocation1 < numOfDepartingPoints){
                startingWaitingTime[route1] = Math.max(0, (startTime1 - arrivalTime1));
                waitingTime1 = 0;
            } else{
                waitingTime1 = Math.max(0, (startTime1 - arrivalTime1));
            }

            double waitingTime2;
            if(currentLocation2 < numOfDepartingPoints){
               startingWaitingTime[route2] = Math.max(0, (startTime2 - arrivalTime2));
               waitingTime2 = 0;
            } else{
                waitingTime2 = Math.max(0, (startTime2 - arrivalTime2));
            }

            totalWaitingTime[route1] += waitingTime1;
            totalWaitingTime[route2] += waitingTime2;
            totalWaitingTime[allCaregivers.length] += (waitingTime1 + waitingTime2);

            for (int i = 0; i < allCaregivers.length; i++) {
                highestIdleTime = Math.max(highestIdleTime, totalWaitingTime[i]);
                totalOvertime += overtime[i];
            }

            routeEndPoint[route1]++;
            routeEndPoint[route2]++;
            routesCurrentTime[route1] = startTime1 + nextPatient.getRequired_caregivers()[0].getDuration();
            routesCurrentTime[route2] = startTime2 + nextPatient.getRequired_caregivers()[1].getDuration();

        } else {
            double finishingTime = startTime1 + nextPatient.getRequired_caregivers()[0].getDuration();
            overtime[route1] = Math.max(0, (finishingTime - caregiver1.getWorking_shift()[1]));
            double waitingTime;
            if(currentLocation1 < numOfDepartingPoints){
                startingWaitingTime[route1] = Math.max(0, (startTime1 - arrivalTime1));
                waitingTime = 0;
            }else {
                waitingTime = Math.max(0, (startTime1 - arrivalTime1));
            }
            totalWaitingTime[route1] += waitingTime;
            totalWaitingTime[allCaregivers.length] += waitingTime;
            //Checks for the highest idleness among all caregivers
            for (int i = 0; i < allCaregivers.length; i++) {
                highestIdleTime = Math.max(highestIdleTime, totalWaitingTime[i]);
                totalOvertime += overtime[i];
            }

            double tardiness = Math.max(0, startTime1 - timeWindow[1]);
            highestAndTotalTardiness[0] = Math.max(highestAndTotalTardiness[0], tardiness);
            highestAndTotalTardiness[1] += tardiness;
            routeEndPoint[route1]++;
            routesCurrentTime[route1] = startTime1 + nextPatient.getRequired_caregivers()[0].getDuration();
        }
        return totalTravelCost + highestAndTotalTardiness[0] + highestAndTotalTardiness[1] + totalWaitingTime[allCaregivers.length] + highestIdleTime + totalOvertime;
    }

    private static int findOtherCaregiver(int patient, int route1, List<Integer>[] genes, double totalTravelCost, double[] routesCurrentTime, double[] highestAndTotalTardiness, double[] totalWaitingTime, double[] startingWaitingTime, double[] overtime, int[] routeEndPoint, Set<Integer> track) {
        for (int i = 0; i < genes.length; i++) {
            if (i != route1 && genes[i].contains(patient)) {
                List<Integer> route = genes[i];
                int patientPosition = route.indexOf(patient);

                int j = routeEndPoint[i];
                while (routeEndPoint[i] != patientPosition && j < route.size()) {
                    int curPatient;
                    if (j == 0)
                        curPatient = allCaregivers[i].getDistance_matrix_index();
                    else
                        curPatient = route.get(j - 1)+numOfDepartingPoints;
                    if (patientIsAssigned(genes, i, curPatient, route.get(j), totalTravelCost, routesCurrentTime, highestAndTotalTardiness, totalWaitingTime, startingWaitingTime, overtime, routeEndPoint, track) == Double.POSITIVE_INFINITY)
                        return Integer.MAX_VALUE;
                    j++;
                }
                return i;
            }
        }
        return Integer.MAX_VALUE;
    }

    private static boolean SwapRoutes(Patient p, int route1, int route2) {
        Required_Caregiver[] requiredCaregivers = p.getRequired_caregivers();
        String service1 = requiredCaregivers[0].getService();
        String service2 = requiredCaregivers[1].getService();

        Set<Integer> service1Routes = dataset.getQualifiedCaregiver(service1);
        Set<Integer> service2Routes = dataset.getQualifiedCaregiver(service2);

        return (service1Routes.contains(route2) && !service2Routes.contains(route2)) ||
                (service2Routes.contains(route1) && !service1Routes.contains(route1)) ||
                (service1Routes.contains(route2) && !service1Routes.contains(route1));
    }
}