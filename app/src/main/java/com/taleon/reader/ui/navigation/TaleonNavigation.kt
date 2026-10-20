package com.taleon.reader.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.taleon.reader.AppContainer
import com.taleon.reader.domain.model.AppSettings
import com.taleon.reader.ui.about.AboutScreen
import com.taleon.reader.ui.details.BookDetailsScreen
import com.taleon.reader.ui.library.LibraryScreen
import com.taleon.reader.ui.reader.ReaderScreen
import com.taleon.reader.ui.settings.SettingsScreen
import com.taleon.reader.ui.theme.TaleonTheme

private object Routes {
    const val LIBRARY = "library"
    const val DETAILS = "book/{bookId}"
    const val READER = "reader/{bookId}?chapter={chapter}&offset={offset}"
    const val SETTINGS = "settings"
    const val ABOUT = "about"

    fun details(bookId: Long) = "book/$bookId"
    fun reader(bookId: Long) = "reader/$bookId"
    fun readerAt(bookId: Long, chapterIndex: Int, charOffset: Int) =
        "reader/$bookId?chapter=$chapterIndex&offset=$charOffset"
}

@Composable
fun TaleonRoot(container: AppContainer) {
    val settings by container.settingsRepository.settings.collectAsStateWithLifecycle(
        initialValue = AppSettings(),
    )
    TaleonTheme(themeMode = settings.themeMode) {
        val navController = rememberNavController()
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
        ) {
            NavHost(
                navController = navController,
                startDestination = Routes.LIBRARY,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { fadeIn(tween(180)) },
                exitTransition = { fadeOut(tween(140)) },
                popEnterTransition = { fadeIn(tween(180)) },
                popExitTransition = { fadeOut(tween(120)) },
            ) {
            composable(Routes.LIBRARY) {
                LibraryScreen(
                    container = container,
                    onOpenBook = { bookId -> navController.navigate(Routes.details(bookId)) },
                    onContinueReading = { bookId -> navController.navigate(Routes.reader(bookId)) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                )
            }
            composable(
                route = Routes.DETAILS,
                arguments = listOf(navArgument("bookId") { type = NavType.LongType }),
            ) { entry ->
                val bookId = entry.arguments?.getLong("bookId") ?: return@composable
                BookDetailsScreen(
                    bookId = bookId,
                    container = container,
                    onBack = { navController.popBackStack() },
                    onRead = { id, chapter, offset ->
                        navController.navigate(Routes.readerAt(id, chapter, offset)) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(
                route = Routes.READER,
                arguments = listOf(
                    navArgument("bookId") { type = NavType.LongType },
                    navArgument("chapter") {
                        type = NavType.IntType
                        defaultValue = -1
                    },
                    navArgument("offset") {
                        type = NavType.IntType
                        defaultValue = -1
                    },
                ),
            ) { entry ->
                val bookId = entry.arguments?.getLong("bookId") ?: return@composable
                val chapter = entry.arguments?.getInt("chapter") ?: -1
                val offset = entry.arguments?.getInt("offset") ?: -1
                ReaderScreen(
                    bookId = bookId,
                    initialChapter = chapter.takeIf { it >= 0 },
                    initialOffset = offset.takeIf { it >= 0 },
                    container = container,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    container = container,
                    onBack = { navController.popBackStack() },
                    onOpenAbout = { navController.navigate(Routes.ABOUT) },
                )
            }
            composable(Routes.ABOUT) {
                AboutScreen(onBack = { navController.popBackStack() })
            }
            }
        }
    }
}
