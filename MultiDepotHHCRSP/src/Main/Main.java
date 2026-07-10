package Main;

import Data.InstancesClass;
import Data.ReadData;
import GeneticAlgorithm.Chromosome;
import GeneticAlgorithm.GeneticAlgorithm;
import Parameters.Parameters;
import Parameters.ParseArgument;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static InstancesClass instance;
    public static void main(String[] args) {

        if (args.length < 2) {
            System.err.println("Usage: java -jar example.jar <instanceIndex> <experimentNumber>");
            System.exit(1);
        }

        try {
            String instancePrefix = args[0];
//            String instancePrefix = "001";

//            double avg = 0;
            double max = Double.POSITIVE_INFINITY;
//            int runs = 1;
//            for(int runCount = 1; runCount <= runs; runCount++){
            int runCount = Integer.parseInt(args[1]);
            long randomSeed = System.currentTimeMillis() + runCount;

            Path dataDir = Paths.get("src/Data/Extended/Validation");
            if (!Files.isDirectory(dataDir)) {
                throw new IllegalStateException("Data directory not found: " + dataDir);
            }

            // Locate instance file
            Optional<Path> instanceFile = Files.list(dataDir)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().startsWith(instancePrefix))
                    .findFirst();

            if (instanceFile.isEmpty()) {
                throw new IllegalArgumentException("No instance file found for prefix: " + instancePrefix);
            }

            Path jsonFile = instanceFile.get();
            String fileName = jsonFile.getFileName().toString();

            // Extract problem size (p<number>)
            Matcher matcher = Pattern.compile("\\bp\\d+\\b").matcher(fileName);
            String problemSize = matcher.find() ? matcher.group().substring(1) : "unknown";

            // Create result directory safely
            String resultDirName = "New_Weighted-MultiDepotHHCRSP_"
                    + fileName.substring(0, Math.min(20, fileName.length()))
                    + "_results";

            Path resultDir = Paths.get("src/", resultDirName);
            Files.createDirectories(resultDir);

            // Redirect output to result file
            Path outputFile = resultDir.resolve(
                    "Result_" + instancePrefix + "_" + runCount + "_" + randomSeed + ".txt"
            );

            try (PrintStream fileOut = new PrintStream(outputFile.toFile())) {
                System.setOut(fileOut);

                System.out.printf(
                        "Config Parameters: ProblemSize=%s, instanceNumber=%s, seed=%d%n",
                        problemSize, instancePrefix, randomSeed
                );

                // Read instance
                instance = ReadData.read(jsonFile.toFile());

                // Parse GA parameters
                Parameters parameters = ParseArgument.getConfiguration(args);

                // Run GA
                GeneticAlgorithm ga = new GeneticAlgorithm(parameters, randomSeed, instance);
                Chromosome bestChromosome = ga.evolve();
//                avg += bestChromosome.getFitness();
                if(bestChromosome.getFitness() < max) {
                    max = bestChromosome.getFitness();
                }

                System.out.println("----------------- Solution ----------------------");
                System.out.println(" Best Fitness: " + bestChromosome.getFitness());
                System.out.println("Total Distance: " + bestChromosome.getTotalTravelCost() + " Total Tardiness: " + bestChromosome.getTotalTardiness() + " Highest Tardiness: " + bestChromosome.getHighestTardiness() +
                        " Total Waiting Time "+ bestChromosome.getTotalWaitingTime()+ " Total Overtime "+ bestChromosome.getOvertime() + " Highest Idle Time "+ bestChromosome.getHighestIdleTime());
                bestChromosome.showSolution(0);
            }

//            }
//            System.out.println("Best: " + max);
//            System.out.println("Average Fitness: " + avg/runs);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

}