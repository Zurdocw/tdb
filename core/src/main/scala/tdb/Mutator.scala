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
package tdb

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import com.typesafe.config.ConfigFactory
import scala.collection.mutable.Buffer
import scala.concurrent.{Await, Future}

import tdb.Constants._
import tdb.ddg.DDG
import tdb.master.MasterConnector
import tdb.messages._
import tdb.list.{ListConf, ListInput}
import tdb.util.OS

class Mutator(_connector: MasterConnector = null) {
  import scala.concurrent.ExecutionContext.Implicits.global

  val futures = Buffer[Future[Any]]()

  val connector =
    if (_connector == null) {
      MasterConnector()
    } else {
      _connector
    }

  private val masterRef = connector.masterRef

  private val id = Await.result(
    (masterRef ? RegisterMutatorMessage), DURATION).asInstanceOf[Int]

  var nextModId = 0
  def createMod[T](value: T): Mod[T] = {
    val message = CreateModMessage(value)
    Await.result((masterRef ? message).mapTo[Mod[T]], DURATION)
  }

  def updateMod[T](mod: Mod[T], value: T) {
    val message = UpdateModMessage(mod.id, value, null)
    futures += (masterRef ? message)
  }

  def read[T](mod: Mod[T]): T = {
    val future = masterRef ? GetModMessage(mod.id, null)
    val ret = Await.result(future, DURATION)

    (ret match {
      case NullMessage => null
      case x => x
    }).asInstanceOf[T]
  }

  def createList[T, U](conf: ListConf = ListConf()): ListInput[T, U] = {
    val future = masterRef ? CreateListMessage(conf)
    Await.result(future.mapTo[ListInput[T, U]], DURATION)
  }

  def run[T](adjust: Adjustable[T]): T = {
    Await.result(Future.sequence(futures), DURATION)
    futures.clear()

    val future = masterRef ? RunMutatorMessage(adjust, id)
    Await.result(future, DURATION).asInstanceOf[T]
  }

  def propagate() {
    Await.result(Future.sequence(futures), DURATION)
    futures.clear()

    val future = masterRef ? PropagateMutatorMessage(id)
    Await.result(future, DURATION)
  }

  def awaitReady() {
    Await.result(Future.sequence(futures), DURATION)
    futures.clear()
  }

  def getDDG(): DDG  = {
    val ddgFuture = masterRef ? GetMutatorDDGMessage(id)
    Await.result(ddgFuture, DURATION).asInstanceOf[DDG]
  }

  def shutdown() {
    Await.result(masterRef ? ShutdownMutatorMessage(id), DURATION)

    if (_connector == null) {
      connector.shutdown()
    }
  }
}
