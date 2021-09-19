package org.contractfirst.generator.model

/**
 * Represents the request body of an operation in the contract.
 */
data class MRequestBody(
    val description: String?,
    val required: Boolean,
    val contents: List<MContent>
)