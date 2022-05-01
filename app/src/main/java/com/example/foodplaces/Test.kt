package com.example.foodplaces

class Test {

    fun main() {
        println("Hello, world!!!")

        var students = getStrings()
        var combos = students.map { a -> "${a.name} : ${a.age}" }
        println("Hello, $combos")

        var maxEgeStudent: Student? = students.maxByOrNull { it.age }

        println("Oldest: ${students.maxByOrNull { it.age }}");


    }

    data class Student(var name: String, var age: Int)

    fun getStrings(): List<Student> {
        return listOf(
            Student("Ginger", 19),
            Student("Michael", 23),
            Student("Maria", 20),
            Student("Joe", 39),
            Student("Bob", 16)
        )
    }
}