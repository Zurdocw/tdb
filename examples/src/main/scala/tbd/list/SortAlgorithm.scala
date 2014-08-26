/**
 * Copyright (C) 2013 Carnegie Mellon University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tbd.examples.list

import scala.collection.{GenIterable, GenMap, Seq}
import scala.collection.immutable.TreeSet
import scala.collection.mutable.Map

import tbd._
import tbd.datastore.IntData
import tbd.list._

object SortAlgorithm {
  def predicate(a: (Int, Int), b: (Int, Int)): Boolean = {
    a._1 < b._1
  }
}

class SortAlgorithm(_conf: Map[String, _], _listConf: ListConf)
    extends Algorithm[Int, AdjustableList[Int, Int]](_conf, _listConf) {
  val input = ListInput[Int, Int](listConf)

  val data = new IntData(input, count, mutations)

  def generateNaive() {
    data.generate()
  }

  def runNaive() {
    naiveHelper(data.table)
  }

  private def naiveHelper(input: Map[Int, Int]) = {
    input map { TreeSet(_) } reduce((one, two) => one ++ two)
  }

  def checkOutput(
      input: Map[Int, Int],
      output: AdjustableList[Int, Int]) = {
    val sortedOutput = output.toBuffer()
    val answer = naiveHelper(input)

    sortedOutput == answer.toBuffer
  }

  def run(implicit c: Context): AdjustableList[Int, Int] = {
    val pages = input.getAdjustableList()

    pages.sort(SortAlgorithm.predicate)
  }
}
