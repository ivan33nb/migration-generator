package ru.vtb.dev.corp.cctv.plugin.migration.generation.generator

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.CodeStyleManager
import ru.vtb.dev.corp.cctv.plugin.migration.generation.plugin.MigrationInput
import java.lang.StringBuilder

object MigrationGenerator {

    fun generate(project: Project, targetDir: VirtualFile, input: MigrationInput): String {
        val result = StringBuilder()
        val tagFileName = "${input.version}-${input.name}.xml"
        val tagFile = if (input.withRollback) buildMigrationTagXml(input) else buildMigrationTagXmlWithoutRollback(input)
        val sqlFileName = "${input.version}-${input.name}.sql"

        WriteCommandAction.runWriteCommandAction(project) {
            if (input.withReleaseTag) {
                val releaseTagName = input.version.replace(Regex("-.*$"), "") + "-00-release-tag.xml"
                val releaseTagFile = buildReleaseTagXml(input.author, input.version)
                val createdFile = createOrReplace(targetDir, releaseTagName, releaseTagFile)
                createdFile.refresh(false, false)
                result
                    .append(generateChangelogTag(releaseTagName))
                    .append("\n")
            }
            val createdTagFile = createOrReplace(targetDir, tagFileName, tagFile)
            val createdSqlFile = createOrReplace(targetDir, sqlFileName, "${input.sql}\n")

            result
                .append(generateChangelogTag(tagFileName))
                .append("\n")

            PsiManager.getInstance(project).findFile(createdSqlFile)
                ?.let { CodeStyleManager.getInstance(project).reformat(it) }

            createdTagFile.refresh(false, false)
            createdSqlFile.refresh(false, false)

            if (input.withRollback) {
                val rollbackFileName = "${input.version}-${input.name}-rollback.sql"
                val rollbackScript = if (input.generateRollback) "${generateRollback(input.sql)}\n" else "${input.rollback}\n"
                val createdRollbackFile = createOrReplace(targetDir, rollbackFileName, rollbackScript)
                result.append(generateChangelogTag(rollbackFileName))
                PsiManager.getInstance(project).findFile(createdRollbackFile)
                    ?.let { CodeStyleManager.getInstance(project).reformat(it) }
                createdRollbackFile.refresh(false, false)
            }
        }
        print(result)
        return result.toString()
    }

    private fun createOrReplace(dir: VirtualFile, fileName: String, content: String): VirtualFile {
        val existing = dir.findChild(fileName)
        val file = existing ?: dir.createChildData(this, fileName)
        VfsUtil.saveText(file, content)
        return file
    }

    private fun buildReleaseTagXml(author: String, version: String): String {
        val modifyingVersion = version.replace(Regex("-.*$"), "")
        val result = """
        <?xml version="1.1" encoding="UTF-8" standalone="no"?>
        <databaseChangeLog
          xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
          http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.1.xsd">
          <changeSet author="$author" id="$modifyingVersion-00-release-tag">
            <tagDatabase tag="$modifyingVersion-00-release-tag"/>
          </changeSet>
        </databaseChangeLog>
        
    """.trimIndent()
        return result
    }

    private fun buildMigrationTagXml(input: MigrationInput): String = """
        <?xml version="1.1" encoding="UTF-8" standalone="no"?>
        <databaseChangeLog
          xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
          http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.1.xsd">
          <changeSet author="${input.author}" id="${input.version}-${input.name}">
            <sqlFile path="${input.version}-${input.name}.sql"
              relativeToChangelogFile="true"/>
            <rollback>
              <sqlFile path="${input.version}-${input.name}-rollback.sql"
                relativeToChangelogFile="true"/>
            </rollback>
          </changeSet>
        </databaseChangeLog>
        
    """.trimIndent()

    private fun buildMigrationTagXmlWithoutRollback(input: MigrationInput): String = """
        <?xml version="1.1" encoding="UTF-8" standalone="no"?>
        <databaseChangeLog
          xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
          http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.1.xsd">
          <changeSet author="${input.author}" id="${input.version}-${input.name}">
            <sqlFile path="${input.version}-${input.name}.sql"
              relativeToChangelogFile="true"/>
          </changeSet>
        </databaseChangeLog>
        
    """.trimIndent()

    private fun generateChangelogTag(tagName: String) = """
        <${tagName}, with bla bla>
    """.trimIndent()
}