package bot.plan.astar

import bot.GamePad
import bot.state.FramePoint
import bot.state.map.MapCell
import util.d
import java.util.*

//What A* Search Algorithm does is that at each step it picks the node according to a value-‘f’ which is a parameter equal to the sum of two other parameters – ‘g’ and ‘h’. At each step it picks the node/cell having the lowest ‘f’, and process that node/cell.
//We define ‘g’ and ‘h’ as simply as possible below
//g = the movement cost to move from the starting point to a given square on the grid, following the path generated to get there.
//h = the estimated movement cost to move from that given square on the grid to the final destination. This is often referred to as the heuristic, which is nothing but a kind of smart guess. We really don’t know the actual distance until we find the path, because all sorts of things can be in the way (walls, water, etc.). There can be many ways to calculate this ‘h’ which are discussed in the later sections.

class AStar {
    var cell: List<List<Node>> = mutableListOf<List<Node>>()
    var pathList = mutableListOf<Node>()
    var closedList = mutableListOf<Node>()
    var additionalPath = false

    fun aStarFinder(mapCell: MapCell, from: FramePoint, to:
        FramePoint
    ): GamePad? {
        cell = mapCell.passable.map.map {
            it.map {
                val hVal = if (it) -1.0 else 1.0
//                var hValue = 0.0
//                var gValue = 0
//                var fValue = 0.0
//                var parent: Node? = null
                val x = 0
                val y = 0
                Node(x, y, hVal)
            }
        }

        generatePath(cell, 20, 20, 40, 40, 1)

        pathList.forEach {
            d { "${it.x}"}
        }
        return GamePad.MoveUp
    }

    /**
     * @param hValue         Node type 2D Array (Matrix)
     * @param Ai             Starting point's y value
     * @param Aj             Starting point's x value
     * @param Bi             Ending point's y value
     * @param Bj             Ending point's x value
     * @param v              Cost between 2 cells located horizontally or vertically next to each other
     */
    fun generatePath(
        hValue: List<List<Node>>,
        Ai: Int,
        Aj: Int,
        Bi: Int,
        Bj: Int,
        v: Int,
    ) {

        //Creation of a PriorityQueue and the declaration of the Comparator
        val openList: PriorityQueue<Node> = PriorityQueue<Node> { cell1, cell2 ->
            //Compares 2 Node objects stored in the PriorityQueue and Reorders the Queue according to the object which has the lowest fValue
            if (cell1.fValue < cell2.fValue) -1 else if (cell1.fValue > cell2.fValue) 1 else 0
        }

        //Adds the Starting cell inside the openList
        openList.add(cell[Ai][Aj])

        //Executes the rest if there are objects left inside the PriorityQueue
        while (true) {

            //Gets and removes the objects that's stored on the top of the openList and saves it inside node
            val node = openList.poll() ?: break

            //Checks if whether node is empty and f it is then breaks the while loop

            //Checks if whether the node returned is having the same node object values of the ending point
            //If it des then stores that inside the closedList and breaks the while loop
            if (node === cell[Bi][Bj]) {
                closedList.add(node)
                break
            }
            // GM because explored it
            closedList.add(node)

            //Left Cell
            try {
                if (cell.get(node.x)
                        .get(node.y - 1).hValue !== -1.0 && !openList.contains(
                        cell.get(node.x).get(node.y - 1)
                    )
                    && !closedList.contains(cell.get(node.x).get(node.y - 1))
                ) {
                    val tCost = node.fValue + v
                    cell.get(node.x).get(node.y - 1).gValue = v
                    val cost: Double =
                        cell.get(node.x).get(node.y - 1).hValue + tCost
                    if (cell.get(node.x)
                            .get(node.y - 1).fValue > cost || !openList.contains(
                            cell.get(node.x).get(node.y - 1)
                        )
                    ) cell.get(node.x).get(node.y - 1).fValue = cost
                    openList.add(cell.get(node.x).get(node.y - 1))
                    cell.get(node.x).get(node.y - 1).parent = node
                }
            } catch (e: IndexOutOfBoundsException) {
            }

            //Right Cell
            try {
                if (cell.get(node.x)
                        .get(node.y + 1).hValue !== -1.0 && !openList.contains(
                        cell.get(node.x).get(node.y + 1)
                    )
                    && !closedList.contains(cell.get(node.x).get(node.y + 1))
                ) {
                    val tCost = node.fValue + v
                    cell.get(node.x).get(node.y + 1).gValue = v
                    val cost: Double =
                        cell.get(node.x).get(node.y + 1).hValue + tCost
                    if (cell.get(node.x)
                            .get(node.y + 1).fValue > cost || !openList.contains(
                            cell.get(node.x).get(node.y + 1)
                        )
                    ) cell.get(node.x).get(node.y + 1).fValue = cost
                    openList.add(cell.get(node.x).get(node.y + 1))
                    cell.get(node.x).get(node.y + 1).parent = node
                }
            } catch (e: IndexOutOfBoundsException) {
            }

            //Bottom Cell
            try {
                if (cell.get(node.x + 1)
                        .get(node.y).hValue !== -1.0 && !openList.contains(
                        cell.get(
                            node.x + 1
                        ).get(node.y)
                    )
                    && !closedList.contains(cell.get(node.x + 1).get(node.y))
                ) {
                    val tCost = node.fValue + v
                    cell.get(node.x + 1).get(node.y).gValue = v
                    val cost: Double =
                        cell.get(node.x + 1).get(node.y).hValue + tCost
                    if (cell.get(node.x + 1)
                            .get(node.y).fValue > cost || !openList.contains(
                            cell.get(node.x + 1).get(node.y)
                        )
                    ) cell.get(node.x + 1).get(node.y).fValue = cost
                    openList.add(cell.get(node.x + 1).get(node.y))
                    cell.get(node.x + 1).get(node.y).parent = node
                }
            } catch (e: IndexOutOfBoundsException) {
            }

            //Top Cell
            try {
                if (cell.get(node.x - 1)
                        .get(node.y).hValue !== -1.0 && !openList.contains(
                        cell.get(
                            node.x - 1
                        ).get(node.y)
                    )
                    && !closedList.contains(cell.get(node.x - 1).get(node.y))
                ) {
                    val tCost = node.fValue + v
                    cell.get(node.x - 1).get(node.y).gValue = v
                    val cost: Double =
                        cell.get(node.x - 1).get(node.y).hValue + tCost
                    if (cell.get(node.x - 1)
                            .get(node.y).fValue > cost || !openList.contains(
                            cell.get(node.x - 1).get(node.y)
                        )
                    ) cell.get(node.x - 1).get(node.y).fValue = cost
                    openList.add(cell.get(node.x - 1).get(node.y))
                    cell.get(node.x - 1).get(node.y).parent = node
                }
            } catch (e: IndexOutOfBoundsException) {
            }
        }

        /*for (int i = 0; i < n; ++i) {
            for (int j = 0; j < n; ++j) {
                System.out.print(cell[i][j].fValue + "    ");
            }
            System.out.println();
        }*/

        //Assigns the last Object in the closedList to the endNode variable
        var endNode: Node? = closedList[closedList.size - 1]

        //Checks if whether the endNode variable currently has a parent Node. if it doesn't then stops moving forward.
        //Stores each parent Node to the PathList so it is easier to trace back the final path
        while (endNode!!.parent != null) {
            val currentNode = endNode
            pathList.add(currentNode)
            endNode = endNode.parent
        }
        pathList.add(cell.get(Ai).get(Aj))
        //Clears the openList
        openList.clear()
        println()
    }
}

data class Node(var x: Int, var y: Int, var hValue:Double = 0.0) {
    var gValue = 0
    var fValue = 0.0
    var parent: Node? = null
}