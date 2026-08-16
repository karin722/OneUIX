package io.github.soclear.oneuix.ui.category

import io.github.soclear.oneuix.BuildConfig
import io.github.soclear.oneuix.data.Package

enum class Category(val packageName: String) {
    Android(Package.ANDROID),
    SystemUI(Package.SYSTEMUI),
    Settings(Package.SETTINGS),
    Call(Package.DIALER),
    Camera(Package.CAMERA),
    Browser(Package.BROWSER),
    Calendar(Package.CALENDAR),
    DualApp(Package.DUAL_APP),
    Gallery(Package.GALLERY),
    GalaxyStore(Package.STORE),
    HealthMonitor(Package.HEALTH_MONITOR),
    Launcher(Package.LAUNCHER),
    Messaging(Package.MESSAGING),
    Notes(Package.NOTES),
    PhotoRetouching(Package.PHOTO_RETOUCHING),
    SketchBook(Package.SKETCH_BOOK),
    SPen(Package.TRANSLATION),
    ThemeCenter(Package.THEME_CENTER),
    Video(Package.VIDEO),
    WatchManager(Package.WATCH_MANAGER),
    Weather(Package.WEATHER),

    /**
     * 「One UI X について」。設定項目を持たないので、
     * 対象アプリの一覧（[CategoryAppInfo]）には含めない。
     */
    About(BuildConfig.APPLICATION_ID);

    /** 対象アプリの一覧に並べるカテゴリかどうか。 */
    val hasPreferences: Boolean get() = this != About

    /**
     * 初期値へのリセットを出せるカテゴリかどうか。
     *
     * アプリ別のカテゴリは設定の保存先が [io.github.soclear.oneuix.data.Preference.Other] で
     * 共通なので、1 つ戻すと他のアプリの設定まで消えてしまう。そのため対象外にしている。
     */
    val resettable: Boolean
        get() = this in setOf(Android, SystemUI, Settings, Call, Camera)

    companion object {
        val preferenceEntries: List<Category> = entries.filter { it.hasPreferences }
    }
}
