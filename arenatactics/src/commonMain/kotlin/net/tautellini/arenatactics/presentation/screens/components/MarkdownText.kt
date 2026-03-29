package net.tautellini.arenatactics.presentation.screens.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tautellini.arenatactics.presentation.theme.*

@Composable
fun MarkdownText(markdown: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        markdown.lines().forEach { line ->
            when {
                line.startsWith("## ") -> {
                    Text(
                        text = line.removePrefix("## "),
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("•", color = Primary, fontSize = 13.sp)
                        Text(
                            text = parseInline(line.drop(2)),
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                    }
                }
                line.trim() == "---" -> {
                    HorizontalDivider(
                        color = DividerColor,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                line.isEmpty() -> {
                    Spacer(Modifier.height(4.dp))
                }
                else -> {
                    Text(
                        text = parseInline(line),
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

internal fun parseInline(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        when {
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                } else {
                    append(text[i]); i++
                }
            }
            text.startsWith("*", i) -> {
                val end = text.indexOf("*", i + 1)
                if (end != -1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    append(text[i]); i++
                }
            }
            else -> { append(text[i]); i++ }
        }
    }
}
