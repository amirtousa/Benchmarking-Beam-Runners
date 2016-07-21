package benchmark.flinkspark.flink;

import com.lambdaworks.redis.RedisConnection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Amir  Bahmanyari
 *
 */
public class BeamAppSupport {

	/**
	 * @param tokens
	 * @return
	 */
	public static Map<String, String> createMT(String[] tokens) {
		Map<String, String> m = new HashMap<String, String>();
		m.put("type", tokens[0]);
		m.put("time", tokens[1]);
		m.put("carId", tokens[2]);
		m.put("speed", tokens[3]);
		m.put("xWay", tokens[4]);
		m.put("lane", tokens[5]);
		m.put("dir", tokens[6]);
		m.put("seg", tokens[7]);
		m.put("pos", tokens[8]);
		m.put("qid", tokens[9]);
		m.put("day", tokens[14]);
		return m;
	}

	/**
	 * @param mt
	 * @param connection
	 * @return
	 */
	public static String LRGetOrCreateSeg(Map<String, String> mt, RedisConnection<String, String> connection) {
		long ltime = Long.parseLong(mt.get("time"));
		String segKey = String.format("%s-%s-%s-%s", mt.get("xWay"), mt.get("dir"), mt.get("seg"), (ltime / 60 + 1)); 
		// Create a new record for a particular seg+min key for this xway+dir if
		// it doesn't exist
		if (((Boolean) (connection.hexists("segSumSpeeds", segKey))).booleanValue() == false
				&& ((Boolean) (connection.hexists("segSumNumReadings", segKey))).booleanValue() == false
				&& ((Boolean) (connection.hexists("segCarIdSet", segKey))).booleanValue() == false) {
			connection.hset("segSumSpeeds", segKey, "0");
			connection.hset("segSumNumReadings", segKey, "0");
			connection.sadd(segKey, mt.get("carId"));
			connection.hset("segCarIdSet", segKey, ""); /// this will overwrite
														/// it!!!?? chech Redis docs on "hset". Maybe.
		}
		return segKey;
	}

	/**
	 * @param mt
	 * @param connection
	 * @return
	 */
	public static String LRCreateCarIfNotExists(Map<String, String> mt, RedisConnection<String, String> connection) {
		String carLine = null;
		if (((Boolean) (connection.hexists("currentcars", mt.get("carId")))).booleanValue() == false) {
			carLine = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s", mt.get("carId"), "-1", "-1", "-1", "-1", "-1",
					"-1", "-1", "0", "0");
			connection.hset("currentcars", mt.get("carId"), carLine);
		} else {
			String val = connection.hget("currentcars", mt.get("carId"));
			carLine = val;
			String[] tokens = val.split(",");
			//Check if its a re-ent car, if so, reset it
			if (mt.get("lane").equals("0") && (Long.parseLong(mt.get("time")) > Long.parseLong(tokens[1]) + 60)) { 
				carLine = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s", mt.get("carId"), "-1", "-1", "-1", "-1", "-1",
						"-1", "-1", "0", "0");
				// connection.set(mt.get("carId"), carLine);
				//Get current car's info from Redis
				connection.hset("currentcars", mt.get("carId"), carLine);
			}
		}
		return carLine;
	}

	/**
	 * @param stoppedKey
	 * @param carId
	 * @param connection
	 * @return
	 */
	public static boolean LRCreateStoppedCar(String stoppedKey, String carId,
			RedisConnection<String, String> connection) { 
		String val = null;
		if (((Boolean) (connection.hexists("stoppedcars", stoppedKey))).booleanValue() == false) {
			String stoppedLine = String.format("%s,%s", carId, "-1");
			connection.hset("stoppedcars", stoppedKey, stoppedLine);
			return true;
		} else {
			val = connection.hget("stoppedcars", stoppedKey);
			String[] tokens = val.split(",");
			if (carId.equals(tokens[0]))
				return false;
			if (carId.equals(tokens[1]))
				return false;
			if (tokens[1].equals("-1")) {
				String stoppedLine = String.format("%s,%s", tokens[0], carId);
				connection.hset("stoppedcars", stoppedKey, stoppedLine);
				return true;
			}
		}
		return false;
	}

	/**
	 * @param stoppedKey
	 * @param accidentKey
	 * @param time
	 * @param connection
	 */
	public static void LRCreateAccident(String stoppedKey, String accidentKey, String time,
			RedisConnection<String, String> connection) {
		String val = null;
		if (((Boolean) (connection.hexists("stoppedcars", stoppedKey))).booleanValue() == true) {
			val = connection.hget("stoppedcars", stoppedKey);
			String[] tokens = val.split(",");
			boolean bolAcc = ((Boolean) (connection.hexists("accidentcars", accidentKey))).booleanValue();
			if (!tokens[0].equals("-1") && !tokens[1].equals("-1") && bolAcc == false) {
				String accLine = String.format("%s,%s,%s,%s", time, "-1", tokens[0], tokens[1]);
				connection.hset("accidentcars", accidentKey, accLine);
			}
		}
	}

	/**
	 * @param lastMinKey
	 * @param connection
	 * @return
	 */
	public static int LRGetNumV(String lastMinKey, RedisConnection<String, String> connection) {
		if (((Boolean) (connection.hexists("segCarIdSet", lastMinKey))).booleanValue() == true) {
			Set<String> mp = new HashSet();
			mp = connection.smembers(lastMinKey);
			int msize = mp.size();
			return msize;
		}
		return 0;
	}

	/**
	 * @param numv
	 * @param connection
	 * @return
	 */
	public static int calcToll(int numv, RedisConnection<String, String> connection) {
		return (int) (2 * Math.pow(50 - numv, 2));
	}

	/**
	 * @param mt
	 * @param min
	 * @param connection
	 * @return
	 */
	public static int LRGetLav(Map<String, String> mt, int min, RedisConnection<String, String> connection) {
		int totalSpeed = 0, totalSpeedReadings = 0;
		String lavKey;
		for (int i = 1; i < 6; i++) {
			lavKey = mt.get("xWay") + "-" + mt.get("dir") + "-" + mt.get("seg") + "-" + String.valueOf((min - i));
			if (((Boolean) (connection.hexists("segSumSpeeds", lavKey))).booleanValue() == true) {
				totalSpeed += Integer.parseInt(connection.hget("segSumSpeeds", lavKey));
			}
			if (((Boolean) (connection.hexists("segSumNumReadings", lavKey))).booleanValue() == true) {
				totalSpeedReadings += Integer.parseInt(connection.hget("segSumNumReadings", lavKey));
			}
		}

		if (totalSpeedReadings > 0)
			return Math.round(totalSpeed / ((float) totalSpeedReadings));
		else
			return 0;
	}

	/**
	 * @param mt
	 * @param min
	 * @param connection
	 * @return
	 */
	public static int LRInAccidentZone(Map<String, String> mt, int min, RedisConnection<String, String> connection) {
		String k;
		// Accident accident;
		for (int i = 0; i < 5; i++) {
			if (mt.get("dir").equals("0")) {
				k = mt.get("xWay") + "-" + mt.get("dir") + "-" + (Integer.parseInt(mt.get("seg")) + i);
			} else {
				k = mt.get("xWay") + "-" + mt.get("dir") + "-" + (Integer.parseInt(mt.get("seg")) - i);
			}
			String val = null; // connection.get(k);
			if (((Boolean) (connection.hexists("accidentcars", k))).booleanValue() == true) {
				val = connection.hget("accidentcars", k);
				String[] tokens = val.split(",");
				int accNotiThresholdMin = Integer.parseInt(tokens[0]) / 60 + 2;
				int accClearMin = Integer.parseInt(tokens[1]) / 60 + 1;
				if (!tokens[1].equals("-1") && accNotiThresholdMin > accClearMin)
					continue;
				if ((min >= accNotiThresholdMin && tokens[1].equals("-1"))
						|| (min <= accClearMin && !tokens[1].equals("-1"))) {
					return Integer.parseInt(k.split("-")[2]);
				}
			}
		}
		return -1;
	}

	/**
	 * @param carId
	 * @param time
	 * @param connection
	 * @param tolls
	 */
	public static void LRAssessToll(String carId, String time, RedisConnection<String, String> connection,
			RedisConnection<String, String> tolls) {
		String val = connection.get(carId);
		if (((Boolean) (connection.hexists("tolls", carId))).booleanValue() == false) {
			String[] tokens = val.split(",");
			tolls.rpush(carId + "-tolls", time, tokens[9]);
		}
	}

	/**
	 * @param mt
	 * @param tokens
	 * @return
	 */
	public static boolean isAnomalousCar(Map<String, String> mt, String[] tokens) {
		if (tokens[4].equals("4") && !mt.get("lane").equals("0")) {
			return true;
		};
		return false;
	}

	/**
	 * @param prevStoppedKey
	 * @param mt
	 * @param connection
	 * @return
	 */
	public static boolean LRRemoveStoppedIfAny(String prevStoppedKey, Map<String, String> mt,
			RedisConnection<String, String> connection) {
		String val = null;
		if (((Boolean) (connection.hexists("stoppedcars", prevStoppedKey))).booleanValue() == true) {
			val = connection.hget("stoppedcars", prevStoppedKey);
			String[] tokens = val.split(",");
			if (mt.get("carId").equals(tokens[0])) {
				if (tokens.length > 1) {
					String stoppedLine = String.format("%s", tokens[1]);
					connection.hset("stoppedcars", prevStoppedKey, stoppedLine);
					return true;
				} else {
					connection.hdel("stoppedcars", prevStoppedKey);
					return true;
				}
			}
			if (mt.get("carId").equals(tokens[1])) {
				connection.hdel(mt.get("carId"), "car2");
				String stoppedLine = String.format("%s", tokens[0]);
				connection.hset("stoppedcars", prevStoppedKey, stoppedLine);
				return true;
			}
		}
		return false;
	}
}
