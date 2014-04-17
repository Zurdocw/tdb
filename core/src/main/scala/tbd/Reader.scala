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
package tbd

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration._

import tbd.datastore.Dataset
import tbd.messages._
import tbd.mod.Mod
import tbd.worker.Worker

class Reader(worker: Worker) {
  implicit val timeout = Timeout(30 seconds)

  def get[T](key: Int): T = {
    val modFuture = worker.datastoreRef ? GetMessage("input", key)
    Await.result(modFuture, timeout.duration)
      .asInstanceOf[T]
  }

  def getArray[T](): Array[T] = {
    val arrayFuture = worker.datastoreRef ? GetArrayMessage("input")
    Await.result(arrayFuture, timeout.duration)
      .asInstanceOf[Array[T]]
  }

  def getDataset[T](partitions: Int = 8): Dataset[T] = {
    val datasetFuture = worker.datastoreRef ? GetDatasetMessage("input", partitions)
    val dataset = Await.result(datasetFuture, timeout.duration)
    worker.datasets += dataset.asInstanceOf[Dataset[Any]]
    dataset.asInstanceOf[Dataset[T]]
  }
}
