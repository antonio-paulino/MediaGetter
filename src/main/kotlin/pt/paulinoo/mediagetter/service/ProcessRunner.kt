package pt.paulinoo.mediagetter.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ProcessResult(
    val exitCode: Int,
    val output: String
)

/**
 * Runs an external process, streaming its (merged stdout+stderr) output line by
 * line to [onLine] while also collecting it for the returned [ProcessResult].
 *
 * [onStart] receives the live [Process] right after launch so callers can keep a
 * handle for cancellation; destroying that process makes the blocking read
 * unwind and this function return.
 */
object ProcessRunner {

    suspend fun run(
        command: List<String>,
        onStart: (Process) -> Unit = {},
        onLine: (String) -> Unit = {}
    ): ProcessResult = withContext(Dispatchers.IO) {

        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()

        onStart(process)

        val output = StringBuilder()

        try {
            process.inputStream.bufferedReader().forEachLine { line ->
                output.appendLine(line)
                onLine(line)
            }
            ProcessResult(process.waitFor(), output.toString())
        } finally {
            if (process.isAlive) process.destroyForcibly()
        }
    }
}
