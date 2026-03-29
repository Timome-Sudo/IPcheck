package com.timome.ipcheck.model

sealed class AppScreen(val route: String, val index: Int) {
    object Welcome : AppScreen("welcome", 0)
    object TermsOfUse : AppScreen("terms_of_use", 1)
    object PermissionCenter : AppScreen("permission_center", 2)
    object Main : AppScreen("main", 3)
    object About : AppScreen("about", 4)
}