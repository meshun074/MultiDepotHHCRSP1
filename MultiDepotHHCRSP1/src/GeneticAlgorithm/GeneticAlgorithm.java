package GeneticAlgorithm;

import com.sun.management.OperatingSystemMXBean;
import Data.InstancesClass;
import Parameters.Parameters;

import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GeneticAlgorithm {
    private final int popSize;
    private final int gen;
    private final int LSRate;
    private final boolean LSStart;
    private final int TSRate;
    private final String selectTechnique;
    private final String crossType;
    private final String mutType;
    private final float mutRate;
    private final int numOfEliteSearch;
    private final double elitismRate;
    private final int elitismSize;
    private final List<Integer> eliteRandomList;
    private final double crossRate;
    private final int crossSize;
    private final int mutSize;
    private final InstancesClass data;
    private Chromosome bestChromosome;
    private final List<Chromosome> nextPopulation;
    private final List<Chromosome> tempPopulation;
    private final List<Chromosome> tempMutPopulation;
    private List<Chromosome> newPopulation;
    private final List<Chromosome> previousPopulation;
    private final List<Chromosome> crossoverChromosomes;
    private final List<Chromosome> mutationChromosomes;
    private final double[] popProbabilities;
    private Map<Integer, Chromosome> LSChromosomes;
    private int terminator = 0;
    private final int patientLength;
    private final int caregiversNum;
    public static Random rand;
    private long startCpuTime;
    private long startTime;
    private final OperatingSystemMXBean osBean;
    private final int limit;
    private static final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();

    public GeneticAlgorithm(Parameters parameters, long seed, InstancesClass instancesClass) {
        this.osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        this.data = instancesClass;
        rand = new Random(seed);
        numOfEliteSearch = parameters.getNumberOfElites();
        LSRate = parameters.getLSRate();
        LSStart = parameters.isLSStart();
        TSRate = parameters.getTSRate();
        popSize = parameters.getPopulationSize();
        gen = parameters.getNumberOfGeneration();
        selectTechnique = parameters.getSelectionMethod();
        mutType = parameters.getMutationMethod();
        mutRate = parameters.getMutRate();
        mutSize = (int) (mutRate * popSize);
        crossType = parameters.getCrossoverMethod();
        crossRate = parameters.getCrossRate();
        crossSize = (int)(popSize*crossRate);
        elitismRate = parameters.getElitismRate();
        elitismSize = (int) (elitismRate * popSize);
        eliteRandomList = new ArrayList<>(elitismSize);
        for (int i = 0; i < elitismSize; i++) {
            eliteRandomList.add(i);
        }
        popProbabilities = new double[popSize];
        patientLength = instancesClass.getPatients().length;
        caregiversNum = instancesClass.getCaregivers().length;
        limit = Math.min(numOfEliteSearch, eliteRandomList.size());
        nextPopulation = new ArrayList<>(popSize);
        tempPopulation = new ArrayList<>(popSize);
        previousPopulation = new ArrayList<>(popSize);
        tempMutPopulation = new ArrayList<>(mutSize);
        mutationChromosomes = Collections.synchronizedList(new ArrayList<>(mutSize));
        crossoverChromosomes = Collections.synchronizedList(new ArrayList<>(crossSize));
    }

    public Chromosome evolve(){
        System.out.printf("Population Size: %d, Generation: %d, SelectionType: %s TSRate: %d, Crossover type: %s\n CrossRate: %f, EliteRate: %f, MutType: %s Mutation Rate: %f\n", popSize, gen,  selectTechnique, TSRate, crossType, crossRate, elitismRate, mutType, mutRate);
        bestChromosome = null;
        startTimer();
        System.out.println("Initializing Population ...");
        newPopulation = Population.clusteredInitialization(popSize, patientLength);
        System.out.printf("Population initialized: CPU Timer(s): %s Timer(s): %s\n", getTotalCPUTimeSeconds(), getTotalTimeSeconds());
        sortPopulation(newPopulation);

        performanceUpdate(0);

        for (int g = 1; g <= gen; g++) {
            elitismByRank();
            crossover();
            mutation();
            update();
            performanceUpdate(g);
//            if (patientLength <= 100) {
//                if (terminator == patientLength / 2 ) break;
//            } else {
//                if (terminator == 50) break;
//            }

        }
//        System.exit(1);
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
        for (int i = 0; i < popSize; i++) {
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
    }

    private void elitismByRank(){
        nextPopulation.clear();
        paretoScheme(newPopulation);
        sortPopulation(newPopulation);
        for (int i = 0; i < elitismSize; i++) {
            nextPopulation.add(newPopulation.get(i));
        }
        sortPopulationByRank(newPopulation);
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
        if (p.getFitness() == Double.POSITIVE_INFINITY) {
            Arrays.fill(pObjectives, Double.POSITIVE_INFINITY);
        }
        if (q.getFitness() == Double.POSITIVE_INFINITY) {
            Arrays.fill(qObjectives, Double.POSITIVE_INFINITY);
        }

        for (int i = 0; i < pObjectives.length; i++) {
            double pVal = pObjectives[i];
            double qVal = qObjectives[i];

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
        ExecutorService es = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        crossoverChromosomes.clear();
        List<Callable<Void>> crossoverTasks = new ArrayList<>();
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
            crossoverTasks.add(() ->{
                if(crossType.equals("BC"))
                    new BestCostRouteCrossover(this, finalR2, p1, p2, rand).run();
                else
                    new BestCostRouteCrossoverSwap(this, finalR2, p1, p2, rand).run();
                return null;
            });
//
//            BestCostRouteCrossover bc = new BestCostRouteCrossover(this, finalR2, p1, p2, rand);
//            tempPopulation.add(bc.Crossover());
            index++;
            int finalR1 = r1;
            if (index < crossSize){
                crossoverTasks.add(()->{
                    if(crossType.equals("BC"))
                        new BestCostRouteCrossover(this, finalR1, p2, p1, rand).run();
                    else
                        new BestCostRouteCrossover(this, finalR1, p2, p1, rand).run();
                    return null;
                });
//                bc = new BestCostRouteCrossover(this, finalR1, p2, p1, rand);
//                tempPopulation.add(bc.Crossover());
                index++;
            }
        }
        invokeThreads(es,crossoverTasks);
    }

    private void invokeThreads(ExecutorService service, List<Callable<Void>> crossoverTasks) {
        try {
            service.invokeAll(crossoverTasks);
            List<Chromosome> xChromosomes = crossoverChromosomes;
            synchronized (xChromosomes) {
                tempPopulation.addAll(xChromosomes);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            service.shutdown();
        }
    }

    public List<Chromosome> getCrossoverChromosomes() {
        return crossoverChromosomes;
    }

    private void mutation() {
        ExecutorService es = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        mutationChromosomes.clear();
        List<Callable<Void>> mutationTasks = new ArrayList<>();
        for (int i = 0; i < mutSize; i++) {
            Chromosome p = newPopulation.get(tournamentSelection());
            mutationTasks.add(() -> {
                new SwapRouteMutation(this, p,rand).run();
                return null;
            });
        }
        tempMutPopulation.clear();
        invokeMutationThreads(es, mutationTasks);
    }

    private void invokeMutationThreads(ExecutorService service, List<Callable<Void>> mutationTasks) {
        try {
            service.invokeAll(mutationTasks);
            List<Chromosome> xChromosomes = mutationChromosomes;
            synchronized (xChromosomes) {
                tempMutPopulation.addAll(xChromosomes);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            service.shutdown();
        }
    }

    public List<Chromosome> getMutationChromosomes() {
        return mutationChromosomes;
    }

    private int tournamentSelection() {
        int[] candidates = new int[TSRate];
        for (int i = 0; i < TSRate; i++) {
            candidates[i] = rand.nextInt(popSize);
        }

        // Find minimum index without full sorting
        int minIndex = 0;
        for (int i = 1; i < TSRate; i++) {
            if (newPopulation.get(candidates[i]).getRank() <
                    newPopulation.get(candidates[minIndex]).getRank()) {
                minIndex = i;
            }
        }
        return (Math.random() < 0.8) ? candidates[minIndex] :
                candidates[rand.nextInt(TSRate)];
    }

    public static boolean conflictCheck(List<Integer> c1Route, List<Integer> c2Route, int m, int n) {
        int index1;
        int index2;
        Set<Integer> route2 = new HashSet<>(c2Route);
        for (int i = 0; i < c1Route.size(); i++) {
            if (route2.contains(c1Route.get(i))) {
                index1 = c1Route.indexOf(c1Route.get(i));
                index2 = c2Route.indexOf(c1Route.get(i));
                if (m <= index1 && n > index2 || m > index1 && n <= index2) {
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
        double averageFitness = newPopulation.stream().mapToDouble(Chromosome::getFitness).sum() / popSize;

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
//            bestChromosome.showSolution(generation);
//            System.exit(0);
        }
    }

    private void update() {
        previousPopulation.clear();
        previousPopulation.addAll(newPopulation);
        newPopulation.clear();
        newPopulation.addAll(nextPopulation);
        newPopulation.addAll(tempMutPopulation);
        sortPopulation(tempPopulation);
//        tempPopulation.forEach(x -> System.out.print(x.getFitness()+" - "));
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
