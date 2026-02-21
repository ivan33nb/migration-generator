package ru.vtb.dev.corp.cctv.plugin.migration.generation.plugin

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import ru.vtb.dev.corp.cctv.plugin.migration.generation.const.MIGRATION_AUTHOR_NAME_KEY
import ru.vtb.dev.corp.cctv.plugin.migration.generation.generator.generateRollback
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.JPanel


data class MigrationInput(
    var withReleaseTag: Boolean,
    val version: String,
    val name: String,
    val author: String,
    val sql: String,
    val withRollback: Boolean,
    val rollback: String
)

class GenerateMigrationDialog(project: Project, version: String) : DialogWrapper(project) {

    private val withReleaseTagCheckBox = JBCheckBox()
    private var versionField = JBTextField()
    private val nameField = JBTextField()
    private var authorField = JBTextField()
    private val sqlField = JBTextArea(8, 80)
    private val sqlFieldScroll = JBScrollPane(sqlField)
    private var withRollbackField = JBCheckBox()
    private val generateRollback = JButton("Generate rollback")
    private val rollbackField = JBTextArea(8, 80)
    private val rollBackFieldScroll = JBScrollPane(rollbackField)

    init {
        title = "GENERATE MIGRATION"
        authorField = JBTextField(
            PropertiesComponent.getInstance(project).getValue(
                MIGRATION_AUTHOR_NAME_KEY
            ) ?: ""
        )
        versionField = JBTextField(version)
        withRollbackField.isEnabled = true
        init()
    }

    override fun createCenterPanel(): JPanel {
        withRollbackField.addActionListener {
            updateRollbackFieldState()
        }
        updateRollbackFieldState()
        generateRollback.addActionListener {
            if (sqlField.text.isEmpty()) {
                showErrorUnderButton(generateRollback)
            } else {
                val rollback = generateRollback(sqlField.text.trim())
                rollbackField.text = rollback
            }
        }
        val panel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Add release tag", withReleaseTagCheckBox)
            .addLabeledComponent("With rollback:", withRollbackField)
            .addLabeledComponent("Version:", versionField)
            .addLabeledComponent("Name:", nameField)
            .addLabeledComponent("Author:", authorField)
            .addLabeledComponent("Sql migration:", sqlFieldScroll, true)
            .addComponent(generateRollback)
            .addLabeledComponent("Rollback migration:", rollBackFieldScroll, true)
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
        withRollback = withRollbackField.isSelected,
        rollback = rollbackField.text.trim()
    )

    private fun updateRollbackFieldState() {
        val enabled = withRollbackField.isSelected
        rollbackField.isEnabled = enabled
        rollbackField.isEditable = enabled
        generateRollback.isEnabled = enabled
    }

    private fun showErrorUnderButton(button: JButton) {
        val balloon = JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(
                "It is impossible to prepare a rollback because there is no SQL script",
                MessageType.ERROR,
                null)
            .setFadeoutTime(10000)
            .createBalloon()

        balloon.show(RelativePoint.getSouthWestOf(button), Balloon.Position.below)
    }
}