import java.util.Scanner

fun main() {
    val reader = Scanner(System.`in`)
    println(" Simple Addition Program ")

    try {
        print("Enter first number: ")
        val num1 = reader.nextDouble()

        print("Enter second number: ")
        val num2 = reader.nextDouble()

        val sum = num1 + num2

        println("\nSuccess! The sum of $num1 and $num2 is: $sum")
    } catch (e: Exception) {
        println("\nError: Please enter valid numbers (use a dot for decimals).")
    }
}