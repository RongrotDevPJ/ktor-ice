package com.example

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class Task(val id: Int, val content: String, val isDone: Boolean)

@Serializable
data class TaskRequest(val content: String, val isDone: Boolean)

object TaskRepository {
    private val tasks = mutableListOf(
        Task(id = 1, content = "Learn Kotlin", isDone = true),
        Task(id = 2, content = "Build a REST API", isDone = false),
        Task(id = 3, content = "Write Unit Tests", isDone = false)
    )

    fun getAll(): List<Task> = tasks

    fun getById(id: Int): Task? = tasks.find { it.id == id }

    fun add(task: Task) {
        tasks.add(task)
    }

    fun update(id: Int, updatedTask: Task): Boolean {
        val taskIndex = tasks.indexOfFirst { it.id == id }
        return if (taskIndex != -1) {
            tasks[taskIndex] = updatedTask
            true
        } else {
            false
        }
    }

    fun delete(id: Int): Boolean {
        return tasks.removeIf { it.id == id }
    }
}

fun Application.configureRouting() {
    routing {
        get("/tasks") {
            val tasks = TaskRepository.getAll()
            call.respond(tasks)
        }

        get("/tasks/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            val task = id?.let { TaskRepository.getById(it) }

            if (task != null) {
                call.respond(task)
            } else {
                call.respondText("Task with id $id not found", status = HttpStatusCode.NotFound)
            }
        }

        post("/tasks") {
            val taskRequest = call.receive<TaskRequest>()
            val newId = (TaskRepository.getAll().maxOfOrNull { it.id } ?: 0) + 1
            val task = Task(id = newId, content = taskRequest.content, isDone = taskRequest.isDone)
            TaskRepository.add(task)
            call.respond(HttpStatusCode.Created, task)
        }

        put("/tasks/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            val taskRequest = call.receive<TaskRequest>()

            when {
                id == null -> call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                else -> {
                    val updatedTask = Task(id = id, content = taskRequest.content, isDone = taskRequest.isDone)
                    if (TaskRepository.update(id, updatedTask)) {
                        call.respond(HttpStatusCode.OK, updatedTask)
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Task not found")
                    }
                }
            }
        }

        delete("/tasks/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id != null && TaskRepository.delete(id)) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound, "Task not found")
            }
        }
    }
}