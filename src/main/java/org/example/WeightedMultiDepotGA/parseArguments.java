package org.example.WeightedMultiDepotGA;

import org.example.Data.InstancesClass;

public class parseArguments {
    public static Configuration getConfiguration(InstancesClass instance, String[] args) {
        int populationSize = 500;
        String selectionMethod = "T";
        double elitismRate = 0.1;
        int TSRate = 3;
        String mutationMethod = "RS";
        float mutRate = 0.2f;
        String crossoverMethod = "BC";
        double crossRate =1.0;
        int time = 25920000;
        int generations = 1000;
//        String arg = "--popSize 300 --mutRate 0.05 --mutMethod S --crossRate 1.0 --crossMethod BS --selection RW --elitism 0.1 --numberOfElites 5 --LSRate 10 --TSRate 4 --instance src/main/java/org/example/Data/instance/200_1.json";
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
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
                case "--Time":
                    time = Integer.parseInt(args[++i]);
                    break;
                case "--Generation":
                    generations = Integer.parseInt(args[++i]);
                    break;
                case "--TSRate":
                    TSRate = Integer.parseInt(args[++i]);
                    break;
            }
        }
        return new Configuration(instance,populationSize,selectionMethod,elitismRate,TSRate,mutationMethod,mutRate,crossoverMethod,crossRate,time, generations);
    }
}
