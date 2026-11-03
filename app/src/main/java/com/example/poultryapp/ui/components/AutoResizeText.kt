package com.example.poultryapp.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * کامپوننت ضد کرش و فوق‌پیشرفته انطباق خودکار اندازه فونت با کادر (Auto-Sizing Text Engine)
 * این کامپوننت فاقد SubcomposeLayout و BoxWithConstraints بوده و ۱۰۰٪ با اندازه‌گیری‌های ذاتی سازگار است.
 */
@Composable
fun AutoResizeText(
    text: String,
    modifier: Modifier = Modifier,
    targetFontSize: TextUnit = 13.sp,
    minFontSize: TextUnit = 8.5.sp,
    maxLines: Int = 1,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    fontStyle: FontStyle? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    softWrap: Boolean = maxLines > 1,
    style: TextStyle = LocalTextStyle.current
) {
    var fontSizeValue by remember(text, targetFontSize) { mutableFloatStateOf(targetFontSize.value) }
    var readyToDraw by remember(text, targetFontSize) { mutableStateOf(false) }

    Text(
        text = text,
        modifier = modifier.drawWithContent {
            if (readyToDraw) drawContent()
        },
        fontSize = fontSizeValue.sp,
        color = color,
        fontWeight = fontWeight,
        fontStyle = fontStyle,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        maxLines = maxLines,
        softWrap = softWrap,
        overflow = TextOverflow.Clip,
        style = style,
        onTextLayout = { result ->
            if ((result.hasVisualOverflow || result.didOverflowWidth || result.didOverflowHeight) && fontSizeValue > minFontSize.value) {
                val nextSize = (fontSizeValue - 0.75f).coerceAtLeast(minFontSize.value)
                if (nextSize < fontSizeValue) {
                    fontSizeValue = nextSize
                } else {
                    readyToDraw = true
                }
            } else {
                readyToDraw = true
            }
        }
    )
}

/**
 * اضافه کردن اورلود برای AnnotatedString
 */
@Composable
fun AutoResizeText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    targetFontSize: TextUnit = 13.sp,
    minFontSize: TextUnit = 8.5.sp,
    maxLines: Int = 1,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    fontStyle: FontStyle? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    softWrap: Boolean = maxLines > 1,
    style: TextStyle = LocalTextStyle.current
) {
    var fontSizeValue by remember(text, targetFontSize) { mutableFloatStateOf(targetFontSize.value) }
    var readyToDraw by remember(text, targetFontSize) { mutableStateOf(false) }

    Text(
        text = text,
        modifier = modifier.drawWithContent {
            if (readyToDraw) drawContent()
        },
        fontSize = fontSizeValue.sp,
        color = color,
        fontWeight = fontWeight,
        fontStyle = fontStyle,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        maxLines = maxLines,
        softWrap = softWrap,
        overflow = TextOverflow.Clip,
        style = style,
        onTextLayout = { result ->
            if ((result.hasVisualOverflow || result.didOverflowWidth || result.didOverflowHeight) && fontSizeValue > minFontSize.value) {
                val nextSize = (fontSizeValue - 0.75f).coerceAtLeast(minFontSize.value)
                if (nextSize < fontSizeValue) {
                    fontSizeValue = nextSize
                } else {
                    readyToDraw = true
                }
            } else {
                readyToDraw = true
            }
        }
    )
}
