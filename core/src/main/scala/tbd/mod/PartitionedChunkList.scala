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
package tbd.mod

import akka.actor.ActorRef
import scala.collection.mutable.{ArrayBuffer, Buffer, Set}

import tbd.{Changeable, Context}
import tbd.datastore.Datastore
import tbd.TBD._

class PartitionedChunkList[T, U](
    val partitions: ArrayBuffer[ChunkList[T, U]]
  ) extends AdjustableChunkList[T, U] {

  def chunkMap[V, W](
      f: (Vector[(T, U)]) => (V, W))
     (implicit c: Context): PartitionedModList[V, W] = {
    def innerChunkMap(i: Int)(implicit c: Context): ArrayBuffer[ModList[V, W]] = {
      if (i < partitions.size) {
        val parTup = par {
          c => partitions(i).chunkMap(f)(c)
        } and {
          c => innerChunkMap(i + 1)(c)
        }

        parTup._2 += parTup._1
      } else {
        ArrayBuffer[ModList[V, W]]()
      }
    }

    new PartitionedModList(innerChunkMap(0))
  }

  def chunkSort(
      comparator: ((T, U), (T, U)) => Boolean)
     (implicit c: Context): Mod[(Int, Array[(T, U)])] = ???

  def filter(
      pred: ((T, U)) => Boolean)
     (implicit c: Context): PartitionedChunkList[T, U] = {
    def parFilter(i: Int)(implicit c: Context): ArrayBuffer[ChunkList[T, U]] = {
      if (i < partitions.size) {
        val parTup = par {
          c => partitions(i).filter(pred)(c)
	} and {
          c => parFilter(i + 1)(c)
        }

        parTup._2 += parTup._1
      } else {
        ArrayBuffer[ChunkList[T, U]]()
      }
    }

    new PartitionedChunkList(parFilter(0))
  }

  def map[V, W](
      f: ((T, U)) => (V, W))
     (implicit c: Context): PartitionedChunkList[V, W] = {
    def innerMap(i: Int)(implicit c: Context): ArrayBuffer[ChunkList[V, W]] = {
      if (i < partitions.size) {
        val parTup = par {
          c => partitions(i).map(f)(c)
        } and {
          c => innerMap(i + 1)(c)
        }

        parTup._2 += parTup._1
      } else {
        ArrayBuffer[ChunkList[V, W]]()
      }
    }

    new PartitionedChunkList(innerMap(0))
  }

  def reduce(
      f: ((T, U), (T, U)) => (T, U))
     (implicit c: Context): Mod[(T, U)] = ???

  def sort(
      comparator: ((T, U), (T, U)) => Boolean)
     (implicit c: Context): AdjustableList[T, U] = ???

  def split(
      pred: ((T, U)) => Boolean)
     (implicit c: Context): (AdjustableList[T, U], AdjustableList[T, U]) = ???

  /* Meta Operations */
  def toBuffer(): Buffer[U] = {
    val buf = ArrayBuffer[U]()

    for (partition <- partitions) {
      var innerNode = partition.head.read()
      while (innerNode != null) {
        buf ++= innerNode.chunk.map(pair => pair._2)
        innerNode = innerNode.nextMod.read()
      }
    }

    buf
  }
}
