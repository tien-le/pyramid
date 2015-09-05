package edu.neu.ccs.pyramid.experiment;

import edu.neu.ccs.pyramid.classification.logistic_regression.LogisticRegression;
import edu.neu.ccs.pyramid.classification.logistic_regression.RidgeLogisticTrainer;
import edu.neu.ccs.pyramid.configuration.Config;
import edu.neu.ccs.pyramid.dataset.ClfDataSet;
import edu.neu.ccs.pyramid.dataset.ClfDataSetBuilder;
import edu.neu.ccs.pyramid.dataset.DataSetType;
import edu.neu.ccs.pyramid.dataset.TRECFormat;
import edu.neu.ccs.pyramid.eval.*;
import org.apache.mahout.math.Vector;

import java.io.File;

/**
 * test ridge logistic regression with noisy featureList
 * Created by chengli on 2/13/15.
 */
public class Exp68 {
    public static void main(String[] args) throws Exception{
        if (args.length !=1){
            throw new IllegalArgumentException("Please specify a properties file.");
        }

        Config config = new Config(args[0]);
        System.out.println(config);
        train(config);
    }

    private static ClfDataSet addNoise(Config config, ClfDataSet dataSet){
        int numFeatures = dataSet.getNumFeatures()+config.getInt("numNoisyFeatures");
        ClfDataSet noisyData = ClfDataSetBuilder.getBuilder().numFeatures(numFeatures)
                .numDataPoints(dataSet.getNumDataPoints()).dense(dataSet.isDense())
                .missingValue(dataSet.hasMissingValue())
                .numClasses(dataSet.getNumClasses())
                .build();
        for (int j=0;j<dataSet.getNumFeatures();j++){
            Vector column = dataSet.getColumn(j);
            for (Vector.Element element: column.nonZeroes()){
                int i = element.index();
                double value = element.get();
                noisyData.setFeatureValue(i,j,value);
            }
        }
        for (int j=dataSet.getNumFeatures();j<numFeatures;j++){
            for (int i=0;i<dataSet.getNumDataPoints();i++){
                if (i%10==0){
                    noisyData.setFeatureValue(i,j,Math.random());
                }

            }
        }

        for (int i=0;i<dataSet.getNumDataPoints();i++){
            noisyData.setLabel(i,dataSet.getLabels()[i]);
        }
        return noisyData;
    }


    private static ClfDataSet doubleIt(ClfDataSet dataSet){
        int numFeatures = dataSet.getNumFeatures()*2;
        ClfDataSet noisyData = ClfDataSetBuilder.getBuilder().numFeatures(numFeatures)
                .numDataPoints(dataSet.getNumDataPoints()).dense(dataSet.isDense())
                .missingValue(dataSet.hasMissingValue())
                .numClasses(dataSet.getNumClasses())
                .build();
        for (int j=0;j<dataSet.getNumFeatures();j++){
            Vector column = dataSet.getColumn(j);
            for (Vector.Element element: column.nonZeroes()){
                int i = element.index();
                double value = element.get();
                noisyData.setFeatureValue(i,j,value);
                noisyData.setFeatureValue(i,j+dataSet.getNumFeatures(),value);
            }
        }

        for (int i=0;i<dataSet.getNumDataPoints();i++){
            noisyData.setLabel(i,dataSet.getLabels()[i]);
        }
        return noisyData;
    }

    private static void train(Config config) throws Exception{
        String input = config.getString("input.folder");
        ClfDataSet dataSet = TRECFormat.loadClfDataSet(new File(input, "train.trec"),
                DataSetType.CLF_SPARSE, true);
        ClfDataSet testSet = TRECFormat.loadClfDataSet(new File(input,"test.trec"),
                DataSetType.CLF_SPARSE, true);
        System.out.println(dataSet.getMetaInfo());

        RidgeLogisticTrainer trainer = RidgeLogisticTrainer.getBuilder()
                .setHistory(5)
                .setGaussianPriorVariance(config.getDouble("gaussianPriorVariance"))
                .setEpsilon(0.1)
                .build();



        LogisticRegression logisticRegression = trainer.train(dataSet);
        System.out.println("train: "+ Accuracy.accuracy(logisticRegression, dataSet));
        System.out.println("test: "+Accuracy.accuracy(logisticRegression,testSet));

        ClfDataSet noisyTrain = addNoise(config,dataSet);
        ClfDataSet noisyTest = addNoise(config,testSet);
        LogisticRegression noisyLogisticRegression = trainer.train(noisyTrain);
        System.out.println("noisy train: "+ Accuracy.accuracy(noisyLogisticRegression, noisyTrain));
        System.out.println("noisy test: "+Accuracy.accuracy(noisyLogisticRegression,noisyTest));


        RidgeLogisticTrainer doubleTrainer = RidgeLogisticTrainer.getBuilder()
                .setHistory(5)
                .setGaussianPriorVariance(0.5*config.getDouble("gaussianPriorVariance"))
                .setEpsilon(0.1)
                .build();

        ClfDataSet doubleTrain = doubleIt(dataSet);
        ClfDataSet doubleTest = doubleIt(testSet);
        LogisticRegression doubleLogisticRegression = doubleTrainer.train(doubleTrain);
        System.out.println("double train: "+ Accuracy.accuracy(doubleLogisticRegression, doubleTrain));
        System.out.println("double test: "+Accuracy.accuracy(doubleLogisticRegression,doubleTest));




    }
}
