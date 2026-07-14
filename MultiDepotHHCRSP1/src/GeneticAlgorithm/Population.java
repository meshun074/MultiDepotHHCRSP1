package GeneticAlgorithm;

import Data.Caregiver;
import Data.InstancesClass;
import Data.Patient;
import Main.Main;

import java.util.*;

public class Population {

    public static List<Chromosome> initialize(int popSize, int chLength) {
        List<Integer> patients= new ArrayList<>();
        List<Integer> patientOrder;
        for (int s = 0; s < chLength; s++) {
            patients.add(s);
        }
        InstancesClass instances = Main.instance;
        List<RouteInitializer> tempPopulation = new ArrayList<>(popSize);
        int caregiverNumber = instances.getCaregivers().length;
        for (int i = 0; i < popSize; i++) {
            patientOrder = new ArrayList<>(patients);
            Collections.shuffle(patientOrder);
            RouteInitializer ri = new RouteInitializer(patientOrder,caregiverNumber,0.0);
            tempPopulation.add(ri);
        }
        AssignPatients.evaluate(tempPopulation);

        List<Chromosome> population = new ArrayList<>(popSize);
        for (RouteInitializer ri : tempPopulation) {
            Chromosome ch = new Chromosome(ri.getCaregiversRoute(),ri.getSolutionCost(),false);
            ch.setTotalTravelCost(ri.getTotalTravelCost());
            ch.setTotalTardiness(ri.getTotalTardiness());
            ch.setHighestTardiness(ri.getHighestTardiness());
            ch.setTotalWaitingTime(ri.getWaitingTime());
            ch.setHighestIdleTime(ri.getHighestIdleTime());
            ch.setOvertime(ri.getOvertime());
            population.add(ch);
        }
        return population;
    }
    public static List<Chromosome> clusteredInitialization1(int popSize, int chLength) {
        List<Chromosome> population = new ArrayList<>(popSize);
        InstancesClass instances = Main.instance;
        Caregiver[] allCaregivers = instances.getCaregivers();
        int caregiverNumber = allCaregivers.length;
        double[][] distanceMatrix = instances.getDistances();
        Patient[] patients = instances.getPatients();
        List<Integer>[] clusterPatient = new ArrayList[caregiverNumber];
        for (int i = 0; i < caregiverNumber; i++) {
            clusterPatient[i] = new ArrayList<>();
        }
        for (int s = 0; s < chLength; s++) {
            Patient patient = patients[s];
            CaregiverPair caregiverPair = getClosestCaregiver(patient, allCaregivers, distanceMatrix);
            clusterPatient[caregiverPair.getFirst()].add(s);
            if(patient.getRequired_caregivers().length > 1){
                clusterPatient[caregiverPair.getSecond()].add(s);
            }
        }
        for (int i = 0; i < popSize; i++){
            List<Integer>[] genes = new ArrayList[caregiverNumber];
            for(int j = 0; j < caregiverNumber; j++){
                List<Integer> route = clusterPatient[j];
                Collections.shuffle(route);
                genes[j] = new ArrayList<>(route);
            }
            population.add(new Chromosome(genes,true));
        }

        EvaluationFunction.EvaluateFitness(population);
        return population;
    }

    public static List<Chromosome> clusteredInitialization2(int popSize, int chLength) {
        List<Chromosome> population = new ArrayList<>(popSize);
        InstancesClass instances = Main.instance;
        Caregiver[] allCaregivers = instances.getCaregivers();
        int caregiverNumber = allCaregivers.length;
        double[][] distanceMatrix = instances.getDistances();
        Patient[] patients = instances.getPatients();
        CaregiverPair[] clusterPatient = new CaregiverPair[chLength];
        List<Integer> patientList= new ArrayList<>();
        List<Integer> patientOrder;
        for (int s = 0; s < chLength; s++) {
            patientList.add(s);
        }
        for (int s = 0; s < chLength; s++) {
            Patient patient = patients[s];
            clusterPatient[s] = getClosestCaregiver(patient, allCaregivers, distanceMatrix);;
        }

        for (int i = 0; i < popSize; i++) {
            patientOrder = new ArrayList<>(patientList);
            Collections.shuffle(patientOrder);
            List<Integer>[] genes = new ArrayList[caregiverNumber];
            for(int j = 0; j < caregiverNumber; j++){
                genes[j] = new ArrayList<>();
            }
            for(int p : patientOrder){
                CaregiverPair caregiverPair = clusterPatient[p];
                genes[caregiverPair.getFirst()].add(p);
                if(caregiverPair.getSecond()!=-1){
                    genes[caregiverPair.getSecond()].add(p);
                }
            }
            population.add(new Chromosome(genes,false));
        }

        EvaluationFunction.EvaluateFitness(population);
        return population;
    }

    public static List<Chromosome> clusteredInitialization(int popSize, int chLength) {
        List<Chromosome> population = new ArrayList<>(popSize);
        int first = (int) (0.1*popSize);
        int second = popSize - first;
        population.addAll(clusteredInitialization1(second, chLength));
        population.addAll(clusteredInitialization2(first, chLength));
        Collections.shuffle(population);
        return population;
    }

    private static CaregiverPair getClosestCaregiver(Patient patient, Caregiver[] allCaregivers, double[][] distanceMatrix) {
        double bestCaregiverDistance = Double.MAX_VALUE;
        CaregiverPair bestCaregiverPair = null;
        for(CaregiverPair caregiverPair : patient.getAllPossibleCaregiverCombinations()){
            double caregiverDistance = distanceMatrix[allCaregivers[caregiverPair.getFirst()].getDistance_matrix_index()][patient.getDistance_matrix_index()];
            if(patient.getRequired_caregivers().length > 1){
                caregiverDistance += distanceMatrix[allCaregivers[caregiverPair.getSecond()].getDistance_matrix_index()][patient.getDistance_matrix_index()];
            }
            if(caregiverDistance < bestCaregiverDistance || caregiverDistance == bestCaregiverDistance && Math.random() > 0.5){
                bestCaregiverDistance = caregiverDistance;
                bestCaregiverPair = caregiverPair;
            }
        }
        return bestCaregiverPair;
    }
}

