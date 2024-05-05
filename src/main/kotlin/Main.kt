import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.enum
import com.jcabi.github.Coordinates
import com.jcabi.github.RtGithub
import com.jcabi.http.wire.RetryWire
import me.tongfei.progressbar.ProgressBar

// Computes similarity between two developers based on their frequencies of contributions to files
enum class SimilarityFunction(val getSimilarity: (Map<String, Int>, Map<String, Int>) -> Double) {
    INTERSECTION({ activity1, activity2 ->
        val intersection = activity1.keys.intersect(activity2.keys)
        val intersectionSize = intersection.sumOf {
            val freq1 = activity1.getOrDefault(it, 0)
            val freq2 = activity2.getOrDefault(it, 0)
            kotlin.math.min(freq1, freq2)
        }

        intersectionSize.toDouble()
    }),
    GEOMETRIC({ activity1, activity2 ->
        val intersection = activity1.keys.intersect(activity2.keys)
        val result = intersection.sumOf {
            val freq1 = activity1.getOrDefault(it, 0).toDouble()
            val freq2 = activity2.getOrDefault(it, 0).toDouble()
            kotlin.math.sqrt(freq1 * freq2)
        }

        result
    }),
    HARMONIC({ activity1, activity2 ->
        val intersection = activity1.keys.intersect(activity2.keys)
        val result = intersection.sumOf {
            val freq1 = activity1.getOrDefault(it, 0).toDouble()
            val freq2 = activity2.getOrDefault(it, 0).toDouble()
            if (freq1 + freq2 == 0.0) 0.0 else 2.0 * freq1 * freq2 / (freq1 + freq2)
        }

        result
    }),
}

class OptionParser: CliktCommand(help = "Find most similar pairs of developers in a GitHub repository") {
    val similarityFunction by option("-f", "--function", help="Similarity function to use. By default, harmonic mean").enum<SimilarityFunction>(ignoreCase = true).default(SimilarityFunction.HARMONIC)
    val numberOfResults by option("-n", "--number", help="Number of pairs of developers to show. By default, 5").convert { it.toInt() }.default(5).validate { require(it > 0) { "Number of results must be positive" } }
    val numberOfCommits by option("-c", "--commits", help="Number of latest commits to consider or -1 to consider all commits. By default, 100").convert { it.toInt() }.default(100).validate { require(it > 0 || it == -1) { "Number of commits must be positive or -1" } }
    val token by option("-t", "--token", help="GitHub token")
    val repoURL by argument(help="URL of the repository")

    override fun run() = Unit
}

// Returns for each contributor the number of times they have changed each file
fun extractContributorsActivity(github: RtGithub, repoURL: String, numberOfCommits: Int): Map<String, Map<String, Int>> {
    val repo = github.repos().get(Coordinates.Https(repoURL))
    val commits = repo.commits().iterate(mutableMapOf())
    val nCommits = if (numberOfCommits < 0) commits.count() else numberOfCommits

    val result = mutableMapOf<String, MutableMap<String, Int>>()

    ProgressBar.wrap(commits.take(nCommits), "Downloading commits info").forEach {commit ->
        val commitJson = commit.json()
        val authorID = commitJson.getJsonObject("commit").getJsonObject("author").getString("email")
        val filenames = commitJson.getJsonArray("files").map { it.asJsonObject().getString("filename") }

        val activity = result.getOrDefault(authorID, mutableMapOf())
        filenames.forEach { filename -> activity[filename] = activity.getOrDefault(filename, 0) + 1 }
        result[authorID] = activity
    }

    return result.mapValues{ it.value.toMap() }.toMap()
}
fun main(args: Array<String>) {
    val parser = OptionParser()
    parser.main(args)

    val github = if (parser.token == null) {
        RtGithub(RtGithub().entry().through(RetryWire::class.java))
    }
    else {
        RtGithub(RtGithub(parser.token).entry().through(RetryWire::class.java))
    }

    val contributorsActivity = extractContributorsActivity(github, parser.repoURL, parser.numberOfCommits)

    val similarityMatrix = mutableMapOf<Pair<String, String>, Double>()
    contributorsActivity.keys.forEach { dev1 ->
        contributorsActivity.keys.forEach { dev2 ->
            if (dev1 < dev2) {
                similarityMatrix[Pair(dev1, dev2)] = parser.similarityFunction.getSimilarity(contributorsActivity.getOrDefault(dev1, mapOf()), contributorsActivity.getOrDefault(dev2, mapOf()))
            }
        }
    }

    val topPairs = similarityMatrix.entries.sortedByDescending { it.value }.take(parser.numberOfResults)
    val maxLenEmail1 = topPairs.maxOf { it.key.first.length }
    val maxLenEmail2 = topPairs.maxOf { it.key.second.length }
    topPairs.forEach { (pair, similarity) ->
        println(String.format("%${-maxLenEmail1}s | %${-maxLenEmail2}s | %f", pair.first, pair.second, similarity))
    }
}