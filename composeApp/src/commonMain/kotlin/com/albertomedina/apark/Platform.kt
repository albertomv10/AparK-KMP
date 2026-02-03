package com.albertomedina.apark

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform