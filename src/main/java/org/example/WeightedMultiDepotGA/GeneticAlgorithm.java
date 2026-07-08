package org.example.WeightedMultiDepotGA;

import com.sun.management.OperatingSystemMXBean;
import org.example.Data.InstancesClass;

import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.*;

public class GeneticAlgorithm {
    private final int popSize;
    private final int gen;
    private final int time;
    private final int TSRate;
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
        nextPopulation = new ArrayList<>(popSize);
        tempPopulation = new ArrayList<>(popSize);
        tempMutPopulation = new ArrayList<>(mutSize);
        previousPopulation = new ArrayList<>(popSize);
        workerPool = Executors.newFixedThreadPool(Math.max(1,
                Math.min(THREAD_POOL_SIZE, popSize)));
        EvaluationFunction.initialize(data);
        BestCostRouteCrossover.initialize(data);
        SwapRouteMutation.initialize(data);
        new AssignPatients(data);
    }

    public Chromosome evolve(){
        try {
            System.out.printf("Population Size: %d, Generation: %d, SelectionType: %s TSRate: %d, Crossover type: %s\n CrossRate: %f, EliteRate: %f, MutType: %s Mutation Rate: %f\n", popSize, gen,  selectTechnique, TSRate, crossType, crossRate, elitismRate, mutType, mutRate);
            bestChromosome = null;
            startTimer();
            System.out.println("Initializing Population ...");
            newPopulation = Population.clusteredInitialization(popSize, patientLength,data);
            System.out.printf("Population initialized: CPU Timer(s): %s Timer(s): %s\n", getTotalCPUTimeSeconds(), getTotalTimeSeconds());
            performanceUpdate(0);

            for (int g = 1; g <= gen; g++) {
                elitism();
                crossover();
                if (mutSize > 0) {
                    mutation();
                }
                update();
                performanceUpdate(g);
                if( getTotalTimeSeconds()>=time)
                    break;
            }

            EvaluationFunction.Evaluate(newPopulation.get(0));
            return newPopulation.get(0);
        } finally {
            workerPool.shutdown();
        }
    }

    private void elitism(){
        nextPopulation.clear();
        for (int i = 0; i < elitismSize; i++) {
            nextPopulation.add(newPopulation.get(i));
        }
    }

    private void crossover() {
        List<Callable<Chromosome>> crossoverTasks = new ArrayList<>(crossSize);
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
        tempPopulation.clear();
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
            throw new IllegalStateException("MultiDepotGA worker execution was interrupted", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("MultiDepotGA worker execution failed", e.getCause());
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
            if (best == -1 || newPopulation.get(candidate).getFitness()
                    < newPopulation.get(best).getFitness()) {
                best = candidate;
            }
        }
        return rand.nextDouble() < 0.8 ? best : fallback;
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
    }

    private void update() {
        previousPopulation.clear();
        previousPopulation.addAll(newPopulation);
        newPopulation.clear();
        newPopulation.addAll(nextPopulation);
        newPopulation.addAll(tempMutPopulation);
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
