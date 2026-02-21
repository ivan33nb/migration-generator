package ru.vtb.dev.corp.cctv.plugin.migration.generation

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.text.StringUtil

class GenerateMigrationAction : AnAction() {

    // Начиная с новых платформ, нужно указать поток для update()
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val vf = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val enabled = vf != null && vf.isDirectory && e.project != null
        e.presentation.isEnabledAndVisible = enabled
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val dir: VirtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        if (!dir.isDirectory) return

        val dialog = GenerateMigrationDialog(project, "${dir.parent.name}.${dir.name}")
        if (!dialog.showAndGet()) return

        val input = dialog.result()

        val author = input.author
        PropertiesComponent.getInstance(project).setValue(MIGRATION_AUTHOR_NAME_KEY, author)

        try {
            val changelogText = MigrationGenerator.generate(project, dir, input)
            Messages.showInfoMessage(
                project,
                StringUtil.escapeXmlEntities(changelogText),
                "Migration Generator"
            )
        } catch (ex: Exception) {
            Messages.showErrorDialog(
                project,
                "Failed to generate files:\n${ex.message}",
                "Migration Generator"
            )
        }
    }
}