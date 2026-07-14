package Parameters;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;

public class Parameters {
    private final String instance;
    private final int numberOfGeneration;
    private final int populationSize;
    private final String selectionMethod;
    private final double elitismRate;
    private final int TSRate;
    private final String mutationMethod;
    private final float mutationRate;
    private final String crossoverMethod;
    private final double crossoverRate;
    private final int numberOfElites;
    private final int LSRate;
    private final boolean LSStart;
    private final double LSStartRate;

    public Parameters(String instance, int numberOfGeneration, int populationSize, String selectionMethod, double elitismRate, int TSRate, String mutationMethod, float mutationRate, String crossoverMethod, double crossoverRate, int numberOfElites, int LSRate, boolean LSStart, double LSStartRate) {
        this.instance = instance;
        this.numberOfGeneration = numberOfGeneration;
        this.populationSize = populationSize;
        this.selectionMethod = selectionMethod;
        this.elitismRate = elitismRate;
        this.TSRate = TSRate;
        this.mutationMethod = mutationMethod;
        this.mutationRate = mutationRate;
        this.crossoverMethod = crossoverMethod;
        this.crossoverRate = crossoverRate;
        this.numberOfElites = numberOfElites;
        this.LSRate = LSRate;
        this.LSStart = LSStart;
        this.LSStartRate = LSStartRate;
    }

    public String getInstance() {
        return instance;
    }

    public int getNumberOfGeneration() {return numberOfGeneration;}

    public int getPopulationSize() {
        return populationSize;
    }

    public String getSelectionMethod() {
        return selectionMethod;
    }

    public double getElitismRate() {
        return elitismRate;
    }

    public int getTSRate() {
        return TSRate;
    }

    public String getMutationMethod() {
        return mutationMethod;
    }

    public float getMutRate() {
        return mutationRate;
    }

    public String getCrossoverMethod() {
        return crossoverMethod;
    }

    public double getCrossRate() {
        return crossoverRate;
    }

    public int getNumberOfElites() {
        return numberOfElites;
    }

    public int getLSRate() {
        return LSRate;
    }

    public boolean isLSStart() {
        return LSStart;
    }

    public double getLSStartRate() {
        return LSStartRate;
    }

}
