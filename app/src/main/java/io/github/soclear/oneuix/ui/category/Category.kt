package io.github.soclear.oneuix.ui.category

import io.github.soclear.oneuix.BuildConfig
import io.github.soclear.oneuix.data.Package

enum class Category(val packageName: String) {
    Android(Package.ANDROID),
    SystemUI(Package.SYSTEMUI),
    Settings(Package.SETTINGS),
    Call(Package.DIALER),
    Camera(Package.CAMERA),
    Other(BuildConfig.APPLICATION_ID),

    /**
     * 「One UI X について」。設定項目を持たないので、
     * 対象アプリの一覧（[CategoryAppInfo]）には含めない。
     */
    About(BuildConfig.APPLICATION_ID);

    /** 設定項目を持つカテゴリかどうか。リセットや検索の対象にできる。 */
    val hasPreferences: Boolean get() = this != About

    companion object {
        val preferenceEntries: List<Category> = entries.filter { it.hasPreferences }
    }
}
