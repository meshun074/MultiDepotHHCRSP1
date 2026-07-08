package org.example.ParetoMultiDepotGA;

import com.sun.management.OperatingSystemMXBean;
import org.example.Data.InstancesClass;
import org.example.WeightedMultiDepotGA.Configuration;

import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.*;

public class GeneticAlgorithm {
    private final int popSize;
    private final int gen;
    private final int TSRate;
    private final int time;
    private final String selectTechnique;
    private final String crossType;
    private final String mutType;
    private final float mutRate;
    private final double elitismRate;
    private final int elitismSize;
    private final double crossRate;
    private final int crossSize;
    private final int mutSize;
    private final InstancesClass data;
    private Chromosome bestChromosome;
    private final List<Chromosome> nextPopulation;
    private final List<Chromosome> tempPopulation;
    private final List<Chromosome> tempMutPopulation;
    private final List<Chromosome> previousPopulation;
    private List<Chromosome> newPopulation;
    private Map<Integer, Chromosome> LSChromosomes;
    private int terminator = 0;
    private final int patientLength;
    private final int caregiversNum;
    private final Random rand;
    private long startCpuTime;
    private long startTime;
    private final OperatingSystemMXBean osBean;
    private final ExecutorService workerPool;
    private static final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();

    public GeneticAlgorithm(Configuration config, long seed) {
        this.osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        this.data = config.instance();
        rand = new Random(seed);
        TSRate = config.TSRate();
        time = config.time();
        popSize = config.populationSize();
        gen = config.generations();
        selectTechnique = config.selectionMethod();
        mutType = config.mutationMethod();
        mutRate = config.mutationRate();
        mutSize = (int) (mutRate * popSize);
        crossType = config.crossoverMethod();
        crossRate = config.crossoverRate();
        crossSize = (int)(popSize*crossRate);
        elitismRate = config.elitismRate();
        elitismSize = (int) (elitismRate * popSize);
        patientLength = data.getPatients().length;
        caregiversNum = data.getCaregivers().length;
        EvaluationFunction.initialize(data);
        BestCostRouteCrossover.initialize(data);
        SwapRouteMutation.initialize(data);
        nextPopulation = new ArrayList<>(popSize);
        tempPopulation = new ArrayList<>(popSize);
        tempMutPopulation = new ArrayList<>(mutSize);
        previousPopulation = new ArrayList<>(popSize);
        workerPool = Executors.newFixedThreadPool(Math.max(1,
                Math.min(THREAD_POOL_SIZE, popSize)));
    }

    public Chromosome evolve(){
        try {
            System.out.printf("Population Size: %d, Generation: %d, SelectionType: %s TSRate: %d, Crossover type: %s\n CrossRate: %f, EliteRate: %f, MutType: %s Mutation Rate: %f\n", popSize, gen,  selectTechnique, TSRate, crossType, crossRate, elitismRate, mutType, mutRate);
            bestChromosome = null;
            startTimer();
            System.out.println("Initializing Population ...");
            newPopulation = Population.clusteredInitialization(popSize, patientLength,data);
            System.out.printf("Population initialized: CPU Timer(s): %s Timer(s): %s\n", getTotalCPUTimeSeconds(), getTotalTimeSeconds());
            sortPopulation(newPopulation);

            performanceUpdate(0);

            for (int g = 1; g <= gen; g++) {
                elitismByRank();
                crossover();
                if (mutSize > 0) {
                    mutation();
                }
                update();
                performanceUpdate(g);
                if( getTotalTimeSeconds()>=time)
                    break;

            }
            paretoScheme(newPopulation);
            sortPopulationByRank(newPopulation);

            System.out.println("Pareto Front  solutions: ");
            List<Double> fitness = new ArrayList<>();
            List<Double> travelCost = new ArrayList<>();
            List<Double> tardiness = new ArrayList<>();
            List<Double> highest = new ArrayList<>();
            List<Double> waitingTime = new ArrayList<>();
            List<Double> overtime = new ArrayList<>();
            List<Double> highestIdleTime = new ArrayList<>();
            for (int i = 0; i < newPopulation.size(); i++) {
                Chromosome ch = newPopulation.get(i);
                if (ch.getRank() == 1) {
                    boolean print  = true;
                    for(int j = 0; j < fitness.size(); j++){
                        if(fitness.get(j)==ch.getFitness() && travelCost.get(j) == ch.getTotalTravelCost() && tardiness.get(j) == ch.getTotalTardiness() &&
                                highest.get(j) == ch.getHighestTardiness() && waitingTime.get(j) == ch.getTotalWaitingTime() && overtime.get(j) == ch.getOvertime() && highestIdleTime.get(j) == ch.getHighestIdleTime()) {
                            print = false;
                            break;
                        }
                    }
                    if(print) {
                        System.out.println("Rank: "+ch.getRank()+" Fitness: "+ch.getFitness() + " Travel: " + ch.getTotalTravelCost() + " Tardiness: " + ch.getTotalTardiness() + " HighestT: " +
                                ch.getHighestTardiness()+ " WaitingTime " + ch.getTotalWaitingTime() + " Overtime " + ch.getOvertime() + " Highest Idle Time " + ch.getHighestIdleTime() + " \n" + ch);
                        fitness.add(ch.getFitness());
                        travelCost.add(ch.getTotalTravelCost());
                        tardiness.add(ch.getTotalTardiness());
                        highest.add(ch.getHighestTardiness());
                        waitingTime.add(ch.getTotalWaitingTime());
                        overtime.add(ch.getOvertime());
                        highestIdleTime.add(ch.getHighestIdleTime());
                    }
                } else
                    break;
            }

            System.out.println("End:------------");
            sortPopulation(newPopulation);

            if(newPopulation.get(0).getCaregiversRouteUp()[0] == null)
                EvaluationFunction.Evaluate(newPopulation.get(0));
            return newPopulation.get(0);
        } finally {
            workerPool.shutdown();
        }
    }

    private void elitismByRank(){
        nextPopulation.clear();
        paretoScheme(newPopulation);
        sortPopulationByRank(newPopulation);
        for (int i = 0; i < elitismSize; i++) {
            nextPopulation.add(newPopulation.get(i));
        }
    }

    private void paretoScheme(List<Chromosome> population) {

        final int size = population.size();

        // Domination structures
        List<List<Integer>> dominates = new ArrayList<>(size);
        int[] dominationCount = new int[size];
        List<List<Integer>> fronts = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            dominates.add(new ArrayList<>());
            dominationCount[i] = 0;
            population.get(i).setRank(-1);
        }

        // Step 1: Compute domination relationships
        for (int i = 0; i < size; i++) {
            Chromosome p = population.get(i);
            for (int j = i + 1; j < size; j++) {
                Chromosome q = population.get(j);
                if (dominates(p, q)) {
                    dominates.get(i).add(j);
                    dominationCount[j]++;
                } else if (dominates(q, p)) {
                    dominates.get(j).add(i);
                    dominationCount[i]++;
                }
            }
        }

        // Step 2: Identify first front
        List<Integer> firstFront = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            if (dominationCount[i] == 0) {
                population.get(i).setRank(1);
                firstFront.add(i);
            }
        }
        fronts.add(firstFront);

        // Step 3: Build subsequent fronts
        int rank = 1;
        while (!fronts.get(rank - 1).isEmpty()) {
            List<Integer> nextFront = new ArrayList<>();

            for (int pIndex : fronts.get(rank - 1)) {
                for (int qIndex : dominates.get(pIndex)) {
                    dominationCount[qIndex]--;
                    if (dominationCount[qIndex] == 0) {
                        population.get(qIndex).setRank(rank + 1);
                        nextFront.add(qIndex);
                    }
                }
            }

            rank++;
            fronts.add(nextFront);
        }
    }

    private boolean dominates(Chromosome p, Chromosome q) {
        boolean betterInAtLeastOne = false;
        double[] pObjectives = p.getObjective();
        double[] qObjectives = q.getObjective();

        for (int i = 0; i < pObjectives.length; i++) {
            double pVal = p.getFitness() == Double.POSITIVE_INFINITY
                    ? Double.POSITIVE_INFINITY
                    : pObjectives[i];
            double qVal = q.getFitness() == Double.POSITIVE_INFINITY
                    ? Double.POSITIVE_INFINITY
                    : qObjectives[i];

            if (pVal > qVal) {
                return false;
            } else if (pVal < qVal) {
                betterInAtLeastOne = true;
            }
        }
        return betterInAtLeastOne;
    }

    private void sortPopulationByRank(List<Chromosome> population) {
        population.sort(Comparator.comparingInt(Chromosome::getRank));
    }


    private void crossover() {
        List<Callable<Chromosome>> crossoverTasks = new ArrayList<>(crossSize);
        tempPopulation.clear();
        int index = 0;
        while (index < crossSize){
            Chromosome p1 = newPopulation.get(tournamentSelection());
            Chromosome p2 = newPopulation.get(tournamentSelection());

            List<Integer>[] genes1 = p1.getGenes();
            List<Integer>[] genes2 = p2.getGenes();

            int r1, r2;
            do{
                r1 = rand.nextInt(caregiversNum);
                r2 = rand.nextInt(caregiversNum);
            } while (genes1[r1].isEmpty() || genes2[r2].isEmpty());

            int finalR2 = r2;
            Random crossoverRandom = new Random(rand.nextLong());
            crossoverTasks.add(() -> new BestCostRouteCrossover(
                    finalR2, p1, p2, crossoverRandom).Crossover());
            index++;
            int finalR1 = r1;
            if (index < crossSize){
                Random reverseRandom = new Random(rand.nextLong());
                crossoverTasks.add(() -> new BestCostRouteCrossover(
                        finalR1, p2, p1, reverseRandom).Crossover());
                index++;
            }
        }
        invokeTasks(crossoverTasks, tempPopulation);
    }

    private <T> void invokeTasks(List<Callable<T>> tasks, List<T> destination) {
        try {
            List<Future<T>> futures = workerPool.invokeAll(tasks);
            for (Future<T> future : futures) {
                destination.add(future.get());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("MMGA MultiDepotGA worker execution was interrupted", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("MMGA MultiDepotGA worker execution failed", e.getCause());
        }
    }

    private void mutation() {
        List<Callable<Chromosome>> mutationTasks = new ArrayList<>(mutSize);
        for (int i = 0; i < mutSize; i++) {
            Chromosome p = newPopulation.get(tournamentSelection());
            Random mutationRandom = new Random(rand.nextLong());
            mutationTasks.add(() -> new SwapRouteMutation(p, mutationRandom).mutate());
        }
        tempMutPopulation.clear();
        invokeTasks(mutationTasks, tempMutPopulation);
    }

    private int tournamentSelection() {
        int populationSize = newPopulation.size();
        if (populationSize == 0) {
            throw new IllegalStateException("Cannot select from an empty population");
        }
        if (TSRate <= 0) {
            throw new IllegalStateException("Tournament size must be positive");
        }
        int fallbackPosition = rand.nextInt(TSRate);
        int fallback = -1;
        int best = -1;
        for (int i = 0; i < TSRate; i++) {
            int candidate = rand.nextInt(populationSize);
            if (i == fallbackPosition) {
                fallback = candidate;
            }
            if (best == -1 || betterTournamentCandidate(candidate, best)) {
                best = candidate;
            }
        }
        return rand.nextDouble() < 0.8 ? best : fallback;
    }

    private boolean betterTournamentCandidate(int candidateIndex, int bestIndex) {
        Chromosome candidate = newPopulation.get(candidateIndex);
        Chromosome best = newPopulation.get(bestIndex);
        int candidateRank = normalizedRank(candidate);
        int bestRank = normalizedRank(best);
        if (candidateRank != bestRank) {
            return candidateRank < bestRank;
        }
        return candidate.getFitness() < best.getFitness();
    }

    private int normalizedRank(Chromosome chromosome) {
        return chromosome.getRank() > 0 ? chromosome.getRank() : Integer.MAX_VALUE;
    }

    public static boolean conflictCheck(List<Integer> c1Route, List<Integer> c2Route, int m, int n) {
        Map<Integer, Integer> route2Positions = new HashMap<>(c2Route.size() * 2);
        for (int i = 0; i < c2Route.size(); i++) {
            route2Positions.putIfAbsent(c2Route.get(i), i);
        }
        Set<Integer> checkedPatients = new HashSet<>(c1Route.size() * 2);
        for (int i = 0; i < c1Route.size(); i++) {
            int patient = c1Route.get(i);
            if (!checkedPatients.add(patient)) {
                continue;
            }
            Integer index2 = route2Positions.get(patient);
            if (index2 != null) {
                if (m <= i && n > index2 || m > i && n <= index2) {
                    return false;
                }
            }
        }
        return true;
    }

    public void startTimer() {
        this.startCpuTime = osBean.getProcessCpuTime();
        this.startTime = System.currentTimeMillis();
    }

    public double getTotalCPUTimeSeconds() {
        long endCpuTime = osBean.getProcessCpuTime();
        return (endCpuTime - startCpuTime) / 1_000_000_000.0;
    }

    public double getTotalTimeSeconds() {
        long endTime = System.currentTimeMillis();
        return (endTime - startTime) / 1_000.0;
    }

    private void sortPopulation(List<Chromosome> population) {
        population.sort(Comparator.comparingDouble(Chromosome::getFitness));
    }

    private void performanceUpdate(int generation) {
        sortPopulation(newPopulation);
        if (generation > 0 && bestChromosome.getFitness() == newPopulation.get(0).getFitness()) {
            terminator++;
        } else {
            terminator = 0;
        }

        bestChromosome = newPopulation.get(0);
        double averageFitness = newPopulation.stream().mapToDouble(Chromosome::getFitness).sum()
                / newPopulation.size();

        System.out.println("Time at: " + getTotalTimeSeconds() + " CPU Timer " + String.format("%.3f", getTotalCPUTimeSeconds()) + " seconds Generation " + generation + " Generation without Improvement: " + terminator + " Best fitness: " + bestChromosome.getFitness() + " Average fitness: " + averageFitness);
        if (generation == gen) {
            System.out.println("----------------- Solution ----------------------");
            paretoScheme(newPopulation);
            EvaluationFunction.Evaluate(bestChromosome);
            System.out.println(" Best Fitness: " + bestChromosome.getFitness() + " Rank: " + bestChromosome.getRank());
            System.out.println("Time at: " + getTotalTimeSeconds() + " CPU Timer " + String.format("%.3f", getTotalCPUTimeSeconds()) +
                    " seconds Generation " + generation + " Fitness: " + bestChromosome.getFitness() + " Total Distance: " + bestChromosome.getTotalTravelCost() +
                    " Total Tardiness: " + bestChromosome.getTotalTardiness() + " Highest Tardiness: " + bestChromosome.getHighestTardiness() +
                    " Total Waiting Time " + bestChromosome.getTotalWaitingTime() + " Total Overtime " + bestChromosome.getOvertime() + " Highest Idle Time " + bestChromosome.getHighestIdleTime());
        }
    }

    private void update() {
        previousPopulation.clear();
        previousPopulation.addAll(newPopulation);
        newPopulation.clear();
        newPopulation.addAll(nextPopulation);
        for (Chromosome chromosome : tempMutPopulation) {
            if (newPopulation.size() >= popSize) {
                break;
            }
            newPopulation.add(chromosome);
        }
        sortPopulation(tempPopulation);
        for (Chromosome c : tempPopulation) {
            if (newPopulation.size() < popSize) {
                newPopulation.add(c);
            } else break;
        }
        for (Chromosome chromosome : previousPopulation) {
            if (newPopulation.size() >= popSize) {
                break;
            }
            newPopulation.add(chromosome);
        }
        if (newPopulation.size() != popSize) {
            throw new IllegalStateException(
                    "Population update produced " + newPopulation.size()
                            + " chromosomes; expected " + popSize);
        }
    }
}
