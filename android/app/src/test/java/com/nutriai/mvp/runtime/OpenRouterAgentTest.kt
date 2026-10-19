package com.nutriai.mvp.runtime

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class OpenRouterAgentTest {
    private fun response(items: List<AiFood> = emptyList(), reply: String = "Проверьте порцию.", finish: String = "stop") = buildJsonObject {
        put("choices",buildJsonArray { add(buildJsonObject {
            put("finish_reason",finish)
            put("message",buildJsonObject { put("content",Json.encodeToString(AiAnswer(reply,items))) })
        }) })
    }.toString()
    private fun food(catalog: String="Борщ", grams: Double=300.0) = AiFood(catalog,"Блюдо",grams,999.0,99.0,99.0,99.0)
    private fun agent(body: String) = OpenRouterAgent("test",ChatTransport { _, _ -> body })
    private fun ask(body: String) = agent(body).respond("Обед",emptyList(),Profile(),emptyList())
    @Test fun modelCannotOverrideCatalogValuesAndAndroidComputesTotals() {
        val result=ask(response(listOf(food(),food("Хлеб",40.0))))
        assertEquals(247.0,Nutrition.totals(result.items).kcal,.0001)
        assertTrue(result.items.all { it.source.startsWith("Локальный справочник") })
        assertTrue(result.reply.contains("247 ккал"))
    }
    @Test fun novelFoodEstimateIsScaledOnAndroidAndClearlyLabelled() {
        val item=ask(response(listOf(AiFood("","Шаурма",250.0,200.0,10.0,8.0,22.0)))).items.single()
        assertEquals(500.0,item.kcal,.001)
        assertEquals(25.0,item.protein,.001)
        assertTrue(item.source.startsWith("Оценка ИИ"))
        assertEquals(400.0,item.scaled(200.0).kcal,.001)
    }
    @Test fun emptyReplyWithValidFoodStillProducesCalculatedConfirmation() {
        assertTrue(ask(response(listOf(food()),reply="")).reply.contains("147 ккал"))
    }
    @Test fun adviceDoesNotCreateAnEntry() { assertTrue(ask(response(reply="Остаток можно посмотреть в аналитике.")).items.isEmpty()) }
    @Test fun malformedOrPartialOutputIsRejected() {
        listOf("not json",response(finish="length"),response(listOf(food(grams=-1.0))),response(listOf(food("Неизвестный ID"))),
            response(listOf(AiFood("","Ошибка",100.0,200.0,90.0,90.0,90.0))),response(List(13) { food() })).forEach { body ->
            assertThrows(AgentUnavailable::class.java) { ask(body) }
        }
    }
    @Test fun providerErrorsAreReadableAndNotLeaked() {
        listOf(401,402,403,429,503).forEach { code ->
            val e=assertThrows(AgentUnavailable::class.java) { ask("""{"error":{"code":$code,"message":"SECRET_PROVIDER_DETAILS"}}""") }
            assertFalse(e.message!!.contains("SECRET_PROVIDER_DETAILS"))
            assertEquals(errorForStatus(code),e.message)
        }
    }
    @Test fun requestUsesExactModelSchemaAndMinimalContextWithoutPhotosOrProfilePii() {
        val history=(1..10).map { Message(it%2==0,"Message $it","/private/picture.jpg") }
        var request=""
        val transport=ChatTransport { key,body -> assertEquals("test",key);request=body;response() }
        OpenRouterAgent("test",transport).respond("Борщ",history,Profile(name="Private Full Name",age=32),emptyList())
        val root=Json.parseToJsonElement(request).jsonObject
        assertEquals(OpenRouterAgent.MODEL,root["model"]!!.jsonPrimitive.content)
        assertEquals("json_schema",root["response_format"]!!.jsonObject["type"]!!.jsonPrimitive.content)
        assertEquals(8,root["messages"]!!.jsonArray.size)
        assertFalse(request.contains("Private Full Name"))
        assertFalse(request.contains("/private/picture.jpg"))
        assertFalse(request.contains("Message 4"))
        assertTrue(request.contains("Message 10"))
    }
    @Test fun invalidInputNeverMakesNetworkRequest() {
        val agent=OpenRouterAgent("test",ChatTransport { _,_ -> fail("No network expected");"" })
        assertThrows(IllegalArgumentException::class.java) { agent.respond("x".repeat(4001),emptyList(),Profile(),emptyList()) }
    }
}
