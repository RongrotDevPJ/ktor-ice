package com.example

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class Poll(val id: Int, val question: String)

@Serializable
data class PollOption(val id: Int, val text: String, var voteCount: Int = 0, val pollId: Int)

@Serializable
data class PollRequest(val question: String)

@Serializable
data class PollOptionRequest(val text: String, val pollId: Int)

@Serializable
data class PollResult(val id: Int, val question: String, val options: List<PollOption>)

object PollRepository {
    private val polls = mutableListOf<Poll>()
    private val pollOptions = mutableListOf<PollOption>()

    // Add this clear method for testing purposes
    fun clear() {
        polls.clear()
        pollOptions.clear()
    }

    fun getAllPolls(): List<Poll> = polls

    fun getPoll(id: Int): Poll? = polls.find { it.id == id }

    fun addPoll(poll: Poll) {
        polls.add(poll)
    }

    fun deletePoll(id: Int): Boolean {
        pollOptions.removeIf { it.pollId == id }
        return polls.removeIf { it.id == id }
    }

    fun addOption(option: PollOption) {
        pollOptions.add(option)
    }

    fun getOptionsByPoll(pollId: Int): List<PollOption> = pollOptions.filter { it.pollId == pollId }

    fun voteOption(id: Int): Boolean {
        val option = pollOptions.find { it.id == id }
        return if (option != null) {
            option.voteCount++
            true
        } else false
    }

    fun getPollWithResults(id: Int): PollResult? {
        val poll = getPoll(id)
        val options = getOptionsByPoll(id)
        return poll?.let { PollResult(it.id, it.question, options) }
    }
}

fun Application.configurePollRouting() {
    routing {
        post("/polls") {
            val request = call.receive<PollRequest>()
            val newId = (PollRepository.getAllPolls().maxOfOrNull { it.id } ?: 0) + 1
            val poll = Poll(newId, request.question)
            PollRepository.addPoll(poll)
            call.respond(HttpStatusCode.Created, poll)
        }

        delete("/polls/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id != null && PollRepository.deletePoll(id)) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound, "Poll not found")
            }
        }

        post("/options") {
            val request = call.receive<PollOptionRequest>()
            val newId = ((PollRepository.getOptionsByPoll(request.pollId).maxOfOrNull { it.id }) ?: 0) + 1
            val option = PollOption(newId, request.text, pollId = request.pollId)
            PollRepository.addOption(option)
            call.respond(HttpStatusCode.Created, option)
        }

        post("/options/{id}/vote") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id != null && PollRepository.voteOption(id)) {
                call.respond(HttpStatusCode.OK, "Vote recorded")
            } else {
                call.respond(HttpStatusCode.NotFound, "Option not found")
            }
        }

        get("/polls/{id}/results") {
            val id = call.parameters["id"]?.toIntOrNull()
            val result = id?.let { PollRepository.getPollWithResults(it) }
            if (result != null) {
                call.respond(result)
            } else {
                call.respond(HttpStatusCode.NotFound, "Poll not found")
            }
        }

        get("/polls") {
            call.respond(PollRepository.getAllPolls())
        }
    }
}