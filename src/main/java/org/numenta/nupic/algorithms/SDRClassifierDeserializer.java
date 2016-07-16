package org.numenta.nupic.algorithms;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import no.uib.cipr.matrix.sparse.FlexCompRowMatrix;
import org.numenta.nupic.util.Tuple;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.numenta.nupic.util.Deque;

public class SDRClassifierDeserializer extends JsonDeserializer<SDRClassifier> {

    @Override
    public SDRClassifier deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {

        ObjectCodec oc = jp.getCodec();
        JsonNode node = oc.readTree(jp);

        SDRClassifier retVal = new SDRClassifier();
        retVal.alpha = node.get("alpha").asDouble();
        retVal.actValueAlpha = node.get("actValueAlpha").asDouble();
        retVal.learnIteration = node.get("learnIteration").asInt();
        retVal.recordNumMinusLearnIteration = node.get("recordNumMinusLearnIteration").asInt();
        retVal.maxInputIdx = node.get("maxInputIdx").asInt();
        retVal.maxBucketIdx = node.get("maxBucketIdx").asInt();

        String[] steps = node.get("steps").asText().split(",");
        TIntList t = new TIntArrayList();
        for (String step : steps) {
            t.add(Integer.parseInt(step));
        }
        retVal.steps = t;

        String[] tupleStrs = node.get("patternNZHistory").asText().split(";");
        Deque<Tuple> patterns = new Deque<Tuple>(tupleStrs.length);
        for (String tupleStr : tupleStrs) {
            String[] tupleParts = tupleStr.split("-");
            int iteration = Integer.parseInt(tupleParts[0]);
            String pattern = tupleParts[1].substring(1, tupleParts[1].indexOf("]")).trim();
            String[] indexes = pattern.split(",");
            int[] indices = new int[indexes.length];
            for (int i = 0; i < indices.length; i++) {
                indices[i] = Integer.parseInt(indexes[i].trim());
            }
            Tuple tup = new Tuple(iteration, indices);
            patterns.append(tup);
        }
        retVal.patternNZHistory = patterns;

        Map<Integer, FlexCompRowMatrix> weightMatrixMap = new HashMap<Integer, FlexCompRowMatrix>();
        String[] matrices = node.get("weightMatrix").asText().split(";");
        for (String matrix : matrices) {
            String[] parts = matrix.split("-");

            int nSteps = Integer.parseInt(parts[0]);

            String[] csvRows = parts[1].replace("[[", "").replace("]]", "").split("], \\[");
            FlexCompRowMatrix weightMatrix = new FlexCompRowMatrix(0, 0);
            for (int i = 0; i < csvRows.length; i++) {
                String[] strRow = csvRows[i].trim().split(", ");
                double[] dblRow = new double[strRow.length];
                for (int j = 0; j < strRow.length; j++) {
                    dblRow[j] = Double.valueOf(strRow[j]);
                }
                weightMatrix.addRow(dblRow);
            }
            weightMatrixMap.put(nSteps, weightMatrix);
        }
        retVal.weightMatrix = weightMatrixMap;

        ArrayNode jn = (ArrayNode) node.get("actualValues");
        List<Object> l = new ArrayList<Object>();
        for (int i = 0; i < jn.size(); i++) {
            JsonNode n = jn.get(i);
            try {
                double d = Double.parseDouble(n.asText().trim());
                l.add(d);
            } catch (Exception e) {
                l.add(n.asText().trim());
            }
        }
        retVal.actualValues = l;

        return retVal;
    }
}
