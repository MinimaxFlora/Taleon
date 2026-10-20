package com.taleon.reader.domain.model

enum class ThemeMode(val label: String) {
    SYSTEM("跟随系统"),
    LIGHT("浅色"),
    DARK("深色"),
}

enum class ReaderTheme(val label: String) {
    PAPER("纸张"),
    LIGHT("明亮"),
    SEPIA("护眼"),
    DARK("夜间"),
}

enum class PageMode(val label: String) {
    PAGED("翻页"),
    SCROLL("滚动"),
}

enum class PageTurnAnimation(val label: String) {
    SLIDE("滑动"),
    FADE("淡入"),
    NONE("无动画"),
}

enum class ReadingFont(val label: String) {
    SERIF("衬线"),
    SANS("无衬线"),
    MONO("等宽"),
}

enum class LibraryViewMode { GRID, LIST }

enum class LibrarySort(val label: String) {
    RECENT("最近阅读"),
    ADDED("添加时间"),
    TITLE("书名"),
    AUTHOR("作者"),
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val fontSizeSp: Float = 18f,
    val readingFont: ReadingFont = ReadingFont.SERIF,
    val lineHeightMultiplier: Float = 1.65f,
    val paragraphSpacingDp: Float = 8f,
    val horizontalPaddingDp: Float = 22f,
    val readerTheme: ReaderTheme = ReaderTheme.PAPER,
    val pageMode: PageMode = PageMode.PAGED,
    val pageTurnAnimation: PageTurnAnimation = PageTurnAnimation.SLIDE,
    val volumeKeysTurnPages: Boolean = true,
    val keepScreenOn: Boolean = false,
    val libraryViewMode: LibraryViewMode = LibraryViewMode.GRID,
    val librarySort: LibrarySort = LibrarySort.RECENT,
) {
    val fontSize: Float get() = fontSizeSp.coerceIn(12f, 36f)
    val lineHeight: Float get() = lineHeightMultiplier.coerceIn(1.1f, 2.4f)
}
