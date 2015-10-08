package edu.neu.ccs.pyramid.multilabel_classification.bmm;

import edu.neu.ccs.pyramid.classification.logistic_regression.LogisticRegression;
import edu.neu.ccs.pyramid.dataset.LabelTranslator;
import edu.neu.ccs.pyramid.dataset.MultiLabel;
import edu.neu.ccs.pyramid.dataset.MultiLabelClfDataSet;
import edu.neu.ccs.pyramid.feature.FeatureList;
import edu.neu.ccs.pyramid.multilabel_classification.MultiLabelClassifier;
import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.Vector;

import java.util.stream.IntStream;

/**
 * Created by chengli on 10/7/15.
 */
public class BMMClassifier implements MultiLabelClassifier {
    int numLabels;
    int numClusters;
    int numSample = 100;
    /**
     * format:[cluster][label]
     */
    BinomialDistribution[][] distributions;
    LogisticRegression logisticRegression;

    public BMMClassifier(int numLabels, int numClusters, int numFeatures) {
        this.numLabels = numLabels;
        this.numClusters = numClusters;
        this.distributions = new BinomialDistribution[numClusters][numLabels];
        // random initialization
        UniformRealDistribution uniform = new UniformRealDistribution(0.25,0.75);
        for (int k=0;k<numClusters;k++){
            for (int l=0;l<numLabels;l++){
                double p = uniform.sample();
                distributions[k][l] = new BinomialDistribution(1,p);
            }
        }
        // num classes in logistic regression = num clusters
        this.logisticRegression = new LogisticRegression(numClusters, numFeatures);
    }

    public BMMClassifier(MultiLabelClfDataSet dataSet, int numClusters){
        this(dataSet.getNumClasses(),numClusters,dataSet.getNumFeatures());
    }

    @Override
    public int getNumClasses() {
        return this.numLabels;
    }

    @Override
    // todo bingyu
    public MultiLabel predict(Vector vector) {

        double maxProb = Double.MIN_VALUE;
        Vector predVector = new DenseVector(numLabels);

        for (int s=0; s<numSample; s++) {
            int[] clusters = IntStream.range(0, numClusters).toArray();
            double[] logisticProb = logisticRegression.predictClassProbs(vector);
            EnumeratedIntegerDistribution enumeratedIntegerDistribution = new EnumeratedIntegerDistribution(clusters,logisticProb);
            int cluster = enumeratedIntegerDistribution.sample();

            Vector candidateVector = new DenseVector(numLabels);

            for (int l=0; l<numLabels; l++) {
                candidateVector.set(l, distributions[cluster][l].sample());
            }

            double prob = 0.0;
            double[] pYnk = clusterConditionalProbArr(candidateVector);
            for (int k=0; k<numClusters; k++) {
                prob += logisticProb[k] * pYnk[k];
            }

            if (prob >= maxProb) {
                predVector = candidateVector;
            }
        }


        MultiLabel predLabel = new MultiLabel();
        for (int l=0; l<numLabels; l++) {
            if (predVector.get(l) == 1.0) {
                predLabel.addLabel(l);
            }
        }

        return predLabel;
    }

    public int getNumSample() {
        return numSample;
    }


    @Override
    public FeatureList getFeatureList() {
        return null;
    }

    @Override
    public LabelTranslator getLabelTranslator() {
        return null;
    }


    public double clusterConditionalProb(Vector vector, int clusterIndex){
        double prob = 1;
        for (int l=0;l< numLabels;l++){
            BinomialDistribution distribution = distributions[clusterIndex][l];
            prob *= distribution.probability((int)vector.get(l));
        }
        return prob;
    }

    /**
     * return the clusterConditionalProb for each cluster.
     * @param vector
     * @return
     */
    public double[] clusterConditionalProbArr(Vector vector){
        double[] probArr = new double[numClusters];

        for (int clusterIndex=0; clusterIndex<numClusters; clusterIndex++) {
            probArr[clusterIndex] = clusterConditionalProb(vector, clusterIndex);
        }
        return probArr;
    }
}
