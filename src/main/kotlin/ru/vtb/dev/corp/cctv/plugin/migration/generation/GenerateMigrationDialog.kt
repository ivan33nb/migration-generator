package ru.vtb.dev.corp.cctv.plugin.migration.generation

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import java.awt.Dimension
import javax.swing.JPanel

data class MigrationInput(
    var withReleaseTag: Boolean,
    val version: String,
    val name: String,
    val author: String,
    val sql: String,
    val withRollback: Boolean,
    val generateRollback: Boolean,
    val rollback: String
)

class GenerateMigrationDialog(project: Project, dirName: String) : DialogWrapper(project) {

    private val withReleaseTagCheckBox = JBCheckBox()
    private var versionField = JBTextField()
    private val nameField = JBTextField()
    private var authorField = JBTextField()
    private val sqlField = JBTextField()
    private val withRollbackCheckBox = JBCheckBox()
    private val generateRollbackCheckBox = JBCheckBox()
    private val rollbackField = JBTextField()

    init {
        title = "Generate migration"
        versionField = JBTextField("$dirName-")
        authorField = JBTextField(PropertiesComponent.getInstance(project).getValue(MIGRATION_AUTHOR_NAME_KEY)?: "")
        init()
    }

    override fun createCenterPanel(): JPanel {
        val panel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Add release tag", withReleaseTagCheckBox)
            .addLabeledComponent("With rollback:", withRollbackCheckBox)
            .addLabeledComponent("Automatically generating rollback", generateRollbackCheckBox)
            .addLabeledComponent("Version:", versionField)
            .addLabeledComponent("Name:", nameField)
            .addLabeledComponent("Author:", authorField)
            .addLabeledComponent("Sql migration:", sqlField)
            .addLabeledComponent("Rollback migration:", rollbackField)
            .panel
        return panel
    }

    override fun getInitialSize(): Dimension = JBUI.size(1000, 600)

    override fun doValidate(): ValidationInfo? {
        if (versionField.text.trim().isEmpty()) return ValidationInfo(
            "Version is required",
            versionField
        )
        if (authorField.text.trim().isEmpty()) return ValidationInfo(
            "Author is required",
            authorField
        )
        return null
    }

    fun result(): MigrationInput = MigrationInput(
        withReleaseTag = withReleaseTagCheckBox.isSelected,
        version = versionField.text.trim(),
        name = nameField.text.trim(),
        author = authorField.text.trim(),
        sql = sqlField.text.trim(),
        withRollback = withRollbackCheckBox.isSelected,
        generateRollback = generateRollbackCheckBox.isSelected,
        rollback = rollbackField.text.trim()
    )
}