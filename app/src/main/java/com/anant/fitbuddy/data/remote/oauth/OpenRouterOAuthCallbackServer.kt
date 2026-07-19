package com.anant.fitbuddy.data.remote.oauth

import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.nio.charset.StandardCharsets

/**
 * Loopback HTTP listener for OpenRouter's OAuth redirect.
 *
 * Keeps accepting until a request includes `code` or `error` — browsers may open
 * speculative connections that must not tear down the listener.
 */
class OpenRouterOAuthCallbackServer(
    private val onCode: (String) -> Unit,
    private val onFailure: (String) -> Unit = {}
) {
    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null

    /** Binds `127.0.0.1` on an ephemeral port and listens until auth completes or [stop]. */
    fun start(scope: CoroutineScope): Int {
        stop()
        val socket = ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))
        serverSocket = socket
        val port = socket.localPort
        acceptJob = scope.launch(Dispatchers.IO) {
            try {
                socket.soTimeout = ACCEPT_IDLE_TIMEOUT_MS
                while (isActive && serverSocket != null) {
                    val client = try {
                        socket.accept()
                    } catch (_: SocketTimeoutException) {
                        // Keep waiting until the overall OAuth window expires via [stop].
                        continue
                    }
                    client.use { handleClient(it) }
                }
            } catch (e: Exception) {
                if (serverSocket != null) {
                    onFailure(e.message ?: "OAuth callback listener failed")
                }
            } finally {
                stop()
            }
        }
        return port
    }

    fun stop() {
        val socket = serverSocket
        serverSocket = null
        acceptJob?.cancel()
        acceptJob = null
        runCatching { socket?.close() }
    }

    /**
     * @return true when the OAuth flow is finished (code or error) and the listener should stop.
     */
    private fun handleClient(client: Socket): Boolean {
        val reader = BufferedReader(InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8))
        val requestLine = reader.readLine() ?: run {
            writePlain(client, 400, "Bad Request")
            return false
        }
        while (true) {
            val line = reader.readLine() ?: break
            if (line.isEmpty()) break
        }

        val target = requestLine.split(' ').getOrNull(1).orEmpty()
        val uri = Uri.parse("http://127.0.0.1$target")
        val code = uri.getQueryParameter("code")?.takeIf { it.isNotBlank() }
        val error = uri.getQueryParameter("error")?.takeIf { it.isNotBlank() }

        return when {
            code != null -> {
                writeHtml(client, 200, "Connected", "You can return to FitBuddy.")
                onCode(code)
                true
            }
            error != null -> {
                writeHtml(client, 400, "Sign-in failed", error)
                onFailure(error)
                true
            }
            else -> {
                // Speculative probe / favicon / prefetch — keep listening.
                writePlain(client, 204, "")
                false
            }
        }.also { finished ->
            if (finished) {
                // Close listener after delivering the result.
                stop()
            }
        }
    }

    private fun writePlain(client: Socket, status: Int, body: String) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        val reason = when (status) {
            200 -> "OK"
            204 -> "No Content"
            else -> "Bad Request"
        }
        val header = buildString {
            append("HTTP/1.1 $status $reason\r\n")
            append("Content-Length: ${bytes.size}\r\n")
            append("Connection: close\r\n\r\n")
        }.toByteArray(StandardCharsets.US_ASCII)
        client.getOutputStream().use { out ->
            out.write(header)
            if (bytes.isNotEmpty()) out.write(bytes)
            out.flush()
        }
    }

    private fun writeHtml(client: Socket, status: Int, title: String, message: String) {
        val safeTitle = title.replace("<", "&lt;")
        val safeMessage = message.replace("<", "&lt;")
        val html = """
            <!DOCTYPE html>
            <html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
            <title>$safeTitle</title>
            <style>
              body{font-family:system-ui,sans-serif;background:#111;color:#eee;display:flex;
              align-items:center;justify-content:center;min-height:100vh;margin:0;padding:24px;text-align:center}
              h1{font-size:1.4rem;margin:0 0 8px}p{opacity:.8;margin:0}
            </style></head>
            <body><div><h1>$safeTitle</h1><p>$safeMessage</p></div>
            <script>setTimeout(function(){window.close()},800)</script>
            </body></html>
        """.trimIndent()
        val bytes = html.toByteArray(StandardCharsets.UTF_8)
        val reason = if (status == 200) "OK" else "Bad Request"
        val header = buildString {
            append("HTTP/1.1 $status $reason\r\n")
            append("Content-Type: text/html; charset=utf-8\r\n")
            append("Content-Length: ${bytes.size}\r\n")
            append("Connection: close\r\n\r\n")
        }.toByteArray(StandardCharsets.US_ASCII)
        client.getOutputStream().use { out ->
            out.write(header)
            out.write(bytes)
            out.flush()
        }
    }

    companion object {
        /** Per-accept idle timeout; overall OAuth window is enforced by the keep-alive service. */
        private const val ACCEPT_IDLE_TIMEOUT_MS = 15_000
    }
}
