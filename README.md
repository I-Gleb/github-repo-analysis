# Contributors analysis

This tool can be used to analyze the contributions from different people to a GitHub repository.
Given the repository URL, the tool will go through all the commits and count the number of times each contributor has changed each file.
After that pairs of developers that most frequently change the same files are shown.

## How to use

### Building the project

This project can be built and run using Gradle. The following command can be used:

```./gradlew run --args='{program arguments}'```

Alternatively, you can run the project using IntelliJ IDEA. To do that, open the project in IntelliJ IDEA and run the `Main.kt` file with the program arguments.

### Usage

The tool has a CLI interface. The following arguments are supported:

```
Usage: option-parser [<options>] <repourl>

  Find most similar pairs of developers in a GitHub repository

Options:
  -f, --function=(INTERSECTION|GEOMETRIC|HARMONIC)
                         Similarity function to use. By default, harmonic mean
  -n, --number=<value>   Number of pairs of developers to show. By default, 5
  -c, --commits=<value>  Number of latest commits to consider or -1 to consider
                         all commits. By default, 100
  -t, --token=<text>     GitHub token
  -h, --help             Show this message and exit

Arguments:
  <repourl>  URL of the repository
```

### GitHub token

To access a private repository or to avoid GitHub API rate limits(60 requests per hour for unauthenticated requests), you can provide a GitHub token.
A personal access token can be generated in [GitHub settings](https://github.com/settings/tokens).
The personal rate limit is 5,000 requests per hour.

### Example

The following command will analyze the last 50 commits of the repository, show 4 most similar pairs of developers and use the default similarity function:

```./gradlew run --args='-n 4 -c 50 https://github.com/JetBrains/gradle-grammar-kit-plugin'```

The output will be:

```
49699333+dependabot[bot]@users.noreply.github.com | yann.cebron@jetbrains.com       | 21.000000
jakub.chrzanowski@jetbrains.com                   | yann.cebron@jetbrains.com       | 8.388889
49699333+dependabot[bot]@users.noreply.github.com | jakub.chrzanowski@jetbrains.com | 7.388889
action@github.com                                 | yann.cebron@jetbrains.com       | 1.000000
```


## Similarity functions

For every contributor, we know how many times they changed each file.
However, we need to define a similarity function to compare two contributors.
Ideally, it should be high if two contributors change common files often and also if they change the same files in similar proportions.

If we consider only the number of changes in common files, the following two cases will have the same similarity:
- The first contributor changed file A 100 times and file B 1 time. The second contributor changed file A 1 time and file B 100 times.
- Both contributors changed file A 100 times and file B 1 time.

To avoid this, we need to take into account the relative frequencies of the changes.
However, if we consider the relative frequencies only, the following two cases will have the same similarity:
- Both contributors changed file A 1 time and file B 1 time.
- Both contributors changed file A 100 times and file B 100 times.

The tool supports several similarity functions that try to balance between these two extremes.
The appropriate function depends on the specific use case.

### Intersection size
The simplest similarity function is the size of the intersection of the multisets of files changed by contributors.
It is calculated as the sum of the minimum number of changes of each file by the two contributors.
This function takes into account both the absolute and relative frequencies of the changes.
However, the values of this function are bounded by the number of changes of the less active contributor.

### Geometric mean
For each file, we calculate the geometric mean of the number of changes by the two contributors.
The similarity is the sum of these geometric means.
This function unlike the intersection size is not bounded by the number of changes of the less active contributor.

### Harmonic mean
Finally, we can use the sum of harmonic means of the number of changes for each file.
This function is something in between the geometric mean and the intersection size.
It is set by default.
