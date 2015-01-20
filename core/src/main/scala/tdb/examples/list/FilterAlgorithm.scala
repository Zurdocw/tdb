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
package tdb.examples.list

import scala.collection.{GenIterable, GenMap, Seq}
import scala.collection.mutable.Map

import tdb._
import tdb.list._
import tdb.util._

object FilterAlgorithm {
  def predicate(pair: (Int, Int)): Boolean = {
    pair._2 % 2 == 0
  }
}

class FilterAdjust(list: AdjustableList[Int, Int])
  extends Adjustable[AdjustableList[Int, Int]] {

  def run(implicit c: Context) = {
    list.filter(FilterAlgorithm.predicate)
  }
}

class FilterAlgorithm(_conf: AlgorithmConf)
    extends Algorithm[Int, AdjustableList[Int, Int]](_conf) {
  val input = mutator.createList[Int, Int](conf.listConf)

  val data = new IntData(input, conf.runs, conf.count, conf.mutations)

  val adjust = new FilterAdjust(input.getAdjustableList())

  var naiveTable: GenIterable[Int] = _
  def generateNaive() {
    data.generate()
    naiveTable = Vector(data.table.values.toSeq: _*).par
  }

  def runNaive() {
    naiveHelper(naiveTable)
  }

  private def naiveHelper(input: GenIterable[Int]) = {
    input.filter(FilterAlgorithm.predicate(0, _))
  }

  def checkOutput(output: AdjustableList[Int, Int]) = {
    val sortedOutput = output.toBuffer(mutator).map(_._2).sortWith(_ < _)
    val answer = naiveHelper(data.table.values)

    sortedOutput == answer.toBuffer.sortWith(_ < _)
  }
}
