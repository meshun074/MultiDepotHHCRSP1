package org.example.WeightedMultiDepotGA;

import org.example.Data.InstancesClass;

public record Configuration(
        InstancesClass instance,
        int populationSize,
        String selectionMethod,
        double elitismRate,
        int TSRate,
        String mutationMethod,
        float mutationRate,
        String crossoverMethod,
        double crossoverRate,
        int time,
        int generations
) {}
