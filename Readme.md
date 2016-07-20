### **Benchmarking Beam Runners**

This repository is only for research and development. The Beam application in this branch is agnostic to the type of the &quot;Runner&quot; engine executing the application code.  The target runners are Flink &amp; Spark. The current code is executing FlinkRunner and the Beam app configuration has a hard coded FlinkRunner value set by design for testing purposes. The future enhancements to make this code 100% runner agnostic will receive the type of the underlying Runner i.e. FlinkRunner or SparkRunner argument from the execution command line. For an understanding of the Flink &amp; Apark runners:

Please see the [Flink Runner in the Beam repository](https://github.com/apache/incubator-beam/tree/master/runners/flink) &amp;
                       [Spark Runner in the Beam repository](https://github.com/apache/incubator-beam/tree/master/runners/spark).

### **Spark &amp; Flink Beam Runners**

Beam Spark &amp; Flink Runners enable you to run Beam applications with Apache Spark &amp; Apache Flink™ engines. It integrates seamlessly with the Google Dataflow API, allowing you to execute Beam programs in streaming or batch mode.
The current Beam application in this repository executed a streaming mode of the incoming big data.

### **Beam Streaming Application**

In order to stress the streaming runner engines utilized by Beam APIs, the Linear Road problem has been implemented as a Beam application to execute at the runtime environment. Please refer to the following link to get familiar with Linear Road problem.
 [Linear Road Problem](http://www.isys.ucl.ac.be/vldb04/eProceedings/contents/pdf/RS12P1.PDF)

#### **Fault-Tolerance**

The current program&#39;s state is persisted by Apache Flink™ Cluster. The program may be re-run and resumed upon failure or if it&#39;s decided to continue the computation at a later time. The later case has not been tested while receiving data from Kafka broker.

#### **Sources and Sinks**

The Data Source for this Beam Streaming application is provided by Apache Kafka. A flat file containing Linear Road records i.e. tuples sends the LR records to Kafka Zookeeper at certain port &amp; corresponds to certain topic as a part of Kafka&#39;s Pub-Sub data distribution model. The Beam application subscribes to Kafka Zookeeper port &amp; subsequently receives LR streaming data. It then executes Beam APIs utilizing the underlying Beam runner i.e. FlinkRunner.  Upon creating the expected output, it Sinks the output data in local flat files.

#### **Kafka &amp; Redis integration**

To execute a Beam program in streaming mode the streaming mode in the PipelineOptions is enabled:

options.setStreaming(true);

The Pipeline options specify the runtime characteristics of the underlying Beam parallel &quot;pipelines&quot;. Each parallel branch of executing pipeline executes one record independently by default. Hence, massively parallel execution space.

To integrate the Beam app (pipelines) running in Flink Cluster to receive the LR records from Kafka, the Beam KafkaIO() API is invoked in an Unbounded mode. The &quot;each&quot; received LR record (from Kafka broker) is stored in a Beam PCollection object defined as a class variable within an instance of the (required) provided inner class that processes that particular record. Once processed, to save the LR output data, Java PrintWriter is utilized to write into a flat file i.e. the Sink. Open Source Redis is integrated with the Beam app to persist any necessary in-memory runtime data artifact. To maintain consistency, the Redis internal database must get purged as per each fresh execution of the LR Beam app. Please refer to the Redis link provided below in the Getting Started below for Redis details.

### **Batch**

This section will be completed when there is adequate R&amp;D information available on &quot;scheduled&quot; data processing executing in Beam pipelines in a Batch mode.

##
# Getting Started

Please browse through our new Apache [Beam community website](http://maven.nanthrax.net/beam/)

[Install &amp; Configure Flink Cluster](https://ci.apache.org/projects/flink/flink-docs-release-0.8/cluster_setup.html)
The above link provides guidelines to install latest Hadoop, configure the Linux servers for SSH amongst the clustered servers and install other necessary artifacts to construct a Flink Cluster.

[Install, Configure &amp; Integrate Kafka with Flink Cluster](http://data-artisans.com/kafka-flink-a-practical-how-to)

[Install, Configure &amp; Integrate Open Source Redis](https://www.digitalocean.com/community/tutorials/how-to-install-and-use-redis)

##
# Build the LR Beam Application

The following procedure is an optional approach to build this Beam application suitable for running in a Cluster i.e. Flink Cluster in the current stage of our R&amp;D.

1. [Install Maven in Linux](http://preilly.me/2013/05/10/how-to-install-maven-on-centos/)
2. Create a new Maven project. Please use the following as a guideline.


**mvn**  **archetype:generate -DgroupId=com.mycompany.benchmarkbeam -DartifactId=benchmark-runners -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false**

1. A default pom.xml is created. Modify the created default pom.xml with your specifics headers  &amp; save it (replacing the default pom.xml) under the just created Maven folder.
Please see pom.xml provided in this package as a guideline to produce the final version. The provided pom.xml has Maven specific tags that causes Maven to create the required the target fat jar file &quot;packaged&quot; to execute in the target clustered streaming servers (Flink Cluster for instance).


##
# Running the LR Beam application on a Flink cluster

A fat jar is necessary if you want to submit your Beam code to a (Flink) cluster. The fat jar includes the Beam app code but also Beam APIs code which is necessary during runtime. Note that this step is necessary because the target Beam Runner is not part of the target cluster i.e. Flink in this case.

1. If there is already a &quot;target&quot; in the project root folder where pom.xml resides, remove it
rm –r target
2. Build clean the fat jar
mvn clean package -Pbuild-jar
3. A folder &quot;target&quot; is created a fat jar named benchmark-runners-1.0.jar is created.

Please submit the Beam fat jar in the target folder to the (Flink) cluster using the command-line utility like so:

**/opt/analytics/apache/flink-1.0.0/bin/flink run /opt/maven305/benchmark-runners/target/ benchmark-runners-1.0.jar --topic lroad --bootstrap.servers kafkahost:9092 --zookeeper.connect kafkahost:2181 --group.id anyMyGroup**

Where:

- /opt/analytics/apache/flink-1.0.0 is where Apache Flink has been installed
- /opt/maven305/benchmark-runners is the Maven project folder
- benchmark-runners-1.0.jar is the Beam fat file in target folder
- lroad is the Kafka pub-sub topic for LR data
- kafkahost:9092 is Kafka broker default port
- kafkahost:21181 is Kafka Zookeeper port

At this point, the Beam application is ready to receive real-time streaming data from kafka. As an option to provide such data to Kafka, from a local directory on the user&#39;s laptop, send the records to Kafka port 9092. The following link provides an abundance of useful information to implement the [client sending to Kafka in Linux](http://kafka.apache.org/07/quickstart.html).

##
# More

For more information, please visit the [Apache Beam Incubator Website](http://beam.incubator.apache.org/).

##
# Disclaimer

Apache®, Apache Flink™, Flink™, Apache Beam™, Beam™, Apache Spark™, Spark™ and the Apache feather logo are trademarks of [The Apache Software Foundation](http://apache.org/).