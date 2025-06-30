package com.example.EZTravel

import androidx.navigation.NavHostController

object EZTravelScreens {
    const val REGISTRATION_SCREEN = "registration"
    const val LOGIN_SCREEN = "login"
    const val HOME_SCREEN = "home"
    const val EXPLORE_SCREEN = "explore"
    const val SEARCH_IMPORT_SCREEN = "searchImport"
    const val USER_SCREEN = "user"
    const val OWNED_TRAVEL_SCREEN = "exploreOwn"
    const val TRAVEL_SCREEN = "travel"
    const val APPLICATIONS_SCREEN = "applications"
    const val CREATE_SCREEN = "create"
    const val CLONE_SCREEN = "clone"
    const val BUDDIES_SCREEN = "buddies"
    const val PHOTOS_SCREEN = "photos"
    const val ADD_TRAVEL_SCREEN = "addTravel"
    const val EDIT_PROFILE_SCREEN = "editProfile"
    const val REVIEW_SCREEN = "review"
    const val NOTIFICATIONS_SCREEN = "notifications"
    const val CHAT_LIST_SCREEN = "chatList"
    const val CHAT_SCREEN = "chat"
}

object EZTravelDestinationsArgs {
    const val TRAVEL_ID = "travelId"
    const val USER_ID = "userId"
    const val N_BUDDIES = "n"
    const val CHAT_ID = "chatId"
}

object EZTravelDestinations {
    const val HOME_ROUTE = EZTravelScreens.HOME_SCREEN
    const val EXPLORE_ROUTE = EZTravelScreens.EXPLORE_SCREEN
    const val SEARCH_IMPORT_ROUTE = EZTravelScreens.SEARCH_IMPORT_SCREEN
    const val USER_ROUTE = EZTravelScreens.USER_SCREEN
    const val EDIT_PROFILE_ROUTE = "${EZTravelScreens.EDIT_PROFILE_SCREEN}/{${EZTravelDestinationsArgs.USER_ID}}"
    const val OTHER_USER_ROUTE =
        "${EZTravelScreens.USER_SCREEN}/{${EZTravelDestinationsArgs.USER_ID}}"
    const val OWNED_TRAVEL_ROUTE = EZTravelScreens.OWNED_TRAVEL_SCREEN
    const val TRAVEL_ROUTE =
        "${EZTravelScreens.TRAVEL_SCREEN}/{${EZTravelDestinationsArgs.TRAVEL_ID}}"
    const val APPLICATIONS_ROUTE =
        "${EZTravelScreens.APPLICATIONS_SCREEN}/{${EZTravelDestinationsArgs.TRAVEL_ID}}"
    const val CREATE_ROUTE = EZTravelScreens.CREATE_SCREEN
    const val CREATE_ROUTE_WITH_ID =
        "${EZTravelScreens.CREATE_SCREEN}/{${EZTravelDestinationsArgs.TRAVEL_ID}}"
    const val CLONE_ROUTE =
        "${EZTravelScreens.CLONE_SCREEN}/{${EZTravelDestinationsArgs.TRAVEL_ID}}"
    const val BUDDIES_ROUTE =
        "${EZTravelScreens.BUDDIES_SCREEN}/{${EZTravelDestinationsArgs.TRAVEL_ID}}/{${EZTravelDestinationsArgs.N_BUDDIES}}"
    const val PHOTOS_ROUTE =
        "${EZTravelScreens.PHOTOS_SCREEN}/{${EZTravelDestinationsArgs.TRAVEL_ID}}"
    const val ADD_TRAVEL_ROUTE = EZTravelScreens.ADD_TRAVEL_SCREEN
    const val REVIEW_ROUTE = "${EZTravelScreens.REVIEW_SCREEN}/{${EZTravelDestinationsArgs.TRAVEL_ID}}"
    const val NOTIFICATIONS_ROUTE = EZTravelScreens.NOTIFICATIONS_SCREEN
    const val LOGIN_ROUTE = EZTravelScreens.LOGIN_SCREEN
    const val REGISTRATION_ROUTE = EZTravelScreens.REGISTRATION_SCREEN
    const val CHAT_LIST_ROUTE = EZTravelScreens.CHAT_LIST_SCREEN
    const val CHAT_ROUTE = "${EZTravelScreens.CHAT_SCREEN}/{${EZTravelDestinationsArgs.CHAT_ID}}"
}

class EzTravelNavigationActions(private val navController: NavHostController) {
    val navigateExplore: () -> Unit = {
        navController.navigate(EZTravelScreens.EXPLORE_SCREEN) {
            popUpTo(EZTravelScreens.HOME_SCREEN) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    val ownedTravelList: () -> Unit = { navController.navigate(EZTravelScreens.EXPLORE_SCREEN) }
    val navigateSearchImport: () -> Unit =
        { navController.navigate(EZTravelScreens.SEARCH_IMPORT_SCREEN) }
    val navigateCreateTravelWithId: (String) -> Unit =
        { id -> navController.navigate("${EZTravelScreens.CREATE_SCREEN}/$id") }
    val navigateCloneTravel: (String) -> Unit =
        { id -> navController.navigate("${EZTravelScreens.CLONE_SCREEN}/$id") }
    val navigateCreateTravelNew: () -> Unit = {
        navController.navigate(EZTravelScreens.CREATE_SCREEN)
    }
    val navigateTravel: (String) -> Unit = { id ->
        navController.navigate("${EZTravelScreens.TRAVEL_SCREEN}/$id")
    }
    val navigateBack: () -> Unit = { navController.popBackStack() }
    val navigateHome: () -> Unit = {

        val currentRoute = navController.currentDestination?.route
        if (currentRoute != EZTravelScreens.HOME_SCREEN) {
            navController.navigate(EZTravelScreens.HOME_SCREEN) {
                launchSingleTop = true
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = false
                }
            }
        }



        /*
        navController.navigate(EZTravelScreens.HOME_SCREEN) {
            popUpTo(navController.graph.startDestinationId) {
                inclusive = true
            }
        }

         */
    }
    val navigateApplications: (String) -> Unit =
        { id -> navController.navigate("${EZTravelScreens.APPLICATIONS_SCREEN}/$id") }
    val navigateBuddies: (String, Int) -> Unit =
        { id, n -> navController.navigate("${EZTravelScreens.BUDDIES_SCREEN}/$id/$n") }
    val navigateOtherUser: (String) -> Unit =
        { id -> navController.navigate("${EZTravelScreens.USER_SCREEN}/$id") }
    val navigateUser: () -> Unit = {
            navController.navigate(EZTravelScreens.USER_SCREEN) {
                popUpTo(EZTravelScreens.HOME_SCREEN) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
    }
    val navigateEditUser : (String) -> Unit =
        {it ->navController.navigate("${EZTravelScreens.EDIT_PROFILE_SCREEN}/$it")}

    val navigatePhotos: (String) -> Unit =
        { id -> navController.navigate("${EZTravelScreens.PHOTOS_SCREEN}/$id") }
    val navigateAddTravel: () -> Unit =
        {navController.navigate(EZTravelScreens.ADD_TRAVEL_SCREEN)}

    val navigateReview: (String) -> Unit =
        {id -> navController.navigate("${EZTravelScreens.REVIEW_SCREEN}/$id")}

    val navigateLogin : () -> Unit ={
        navController.navigate(EZTravelScreens.LOGIN_SCREEN)
    }

    val navigateRegistration : () -> Unit = {
        navController.navigate(EZTravelScreens.REGISTRATION_SCREEN)
    }

    val navigateNotifications: () -> Unit = {
        navController.navigate(EZTravelScreens.NOTIFICATIONS_SCREEN)
    }

    val navigateChatList: () -> Unit = {
        navController.navigate(EZTravelScreens.CHAT_LIST_SCREEN) {
            popUpTo(EZTravelScreens.HOME_SCREEN) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val navigateChat: (String) -> Unit = {
        navController.navigate("${EZTravelScreens.CHAT_SCREEN}/$it")
    }


}