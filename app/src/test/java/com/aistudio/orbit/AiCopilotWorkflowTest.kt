package com.aistudio.orbit

import com.aistudio.orbit.forensics.ai.providers.AiExecutionResult
import com.aistudio.orbit.forensics.ai.providers.AiOutputStatus
import com.aistudio.orbit.forensics.ai.providers.AiProvider
import com.aistudio.orbit.forensics.ai.providers.AiProviderType
import com.aistudio.orbit.model.BlockchainNetwork
import com.aistudio.orbit.model.InvestigationCase
import org.junit.Assert.assertEquals
import org.junit.Test

class AiCopilotWorkflowTest {

    @Test
    fun testAiProviderInterfaceContract() {
        val testProvider = object : AiProvider {
            override val name: String = "Test Mock Provider"
            override val version: String = "1.0.0"
            override val defaultModels: List<String> = listOf("model-test")
            override val providerType: AiProviderType = AiProviderType.GEMINI
            override val costCategory: String = "FREE_TIER"

            override suspend fun testConnection(apiKey: String, endpoint: String?): Boolean {
                return apiKey.isNotBlank()
            }

            override suspend fun executePrompt(
                prompt: String,
                apiKey: String,
                model: String,
                endpoint: String?,
                temperature: Float,
                maxTokens: Int
            ): AiExecutionResult {
                return AiExecutionResult(
                    status = AiOutputStatus.SUCCESS,
                    output = "Forensic assessment: Address shows interaction with cluster [EVID-101].",
                    modelUsed = model,
                    provider = AiProviderType.GEMINI,
                    promptVersion = "1.0",
                    executionDurationMs = 120
                )
            }
        }

        assertEquals("Test Mock Provider", testProvider.name)
        assertEquals(AiProviderType.GEMINI, testProvider.providerType)
    }

    @Test
    fun testInvestigationCaseContextFormat() {
        val caseObj = InvestigationCase(
            caseId = "case_test_001",
            referenceNumber = "BYN-TEST-001",
            caseName = "Test Suspicious Flow",
            targetAddress = "bc1qtest1234567890",
            network = BlockchainNetwork.BITCOIN
        )

        assertEquals("BYN-TEST-001", caseObj.referenceNumber)
        assertEquals("bc1qtest1234567890", caseObj.targetAddress)
        assertEquals(BlockchainNetwork.BITCOIN, caseObj.network)
    }
}

