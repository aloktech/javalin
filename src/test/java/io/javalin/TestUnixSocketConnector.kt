package io.javalin

import io.javalin.util.TestUtil
import jnr.unixsocket.UnixSocketAddress
import jnr.unixsocket.UnixSocketChannel
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.unixsocket.UnixSocketConnector
import org.junit.Test
import java.io.*

class TestUnixSocketConnector {
    private val socketFileName = "/tmp/javalin.sock"
    private val testPath = "/unixsocket"
    private val expectedResultString = "hello unixsocket"
    private val unixSocketJavalin = Javalin.create().server {
        val server = Server()
        val unixSocketConnector = UnixSocketConnector(server)
        unixSocketConnector.unixSocket = socketFileName
        server.addConnector(unixSocketConnector)

        val serverConnector = ServerConnector(server)
        serverConnector.port = 0
        server.addConnector(serverConnector)

        server
    }

    @Test
    fun `using unixsocket`() = TestUtil.test(unixSocketJavalin) { app, http ->
        app.get(testPath) { ctx -> ctx.status(200).result(expectedResultString) }

        val socketAddress = UnixSocketAddress(File(socketFileName))
        val socket = UnixSocketChannel.open(socketAddress).socket()
        val w = BufferedWriter(OutputStreamWriter(socket.outputStream, "UTF8"))
        val r = BufferedReader(InputStreamReader(socket.inputStream))
        val response = arrayListOf<String>()

        w.write("GET ${testPath} HTTP/1.0\r\nHost:localhost\r\n\r\n")
        w.flush()

        while (true) {
            val line = r.readLine() ?: break
            response.add(line)
        }

        w.close()
        r.close()

        val arr = response.first().split(" ")
        assertThat("${arr.get(1)} ${arr.last()}").isEqualTo("200 OK")
        assertThat(response.last()).isEqualTo(expectedResultString)
    }
}
