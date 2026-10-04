
## Code Review  

### 1. All logic is located inside `main()`

The entire application logic is implemented inside one method.

The method is responsible for file reading, text processing, word counting, sorting, output, and performance measurement.

This makes the code difficult to understand, test, and maintain.

### 2. `main()` has too many responsibilities

The `main()` method performs several unrelated tasks.

The functionality should be divided into separate methods responsible for specific operations.

### 3. Hard-coded file path

The path to the input file is directly written in the code:

```java
Paths.get("src/txt/harry.txt")
```

This makes the application dependent on a specific project structure.

The path should be stored in a constant or provided as a program argument.

### 4. Unused variable

The variable:

```java
int size = content.length();
```

is never used after initialization.

It should be removed.

### 5. Hard-coded number of output results

The value:

```java
30
```

is used directly in the output loop.

This is a magic number and should be replaced with a named constant such as `TOP_WORDS_COUNT`.

### 6. Poor variable naming

Some variable names are too short or do not clearly describe their purpose:

```java
content
size
freq
```

More descriptive names improve code readability.

### 7. Using a `String` to store unique words

The program uses:

```java
String distinctString = " ";
```

as a data structure for storing unique words.

A `Set<String>` or `Map<String, Integer>` is much more appropriate for this purpose.

### 8. Inefficient string concatenation

The following operation is performed repeatedly:

```java
distinctString += words[i] + " ";
```

Since `String` objects are immutable in Java, repeated concatenation creates many temporary objects.

A collection or `StringBuilder` should be used instead.

### 9. Incorrect data structure for checking unique words

The code uses:

```java
distinctString.contains(words[i])
```

The `contains()` method searches for a substring rather than a complete word.

For example, one word can be a substring of another word.

A `Set<String>` should be used when checking whether a complete word already exists.

### 10. Inefficient frequency calculation

The original implementation uses nested loops:

```java
for (int i = 0; i < distincts.length; i++) {
    for (int j = 0; j < words.length; j++) {
        ...
    }
}
```

This causes the entire words array to be scanned for every distinct word.

The frequency can be calculated in a single pass using a `Map<String, Integer>`.

### 11. Incorrect data structure for word frequencies

The actual problem is to store a relationship between a word and the number of times it occurs.

This is naturally represented by:

```java
Map<String, Integer>
```

The original implementation instead uses separate arrays and strings.

### 12. Repeated processing of the same data

The program first identifies distinct words and then performs another full scan of the original words to calculate their frequencies.

The frequency can be calculated when each word is processed, avoiding unnecessary iterations.

### 13. Unnecessary creation of the `distincts` array

The following array exists only because the unique words were first stored in a string:

```java
String[] distincts = distinctString.split(" ");
```

With a `Map<String, Integer>`, this intermediate structure is unnecessary.

### 14. Mixing words and frequencies into a single string

The code modifies every element using:

```java
distincts[i] += " " + freq[i];
```

This produces strings such as:

```text
harry 150
```

The word and its frequency should remain separate pieces of data.

### 15. Regular expression used to extract numbers

The sorting code contains:

```java
str.replaceAll("[^0-9]", "")
```

This is necessary only because the word and its frequency were combined into one string.

With `Map.Entry<String, Integer>`, the frequency can be accessed directly.

### 16. Complex sorting expression

The following expression is difficult to read and understand:

```java
Arrays.sort(
    distincts,
    Comparator.comparing(str ->
        Integer.valueOf(str.replaceAll("[^0-9]", ""))
    )
);
```

The code becomes much simpler when sorting `Map.Entry` objects by their integer value.

### 17. Unnecessary comments containing data sizes

Comments such as:

```java
// 400 000
```

and:

```java
// 5 000
```

do not explain the code or its purpose.

If performance characteristics are important, they should be documented separately or measured using benchmarks.

### 18. Error handling is delegated to the caller

The program uses:

```java
throws IOException
```

without providing a user-friendly error message.

The exception should be handled and an understandable error message should be displayed.

### 19. `LocalDateTime` is used for measuring execution time

`LocalDateTime` represents calendar date and time.

For measuring elapsed execution time, `System.nanoTime()` is more appropriate.

### 20. Performance measurement is mixed with application logic

The timing logic is located directly inside the main algorithm.

It should be separated from the actual text-processing logic as much as possible.

### 21. Lack of automated tests

The original structure makes testing difficult because all operations are inside `main()`.

Extracting individual methods makes it possible to test file processing, word counting, and sorting separately.

## Refactoring Performed

The following changes were made:

- divided the program into smaller methods with separate responsibilities;
- introduced constants for configuration values;
- replaced the hard-coded magic number `30` with `TOP_WORDS_COUNT`;
- removed the unused `size` variable;
- improved variable and method names;
- replaced string-based storage of unique words with `Map<String, Integer>`;
- removed unnecessary arrays;
- replaced nested loops used for frequency calculation with a single-pass `HashMap` approach;
- removed unnecessary string concatenation;
- removed the need for regular expressions when extracting frequencies;
- simplified sorting using `Map.Entry`;
- improved exception handling;
- replaced `LocalDateTime` with `System.nanoTime()` for execution-time measurement;
- separated file reading, word counting, and result printing into different methods;
- improved the overall readability and maintainability of the code.

## Result

After refactoring, the program has a simpler structure and uses data structures that better represent the problem domain.

The main improvement is the replacement of the nested-loop frequency calculation with a `Map<String, Integer>`. Each word is processed once while its frequency is accumulated.

