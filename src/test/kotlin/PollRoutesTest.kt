package org.example

import com.example.PollRepository
import com.example.configurePollRouting
import com.example.PollOptionRequest
import com.example.PollRequest
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.intOrNull

class PollRoutesTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun Application.testModule() {
		install(ContentNegotiation) {
			json()
		}
        configurePollRouting()
    }

    @BeforeTest
    fun setup() {
        PollRepository.clear()
    }

    @Test
    fun testCreatePoll() = testApplication {
        application { testModule() }

        val pollRequest = PollRequest("What's your favorite color?")
        val response = client.post("/polls") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(pollRequest))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("What's your favorite color?", body["question"]?.jsonPrimitive?.content)
    }

    @Test
    fun testAddOptionAndVote() = testApplication {
        application { testModule() }

        // Create poll
        val pollResponse = client.post("/polls") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(PollRequest("Best pet?")))
        }
        val pollId = json.parseToJsonElement(pollResponse.bodyAsText()).jsonObject["id"]?.jsonPrimitive?.intOrNull!!

        // Add option
        val optionResponse = client.post("/options") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(PollOptionRequest("Cat", pollId)))
        }
        val optionId = json.parseToJsonElement(optionResponse.bodyAsText()).jsonObject["id"]?.jsonPrimitive?.intOrNull!!

        // Vote
        val voteResponse = client.post("/options/$optionId/vote")
        assertEquals(HttpStatusCode.OK, voteResponse.status)
    }

    @Test
    fun testGetResults() = testApplication {
        application { testModule() }

        // Create poll
        val pollResponse = client.post("/polls") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(PollRequest("Best programming language?")))
        }
        val pollId = json.parseToJsonElement(pollResponse.bodyAsText()).jsonObject["id"]?.jsonPrimitive?.intOrNull!!

        // Add two options
        repeat(2) {
            client.post("/options") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(PollOptionRequest("Option $it", pollId)))
            }
        }

        // Get results
        val resultResponse = client.get("/polls/$pollId/results")
        assertEquals(HttpStatusCode.OK, resultResponse.status)
        val resultJson = json.parseToJsonElement(resultResponse.bodyAsText()).jsonObject
        assertEquals("Best programming language?", resultJson["question"]?.jsonPrimitive?.content)
    }

    @Test
    fun testDeletePoll() = testApplication {
        application { testModule() }

        // Create poll
        val pollResponse = client.post("/polls") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(PollRequest("Delete me?")))
        }
        val pollId = json.parseToJsonElement(pollResponse.bodyAsText()).jsonObject["id"]?.jsonPrimitive?.intOrNull!!

        // Delete poll
        val deleteResponse = client.delete("/polls/$pollId")
        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

        // Try get results again
        val result = client.get("/polls/$pollId/results")
        assertEquals(HttpStatusCode.NotFound, result.status)
    }
}
