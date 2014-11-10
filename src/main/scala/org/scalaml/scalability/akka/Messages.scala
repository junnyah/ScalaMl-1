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
package org.scalaml.scalability.akka


import akka.actor._
import org.scalaml.core.types.ScalaMl._
import org.scalaml.core.XTSeries

sealed abstract class Message(val id: Int)
case class Terminate(val _id: Int) extends Message(_id)

case class Start(val _id: Int =0) extends Message(_id)

case class Completed(val _id: Int, val xt: XTSeries[Double]) extends Message(_id)

case class Activate(val xt: XTSeries[Double], 
		            val sender: ActorRef) extends Message(0)



// ---------------------------------  EOF -------------------------