package org.example;

import org.example.Data.InstancesClass;
import org.example.Data.ReadData;
import org.example.WeightedMultiDepotGA.Configuration;
import org.example.WeightedMultiDepotGA.parseArguments;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Main {

    private static final Path DEFAULT_DATA_DIRECTORY =
            Paths.get("src", "main", "java", "org", "example",
                    "Data", "Extended", "Validation");

    private static final Path DEFAULT_RESULTS_DIRECTORY =
            Paths.get("NewResults");

    /*
     * Matches names such as:
     * instance_p10_01.json
     * p25-example.json
     */
    private static final Pattern PROBLEM_SIZE_PATTERN =
            Pattern.compile("(?i)(?:^|[^A-Za-z0-9])p(\\d+)(?=$|[^0-9])");

    private Main() {
        // Utility class
    }

    public static void main(String[] args) {
        try {
            ProgramArguments arguments = ProgramArguments.parse(args);
            execute(arguments);
        } catch (IllegalArgumentException e) {
            System.err.println("Argument error: " + e.getMessage());
            printUsage();
            System.exit(2);
        } catch (Exception e) {
            System.err.println("Execution failed: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static void execute(ProgramArguments arguments) throws IOException {
        Path dataDirectory = Paths.get(
                System.getProperty(
                        "data.dir",
                        DEFAULT_DATA_DIRECTORY.toString()
                )
        );

        Path resultsDirectory = Paths.get(
                System.getProperty(
                        "NewResults.dir",
                        DEFAULT_RESULTS_DIRECTORY.toString()
                )
        );

        validateDataDirectory(dataDirectory);

        Path instanceFile = findInstanceFile(
                dataDirectory,
                arguments.instancePrefix
        );

        String fileName = instanceFile.getFileName().toString();
        String problemSize = extractProblemSize(fileName);

        Path resultDirectory = createResultDirectory(
                resultsDirectory,
                arguments.algorithm,
                fileName
        );

        Path outputFile = createOutputFile(
                resultDirectory,
                arguments
        );

        runWithRedirectedOutput(
                outputFile,
                instanceFile,
                problemSize,
                arguments
        );

        System.out.println("Results written to: " + outputFile.toAbsolutePath());
    }

    private static void validateDataDirectory(Path dataDirectory) {
        if (!Files.exists(dataDirectory)) {
            throw new IllegalArgumentException(
                    "Data directory does not exist: "
                            + dataDirectory.toAbsolutePath()
            );
        }

        if (!Files.isDirectory(dataDirectory)) {
            throw new IllegalArgumentException(
                    "Data path is not a directory: "
                            + dataDirectory.toAbsolutePath()
            );
        }

        if (!Files.isReadable(dataDirectory)) {
            throw new IllegalArgumentException(
                    "Data directory is not readable: "
                            + dataDirectory.toAbsolutePath()
            );
        }
    }

    private static Path findInstanceFile(
            Path dataDirectory,
            String instancePrefix
    ) throws IOException {

        List<Path> matches;

        /*
         * Files.list() must be closed. The original implementation left
         * the directory stream open.
         */
        try (Stream<Path> files = Files.list(dataDirectory)) {
            matches = files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName()
                            .toString()
                            .startsWith(instancePrefix))
                    .sorted(Comparator.comparing(
                            path -> path.getFileName().toString()
                    ))
                    .collect(Collectors.toList());
        }

        if (matches.isEmpty()) {
            throw new IllegalArgumentException(
                    "No instance file found with prefix '"
                            + instancePrefix
                            + "' in "
                            + dataDirectory.toAbsolutePath()
            );
        }

        if (matches.size() > 1) {
            String matchingNames = matches.stream()
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.joining(", "));

            throw new IllegalArgumentException(
                    "Prefix '" + instancePrefix
                            + "' matches multiple files: "
                            + matchingNames
            );
        }

        return matches.get(0);
    }

    private static String extractProblemSize(String fileName) {
        Matcher matcher = PROBLEM_SIZE_PATTERN.matcher(fileName);
        return matcher.find() ? matcher.group(1) : "unknown";
    }

    private static Path createResultDirectory(
            Path resultsRoot,
            Algorithm algorithm,
            String instanceFileName
    ) throws IOException {

        String instanceName = removeExtension(instanceFileName);
        String safeInstanceName = sanitizeFileComponent(instanceName);

        if (safeInstanceName.length() > 80) {
            safeInstanceName = safeInstanceName.substring(0, 80);
        }

        String directoryName =
                algorithm.resultDirectoryPrefix
                        + safeInstanceName
                        + "_results";

        Path resultDirectory = resultsRoot.resolve(directoryName);
        Files.createDirectories(resultDirectory);

        return resultDirectory;
    }

    private static Path createOutputFile(
            Path resultDirectory,
            ProgramArguments arguments
    ) {
        String safePrefix =
                sanitizeFileComponent(arguments.instancePrefix);

        String outputFileName = String.format(
                Locale.ROOT,
                "Result_%s_%d_%d.txt",
                safePrefix,
                arguments.experimentNumber,
                arguments.randomSeed
        );

        return resultDirectory.resolve(outputFileName);
    }

    private static void runWithRedirectedOutput(
            Path outputFile,
            Path instanceFile,
            String problemSize,
            ProgramArguments arguments
    ) throws IOException {

        PrintStream originalOutput = System.out;

        try (
                PrintStream fileOutput = new PrintStream(
                        new BufferedOutputStream(
                                Files.newOutputStream(outputFile)
                        ),
                        true,
                        StandardCharsets.UTF_8
                )
        ) {
            /*
             * This is retained because evolve() and showSolution() may print
             * directly to System.out. Ideally, those classes should accept a
             * PrintStream or logger instead.
             */
            System.setOut(fileOutput);

            printConfiguration(
                    instanceFile,
                    problemSize,
                    arguments
            );

            InstancesClass instance =
                    ReadData.read(instanceFile.toFile());

            Configuration configuration =
                    parseArguments.getConfiguration(
                            instance,
                            new String[0]
                    );

            if (arguments.algorithm == Algorithm.PARETO) {
                runParetoAlgorithm(
                        configuration,
                        arguments.randomSeed
                );
            } else {
                runWeightedAlgorithm(
                        configuration,
                        arguments.randomSeed
                );
            }

            if (fileOutput.checkError()) {
                throw new IOException(
                        "An error occurred while writing to " + outputFile
                );
            }
        } finally {
            /*
             * The original code permanently replaced System.out for the
             * entire JVM. Always restore it.
             */
            System.setOut(originalOutput);
        }
    }

    private static void printConfiguration(
            Path instanceFile,
            String problemSize,
            ProgramArguments arguments
    ) {
        System.out.printf(
                Locale.ROOT,
                "Config Parameters:%n"
                        + "  Algorithm=%s%n"
                        + "  ProblemSize=%s%n"
                        + "  InstancePrefix=%s%n"
                        + "  InstanceFile=%s%n"
                        + "  ExperimentNumber=%d%n"
                        + "  Seed=%d%n%n",
                arguments.algorithm,
                problemSize,
                arguments.instancePrefix,
                instanceFile.toAbsolutePath(),
                arguments.experimentNumber,
                arguments.randomSeed
        );
    }

    private static void runParetoAlgorithm(
            Configuration configuration,
            long randomSeed
    ) {
        org.example.ParetoMultiDepotGA.GeneticAlgorithm algorithm =
                new org.example.ParetoMultiDepotGA.GeneticAlgorithm(
                        configuration,
                        randomSeed
                );

        org.example.ParetoMultiDepotGA.Chromosome best =
                algorithm.evolve();

        System.out.println("----------------- Solution -----------------");
        printParetoSolution(best);
        best.showSolution(0);
    }

    private static void runWeightedAlgorithm(
            Configuration configuration,
            long randomSeed
    ) {
        org.example.WeightedMultiDepotGA.GeneticAlgorithm algorithm =
                new org.example.WeightedMultiDepotGA.GeneticAlgorithm(
                        configuration,
                        randomSeed
                );

        org.example.WeightedMultiDepotGA.Chromosome best =
                algorithm.evolve();

        System.out.println("----------------- Solution -----------------");
        printWeightedSolution(best);
        best.showSolution(0);
    }

    private static void printParetoSolution(
            org.example.ParetoMultiDepotGA.Chromosome chromosome
    ) {
        System.out.printf(
                Locale.ROOT,
                "Best Fitness: %s%n"
                        + "Total Distance: %s%n"
                        + "Total Tardiness: %s%n"
                        + "Highest Tardiness: %s%n"
                        + "Total Waiting Time: %s%n"
                        + "Total Overtime: %s%n"
                        + "Highest Idle Time: %s%n",
                chromosome.getFitness(),
                chromosome.getTotalTravelCost(),
                chromosome.getTotalTardiness(),
                chromosome.getHighestTardiness(),
                chromosome.getTotalWaitingTime(),
                chromosome.getOvertime(),
                chromosome.getHighestIdleTime()
        );
    }

    private static void printWeightedSolution(
            org.example.WeightedMultiDepotGA.Chromosome chromosome
    ) {
        System.out.printf(
                Locale.ROOT,
                "Best Fitness: %s%n"
                        + "Total Distance: %s%n"
                        + "Total Tardiness: %s%n"
                        + "Highest Tardiness: %s%n"
                        + "Total Waiting Time: %s%n"
                        + "Total Overtime: %s%n"
                        + "Highest Idle Time: %s%n",
                chromosome.getFitness(),
                chromosome.getTotalTravelCost(),
                chromosome.getTotalTardiness(),
                chromosome.getHighestTardiness(),
                chromosome.getTotalWaitingTime(),
                chromosome.getOvertime(),
                chromosome.getHighestIdleTime()
        );
    }

    private static String removeExtension(String fileName) {
        int extensionIndex = fileName.lastIndexOf('.');

        if (extensionIndex <= 0) {
            return fileName;
        }

        return fileName.substring(0, extensionIndex);
    }

    private static String sanitizeFileComponent(String value) {
        String sanitized = value.replaceAll(
                "[^A-Za-z0-9._-]",
                "_"
        );

        return sanitized.isEmpty() ? "unknown" : sanitized;
    }

    private static void printUsage() {
        System.err.println(
                "Usage: java -jar example.jar "
                        + "<instancePrefix> "
                        + "<experimentNumber> "
                        + "<algorithm> "
                        + "[randomSeed]"
        );
        System.err.println("  algorithm: p = Pareto, w = Weighted");
        System.err.println();
        System.err.println("Optional JVM properties:");
        System.err.println(
                "  -Ddata.dir=<instance-directory>"
        );
        System.err.println(
                "  -Dresults.dir=<results-directory>"
        );
    }

    private enum Algorithm {
        PARETO("Pareto-MultiDepotHHCRSP_"),
        WEIGHTED("Weighted-MultiDepotHHCRSP_");

        private final String resultDirectoryPrefix;

        Algorithm(String resultDirectoryPrefix) {
            this.resultDirectoryPrefix = resultDirectoryPrefix;
        }

        private static Algorithm parse(String value) {
            if (value == null) {
                throw new IllegalArgumentException(
                        "Algorithm must be specified"
                );
            }

            switch (value.trim().toLowerCase(Locale.ROOT)) {
                case "p":
                case "pareto":
                    return PARETO;

                case "w":
                case "weighted":
                    return WEIGHTED;

                default:
                    throw new IllegalArgumentException(
                            "Unsupported algorithm '" + value
                                    + "'. Use p/Pareto or w/Weighted."
                    );
            }
        }
    }

    private static final class ProgramArguments {

        private final String instancePrefix;
        private final int experimentNumber;
        private final Algorithm algorithm;
        private final long randomSeed;

        private ProgramArguments(
                String instancePrefix,
                int experimentNumber,
                Algorithm algorithm,
                long randomSeed
        ) {
            this.instancePrefix = instancePrefix;
            this.experimentNumber = experimentNumber;
            this.algorithm = algorithm;
            this.randomSeed = randomSeed;
        }

        private static ProgramArguments parse(String[] args) {
            if (args == null || args.length < 3 || args.length > 4) {
                throw new IllegalArgumentException(
                        "Expected three or four arguments"
                );
            }

            String instancePrefix = args[0].trim();

            if (instancePrefix.isEmpty()) {
                throw new IllegalArgumentException(
                        "Instance prefix cannot be empty"
                );
            }

            int experimentNumber;

            try {
                experimentNumber = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Experiment number must be an integer: "
                                + args[1],
                        e
                );
            }

            if (experimentNumber < 0) {
                throw new IllegalArgumentException(
                        "Experiment number cannot be negative"
                );
            }

            Algorithm algorithm = Algorithm.parse(args[2]);

            long randomSeed;

            if (args.length == 4) {
                try {
                    randomSeed = Long.parseLong(args[3]);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "Random seed must be a long integer: "
                                    + args[3],
                            e
                    );
                }
            } else {
                randomSeed =
                        System.currentTimeMillis()
                                ^ ((long) experimentNumber << 32);
            }

            return new ProgramArguments(
                    instancePrefix,
                    experimentNumber,
                    algorithm,
                    randomSeed
            );
        }
    }
}