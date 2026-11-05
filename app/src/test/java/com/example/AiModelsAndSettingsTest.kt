package com.example

import com.example.ai.AiModelConfig
import com.example.ai.AiModelRole
import com.example.ai.AiMultiModelConfig
import com.example.ai.DefaultAiCatalog
import com.example.ai.PricingVerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiModelsAndSettingsTest {

    @Test
    fun testDefaultConfigBlocksPaidModels() {
        val config = AiMultiModelConfig()
        assertFalse(config.allowPaidModels)

        val paidModel = AiModelConfig(
            id = "paid_1",
            providerId = "provider_1",
            modelName = "gpt-4o",
            displayName = "GPT-4o",
            isFree = false,
            pricingStatus = PricingVerificationStatus.PAID,
            isEnabled = true
        )
        assertFalse(config.canUseModel(paidModel))

        val withPermission = config.copy(allowPaidModels = true)
        assertTrue(withPermission.canUseModel(paidModel))
    }

    @Test
    fun testMultiModelConfigSerializationRoundTrip() {
        val original = AiMultiModelConfig(
            autoRoutingEnabled = true,
            allowPaidModels = false,
            primaryModelId = "model_gemini_2_5_flash",
            fallbackModelIds = listOf("model_openrouter_free_auto")
        )
        val json = original.toJsonString()
        val restored = AiMultiModelConfig.fromJsonString(json)
        assertNotNull(restored)
        assertEquals(original.autoRoutingEnabled, restored!!.autoRoutingEnabled)
        assertEquals(original.allowPaidModels, restored.allowPaidModels)
        assertEquals(original.primaryModelId, restored.primaryModelId)
        assertEquals(original.fallbackModelIds, restored.fallbackModelIds)
        assertEquals(original.providers.size, restored.providers.size)
        assertEquals(original.models.size, restored.models.size)
    }

    @Test
    fun testLocalModelDoesNotClaimToolCalling() {
        val localModels = DefaultAiCatalog.defaultModels().filter { it.isLocal }
        assertTrue(localModels.isNotEmpty())
        localModels.forEach { model ->
            assertFalse(model.supportsToolCalling)
            assertTrue(model.isFree)
        }
    }
}
