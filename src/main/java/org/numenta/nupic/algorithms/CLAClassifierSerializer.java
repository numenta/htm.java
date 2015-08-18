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

import java.io.IOException;
import java.util.Arrays;

import org.numenta.nupic.util.Tuple;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class CLAClassifierSerializer extends JsonSerializer<CLAClassifier> {

	@Override
	public void serialize(CLAClassifier cla, JsonGenerator jgen, SerializerProvider arg2) throws IOException,
			JsonProcessingException {
		
		jgen.writeStartObject();
		jgen.writeNumberField("alpha", cla.alpha);
		jgen.writeNumberField("actValueAlpha", cla.actValueAlpha);
		jgen.writeNumberField("learnIteration", cla.learnIteration);
		jgen.writeNumberField("recordNumMinusLearnIteration", cla.recordNumMinusLearnIteration);
		jgen.writeNumberField("maxBucketIdx", cla.maxBucketIdx);
		
		StringBuilder sb = new StringBuilder();
		for(int i : cla.steps.toArray()) {
			sb.append(i).append(",");
		}
		sb.setLength(sb.length() - 1);
		jgen.writeStringField("steps", sb.toString());
		
		sb = new StringBuilder();
		for(Tuple t : cla.patternNZHistory) {
			sb.append(t.get(0)).append("-").append(Arrays.toString((int[])t.get(1))).append(";");
		}
		sb.setLength(sb.length() - 1);
		jgen.writeStringField("patternNZHistory", sb.toString());
		
		sb = new StringBuilder();
		for(Tuple t : cla.activeBitHistory.keySet()) {
			sb.append(t.get(0)).append(",").append(t.get(1)).append("-");
			BitHistory bh = cla.activeBitHistory.get(t);
			sb.append(bh.id).append("=").append(bh.stats).append("=").append(bh.lastTotalUpdate)
			.append(";");
		}
		sb.setLength(sb.length() - 1);
		jgen.writeStringField("activeBitHistory", sb.toString());
		
		jgen.writeArrayFieldStart("actualValues");
		for(Object o : cla.actualValues) {
			jgen.writeObject(o);
		}
		jgen.writeEndArray();
		jgen.writeEndObject();
		
	}
	
}
