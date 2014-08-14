package org.apache.spark.rdd

import org.apache.spark.h2o.H2OContext
import org.apache.spark.{TaskContext, Partition, SparkContext}
import water.fvec.Chunk

/**
 * Testing support for H2O lazy RDD loading data from H2O KV-store.
 *
 * It should contain only RDD-related implementation, all support methods
 * should be offloaded into H2ORDDLike trait.
 *
 * NOTES:
 *  - spark context to contain h2o info
 *  - sparkcontext.initH2O() returns H2OContext
 *  - import H2OContext
 *  - registerRDDasFrame(RDD, "frame reference")
 *
 *  TODO: create JIRA support arbitrary spark runtime extension, arbitrary extension data
 *
 */
private[spark]
class H2ORDD(@transient val hc: H2OContext, @transient sc: SparkContext)
  extends RDD[Chunk](sc, Nil)
  with H2ORDDLike {

  def h2oRDD: H2ORDD = this
  /**
   * :: DeveloperApi ::
   * Implemented by subclasses to compute a given partition.
   */
  override def compute(split: Partition, context: TaskContext): Iterator[Chunk] = ???

  /**
   * Implemented by subclasses to return the set of partitions in this RDD. This method will only
   * be called once, so it is safe to implement a time-consuming computation in it.
   */
  override protected def getPartitions: Array[Partition] = ???
}


/** This trait provides support methods for H2O RDD. */
private[spark]
trait H2ORDDLike {
  val hc: H2OContext

  // Should be filled by mixer
  private[spark] def h2oRDD: H2ORDD

  def registerFrame(frameName: String): Unit = {
    println(s"registerFrame(frameName='$frameName')")
    hc.registerRDDAsFrame(h2oRDD, frameName)
  }
}