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

package tbd.visualization.graph

import scala.collection.mutable.{HashMap, HashSet, ArrayBuffer}
import tbd.visualization.analysis.MethodInfo

/*
 * Represents any graph.
 */
class Graph() {
  val adj = new HashMap[Node, ArrayBuffer[Edge]]()
  val nodes = new HashSet[Node]()
}

/*
 * Represents a DDG.
 *
 * A DDG stripped of all edged except the control edges is guaranteed to be a
 * tree. A DDG has a root.
 */
class DDG(val root: Node) extends Graph {

  //Gets all children of a node, connected by control edges.
  def getCallChildren(node: Node): Seq[Node] = {
    adj(node).filter((e: Edge) => {
      (e.isInstanceOf[Edge.Control])
    }).map(e => e.destination)
  }

  //Gets the parent of a node, connected by an inverse control edge.
  def getCallParent(node: Node): Node = {
    val parent = adj(node).filter(e => {
      e.isInstanceOf[Edge.InverseControl]
    }).headOption
    if(parent.isEmpty) {
      null
    } else {
      parent.get.destination
    }
  }
}

//Helper object to create visualizer.graph.DDGs from tbd.ddg.DDGs.
//This wey we can maintain an independent copy of all necassary information
//for ourselfs, without creating memory leaks or accessing disposed mods.
object DDG {
  //Recursivley creates a visualizer DDG from a TBD DDG.
  def create(root: tbd.ddg.RootNode): DDG = {

    val newNode = new Node(root)
    val result = new DDG(newNode)

    result.nodes += newNode
    result.adj += (newNode -> new ArrayBuffer[Edge]())

    getChildren(root).foreach(x => {
      append(newNode, x, result)
    })

    result
  }

  //Fetches child nodes for a given node. Takes extra care of par nodes.
  private def getChildren(node: tbd.ddg.Node): Seq[tbd.ddg.Node] = {
    node match {
      case parNode: tbd.ddg.ParNode =>
        Seq(parNode.getFirstSubtree().root.children(0),
        parNode.getSecondSubtree().root.children(0))
      case _ => node.children
    }
  }

  //Recursivley creates a visualizer DDG from a TBD DDG.
  private def append(node: Node, ddgNode: tbd.ddg.Node, result: DDG): Unit = {
    val newNode = new Node(ddgNode)

    result.nodes += newNode
    result.adj += (newNode -> new ArrayBuffer[Edge]())
    result.adj(node) += new Edge.Control(node, newNode)
    result.adj(newNode) += new Edge.InverseControl(newNode, node)

    getChildren(ddgNode).foreach(x => {
      append(newNode, x, result)
    })
  }
}