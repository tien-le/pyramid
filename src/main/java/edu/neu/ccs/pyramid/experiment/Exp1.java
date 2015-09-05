package edu.neu.ccs.pyramid.experiment;

import edu.neu.ccs.pyramid.configuration.Config;
import edu.neu.ccs.pyramid.dataset.*;
import edu.neu.ccs.pyramid.eval.MSE;
import edu.neu.ccs.pyramid.regression.regression_tree.RegTreeConfig;
import edu.neu.ccs.pyramid.regression.regression_tree.RegTreeTrainer;
import edu.neu.ccs.pyramid.regression.regression_tree.RegressionTree;
import org.apache.commons.lang3.time.StopWatch;

import java.util.stream.IntStream;

/**
 * regression data set memory usage test
 * Created by chengli on 8/11/14.
 */
public class Exp1 {
    public static void main(String[] args) throws Exception{
        if (args.length !=1){
            throw new IllegalArgumentException("Please specify a properties file.");
        }

        Config config = new Config(args[0]);
        System.out.println(config);
        int numDataPoints = config.getInt("numDataPoints");
        int numFeatures = config.getInt("numFeatures");
        int numLeaves = config.getInt("numLeaves");
        RegDataSet dataSet = RegDataSetBuilder.getBuilder()
                .numDataPoints(numDataPoints).numFeatures(numFeatures)
                .dense(!config.getBoolean("sparse")).missingValue(false).build();

        double nonzeroPercentage = config.getDouble("nonzeroPercentage");

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        System.out.println("generating dataset");
        IntStream.range(0, dataSet.getNumDataPoints())
                .forEach(i-> IntStream.range(0,dataSet.getNumFeatures())
                        .forEach(j-> {
                            boolean set = Math.random()<nonzeroPercentage;
                            if (set){
                                dataSet.setFeatureValue(i,j,Math.random());
                            }
                            dataSet.setLabel(i,Math.random());
                        }));
        System.out.println("created");
        System.out.println(stopWatch);

        stopWatch.reset();




        int[] activeFeatures = IntStream.range(0, dataSet.getNumFeatures()).toArray();
        int[] activeDataPoints = IntStream.range(0,dataSet.getNumDataPoints()).toArray();
        RegTreeConfig regTreeConfig = new RegTreeConfig();


        regTreeConfig.setMaxNumLeaves(numLeaves);
        regTreeConfig.setMinDataPerLeaf(5);


        regTreeConfig.setNumSplitIntervals(config.getInt("numSplitIntervals"));

        System.out.println("start training");
        stopWatch.start();
        RegressionTree regressionTree = RegTreeTrainer.fit(regTreeConfig, dataSet);

        System.out.println("training done");
        System.out.println(stopWatch);

        double mseValue = MSE.mse(regressionTree, dataSet);
        System.out.println(mseValue);

    }
}
