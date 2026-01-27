/*
 * Copyright © 2021 Damyan Ivanov.
 * This file is part of MoLe.
 * MoLe is free software: you can distribute it and/or modify it
 * under the term of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your opinion), any later version.
 *
 * MoLe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License terms for details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MoLe. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ktnx.mobileledger.ui

import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import net.ktnx.mobileledger.R

private val MarkdownLinkPattern = "\\[([^\\[]+)]\\(([^)]*)\\)".toRegex()

@Composable
fun HelpDialog(@StringRes title: Int, @ArrayRes content: Int, onDismiss: () -> Unit) {
    val contentText = stringArrayResource(content).joinToString("\n\n")
    val linkStyle = SpanStyle(
        color = MaterialTheme.colorScheme.primary,
        textDecoration = TextDecoration.Underline
    )
    val uriHandler = LocalUriHandler.current
    val annotatedText = remember(contentText, linkStyle) {
        buildHelpAnnotatedString(contentText, linkStyle)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(title)) },
        text = {
            val scrollState = rememberScrollState()
            @Suppress("DEPRECATION")
            ClickableText(
                text = annotatedText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.verticalScroll(scrollState),
                onClick = { offset ->
                    annotatedText.getStringAnnotations("URL", offset, offset)
                        .firstOrNull()
                        ?.let { uriHandler.openUri(it.item) }
                }
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.close_button))
            }
        }
    )
}

private fun buildHelpAnnotatedString(text: String, linkStyle: SpanStyle): AnnotatedString = buildAnnotatedString {
    var currentIndex = 0
    MarkdownLinkPattern.findAll(text).forEach { match ->
        val start = match.range.first
        val end = match.range.last + 1
        if (start > currentIndex) {
            append(text.substring(currentIndex, start))
        }

        val linkText = match.groups[1]?.value.orEmpty()
        val linkUrl = match.groups[2]?.value.orEmpty()
        val displayText = if (linkText.isBlank()) linkUrl else linkText

        val spanStart = length
        append(displayText)
        val spanEnd = length
        addStyle(linkStyle, spanStart, spanEnd)
        if (linkUrl.isNotBlank()) {
            addStringAnnotation("URL", linkUrl, spanStart, spanEnd)
        }

        currentIndex = end
    }
    if (currentIndex < text.length) {
        append(text.substring(currentIndex))
    }
}
