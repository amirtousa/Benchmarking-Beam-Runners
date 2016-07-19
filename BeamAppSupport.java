package benchmark.flinkspark.flink;

import com.lambdaworks.redis.RedisConnection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * Static supporty methods
 * 
 */
public class BeamAppSupport {
   
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

    public static String MYgetOrCreateSeg(Map<String, String> mt, RedisConnection<String, String> connection) { 
    	long ltime = Long.parseLong(mt.get("time"));
    	String segKey = String.format("%s-%s-%s-%s", mt.get("xWay"), mt.get("dir"), mt.get("seg"), (ltime / 60 + 1));  // Oh, duh, of COURSE you need the parens, otherwise you get time/60 (i.e. 0) + 1 => 01, not 1
        
        // Create a new record for a particular seg+min key for this xway+dir if it doesn't exist
        if (((Boolean)(connection.hexists("segSumSpeeds", segKey))).booleanValue() ==  false  &&
        	((Boolean)(connection.hexists("segSumNumReadings", segKey))).booleanValue() ==  false &&
        			((Boolean)(connection.hexists("segCarIdSet", segKey))).booleanValue() ==  false	)			
        {
        	connection.hset("segSumSpeeds", segKey, "0");
	        connection.hset("segSumNumReadings", segKey, "0");
	        connection.sadd(segKey, mt.get("carId"));
	        connection.hset("segCarIdSet", segKey, ""); ///this will overwrite it!!!
        }        
        return segKey;
    }
    
    
    public static String MYcreateCarIfNotExists(Map<String, String> mt, RedisConnection<String, String> connection) {
    	String carLine = null;
    	if (((Boolean)(connection.hexists("currentcars", mt.get("carId")))).booleanValue() ==  false){
            carLine = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s", mt.get("carId"), "-1", "-1", "-1", "-1", "-1", "-1", "-1", "0", "0");
            connection.hset("currentcars", mt.get("carId"), carLine);
        } else {
        	String val = connection.hget("currentcars", mt.get("carId"));
            carLine = val;
            String[] tokens = val.split(",");
            if (mt.get("lane").equals("0") && (Long.parseLong(mt.get("time")) > Long.parseLong(tokens[1]) + 60)) {  // Check if the currentCar is a re-entrant car.  If it is reset its values.
                carLine = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s", mt.get("carId"), "-1", "-1", "-1", "-1", "-1", "-1", "-1", "0", "0");
                //connection.set(mt.get("carId"), carLine);
                connection.hset("currentcars", mt.get("carId"), carLine);
            }
        }
        return carLine;
    }

    public static boolean MYcreateStoppedCar(String stoppedKey, String carId, RedisConnection<String, String> connection) {  // Return true if a new SimpleTopology.stopped.car was added to 'this.SimpleTopology.stopped.
    	String val = null;
    	if (((Boolean)(connection.hexists("stoppedcars", stoppedKey))).booleanValue() ==  false){
            String stoppedLine = String.format("%s,%s", carId, "-1");
            connection.hset("stoppedcars",stoppedKey, stoppedLine);
            return true;
        } else {
        	val = connection.hget("stoppedcars",stoppedKey);
            String[] tokens = val.split(",");
            if (carId.equals(tokens[0])) return false;
            if (carId.equals(tokens[1])) return false;
            if (tokens[1].equals("-1")) {
                String stoppedLine = String.format("%s,%s", tokens[0], carId);
                connection.hset("stoppedcars",stoppedKey, stoppedLine);
                return true;
            }
        }
        return false;
    }
    
    public static void MYcreateAccident(String stoppedKey, String accidentKey, String time, RedisConnection<String, String> connection) {
        String val = null; 
        if (((Boolean)(connection.hexists("stoppedcars", stoppedKey))).booleanValue() ==  true){
        	val = connection.hget("stoppedcars", stoppedKey);
            String[] tokens = val.split(",");
            boolean bolAcc = ((Boolean)(connection.hexists("accidentcars", accidentKey))).booleanValue();
            if (!tokens[0].equals("-1") && !tokens[1].equals("-1") && bolAcc == false) {
                String accLine = String.format("%s,%s,%s,%s", time, "-1", tokens[0], tokens[1]);
                connection.hset("accidentcars", accidentKey, accLine);
            }
        }
    }
    

    public static int MYgetNumV(String lastMinKey, RedisConnection<String, String> connection) {
        if (((Boolean)(connection.hexists("segCarIdSet", lastMinKey))).booleanValue() ==  true){
        	Set<String>  mp = new HashSet();
        	mp = connection.smembers(lastMinKey);
        	int msize = mp.size();
        	return msize;
        }
        return 0;
    }


    public static int calcToll(int numv, RedisConnection<String, String> connection) {
        return (int) (2 * Math.pow(50 - numv, 2));
    }

  
    public static int MYgetLav(Map<String, String> mt, int min, RedisConnection<String, String> connection) {
        int totalSpeed = 0, totalSpeedReadings = 0;
        String lavKey;
        for (int i = 1; i < 6; i++) {
            lavKey = mt.get("xWay") + "-" + mt.get("dir") + "-"+mt.get("seg") + "-" +  String.valueOf((min - i));            
            if (((Boolean)(connection.hexists("segSumSpeeds", lavKey))).booleanValue() ==  true){
            	totalSpeed += Integer.parseInt(connection.hget("segSumSpeeds", lavKey));
            }
            if (((Boolean)(connection.hexists("segSumNumReadings", lavKey))).booleanValue() ==  true){
            	totalSpeedReadings += Integer.parseInt(connection.hget("segSumNumReadings", lavKey));
            }
        }

        if (totalSpeedReadings > 0) return Math.round(totalSpeed / ((float) totalSpeedReadings));
        else return 0;
    }
    
    
    
    	public static int MYinAccidentZone(Map<String, String> mt, int min, RedisConnection<String, String> connection) {
            String k;
            //AMIR System.out.println("XXXX UtilitySL: inAccidentZone enter ");
//            Accident accident;
            for (int i = 0; i < 5; i++) {
                if (mt.get("dir").equals("0")) {
                    k = mt.get("xWay") + "-" + mt.get("dir") + "-" + (Integer.parseInt(mt.get("seg")) + i);
                } else {
                    k = mt.get("xWay") + "-" + mt.get("dir") + "-" + (Integer.parseInt(mt.get("seg")) - i);
                }
                String val = null; //connection.get(k);
                if (((Boolean)(connection.hexists("accidentcars", k))).booleanValue() ==  true){
                	val = connection.hget("accidentcars", k);
              //  if (val != null) {
                    String[] tokens = val.split(",");
                    int accNotiThresholdMin = Integer.parseInt(tokens[0]) / 60 + 2;
                    int accClearMin = Integer.parseInt(tokens[1]) / 60 + 1;
                    if (!tokens[1].equals("-1") && accNotiThresholdMin > accClearMin) continue;
                    if ((min >= accNotiThresholdMin && tokens[1].equals("-1")) ||
                            (min <= accClearMin && !tokens[1].equals("-1"))) {
                        return Integer.parseInt(k.split("-")[2]);
                    }
                }
            }
            return -1;
        }    	

    public static void MYassessToll(String carId, String time, RedisConnection<String, String> connection, RedisConnection<String, String> tolls) {
        String val = connection.get(carId);
        if (((Boolean)(connection.hexists("tolls", carId))).booleanValue() ==  false){
            String[] tokens = val.split(",");
            tolls.rpush(carId + "-tolls", time, tokens[9]);
        }
    }
    
    public static boolean isAnomalousCar(Map<String, String> mt, String[] tokens) {
        if (tokens[4].equals("4") && !mt.get("lane").equals("0")) {
            return true;
        };
        return false;
    }

    public static boolean MYremoveStoppedIfAny(String prevStoppedKey, Map<String, String> mt, RedisConnection<String, String> connection) {
        String val = null; 
        if (((Boolean)(connection.hexists("stoppedcars", prevStoppedKey))).booleanValue() ==  true){
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
       

   public static boolean MYclearAccidentIfAny(String prevAccidentKey, Map<String, String> mt, RedisConnection<String, String> connection) {
        String val = null; 
        if (((Boolean)(connection.hexists("accidentcars", prevAccidentKey))).booleanValue() ==  true){
        	val = connection.hget("accidentcars", prevAccidentKey);
            String[] tokens = val.split(",");
            if (tokens[1].equals("-1")) {
                if (mt.get("carId").equals(tokens[2]) || mt.get("carId").equals(tokens[3])) {
                    String accLine = String.format("%s,%s,%s,%s", tokens[0], mt.get("time"), tokens[2], tokens[3]);
                    connection.hset("accidentcars",prevAccidentKey, accLine);
                    return true;
                }
            }
        }
        return false;
    }
        
}
