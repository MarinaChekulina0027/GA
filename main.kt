import java.util.*
import javax.swing.JOptionPane

val PATH_LENGTH = 20
val NODE_COUNT = 20
val INNER_LENGTH = PATH_LENGTH - 2
val GENERATION_SIZE = 20
val GENERATION_COUNT = 2000
val RANDOM = Random()

fun randomNode(exclude: Int? = null): Int {
    var c: Int
    do {
        c = RANDOM.nextInt(NODE_COUNT)
    } while (exclude != null && c == exclude)
    return c
}

fun randomInner() = RANDOM.nextInt(INNER_LENGTH)

fun checkRange(node: Int) = node >= 0 && node < NODE_COUNT

class Path(val start: Int, val end: Int, val nodes: List<Int>, private val m: (Int, Int) -> Double) {
    init {
        assert(checkRange(start))
        assert(checkRange(end))
        assert(nodes.all(::checkRange))
        assert(nodes.size == INNER_LENGTH)
    }

    fun length(): Double {
        var length = (0 until nodes.size - 1).sumByDouble { m(nodes[it], nodes[it + 1]) }
        length += m(start, nodes.first())
        length += m(nodes.last(), end)
        return length
    }

    fun mutate(callback: (before: Path, after: Path) -> Unit): Path {
        val result = Path(start, end, nodes.swap(randomInner(), randomInner()), m)
        callback.invoke(this, result)
        return result
    }

    fun pack(): Path {
        val result = ArrayList<Int>()
        var previous = start
        nodes.forEach {
            if (it != previous) {
                result.add(previous)
                previous = it
            }
        }
        result.add(previous)
        if (end != previous) {
            result.add(end)
        }
        return Path(result.first(), result.last(), result.subList(1, result.size - 1), m)
    }

    override fun toString(): String {
        return "$start, ${nodes.joinToString { String.format("%2d", it) }}, $end"
    }

    companion object {
        fun cross(path1: Path, path2: Path, callback: (p1: Path, p2: Path, c: Path) -> Unit): Path {
            assert(path1.start == path2.start)
            assert(path1.end == path2.end)
            val s = randomInner()
            val resultNodes = ArrayList<Int>()
            resultNodes.addAll(path1.nodes.subList(0, s))
            resultNodes.addAll(path2.nodes.subList(s, INNER_LENGTH))
            val result = Path(path1.start, path1.end, resultNodes, path1.m)
            callback.invoke(path1, path2, result)
            return result
        }

        fun random(start: Int, end: Int, m: (Int, Int) -> Double) = Path(start, end, (0 until INNER_LENGTH).map { randomNode() }, m)
    }
}

class Network {
    private val d: Array<Array<Double>>

    init {
        d = Array(NODE_COUNT) { Array(NODE_COUNT) { RANDOM.nextDouble() } }
        (0 until NODE_COUNT).forEach {
            d[it][it] = 0.0
        }
    }

    fun m() = fun(a: Int, b: Int) = d[a][b]

    override fun toString(): String {
        val sb = StringBuilder()
        d.forEach {
            it.forEach {
                sb.append(String.format("%5.2f", it)).append(" ")
            }
            sb.append('\n')
        }
        return sb.toString()
    }
}

class MutationHolder(val before: Path, val after: Path) {
    override fun toString() = "Mutation {\n    $before\n    $after\n}"
}

class CrossHolder(val p1: Path, val p2: Path, val c: Path) {
    override fun toString() = "Cross:\n    $p1\n    $p2\n    $c"
}

class Generation(val paths: Array<Path>) {
    val mutations = ArrayList<MutationHolder>()
    val crosses = ArrayList<CrossHolder>()

    init {
        assert(paths.size == GENERATION_SIZE)
    }

    fun next(generation: Int): Generation {
        val descendants = ArrayList<Path>()

        paths.sortBy(Path::length)

        descendants.add(paths.first())
        repeat((GENERATION_SIZE / 10.0).toInt()) {
            descendants.add(paths[it].mutate { before, after ->
                mutations.add(MutationHolder(before, after))
            })
        }
        repeat(paths.size - descendants.size) {
            descendants.add(Path.cross(paths[RANDOM.nextInt(GENERATION_SIZE)], paths[RANDOM.nextInt(GENERATION_SIZE)]) { p1, p2, c ->
                crosses.add(CrossHolder(p1, p2, c))
            })
        }

        if (generation % 10 == 0) {
            print()
        }
        return Generation(descendants.toTypedArray())
    }

    fun print() {
        mutations.take(5).forEach(::println)
        crosses.take(5).forEach(::println)
    }

    fun min() = paths.minBy(Path::length)

    companion object {
        fun initial(start: Int, end: Int, m: (Int, Int) -> Double) = Generation((0 until GENERATION_SIZE).map { Path.random(start, end, m) }.toTypedArray())
    }
}


fun main(args: Array<String>) {
    val network = Network()
    println(network)

//    val start = randomNode()
//    val end = randomNode(exclude = start)

    val input1 = JOptionPane.showInputDialog("Начальная вершина: ");
    val start = Integer.parseInt(input1);
    val input2 = JOptionPane.showInputDialog("Конечная вершина: ");
    val end = Integer.parseInt(input2);


    var generation = Generation.initial(start, end, network.m())

    repeat(GENERATION_COUNT) {
        generation = generation.next(it)
        if (it % 10 == 0) {
            println("========\n$it: ${generation.min()?.length()}")
            println("best: ${generation.min()?.pack()}\n========")
        }
    }
}


fun <A> List<A>.swap(a: Int, b: Int): List<A> {
    val copy = ArrayList(this)
    val t = copy[a]
    copy[a] = copy[b]
    copy[b] = t
    return Collections.unmodifiableList(copy)
}