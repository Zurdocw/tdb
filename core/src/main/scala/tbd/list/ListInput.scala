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
package tbd.list

import tbd.Input

object ListInput {
  def apply[T, U](conf: ListConf = new ListConf()): ListInput[T, U] = {
    if (conf.partitions == 1) {
      new ModListInput()
    } else {
      new PartitionedListInput(conf)
    }
  }
}

trait ListInput[T, U] extends Input[T, U] {
  def getAdjustableList(): AdjustableList[T, U]
}

object ChunkListInput {
  def apply[T, U](conf: ListConf = new ListConf()): ChunkListInput[T, U] = {
    if (conf.partitions == 1) {
      new ModChunkListInput(conf)
    } else {
      new PartitionedChunkListInput(conf)
    }
  }
}

trait ChunkListInput[T, U] extends Input[T, U] {
  def getChunkList(): AdjustableChunkList[T, U]
}