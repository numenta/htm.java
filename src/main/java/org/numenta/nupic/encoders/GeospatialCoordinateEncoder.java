package org.numenta.nupic.encoders;

import java.util.ArrayList;
import java.util.List;

import org.numenta.nupic.util.Tuple;

public class GeospatialCoordinateEncoder extends CoordinateEncoder {
	private int scale;
	private int timestep;
	
	
	public GeospatialCoordinateEncoder() {
		
	}
	
	/**
	 * Returns a builder for building ScalarEncoders. 
	 * This builder may be reused to produce multiple builders
	 * 
	 * @return a {@code CoordinateEncoder.Builder}
	 */
	public static GeospatialCoordinateEncoder.Builder geobuilder() {
		return new GeospatialCoordinateEncoder.Builder();
	}
	
	/**
	 * Returns a {@link List} of {@link Tuple}s containing
	 * [String:"name", int:offset]
	 * 
	 * @return List of Tuple(String, int)'s
	 * @see Encoder for more information
	 */
	@Override
	public List<Tuple> getDescription() {
		List<Tuple> retVal = new ArrayList<Tuple>();
		Tuple desc = new Tuple(2, "longitude", 0);
		Tuple desc2 = new Tuple(2, "lattitude", 1);
		Tuple desc3 = new Tuple(2, "speed", 2);
		retVal.add(desc);
		retVal.add(desc2);
		retVal.add(desc3);
		
		return retVal;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void encodeIntoArray(Tuple inputData, int[] output) {
		double longitude = (double)inputData.get(0);
		double lattitude = (double)inputData.get(1);
		double speed = (double)inputData.get(2);
		int[] coordinate = coordinateForPosition(longitude, lattitude);
		double radius = radiusForSpeed(speed);
		
		super.encodeIntoArray(new Tuple(2, coordinate, radius), output);
	}
	
	public int[] coordinateForPosition(double longitude, double lattitude) {
		double[] coordinate = toMercator(longitude, lattitude);
		coordinate[0] /= scale;
		coordinate[1] /= scale;
		return new int[] { (int)coordinate[0], (int)coordinate[1] };
	}
	
	/**
	 * Returns coordinates converted to Mercator Spherical projection
	 * 
	 * @param lon	the longitude
	 * @param lat	the lattitude
	 * @return
	 */
	protected double[] toMercator(double lon, double lat) {
		double x = lon * 20037508.34d / 180;
		double y = Math.log(Math.tan((90 + lat) * Math.PI / 360)) / (Math.PI / 180);
		y = y * 20037508.34d / 180;
		
		return new double[] { x, y};
	}
	
	/**
	 * Returns coordinates converted to Long/Lat from Mercator Spherical projection
	 * 
	 * @param lon	the longitude
	 * @param lat	the lattitude
	 * @return
	 */
	protected double[] inverseMercator(double x, double y) {
		double lon = (x / 20037508.34d) * 180;
		double lat = (y / 20037508.34d) * 180;
		
		lat = 180/Math.PI * (2 * Math.atan(Math.exp(lat * Math.PI / 180)) - Math.PI / 2);
		
		return new double[] { lon, lat };
	}
	
	/**
	 * Tries to get the encodings of consecutive readings to be
     * adjacent with some overlap.
     * 
	 * @param speed	Speed (in meters per second)
	 * @return	Radius for given speed
	 */
	public double radiusForSpeed(double speed) {
		double overlap = 1.5;
		double coordinatesPerTimestep = speed * timestep / scale;
		int radius = (int)Math.round(coordinatesPerTimestep / 2D * overlap); 
		int minRadius = (int)Math.ceil((Math.sqrt(w) - 1) / 2);
		return Math.max(radius, minRadius);
	}
	
	/**
	 * Returns a {@link EncoderBuilder} for constructing {@link GeospatialCoordinateEncoder}s
	 * 
	 * The base class architecture is put together in such a way where boilerplate
	 * initialization can be kept to a minimum for implementing subclasses, while avoiding
	 * the mistake-proneness of extremely long argument lists.
	 * 
	 * @see ScalarEncoder.Builder#setStuff(int)
	 */
	public static class Builder extends Encoder.Builder<GeospatialCoordinateEncoder.Builder, GeospatialCoordinateEncoder> {
		private int scale;
		private int timestep;
		
		private Builder() {}

		@Override
		public GeospatialCoordinateEncoder build() {
			//Must be instantiated so that super class can initialize 
			//boilerplate variables.
			encoder = new GeospatialCoordinateEncoder();
			
			//Call super class here
			super.build();
			
			////////////////////////////////////////////////////////
			//  Implementing classes would do setting of specific //
			//  vars here together with any sanity checking       //
			////////////////////////////////////////////////////////
			if(scale == 0 || timestep == 0) {
				throw new IllegalStateException("Scale or Timestep not set");
			}
			
			((GeospatialCoordinateEncoder)encoder).scale = scale;
			((GeospatialCoordinateEncoder)encoder).timestep = timestep;
			
			if(w <= 0 || w % 2 == 0) {
				throw new IllegalArgumentException("w must be odd, and must be a positive integer");
			}
			
			if(n <= 6 * w) {
				throw new IllegalArgumentException(
					"n must be an int strictly greater than 6*w. For " +
                       "good results we recommend n be strictly greater than 11*w");
			}
			
			if(name == null || name.equals("None")) {
				name = new StringBuilder("[").append(n).append(":").append(w).append("]").toString();
			}
			
			return (GeospatialCoordinateEncoder)encoder;
		}
		
		/**
		 * Scale of the map, as measured by
         * distance between two coordinates
         * (in meters per dimensional unit)
		 * @param scale
		 * @return
		 */
		public Builder scale(int scale) {
			this.scale = scale;
			return (Builder)this;
		}
		
		/**
		 * Time between readings
		 * @param timestep
		 * @return
		 */
		public Builder timestep(int timestep) {
			this.timestep = timestep;
			return (Builder)this;
		}
	}
}
