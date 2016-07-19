### **Benchmarking Beam Runners**

This repository is only for research and development. The Beam application in this branch is agnostic to the type of the &quot;Runner&quot; engine executing the application code.  The target runners are Flink &amp; Spark. The current code is executing FlinkRunner and the Beam app configuration has a hard coded FlinkRunner value set by design for testing purposes. The future enhancements to make this code 100% runner agnostic will receive the type of the underlying Runner i.e. FlinkRunner or SparkRunner argument from the execution command line. For an understanding of the Flink &amp; Apark runners:

Please see the [Flink Runner in the Beam repository](https://github.com/apache/incubator-beam/tree/master/runners/flink) &amp;
                       [Spark Runner in the Beam repository](https://github.com/apache/incubator-beam/tree/master/runners/spark).

### **Spark &amp; Flink Dataflows**

Spark &amp; Flink Dataflows are Runners for Google Dataflow (aka Apache Beam™) which enables you to run Dataflow programs with Apache Spark &amp; Apache Flink™. It integrates seamlessly with the Dataflow API, allowing you to execute Dataflow programs in streaming or batch mode.
The current Beam application in this repository executed a streaming mode of the incoming big data.

### **Beam Streaming Application**

In order to stress the underlying Beam streaming runner engines, the Linear Road problem has been implemented to execute at the runtime. Please refer to the following link to get familiar with Linear Road problem.
 [Linear Road Problem](http://www.isys.ucl.ac.be/vldb04/eProceedings/contents/pdf/RS12P1.PDF)

#### **Fault-Tolerance**

The current program&#39;s state is persisted by Apache Flink™ Cluster. The program may be re-run and resumed upon failure or if it&#39;s decided to continue the computation at a later time. The later case has not been tested while receiving data from Kafka broker.

#### **Sources and Sinks**

The Data Source for this Beam Streaming application is provided by Apache Kafka. A flat file containing Linear Road records i.e. tuples sends the LR records to Kafka Zookeeper at certain port &amp; corresponds to certain topic as a part of Kafka&#39;s Pub-Sub data distribution model. The Beam application subscribes to Kafka Zookeeper port &amp; subsequently receives LR streaming data. It then executes Beam APIs utilizing the underlying Beam runner i.e. FlinkRunner.  Upon creating the expected output, it Sinks the output data in local flat files.

#### **Kafka &amp; Redis integration**

To execute a Dataflow program in streaming mode the streaming mode in the PipelineOptions is enabled:

options.setStreaming(true);

To integrate the Beam app running in Flink Cluster with Kafka, the Beam KafkaIO() is invoked. The received data is stored in a Beam Pcollection object. To save the output data, Java PrintWriter is utilized to write into a flat file. Open Source Redis is integrated with the Beam app to contain the runtime required in-memory information. Redis database gets purged as per each fresh execution of the Beam app.

### **Batch**

TBD

##
# Getting Started

# [Install &amp; Configure Flink Cluster](https://ci.apache.org/projects/flink/flink-docs-release-0.8/cluster_setup.html)

[Install, Configure &amp; Integrate Kafka with Flink Cluster](http://data-artisans.com/kafka-flink-a-practical-how-to)

[Install, Configure &amp; Integrate Open Source Redis](https://www.digitalocean.com/community/tutorials/how-to-install-and-use-redis)

##
# Build the LR Beam Application

The following procedure is an optional approach to build this Beam application suitable for running in a Cluster i.e. Flink Cluster in the current stage of our R&amp;D.

1. [Install Maven in Linux](http://preilly.me/2013/05/10/how-to-install-maven-on-centos/)
2. Create a new Maven project. Please use the following as a guideline.
mvn archetype:generate -DgroupId=com.mycompany.benchmarkbeam -DartifactId=benchmark-runners -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false
3. Modify the created default pom.xml with &quot;roughly&quot; the following &amp; save it as pom.xml (replacing the default one) under the just created folder.


&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;

&lt;project xmlns=&quot;http://maven.apache.org/POM/4.0.0&quot;

         xmlns:xsi=&quot;http://www.w3.org/2001/XMLSchema-instance&quot;

         xsi:schemaLocation=&quot;http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd&quot;&gt;

    &lt;modelVersion&gt;4.0.0&lt;/modelVersion&gt;

    &lt;groupId&gt;com.mycompany.benchmarkbeam&lt;/groupId&gt;

    &lt;artifactId&gt;benchmark-runners&lt;/artifactId&gt;

    &lt;version&gt;1.0&lt;/version&gt;

&lt;packaging&gt;jar&lt;/packaging&gt;

 &lt;properties&gt;

    &lt;project.build.sourceEncoding&gt;UTF-8&lt;/project.build.sourceEncoding&gt;

  &lt;/properties&gt;

    &lt;dependencies&gt;



&lt;dependency&gt;

  &lt;groupId&gt;biz.paluch.redis&lt;/groupId&gt;

  &lt;artifactId&gt;lettuce&lt;/artifactId&gt;

  &lt;version&gt;3.4.3.Final&lt;/version&gt;

&lt;/dependency&gt;

&lt;dependency&gt;

      &lt;groupId&gt;org.apache.beam&lt;/groupId&gt;

      &lt;artifactId&gt;flink-runner\_2.10&lt;/artifactId&gt;

     &lt;version&gt;0.1.0-incubating-SNAPSHOT&lt;/version&gt;

    &lt;/dependency&gt;

&lt;dependency&gt;

    &lt;groupId&gt;org.apache.beam&lt;/groupId&gt;

    &lt;artifactId&gt;beam-sdks-java-io-kafka&lt;/artifactId&gt;

    &lt;version&gt;0.1.0-incubating&lt;/version&gt;

&lt;/dependency&gt;

    &lt;/dependencies&gt;

    &lt;build&gt;

        &lt;plugins&gt;

            &lt;plugin&gt;

                &lt;groupId&gt;org.apache.maven.plugins&lt;/groupId&gt;

                &lt;artifactId&gt;maven-shade-plugin&lt;/artifactId&gt;

                &lt;version&gt;2.4.1&lt;/version&gt;

                &lt;executions&gt;

                    &lt;execution&gt;

                        &lt;phase&gt;package&lt;/phase&gt;

                        &lt;goals&gt;

                            &lt;goal&gt;shade&lt;/goal&gt;

                        &lt;/goals&gt;

                        &lt;configuration&gt;

                            &lt;transformers&gt;

                                &lt;transformer implementation=&quot;org.apache.maven.plugins.shade.resource.ManifestResourceTransformer&quot;&gt;

                                    &lt;mainClass&gt;benchmark.flinkspark.flink.BenchBeamRunners&lt;/mainClass&gt;

                                &lt;/transformer&gt;

                            &lt;/transformers&gt;

                            &lt;artifactSet&gt;

                                &lt;excludes&gt;

                                    &lt;exclude&gt;org.apache.flink:\*&lt;/exclude&gt;

                                &lt;/excludes&gt;

                            &lt;/artifactSet&gt;

                        &lt;/configuration&gt;

                    &lt;/execution&gt;

                &lt;/executions&gt;

            &lt;/plugin&gt;

&lt;plugin&gt;

                &lt;groupId&gt;org.apache.maven.plugins&lt;/groupId&gt;

                &lt;artifactId&gt;maven-compiler-plugin&lt;/artifactId&gt;

                &lt;version&gt;3.1&lt;/version&gt;

                &lt;configuration&gt;

                    &lt;source&gt;1.8&lt;/source&gt;

                    &lt;target&gt;1.8&lt;/target&gt;

                &lt;/configuration&gt;

            &lt;/plugin&gt;

        &lt;/plugins&gt;

    &lt;/build&gt;

&lt;/project&gt;

##
# Running the LR Beam application on a Flink cluster

A fat jar is necessary if you want to submit your Beam code to a (Flink) cluster. The fat jar includes the Beam app code but also Beam APIs code which is necessary during runtime. Note that this step is necessary because the target Beam Runner is not part of the target cluster i.e. Flink in this case.

1. If there is already a &quot;target&quot; in the project root folder where pom.xml resides, remove it
rm –r target
2. Build clean the fat jar
mvn clean package -Pbuild-jar
3. A folder &quot;target&quot; is created a fat jar named benchmark-runners-1.0.jar is created.

Please submit the Beam fat jar in the target folder to the (Flink) cluster using the command-line utility like so:

/opt/analytics/apache/flink-1.0.0/bin/flink run /opt/maven305/benchmark-runners/target/ benchmark-runners-1.0.jar --topic lroad --bootstrap.servers kafkahost:9092 --zookeeper.connect kirk:2181 --group.id anyMyGroup

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