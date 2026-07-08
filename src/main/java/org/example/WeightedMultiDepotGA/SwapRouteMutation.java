package org.example.WeightedMultiDepotGA;

import org.example.Data.CaregiverPair;
import org.example.Data.InstancesClass;
import org.example.Data.Patient;

import java.util.*;

public class SwapRouteMutation {
    private final Random rand;
    private final Chromosome ch;
    private final List<Integer>[] chGenes;
    private final Map<Integer, Set<Integer>> patientRoutes;
    private static Patient[] allPatients;

    public SwapRouteMutation(Chromosome ch, Random rand) {
        this.rand =rand;
        this.ch = ch;
        this.chGenes = ch.getGenes();
        this.patientRoutes = buildPatientRouteMap(chGenes);
    }

    public static void initialize(InstancesClass instances) {
        allPatients = instances.getPatients();
    }

    public Chromosome mutate() {
        Chromosome mutant = new Chromosome( chGenes, ch.getFitness(), true);
        List<Integer>[] genes = mutant.getGenes();

        // Select two distinct non-empty routes
        int[] routeIndices = selectMutationRoutes(genes);
        if (routeIndices == null) {
            return mutant;
        }
        int r1 = routeIndices[0];
        int r2 = routeIndices[1];

        //Perform the mutation
        List<Integer> route1 = genes[r1];
        List<Integer> route2 = genes[r2];
        List<Integer> newRoute1 = new ArrayList<>(route1.size() + route2.size());
        List<Integer> newRoute2 = new ArrayList<>(route1.size() + route2.size());
        int[] mutable1 = newRoute(route1, r1, r2);
        int[] mutable2 = newRoute(route2, r2, r1);
        for (int i = 0; i < mutable1.length; i++) {
            if(mutable1[i]==-1)
                newRoute1.add(route1.get(i));
            if(i<mutable2.length&&mutable2[i]!=-1)
                newRoute1.add(mutable2[i]);
        }
        for(int i = mutable1.length; i<mutable2.length; i++){
            if(mutable2[i]!=-1){
                newRoute1.add(mutable2[i]);
            }
        }
        for(int i=0; i< mutable2.length; i++){
            if(mutable2[i]==-1){
                newRoute2.add(route2.get(i));
            }
            if(i<mutable1.length&&mutable1[i]!=-1){
                newRoute2.add(mutable1[i]);
            }
        }
        for(int i = mutable2.length; i<mutable1.length; i++){
            if(mutable1[i]!=-1){
                newRoute2.add(mutable1[i]);
            }
        }
        genes[r1] = newRoute1;
        genes[r2] = newRoute2;
        mutant.setGenes(genes);
        EvaluationFunction.EvaluateFitness(mutant);
        return mutant;
    }

    private int[] selectMutationRoutes(List<Integer>[] genes) {
        if (genes.length < 2) {
            return null;
        }
        List<Integer> nonEmptyRoutes = new ArrayList<>();
        for (int i = 0; i < genes.length; i++) {
            if (!genes[i].isEmpty()) {
                nonEmptyRoutes.add(i);
            }
        }
        if (nonEmptyRoutes.isEmpty()) {
            return null;
        }
        int r1 = nonEmptyRoutes.get(rand.nextInt(nonEmptyRoutes.size()));
        int r2 = rand.nextInt(genes.length - 1);
        if (r2 >= r1) {
            r2++;
        }
        return new int[]{r1, r2};
    }

    private int[] newRoute(List<Integer> route1, int r1, int r2) {
        int[] newRoute = new int[route1.size()];
        Arrays.fill(newRoute, -1);
        List<Integer>[]genes = chGenes;
        for (int i = 0; i < route1.size(); i++) {
            int p = route1.get(i);
            Patient patient = allPatients[p];
            if(patient.getRequired_caregivers().length>1){
                Set<Integer> allCaregivers = patient.getAllCaregiversForDoubleService();
                Set<Integer> assignedRoutes = patientRoutes.get(p);
                if(!allCaregivers.contains(r2)
                        || assignedRoutes != null && assignedRoutes.contains(r2)) continue;
                int otherIndex = getCaregiverIndex(r1,p,genes,allCaregivers);
                List<CaregiverPair> allCaregiverCombinations = patient.getAllPossibleCaregiverCombinations();
                for (CaregiverPair pair : allCaregiverCombinations) {
                    if(pair.getFirst()==r2&&pair.getSecond()==otherIndex || pair.getFirst()==otherIndex&&pair.getSecond()==r2){
                        newRoute[i] = p;
                        break;
                    }
                }
            }else {
                if(patient.getPossibleFirstCaregiver().contains(r2))
                    newRoute[i] = p;
            }
        }
        return newRoute;
    }
    private int getCaregiverIndex(int r1, int p, List<Integer>[]genes, Set<Integer> allCaregivers) {
        Set<Integer> assignedRoutes = patientRoutes.get(p);
        if (assignedRoutes == null) {
            return -1;
        }
        for(int i: allCaregivers){
            if(r1==i) continue;
            if(assignedRoutes.contains(i)) return i;
        }
        return -1;
    }

    private static Map<Integer, Set<Integer>> buildPatientRouteMap(List<Integer>[] genes) {
        Map<Integer, Set<Integer>> routesByPatient = new HashMap<>();
        for (int routeIndex = 0; routeIndex < genes.length; routeIndex++) {
            for (int patient : genes[routeIndex]) {
                routesByPatient
                        .computeIfAbsent(patient, ignored -> new HashSet<>(2))
                        .add(routeIndex);
            }
        }
        return routesByPatient;
    }
}
