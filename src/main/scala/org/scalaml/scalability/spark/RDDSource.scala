/**
 * Copyright 2013, 2014  by Patrick Nicolas - Scala for Machine Learning - All rights reserved
 *
 * The source code in this file is provided by the author for the sole purpose of illustrating the 
 * concepts and algorithms presented in "Scala for Machine Learning" ISBN: 978-1-783355-874-2 Packt Publishing.
 * Unless required by applicable law or agreed to in writing, software is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 
 * Version 0.95c
 */
package org.scalaml.scalability.spark


import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel
import scala.annotation.implicitNotFound
import scala.io.Source
import java.io.{FileNotFoundException, IOException}
import org.scalaml.workflow.data.DataSource
import org.scalaml.core.design.PipeOperator
import org.scalaml.core.types.ScalaMl._
import org.scalaml.core.XTSeries
import org.scalaml.util.Display
import org.apache.log4j.Logger


case class RDDConfig(val cache: Boolean, val persist: StorageLevel)


	/**
	 * <P>Data extractor used to load and consolidate multiple data source (CSV files).</p>
	 * @param pathName relative path for data sources
	 * @param suffix suffix for the data files
	 * @param reversedOrder specify that the order of the data in the CSV file has to be revered before processing
	 * @param header number of lines dedicated to header information (usually 0 if pure data file, 1 for column header name)
	 * @throws IllegalArgumentException if the pathName or the file suffix is undefined.
	 * 
	 * @author Patrick Nicolas
	 * @since April 1, 2014
	 * @note Scala for Machine Learning
	 */
@implicitNotFound("Spark context is implicitly undefined")
final class RDDSource(	val pathName: String, 
		        		val normalize: Boolean, 
		        		val reverseOrder: Boolean, 
		        		val headerLines: Int,
		        		val state: RDDConfig)(implicit sc: SparkContext)  
		          extends PipeOperator[Array[String] =>DblVector, RDD[DblVector]] {

   check(pathName, headerLines, state)
   private val src = DataSource(pathName, normalize, reverseOrder, headerLines)
   private val logger = Logger.getLogger("RDDSource")
  
   
      	/**
   		 * Load, extracts, convert and normalize a list of fields using an extractors.
   		 * @param ext function to extract and convert a list of comma delimited fields into a double vector
   		 * @return a RDD of DblVector if succeed, None otherwise
   		 * @throws IllegalArgumentException if the extraction function is undefined
   		 */   
   override def |> : PartialFunction[(Array[String] => DblVector), RDD[DblVector]] = {
     case extractor: (Array[String] => DblVector) if(extractor != null) => {
        val ts = src.load(extractor) 
        if( ts != None) {
           val rdd = sc.parallelize(ts.get.toArray)
      	   rdd.persist(state.persist)
           if( state.cache)
             rdd.cache
           rdd
        }
        else { Display.error("RDDSource.|> ", logger); null} 
     }
   }
  
   private def check(pathName: String, headerLines: Int, state: RDDConfig) {
      require(pathName != null && pathName.length > 2, "Cannot create a RDD source with undefined path name")
      require(headerLines >= 0, "Cannot generate a RDD from an input file with " + headerLines + " header lines")
      require(state != null, "Cannot create a RDD source for undefined stateuration")
   }
}


   
	/**
	 * Companion object to RDDSource used to defines several version of constructors
	 * and the conversion from time series to RDD
	 */
object RDDSource {
   import org.apache.spark.mllib.linalg.{Vector, DenseVector}
   
   final val DefaultRDDConfig = new RDDConfig(true, StorageLevel.MEMORY_ONLY)
   def apply(pathName: String, normalize: Boolean, reverseOrder: Boolean, headerLines: Int, state: RDDConfig)(implicit sc: SparkContext): RDDSource  = new RDDSource(pathName, normalize, reverseOrder, headerLines, state: RDDConfig)
   def apply(pathName: String, normalize: Boolean, reverseOrder: Boolean, headerLines: Int)(implicit sc: SparkContext): RDDSource  = new RDDSource(pathName, normalize, reverseOrder, headerLines, DefaultRDDConfig)
   
   @implicitNotFound("Spark context is implicitly undefined")
   def convert(xt: XTSeries[DblVector], rddConfig: RDDConfig)(implicit sc: SparkContext): RDD[Vector] = {
  	 require(xt != null && xt.size > 0, "Cannot generate a RDD from undefined time series")
     require(rddConfig != null, "Cannot generate a RDD from a time series without an RDD stateuration")
     
     val rdd: RDD[Vector] = sc.parallelize(xt.toArray.map( x => new DenseVector(x)))
     rdd.persist(rddConfig.persist)
     if( rddConfig.cache)
         rdd.cache
     rdd
   }
   
}

// ----------------------------------  EOF ---------------------------------------------------
