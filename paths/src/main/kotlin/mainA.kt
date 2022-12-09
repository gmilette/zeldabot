import sequence.findpaths.FindPaths

fun main(args: Array<String>) {
    println("Hello World!")

    // Try adding program arguments via Run/Debug configuration.
    // Learn more about running applications: https://www.jetbrains.com/help/idea/running-applications.html.
    println("Program arguments: ${args.joinToString()}")

//    sequence.findpaths.FindPaths().start()
    sequence.findpaths.FindPaths().buildMapLocFiles()
//    FindPaths().createMapCells()

}
