package org.apache.spark.examples.h2o

import java.io.File
import java.util.Properties
import hex.schemas.KMeansV2
import org.apache.spark.h2o.H2OContext
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkContext, SparkConf}
import water.AutoBuffer
import water.fvec.DataFrame

object ProstateDemo {

  def main(args: Array[String]) {

    // Create Spark context which will drive computation
    // By default we use local spark context (which is useful for development)
    // but for cluster spark context, you should pass VM options -Dspark.master=spark://localhost:7077
    val sc = createSparkContext()

    // Start H2O-in-Spark
    if (sc.conf.get("spark.master").startsWith("local")) {
      water.H2OApp.main2("../h2o-dev")
      water.H2O.waitForCloudSize(1 /*One H2ONode to match the one Spark local-mode worker*/ , 1000)
    }

    // Load H2O from CSV file
    val h2oFromCSV = new DataFrame(new File("h2o-examples/smalldata/prostate.csv"))

    val table : RDD[Prostate] = H2OContext.toRDD[Prostate](sc,h2oFromCSV)

    // Convert to SQL type RDD
    val sqlContext = new SQLContext(sc)
    import sqlContext._ // import implicit conversions
    table.registerTempTable("prostate_table")

    // Invoke query on data; select a subsample
    val query = "SELECT * FROM prostate_table WHERE capsule=1"
    val result = sql(query) // Using a registered context and tables

    // Convert back to H2O
    val h2oFromRDD = H2OContext.toDataFrame(result)

    // Build a KMeansV2 model, setting model parameters via a Properties
    val props = new Properties
    for ((k,v) <- Seq("K"->"3")) props.setProperty(k,v)
    val job = new KMeansV2().fillFromParms(props).createImpl(h2oFromRDD)
    val kmm = job.train().get()
    job.remove()
    // Print the JSON model
    println(new String(kmm._output.writeJSON(new AutoBuffer()).buf()))

    // Stop Spark local worker; stop H2O worker
    sc.stop()
    water.H2O.exit(0)
  }

  private def createSparkContext(sparkMaster:String = null): SparkContext = {
    // Create application configuration
    val conf = new SparkConf()
      .setAppName("H2O Integration Example")
      //.set("spark.executor.memory", "1g")
    //if (!local) // Run 'sbt assembly to produce target/scala-2.10/h2o-sparkling-demo-assembly-1.0.jar
    //  conf.setJars(Seq("h2o-examples/target/spark-h2o-examples_2.10-1.1.0-SNAPSHOT.jar"))
    if (System.getProperty("spark.master")==null) conf.setMaster("local")
    new SparkContext(conf)
  }
}

/** Prostate schema definition. */
case class Prostate(id      :Option[Int],
                    capsule :Option[Int],
                    age     :Option[Int],
                    race    :Option[Int],
                    dpros   :Option[Int],
                    dcaps   :Option[Int],
                    psa     :Option[Float],
                    vol     :Option[Float],
                    gleason :Option[Int])

