package com.taleon.reader

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.taleon.reader.ui.components.EmptyShelf
import com.taleon.reader.ui.theme.TaleonTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EmptyShelfUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyShelfShowsImportActionAndInvokesCallback() {
        var clicked = false
        composeRule.setContent {
            TaleonTheme {
                EmptyShelf(onImport = { clicked = true })
            }
        }

        composeRule.onNodeWithText("你的书架还很安静").assertIsDisplayed()
        composeRule.onNodeWithText("导入第一本书").assertIsDisplayed().performClick()
        assertTrue(clicked)
    }
}
