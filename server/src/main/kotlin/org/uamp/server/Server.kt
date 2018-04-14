package org.uamp.server

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.File
import java.net.InetSocketAddress
import java.net.URI


fun main(args: Array<String>) {
    val port = args.getOrNull(0)?.toIntOrNull() ?: 8080
    val basePath = File(args.getOrElse(1, { System.getProperty("user.dir")!! }))

    HttpServer.create(InetSocketAddress(port), 0).apply {
        createContext("/") { httpExchange ->
            println("New client request...")
            val localFile = fetchFileOrNull(httpExchange, basePath)

            when (localFile) {
                null -> httpExchange.sendResponseHeaders(404, 0)
                else -> {
                    httpExchange.sendResponseHeaders(200, localFile.length())
                    localFile.inputStream().copyTo(httpExchange.responseBody)
                }
            }

            httpExchange.close()
        }

        start()
    }
}

fun fetchFileOrNull(exchange: HttpExchange, basePath: File): File? =
    when (validRequest(exchange)) {
        true -> {
            localFileFromRequestUri(basePath, exchange.requestURI).let { requestedFile ->
                if (requestedFile.isFile) requestedFile else null
            }
        }
        false -> null
    }

fun localFileFromRequestUri(basePath: File, uri: URI): File {
    val basePathElements = basePath.absolutePath.split(File.separator).filter { it.isNotEmpty() }.toMutableSet()
    val clientPathElements = uri.path.substring(1).split(File.separatorChar)
    val filteredPathElements = clientPathElements.mapNotNull {
        if (!basePathElements.remove(it)) {
            it
        } else {
            null
        }
    }.joinToString(separator = File.separator) { it }

    return File(basePath, filteredPathElements)
}

fun validRequest(exchange: HttpExchange): Boolean =
        exchange.requestHeaders.containsKey("User-Agent")
                && exchange.requestMethod == "GET"