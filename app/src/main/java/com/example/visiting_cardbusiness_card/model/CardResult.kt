package com.example.visiting_cardbusiness_card.model

data class CardResult(
    val pipelineName: String,
    val company: String = "",
    val name: String = "",
    val designation: String = "",
    val phone: List<String> = emptyList(),
    val email: List<String> = emptyList(),
    val website: List<String> = emptyList(),
    val address: String = "",
    val fax: String = "",
    val gstin: String = "",
    val extras: String = "",
    val rawOcrText: String = "",
    val rawOcrBlocks: List<String> = emptyList()
)
