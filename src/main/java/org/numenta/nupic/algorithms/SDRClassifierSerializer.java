/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014, Numenta, Inc.  Unless you have an agreement
 * with Numenta, Inc., for a separate license for this software code, the
 * following terms and conditions apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero Public License for more details.
 *
 * You should have received a copy of the GNU Affero Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 *
 * http://numenta.org/licenses/
 * ---------------------------------------------------------------------
 */

package org.numenta.nupic.algorithms;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import no.uib.cipr.matrix.Matrices;
import no.uib.cipr.matrix.sparse.FlexCompRowMatrix;
import org.numenta.nupic.util.Tuple;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

public class SDRClassifierSerializer extends JsonSerializer<SDRClassifier> {

    @Override
    public void serialize(SDRClassifier sdrc, JsonGenerator jgen, SerializerProvider serializerProvider)
            throws IOException, JsonProcessingException {

        jgen.writeStartObject();
        jgen.writeNumberField("alpha", sdrc.alpha);
        jgen.writeNumberField("actValueAlpha", sdrc.actValueAlpha);
        jgen.writeNumberField("learnIteration", sdrc.learnIteration);
        jgen.writeNumberField("recordNumMinusLearnIteration", sdrc.recordNumMinusLearnIteration);
        jgen.writeNumberField("maxInputIdx", sdrc.maxInputIdx);
        jgen.writeNumberField("maxBucketIdx", sdrc.maxBucketIdx);

        StringBuilder sb = new StringBuilder();
        for(int i : sdrc.steps.toArray()) {
            sb.append(i).append(",");
        }
        sb.setLength(sb.length() - 1);
        jgen.writeStringField("steps", sb.toString());

        sb = new StringBuilder();
        for(Tuple t : sdrc.patternNZHistory) {
            sb.append(t.get(0)).append("-").append(Arrays.toString((int[])t.get(1))).append(";");
        }
        sb.setLength(sb.length() - 1);
        jgen.writeStringField("patternNZHistory", sb.toString());

        sb = new StringBuilder();
        for(Map.Entry<Integer, FlexCompRowMatrix> entry : sdrc.weightMatrix.entrySet()) {
            sb.append(entry.getKey()).append("-");
            double[][] matrix = Matrices.getArray(entry.getValue());
            jgen.writeStartArray();
            for(int row = 0; row < matrix.length; row++) {
                sb.append(Arrays.toString(matrix[row])).append(", ");
            }
            sb.setLength(sb.length() - 2);
            jgen.writeEndArray();
            sb.append(";");
        }
        sb.setLength(sb.length() - 1);
        jgen.writeStringField("weightMatrix", sb.toString());

        jgen.writeArrayFieldStart("actualValues");
        for(Object o : sdrc.actualValues) {
            jgen.writeObject(o);
        }
        jgen.writeEndArray();
        jgen.writeEndObject();
    }
}
