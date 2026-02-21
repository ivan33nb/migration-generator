package ru.vtb.dev.corp.cctv.plugin.migration.generation.generator


fun generateRollback(query: String): String {
    val statements = splitSqlStatements(query)

    val rollbacks = statements.mapNotNull { stmt ->
        val trimmed = stmt.trim()
        if (trimmed.isEmpty()) return@mapNotNull null
        rollbackForStatement(trimmed)
    }

    return rollbacks.joinToString("; ")
}

private fun rollbackForStatement(statement: String): String {
    val s = statement.trim()

    val createTableRegex = Regex(
        pattern = """(?is)^\s*create\s+table\s+(?:if\s+not\s+exists\s+)?([^\s(]+)\s*\(.*$"""
    )
    createTableRegex.matchEntire(s)?.let { m ->
        val tableName = m.groupValues[1]
        return "drop table if exists $tableName"
    }

    val alterTableRegex = Regex(
        pattern = """(?is)^\s*alter\s+table\s+([^\s]+)\s+(.+)$"""
    )
    alterTableRegex.matchEntire(s)?.let { m ->
        val tableName = m.groupValues[1]
        val actionsPart = m.groupValues[2].trim()

        val rollbackActions = splitTopLevelByComma(actionsPart).map { action ->
            invertAlterAction(action.trim())
        }

        return "alter table $tableName ${rollbackActions.joinToString(", ")}"
    }

    return "-- TODO rollback for: $s"
}

private fun invertAlterAction(action: String): String {
    val addColumnRegex = Regex(
        pattern = """(?is)^\s*add\s+column\s+(?:if\s+not\s+exists\s+)?([^\s,]+)\b.*$"""
    )
    addColumnRegex.matchEntire(action)?.let { m ->
        val columnName = m.groupValues[1]
        return "drop column if exists $columnName"
    }

    val dropColumnRegex = Regex(
        pattern = """(?is)^\s*drop\s+column\s+(?:if\s+exists\s+)?([^\s,]+)\b.*$"""
    )
    dropColumnRegex.matchEntire(action)?.let { m ->
        val columnName = m.groupValues[1]
        return "add column if not exists $columnName"
    }

    return "-- TODO invert alter action: $action"
}

private fun splitSqlStatements(sql: String): List<String> {
    val result = mutableListOf<String>()
    val current = StringBuilder()

    var inSingleQuote = false
    var inDoubleQuote = false
    var parenDepth = 0
    var i = 0

    while (i < sql.length) {
        val ch = sql[i]

        when (ch) {
            '\'' -> {
                if (!inDoubleQuote) inSingleQuote = !inSingleQuote
                current.append(ch)
            }

            '"' -> {
                if (!inSingleQuote) inDoubleQuote = !inDoubleQuote
                current.append(ch)
            }

            '(' -> {
                if (!inSingleQuote && !inDoubleQuote) parenDepth++
                current.append(ch)
            }

            ')' -> {
                if (!inSingleQuote && !inDoubleQuote && parenDepth > 0) parenDepth--
                current.append(ch)
            }

            ';' -> {
                if (!inSingleQuote && !inDoubleQuote && parenDepth == 0) {
                    val stmt = current.toString().trim()
                    if (stmt.isNotEmpty()) result += stmt
                    current.clear()
                } else {
                    current.append(ch)
                }
            }

            else -> current.append(ch)
        }

        i++
    }

    val tail = current.toString().trim()
    if (tail.isNotEmpty()) result += tail

    return result
}

private fun splitTopLevelByComma(input: String): List<String> {
    val parts = mutableListOf<String>()
    val current = StringBuilder()

    var inSingleQuote = false
    var inDoubleQuote = false
    var parenDepth = 0

    for (ch in input) {
        when (ch) {
            '\'' -> {
                if (!inDoubleQuote) inSingleQuote = !inSingleQuote
                current.append(ch)
            }

            '"' -> {
                if (!inSingleQuote) inDoubleQuote = !inDoubleQuote
                current.append(ch)
            }

            '(' -> {
                if (!inSingleQuote && !inDoubleQuote) parenDepth++
                current.append(ch)
            }

            ')' -> {
                if (!inSingleQuote && !inDoubleQuote && parenDepth > 0) parenDepth--
                current.append(ch)
            }

            ',' -> {
                if (!inSingleQuote && !inDoubleQuote && parenDepth == 0) {
                    parts += current.toString().trim()
                    current.clear()
                } else {
                    current.append(ch)
                }
            }

            else -> current.append(ch)
        }
    }

    val tail = current.toString().trim()
    if (tail.isNotEmpty()) parts += tail

    return parts
}