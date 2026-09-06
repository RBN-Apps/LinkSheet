package fe.buildsrc.clearurls

import fe.buildsrc.util.asString
import fe.buildsrc.util.get
import fe.buildsrc.util.httpClient
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

abstract class UpdateRulesTask : DefaultTask() {
    @get:Input
    abstract val file: Property<String>

    @Input
    val rawUrl: String =
        "https://raw.githubusercontent.com/RBN-Apps/CleanURLs-Rules/refs/heads/master/data.min.json"

    @TaskAction
    fun fetch() {
        val jsonFile = project.file(file.get())

        val response = httpClient.send(get(rawUrl), asString)

        check(response.statusCode() in 200..299) {
            "Unable to download ClearURLs rules: HTTP ${response.statusCode()} from $rawUrl"
        }

        jsonFile.writeText(response.body())
    }
}
