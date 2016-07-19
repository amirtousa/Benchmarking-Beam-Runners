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

The current program&#39;s state is persisted by Apache Flink™ Cluster. You may re-run and resume your program upon failure or if you decide to continue computation at a later time.

#### **Sources and Sinks**

The Data Source for this Beam Streaming application is provided by Apache Kafka. A flat file containing Linear Road records i.e. tuples sends the LR records to Kafka Zookeeper at certain port &amp; corresponds to certain topic as a part of Kafka&#39;s Pub-Sub data distribution model. The Beam application subscribes to Kafka Zookeeper port &amp; subsequently receives LR streaming data. It then executes Beam APIs utilizing the underlying Beam runner i.e. FlinkRunner.  Upon creating the expected output, it Sinks the output data in local flat files.

#### **Kafka &amp; Redis integration**

To execute a Dataflow program in streaming mode the streaming mode in the PipelineOptions is enabled:

options.setStreaming(true);

To integrate the Beam app running in Flink Cluster with Kafka, the Beam KafkaIO() is invoked. The received data is stored in a Beam Pcollection object. To save the output data, Java PrintWriter is utilized to write into a flat file. Open Source Redis is integrated with the Beam app to contain the runtime required in-memory information. Redis database gets purged as per each fresh execution of the Beam app.

### **Batch**

**TBD**

##
# Getting Started

### **Install Flink-Dataflow**

TBD

##
# Running the LR Beam application on a Flink cluster (TBD)

You can run your Dataflow program on an Apache Flink cluster. Please start off by creating a new Maven project.

mvn archetype:generate -DgroupId=com.mycompany.dataflow -DartifactId=dataflow-test \

    -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false

The contents of the root pom.xml should be slightly changed aftewards (explanation below):

&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;

&lt;projectxmlns=&quot;http://maven.apache.org/POM/4.0.0&quot;

         xmlns:xsi=&quot;http://www.w3.org/2001/XMLSchema-instance&quot;

         xsi:schemaLocation=&quot;http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd&quot;&gt;

    &lt;modelVersion&gt;4.0.0&lt;/modelVersion&gt;

    &lt;groupId&gt;com.mycompany.dataflow&lt;/groupId&gt;

    &lt;artifactId&gt;dataflow-test&lt;/artifactId&gt;

    &lt;version&gt;1.0&lt;/version&gt;

    &lt;dependencies&gt;

        &lt;dependency&gt;

            &lt;groupId&gt;com.dataartisans&lt;/groupId&gt;

            &lt;artifactId&gt;flink-dataflow\_2.10&lt;/artifactId&gt;

            &lt;version&gt;0.3.0&lt;/version&gt;

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

                                &lt;transformerimplementation=&quot;org.apache.maven.plugins.shade.resource.ManifestResourceTransformer&quot;&gt;

                                    &lt;mainClass&gt;com.dataartisans.flink.dataflow.examples.WordCount&lt;/mainClass&gt;

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

        &lt;/plugins&gt;

    &lt;/build&gt;

&lt;/project&gt;

The following changes have been made:

1. The Flink Dataflow Runner was added as a dependency.
2. The Maven Shade plugin was added to build a fat jar.

A fat jar is necessary if you want to submit your Dataflow code to a Flink cluster. The fat jar includes your program code but also Dataflow code which is necessary during runtime. Note that this step is necessary because the Dataflow Runner is not part of Flink.

You can then build the jar using mvn clean package. Please submit the fat jar in the target folder to the Flink cluster using the command-line utility like so:

./bin/flink run /path/to/fat.jar

##
# More

For more information, please visit the [Apache Flink Website](http://flink.apache.org/) or contact the [Mailinglists](http://flink.apache.org/community.html#mailing-lists).

##
# Disclaimer

Apache®, Apache Flink™, Flink™, and the Apache feather logo are trademarks of [The Apache Software Foundation](http://apache.org/).