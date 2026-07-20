package Parameters;

public class ParseArgument {
    public static Parameters getConfiguration(String[] args) {
        String instance = "";
        int numberOfGeneration = 1000;
        int populationSize = 500;
        String selectionMethod = "T";
        double elitismRate = 0.1;
        int TSRate = 3;
        String mutationMethod = "RS";
        float mutRate = 0.2f;
        String crossoverMethod = "BC";
        double crossRate = 1.0;
        int numberOfElites= 5;
        int LSRate =10;
        boolean LSStart = false;
        double LSStartRate = 0.9;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--generation":
                    numberOfGeneration = Integer.parseInt(args[++i]);
                    break;
                case "--instance":
                    instance = args[++i];
                    break;
                case "--popSize":
                    populationSize = Integer.parseInt(args[++i]);
                    break;
                case "--mutRate":
                    mutRate = Float.parseFloat(args[++i]);
                    break;
                case "--mutMethod":
                    mutationMethod = args[++i];
                    break;
                case "--crossRate":
                    crossRate = Double.parseDouble(args[++i]);
                    break;
                case "--crossMethod":
                    crossoverMethod = args[++i];
                    break;
                case "--selection":
                    selectionMethod = args[++i];
                    break;
                case "--elitism":
                    elitismRate = Double.parseDouble(args[++i]);
                    break;
                case "--numberOfElites":
                    numberOfElites = Integer.parseInt(args[++i]);
                    break;
                case "--LSRate":
                    LSRate = Integer.parseInt(args[++i]);
                    break;
                case "--LSStart":
                    LSStart = Boolean.parseBoolean(args[++i]);
                    break;
                case "--LSStartRate":
                    LSStartRate = Double.parseDouble(args[++i]);
                    break;
                case "--TSRate":
                    TSRate = Integer.parseInt(args[++i]);
                    break;
            }
        }
        return new Parameters(instance, numberOfGeneration, populationSize,selectionMethod,elitismRate,TSRate,mutationMethod,mutRate,crossoverMethod,crossRate,numberOfElites,LSRate,LSStart,LSStartRate);
    }
}
