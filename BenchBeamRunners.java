package benchmark.flinkspark.flink;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.RedisConnection;

import org.apache.beam.runners.flink.FlinkPipelineOptions;
import org.apache.beam.runners.flink.FlinkPipelineRunner;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.io.kafka.KafkaIO;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;

/**
 * Read Linear Road records come from Kafka server and process them in the provided inner class .
 *
 * Please pass the following arguments to the Beam Runner's run command:
 * 	--topic lroad --bootstrap.servers "kirk":9092 --zookeeper.connect "kafkahost":2181 --group.id eventsGroup
 *
 */

/**
 * @author Amir  Bahmanyari
 *
 */
public class BenchBeamRunners {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		// Create Beam Options for the Flink Runner.
		FlinkPipelineOptions options = PipelineOptionsFactory.as(FlinkPipelineOptions.class);
		// Set the Streaming engine as FlinkRunner
		options.setRunner(FlinkPipelineRunner.class);
		// This is a Streaming process (as opposed to Batch=false)
		options.setStreaming(true);
		//Create the DAG pipeline for parallel processing of independent LR records
		Pipeline p = Pipeline.create(options);
		//Kafka broker topic is identified as "lroad" 
		List<String> topics = Arrays.asList("lroad");
		int type3Processe = 0;
		
		// Invoke Beam KafkaIO to read Linear Road records from Kafka broker typed as "lroad".
		// Kafka is running in kirk at port 9092 by default.
		try {
			PCollection<KV<String, String>> kafkarecords = p
					.apply(KafkaIO.read().withBootstrapServers("kirk:9092").withTopics(topics)
							.withValueCoder(StringUtf8Coder.of()).withoutMetadata())
					.apply(ParDo.named("startBundle").of(
							new DoFn<KV<byte[], String>, KV<String, String>>() {
								private static final long serialVersionUID = 1L;
								RedisClient redisClient;
								RedisClient histsredis;
								RedisClient tollsredis;
								RedisConnection<String, String> connection = null;
								RedisConnection<String, String> tolls = null;
								RedisConnection<String, String> histClient = null;
								String line = null;
								String strkey = null;
								String _outfileName = null;
								String _outT3fileName = null;
								boolean bolEndOfFile = false;
								// PrintWriter _writer = null;
								PrintWriter _writerT3 = null;
								
								// This method is invoked upon receiving a record from Kafka broker.
								// ctx embeds the data string as per one record
								@Override
								public void processElement(ProcessContext ctx) throws Exception {
									Map<String, String> mt;
									//Drop the appended prefix by Kafka
									int posA = ctx.element().toString().lastIndexOf("KV{[], ");
									if (posA == -1) {
										line = ctx.element().toString();
									} else {
										int adjustedPosA = posA + "KV{[], ".length();
										line = ctx.element().toString().substring(adjustedPosA);
									}
									if (line != null){
										//Drop the appended } by Kafka
										line = line.replaceAll("}", "");
										try {
											if (null == connection || null == tolls || null == histClient) {
												// Create connection objects to redis
												connRedis();
												// Populate Redis with LR Historical data required at runtime
												if (null != histClient)
													populateHistRedis();
											}
										} catch (Exception ex) {
											ex.printStackTrace();
										}

										try {
											mt = BeamAppSupport.createMT(line.split(","));
										} catch (NullPointerException exp) {
											// do nothing;
											System.out.println("ADEBUG Did nothing in UtilitySL.createMT");
											return;
										}
										int type = Integer.parseInt(mt.get("type"));
										switch (type) {
										case 0:
											connection.incr("type0Seen");
											t0(mt);
											break;
										case 2:
											connection.incr("type2Seen");
											t2(mt);
											break;
										case 3:
											connection.incr("type3Seen");
											t3(mt);
											break;
										case 9999: // end of file
											bolEndOfFile = true;
											break;
										}

									} // of if
								}

								/**
								 * Populate Redis with historical contents from matchedTolls.dat file
								 */
								public void populateHistRedis() {
									String tollskeys;
									String[] Tollstokens;

									try {
										BufferedReader reader = new BufferedReader(
												new FileReader(new File("/tmp/matchedTolls.dat")));
										while ((line = reader.readLine()) != null) {
											Tollstokens = line.split(",");
											tollskeys = Tollstokens[0] + "-" + Tollstokens[1] + "-" + Tollstokens[2];
											histClient.hset("historics", tollskeys, Tollstokens[3]);
										}
										if (null != reader)
											reader.close();
									} catch (IOException ioexp) {
										ioexp.printStackTrace();
									}
									System.out.println("ADEBUG Finished loading historicals in Redis");

								}

								/**
								 * Create Redis connection objects
								 */
								public void connRedis() {
									if (null == connection) {
										redisClient = new RedisClient(RedisURI.create("redis://spock:6379"));
										connection = redisClient.connect();
									}
									if (null == tolls) {
										tollsredis = new RedisClient(RedisURI.create("redis://spock:6379"));
										tolls = tollsredis.connect();
									}
									if (null == histClient) {
										histsredis = new RedisClient(RedisURI.create("redis://spock:6379"));
										histClient = histsredis.connect();
									}
									connection.set("type0Processed", "0");
									connection.set("type1Processed", "0");
									connection.set("type2Processed", "0");
									connection.set("type3Processed", "0");
									connection.set("type0Seen", "0");
									connection.set("type2Seen", "0");
									connection.set("type3Seen", "0");
									
								}// of connRedis

								
								/**
								 * @param mt
								 * t0 is the type used to calculate tolls & emit notifications
								 */
								public void t0(Map<String, String> mt) {
									String val = null;
									String[] tokens = null;
									long startTime = System.currentTimeMillis();
									int min = Integer.parseInt(mt.get("time")) / 60 + 1;						
									String stoppedKey = String.format("%s-%s-%s-%s-%s", mt.get("xWay"), mt.get("dir"),
											mt.get("lane"), mt.get("seg"), mt.get("pos"));
									//Create a new Seg-Min combo. Make sure it doesnt already exist.
									String segKey = BeamAppSupport.LRGetOrCreateSeg(mt, connection); 																										
																										
									val = BeamAppSupport.LRCreateCarIfNotExists(mt, connection);
									if (val != null)
										tokens = val.split(",");
									else
										return; 
									if (BeamAppSupport.isAnomalousCar(mt, tokens) == true) {
										return;
									}
							        //connection.hset(mt.get("carId"), "carId", mt.get("carId")); 0
							        //connection.hset(mt.get("carId"), "lastTime", "-1"); 1
							        //connection.hset(mt.get("carId"), "lastSpeed", "-1"); 2
							        //connection.hset(mt.get("carId"), "lastXWay", "-1"); 3
							        //connection.hset(mt.get("carId"), "lastLane", "-1"); 4
							        //connection.hset(mt.get("carId"), "lastDir", "-1"); 5
							        //connection.hset(mt.get("carId"), "lastSeg", "-1"); 6
							        //connection.hset(mt.get("carId"), "lastPos", "-1"); 7
							        //connection.hset(mt.get("carId"), "xPos", "0"); 8
							        //connection.hset(mt.get("carId"), "lastToll", "0"); 9
									// SAME POSITION?
									if (tokens[7].equals(mt.get("pos")) && tokens[4].equals(mt.get("lane"))) { //42000
									//if (tokens[3].equals(mt.get("xWay")) && tokens[5].equals(mt.get("dir")) && tokens[7].equals(mt.get("pos")) && tokens[4].equals(mt.get("lane"))) {
										if (tokens[8].equals("3")) { // Already seen 3 times, create a Stopped car																		
											if (BeamAppSupport.LRCreateStoppedCar(stoppedKey, mt.get("carId"),
													connection)) {
												BeamAppSupport.LRCreateAccident(
														stoppedKey, String.format("%s-%s-%s", mt.get("xWay"),
																mt.get("dir"), mt.get("seg")),
														mt.get("time"), connection);
											}
										}
										tokens[8] = Integer.toString(Integer.parseInt(tokens[8]) + 1);
										// NEW POSITION
									} else { 
										String prevStoppedKey = String.format("%s-%s-%s-%s-%s", tokens[3], tokens[5],
												tokens[4], tokens[6], tokens[7]);
										BeamAppSupport.LRRemoveStoppedIfAny(prevStoppedKey, mt, connection);
										String prevAccidentKey = String.format("%s-%s-%s", tokens[3], tokens[5],
												tokens[6]);
										BeamAppSupport.LRClearAccidentIfAny(prevAccidentKey, mt, connection);
										tokens[8] = "1"; // Reset current car's number of times at this position

										// NEW POSITION BUT SAME SEGMENT
										if (mt.get("seg").equals(tokens[6])) {
											if (mt.get("lane").equals("4")) {
												tokens[4] = "4";
											}
											// NEW POSITION NEW SEGMENT, thats t0..need to send toll notifs
										} else {
											int currToll = 0;
											int numv = 0;
											int lav = 0;
										if (!(mt.get("lane").equals("4"))) {
										// testing 50k	if (tokens[3].equals(mt.get("xWay")) && tokens[5].equals(mt.get("dir")) && !(mt.get("lane").equals("4"))) {
												String lastMinKey = String.format("%s-%s-%s-%d", mt.get("xWay"),
														mt.get("dir"), mt.get("seg"), (min - 1));
												numv = BeamAppSupport.LRGetNumV(lastMinKey, connection);
												if (numv > 50)
													currToll = BeamAppSupport.calcToll(numv, connection);
												lav = BeamAppSupport.LRGetLav(mt, min, connection);
												if (lav >= 40)
													currToll = 0;
												// ACCIDENTS
												int accSeg = BeamAppSupport.LRInAccidentZone(mt, min, connection);
												String strOutput = "INIT_OUT";
												if (accSeg >= 0) {
													currToll = 0;
													strOutput = String.format("1,%s,%d,%s,%s,%s,%s\n", mt.get("time"),
															Integer.parseInt(mt.get("time"))
																	+ (System.currentTimeMillis() - startTime),
															mt.get("xWay"), accSeg, mt.get("dir"), mt.get("carId"));
													connection.incr("type1Processed");
												}
												strOutput = String.format("0,%s,%s,%d,%d,%d\n", mt.get("carId"),
														mt.get("time"), (System.currentTimeMillis() - startTime), lav,
														currToll);
												connection.incr("type0Processed");
											}
											// PREVIOUS TOLL
											if (Integer.parseInt(tokens[9]) > 0) {
												tolls.rpush(tokens[0] + "-tolls", mt.get("time"), tokens[9]);
											}
											tokens[9] = Integer.toString(currToll);
										}
									}
									// Update car and segment info. Car info in redis
									String carLine = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s", tokens[0],
											mt.get("time"), mt.get("speed"), mt.get("xWay"), mt.get("lane"),
											mt.get("dir"), mt.get("seg"), mt.get("pos"), tokens[8], tokens[9]);
									connection.hset("currentcars", tokens[0], carLine);

									// added due to difference with Validator
									int removeMin = min - 6;
									String segRemovedKey = String.format("%s-%s-%s-%s", mt.get("xWay"), mt.get("dir"),
											mt.get("seg"), removeMin); 
									if (null != connection.hget("segSumSpeeds", segRemovedKey))
										connection.del("segSumSpeeds", segRemovedKey);
									if (null != connection.hget("segSumNumReadings", segRemovedKey))
										connection.del("segSumNumReadings", segRemovedKey);
									if (null != connection.hget("segCarIdSet", segRemovedKey))
										connection.del("segCarIdSet", segRemovedKey);

									if (null != connection.hget("segSumSpeeds", segKey))
										connection.hset("segSumSpeeds", segKey,
												String.valueOf(Integer.parseInt(connection.hget("segSumSpeeds", segKey))
														+ Integer.parseInt(mt.get("speed"))));
									if (null != connection.hget("segSumNumReadings", segKey))
										connection.hset("segSumNumReadings", segKey, String.valueOf(
												Integer.parseInt(connection.hget("segSumNumReadings", segKey)) + 1));

									if (((Boolean) (connection.hexists("segCarIdSet", segKey)))
											.booleanValue() == true) { 
										connection.smembers(segKey).add(mt.get("carId"));
									}
								}

								/**
								 * @param mt
								 * Type t3 LR record. Must match 120. Does.
								 */
								public void t3(Map<String, String> mt) {
									try {
										String k = mt.get("carId") + "-" + mt.get("day") + "-" + mt.get("xWay");
										// int toll = 0;
										if (((Boolean) (histClient.hexists("historics", k))).booleanValue() == true
												&& Integer.parseInt(mt.get("day")) != 0) {
											// toll = Integer.parseInt((String)
											// histClient.hget("historics", k));
											connection.incr("type3Processed");
											System.out.println("ADEBUG Number of Type3 Processed so far: "
													+ connection.get("type3Processed"));
											if (null != _writerT3
													&& Integer.parseInt(connection.get("type3Processed")) > 110)
												_writerT3.flush();
										}
									} catch (Exception exp) {
										// swallow, do nothing...
										System.out.println("ADEBUG Strange record: Did nothing in t3 " + exp.getMessage());
										// exp.printStackTrace(); if needed
										return; // skip this strange record
									}
								}
								
								/**
								 * @param mt
								 * Type t2 LR record. Will test later.
								 */
								public void t2(Map<String, String> mt) { 
									connection.incr("type2Processed");
								}

							}));
		} catch (Exception exp) {
			exp.printStackTrace();
		}
		System.out.printf("\n...ADEBUG Completed method");
		try {
			System.out.printf("\n...ADEBUG about to run pipeline");
			p.run(); // Start Beam Pipeline(s) in FlinkC Cluster
		} catch (Throwable ex) {
			System.out.printf("\n...ADEBUG Running thread  threw:  ");
			ex.printStackTrace();
		}
		System.out.printf("\n...ADEBUG after running thread    ");
	}
}
