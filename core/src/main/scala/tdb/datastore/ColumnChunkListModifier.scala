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
package tdb.datastore

import scala.collection.mutable.{Buffer, Map}
import scala.concurrent.{ExecutionContext, Future}

import tdb.{Mod, Mutator}
import tdb.Constants._
import tdb.list._

class ColumnChunkListModifier(datastore: Datastore, conf: ColumnListConf)
    (implicit ec: ExecutionContext)
  extends Modifier {

  val chunks = Map[ModId, Iterable[Any]]()
  val values = Map[Any, Buffer[Any]]()

  // Contains the last DoubleChunkListNode before the tail node. If the list is
  // empty, the contents of this mod will be null.
  private var lastNodeMod = datastore.createMod[ColumnListNode[Any]](null)

  val nodes = Map[Any, Mod[ColumnListNode[Any]]]()
  val previous = Map[Any, Mod[ColumnListNode[Any]]]()

  val list =
    new ColumnList[Any](lastNodeMod, conf, false, datastore.workerInfo.workerId)

  def loadInput(keys: Iterable[Any]) = ???

  private def makeNewColumns(column: String, key: Any, value: Any) = {
    val newColumns = Map[String, Mod[Iterable[Any]]]()

    for ((columnName, (columnType, defaultValue)) <- conf.columns) {
      columnName match {
        case "key" =>
          newColumns("key") = datastore.createMod(Iterable(key))
        case name if name == column =>
          newColumns(columnName) = datastore.createMod(Iterable(value))
        case _ =>
          newColumns(columnName) = datastore.createMod(Iterable(defaultValue))
      }
    }

    newColumns
  }

  private def appendIn(column: String, key: Any, value: Any): Future[_] = {
    val lastNode = datastore.read(lastNodeMod)

    if (lastNode == null) {
      // The list must be empty.
      val newColumns = makeNewColumns(column, key, value)
      val size = 1

      previous(key) = null

      val tailMod = datastore.createMod[ColumnListNode[Any]](null)
      val newNode = new ColumnListNode(newColumns, tailMod, size)

      nodes(key) = lastNodeMod

      datastore.updateMod(lastNodeMod.id, newNode)
    } else if (lastNode.size >= conf.chunkSize) {
      val newColumns = makeNewColumns(column, key, value)
      previous(key) = lastNodeMod

      lastNodeMod = lastNode.nextMod

      val tailMod = datastore.createMod[ColumnListNode[Any]](null)
      val newNode = new ColumnListNode(newColumns, tailMod, 1)

      nodes(key) = lastNode.nextMod

      datastore.updateMod(lastNode.nextMod.id, newNode)
    } else {
      val futures = Buffer[Future[Any]]()
      val oldColumns = lastNode.columns

      for ((columnName, chunkMod) <- oldColumns) {
        columnName match {
          case "key" =>
            val keys = datastore.read(chunkMod)
            previous(key) = previous(keys.head)
            futures += datastore.updateMod(chunkMod.id, keys ++ Iterable(key))
          case name if name == column =>
            val values = datastore.read(chunkMod)
            futures += datastore.updateMod(
              chunkMod.id, values ++ Iterable(value))
          case _ =>
            val values = datastore.read(chunkMod)
            futures += datastore.updateMod(
              chunkMod.id, values ++ Iterable(conf.columns(columnName)._2))
        }
      }
      val size = lastNode.size + 1

      val tailMod = datastore.createMod[ColumnListNode[Any]](null)
      val newNode = new ColumnListNode(oldColumns, tailMod, size)

      nodes(key) = lastNodeMod

      datastore.updateMod(lastNodeMod.id, newNode)
    }
  } //ensuring(isValid())

  def put(key: Any, value: Any): Future[_] = ???

  def putIn(column: String, key: Any, value: Any): Future[_] = {
    if (!nodes.contains(key)) {
      //println("appendIn " + column + " key " + key + " value " + value + " " + this)
      appendIn(column, key, value)
    } else {
      //println("updating " + column + " " + key + " " + value)
      val node = datastore.read(nodes(key))

      val keyIter = datastore.read(node.columns("key")).iterator

      val columnMod = node.columns(column)
      val columnType = conf.columns(column)._1
      val chunk = datastore.read(columnMod)
      var found = false
      val newChunk = chunk.map {
        case _value =>
          val _key = keyIter.next
          if (key == _key) {
            assert(!found)
            found = true

            columnType match {
              case aggregatedColumn: AggregatedColumn =>
                aggregatedColumn.aggregator(_value, value)
              case _ =>
                value
            }
          } else {
            _value
          }
      }
      assert(found)

      //val newNode = new ColumnListNode(node.columns, node.nextMod, node.size)
      //datastore.updateMod(nodes(key).id, newNode)
      datastore.updateMod(columnMod.id, newChunk)
    }
  }

  def get(key: Any): Any = ???

  def remove(key: Any, value: Any): Future[_] = ???

  def contains(key: Any): Boolean = {
    nodes.contains(key)
  }

  def getAdjustableList() = list.asInstanceOf[AdjustableList[Any, Any]]

  def toBuffer(): Buffer[(Any, Any)] = ???
}