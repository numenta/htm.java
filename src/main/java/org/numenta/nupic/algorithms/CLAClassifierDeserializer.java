/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014, Numenta, Inc.  Unless you have an agreement
 * with Numenta, Inc., for a separate license for this software code, the
 * following terms and conditions apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 *
 * http://numenta.org/licenses/
 * ---------------------------------------------------------------------
 */

package org.numenta.nupic.algorithms;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.numenta.nupic.util.Deque;
import org.numenta.nupic.util.Tuple;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class CLAClassifierDeserializer extends JsonDeserializer<CLAClassifier> {
	@Override
	public CLAClassifier deserialize(JsonParser jp, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		
		ObjectCodec oc = jp.getCodec();
        JsonNode node = oc.readTree(jp);
        
        CLAClassifier retVal = new CLAClassifier();
        retVal.alpha = node.get("alpha").asDouble();
        retVal.actValueAlpha = node.get("actValueAlpha").asDouble();
        retVal.learnIteration = node.get("learnIteration").asInt();
        retVal.recordNumMinusLearnIteration = node.get("recordNumMinusLearnIteration").asInt();
        retVal.maxBucketIdx = node.get("maxBucketIdx").asInt();
        
        String[] steps = node.get("steps").asText().split(",");
        TIntList t = new TIntArrayList();
        for(String step : steps) {
        	t.add(Integer.parseInt(step));
        }
        retVal.steps = t;
        
        String[] tupleStrs = node.get("patternNZHistory").asText().split(";");
        Deque<Tuple> patterns = new Deque<Tuple>(tupleStrs.length);
        for(String tupleStr : tupleStrs) {
        	String[] tupleParts = tupleStr.split("-");
        	int iteration = Integer.parseInt(tupleParts[0]);
        	String pattern = tupleParts[1].substring(1, tupleParts[1].indexOf("]")).trim();
        	String[] indexes = pattern.split(",");
        	int[] indices = new int[indexes.length];
        	for(int i = 0;i < indices.length;i++) {
        		indices[i] = Integer.parseInt(indexes[i].trim());
        	}
        	Tuple tup = new Tuple(iteration, indices);
        	patterns.append(tup);
        }
        retVal.patternNZHistory = patterns;
        
        Map<Tuple, BitHistory> bitHistoryMap = new HashMap<Tuple, BitHistory>();
        String[] bithists = node.get("activeBitHistory").asText().split(";");
        for(String bh : bithists) {
        	String[] parts = bh.split("-");
        	
        	String[] left = parts[0].split(",");
        	Tuple iteration = new Tuple(Integer.parseInt(left[0].trim()), Integer.parseInt(left[1].trim()));
        	
        	BitHistory bitHistory = new BitHistory();
        	String[] right = parts[1].split("=");
        	bitHistory.id = right[0].trim();
        	
        	TDoubleList dubs = new TDoubleArrayList();
        	String[] stats = right[1].substring(1, right[1].indexOf("}")).trim().split(",");
        	for(int i = 0;i < stats.length;i++) {
        		dubs.add(Double.parseDouble(stats[i].trim()));
        	}
        	bitHistory.stats = dubs;
        	
        	bitHistory.lastTotalUpdate = Integer.parseInt(right[2].trim());
        	
        	bitHistoryMap.put(iteration, bitHistory);
        }
        retVal.activeBitHistory = bitHistoryMap;
        
        ArrayNode jn = (ArrayNode)node.get("actualValues");
        List<Object> l = new ArrayList<Object>();	
        for(int i = 0;i < jn.size();i++) {
        	JsonNode n = jn.get(i);
        	try {
        		double d = Double.parseDouble(n.asText().trim());
        		l.add(d);
        	}catch(Exception e) {
        		l.add(n.asText().trim());
        	}
        }
        retVal.actualValues = l;
        
        //Go back and set the classifier on the BitHistory objects
        for(Tuple tuple : bitHistoryMap.keySet()) {
        	bitHistoryMap.get(tuple).classifier = retVal;
        }
        
        return retVal;
	}
}
