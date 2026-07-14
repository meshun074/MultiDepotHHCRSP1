package GeneticAlgorithm;

import Data.Patient;
import Main.Main;

import java.util.*;

public class SwapRouteMutation implements Runnable {
    @Override
    public void run() {
        ga.getMutationChromosomes().add(mutate());
    }
    public final GeneticAlgorithm ga;
    private final Random rand;
    private final Chromosome ch;
    private final List<Integer>[] chGenes;
    private static Patient[] allPatients = Main.instance.getPatients();

    public SwapRouteMutation(GeneticAlgorithm ga, Chromosome ch, Random rand) {
        this.ga = ga;
        this.rand =rand;
        this.ch = ch;
        this.chGenes = ch.getGenes();
    }

    public Chromosome mutate() {
        Chromosome mutant = new Chromosome( chGenes, ch.getFitness(), true);
        List<Integer>[] genes = mutant.getGenes();

        // Select two distinct non-empty routes
        int[] routeIndices = selectMutationRoutes(genes);
        int r1 = routeIndices[0];
        int r2 = routeIndices[1];

        //Perform the mutation
        List<Integer> route1 = genes[r1];
        List<Integer> route2 = genes[r2];
        List<Integer> newRoute1 = new ArrayList<>();
        List<Integer> newRoute2 = new ArrayList<>();
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
        int r1 = rand.nextInt(genes.length);
        int r2;
        do {
            r2 = rand.nextInt(genes.length);
        } while ((genes[r1].isEmpty() && genes[r2].isEmpty()) || r1 == r2);
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
                if(!allCaregivers.contains(r2)||genes[r2].contains(p)) continue;
                int otherIndex = getCaregiverIndex(r1,p,genes,allCaregivers);
                List<CaregiverPair> allCaregiverCombinations = patient.getAllPossibleCaregiverCombinations();
                for(int x=0;x<allCaregiverCombinations.size();x++){
                    CaregiverPair pair = allCaregiverCombinations.get(x);
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
        for(int i: allCaregivers){
            if(r1==i) continue;
            if(genes[i].contains(p)) return i;
        }
        return -1;
    }
}
