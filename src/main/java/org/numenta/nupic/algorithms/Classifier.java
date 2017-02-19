package org.numenta.nupic.algorithms;

import java.util.Map;

/**
 * Classifier is an interface for Classifier types used to predict future inputs
 * to the system, such as {@link CLAClassifier} or {@link SDRClassifier}.
 */
public interface Classifier {
    public <T> Classification<T> compute(int                 recordNum,
                                         Map<String, Object> classification,
                                         int[]               patternNZ,
                                         boolean             learn,
                                         boolean             infer);
}
