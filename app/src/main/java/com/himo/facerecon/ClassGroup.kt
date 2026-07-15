package com.himo.facerecon

import java.util.UUID

data class ClassGroup(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val studentNames: List<String>,
    val group: String = ""
)
